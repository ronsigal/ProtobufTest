package org.jboss.resteasy.plugins.protobuf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public class ProtobufCompiler
{
//   private static final String PROTOC = "/home/rsigal/bin/protoc ";
//   private static final String PROTOBUF_JAVA_JAR = "/home/rsigal/tmp/git.master.proto/Resteasy/testsuite/integration-tests/target/test-server/wildfly-19.0.0.Final/modules/system/layers/base/org/jboss/resteasy/resteasy-jaxrs/main/protobuf-java-3.12.0.jar";
   private static final String RESOURCES_DIR = System.getProperty("user.dir") + "/resources";
   private static final String PROTOC = RESOURCES_DIR + "/protoc ";
   private static final String PROTOBUF_JAVA_JAR = RESOURCES_DIR + "/protobuf-java-3.12.0.jar";

   
   public static Message compile(String directory, Object source) throws Exception
   {
      if (source instanceof Message)
      {
         return (Message) source;
      }
      Message message = getDefaultMessage(directory, source.getClass());
      return copyToProtobufClass(source, message.getDescriptorForType());
   }

   public static Object decompile(String directory, Class<?> clazz, InputStream inputStream) throws Exception
   {
      if (DynamicMessage.class.isAssignableFrom(clazz))
      {
         Method getDescriptorForType = clazz.getMethod("getDescriptorForType");
         getDescriptorForType.setAccessible(true);
         Method parseFrom = clazz.getMethod("parseFrom", Descriptor.class, InputStream.class);
         parseFrom.setAccessible(true);
         return parseFrom.invoke(null, getDescriptorForType.invoke(clazz.newInstance()));
      }
      if (Message.class.isAssignableFrom(clazz))
      {
//         Method getParserForType = clazz.getMethod("getParserForType");
//         getParserForType.setAccessible(true);
//         Method getDefaultInstance = clazz.getMethod("getDefaultInstance");
//         getDefaultInstance.setAccessible(true);
//         Parser<?> parser = (Parser<?>) getParserForType.invoke(getDefaultInstance.invoke(null));
         Method parseFrom = clazz.getMethod("parseFrom", InputStream.class);
         parseFrom.setAccessible(true);
         return parseFrom.invoke(null, inputStream);
      }
      Message message = getDefaultMessage(directory, clazz);
      final Message parsedMessage = message.getParserForType().parseFrom(inputStream);
      Descriptor descriptor = message.getDescriptorForType();
      final Object object = clazz.newInstance();
      List<ProtobufProvider.AssignFrom> assigns = ProtobufProvider.getAssignFromMap().get(clazz);
      if (assigns != null)
      {
         for (ProtobufProvider.AssignFrom assign : assigns)
         {
            assign.assign(parsedMessage, object);
         }
      }
      else
      {
         assigns = new ArrayList<ProtobufProvider.AssignFrom>();
         for (FieldDescriptor fd : descriptor.getFields())
         {
            final Field field = clazz.getDeclaredField(fd.getName());
            field.setAccessible(true);
            final Method m = getGetMethod(parsedMessage.getClass(), fd.getName());
            ProtobufProvider.AssignFrom assign = (msg, obj) ->
            {
               try
               {
                  field.set(obj, m.invoke(msg));
               }
               catch (Exception e)
               {
                  //
               }
            };

            assign.assign(parsedMessage, object);
            assigns.add(assign);
         }
         ProtobufProvider.getAssignFromMap().put(clazz, assigns);
      }
      return object;
   }

   private static Message getDefaultMessage(String directory, Class<?> clazz) throws Exception
   {
      Message message = null;
      Method getDefaultInstance = ProtobufProvider.getMap().get(clazz);
      if (getDefaultInstance != null)
      {
         message = (Message) getDefaultInstance.invoke(null);
      }
      else
      {
         String protoDescriptor = translateToProtobuf(clazz);
         String protoFilename = writeProtoDescriptor(directory, protoDescriptor, clazz);
         compileProtoDescriptor(directory, protoFilename);
         message = compileGeneratedJava(directory, clazz); 
      }
      return message;
   }

   private static String translateToProtobuf(Class<?> clazz)
   {
      AtomicInteger count = new AtomicInteger(1);
      StringBuilder sb = new StringBuilder();
      sb.append("syntax = \"proto3\";\n");
      String packageName = clazz.getPackage().getName();
      sb.append("package " + packageName + ";\n");
      sb.append("option java_package = \"" + packageName + "\";\n");
      sb.append("option java_outer_classname = \"" + clazz.getSimpleName() + "_proto\";\n");
      sb.append("message " + clazz.getSimpleName() + " {\n");

      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields)
      {
         sb.append(" ");
         switch (field.getType().getName())
         {
            case "int":
               sb.append("int32 ");
               break;

            case "java.lang.String":
               sb.append("string ");
               break;

            default:
               sb.append("Object ");
               break;
         }
         sb.append(field.getName() + " = " + count.getAndIncrement() + ";\n");
      }
      sb.append("}\n");
      return sb.toString();
   }

   private static String writeProtoDescriptor(String directory, String proto, Class<?> clazz) throws IOException
   {
      String filename = directory + "/" + clazz.getSimpleName() + "_proto";
      BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
      writer.write(proto);
      writer.close();
      return filename;
   }

   private static void compileProtoDescriptor(String directory, String protoFilename) throws IOException, InterruptedException
   {
//      System.out.println("jboss.home.dir: " + System.getProperty("jboss.home.dir "));
//      File f = new File("/opt/eap");
//      System.out.println("/opt/eap:");
//      if (f != null)
//      {
//         String[] pathnames = f.list();
//
//         // For each pathname in the pathnames array
//         for (String pathname : pathnames) {
//             // Print the names of files and directories
//             System.out.println(pathname);
//         }
//      }
//      f = new File("/opt/eap/standalone/deployments/ProtobufTest-0.0.1-SNAPSHOT.war");
//      System.out.println("/opt/eap/standalone/deployments/ProtobufTest-0.0.1-SNAPSHOT.war:");
//      if (f != null)
//      {
//         String[] pathnames = f.list();
//
//         // For each pathname in the pathnames array
//         for (String pathname : pathnames) {
//             // Print the names of files and directories
//             System.out.println(pathname);
//         }
//      }
//      System.out.println("user.dir: " + System.getProperty("user.dir"));
//      System.out.println("JBOSS_HOME: " + System.getenv("$JBOSS_HOME"));
//      System.out.println("jboss.home.dir: " + System.getProperty("jboss.home.dir"));
//      System.out.println("jboss.server.config.url: " + System.getProperty("jboss.server.config.url"));
//      System.out.println("jboss.server.config.url: " + System.getenv("jboss.server.config.url"));
//      Map<String, String> map = System.getenv();
//      for (Entry<String, String> entry : map.entrySet()) {
//         System.out.println(entry.getKey() + "->" + entry.getValue());
//      }
//      String[] pathnames = f.list();
//
//      // For each pathname in the pathnames array
//      for (String pathname : pathnames) {
//          // Print the names of files and directories
//          System.out.println(pathname);
//      }
      
      String command = PROTOC + "-I=" + directory + " --java_out=" + directory + " " + protoFilename;
      Runtime run  = Runtime.getRuntime(); 
      Process proc = run.exec(command);
      synchronized (proc)
      {
         proc.wait(2000);
      }
      return;
   }

   /*
    * Got all of this from stackoverflow
    */
   private static Message compileGeneratedJava(String directory, Class<?> clazz)
   { 
      URLClassLoader classLoader = null;
      StandardJavaFileManager fileManager = null;
      System.out.println("Working Directory = " + System.getProperty("user.dir"));

      File file = new File(directory + classnameToFilename("/" + clazz.getName() + "_proto") + ".java");
      Message message = null;

      try
      {
         /** Compilation Requirements *********************************************************************************************/
         DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
         JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
         fileManager = compiler.getStandardFileManager(diagnostics, null, null);

         List<String> optionList = new ArrayList<String>();
         optionList.add("-classpath");
         optionList.add(System.getProperty("java.class.path") + File.pathSeparator + directory + File.pathSeparator + PROTOBUF_JAVA_JAR);

         Iterable<? extends JavaFileObject> compilationUnit = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(file));
         JavaCompiler.CompilationTask task = compiler.getTask(
               null, 
               fileManager, 
               diagnostics, 
               optionList, 
               null, 
               compilationUnit);
         /********************************************************************************************* Compilation Requirements **/
         if (task.call())
         {
            // Create a new custom class loader, pointing to the directory that contains the compiled
            // classes, this should point to the top of the package structure!
            URL url = new File(directory).toURI().toURL();
            classLoader = new URLClassLoader(new URL[] {url}, Thread.currentThread().getContextClassLoader());

            // Load the class from the classloader by name....
            Class<?> subclass = classLoader.loadClass(classToProtoClassname(clazz));
            Method getDefaultInstance = subclass.getMethod("getDefaultInstance");
            ProtobufProvider.getMap().put(clazz, getDefaultInstance);
            message = (Message) getDefaultInstance.invoke(null);
            message.getDescriptorForType();
            /************************************************************************************************* Load and execute **/
         }
         else
         {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
            {
               System.out.format("Error on line %d in %s%n: %s",
                     diagnostic.getLineNumber(),
                     diagnostic.getSource().toUri(),
                     diagnostic.getMessage(null));
            }
         }
         return message;
      } catch (Exception exp)
      {
         exp.printStackTrace();
         return null;
      }
      finally
      {
         try
         {
            if (fileManager != null)
            {
               fileManager.close();  
            }
            if (classLoader != null)
            {
               classLoader.close();  
            }
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }
   }

   private static Message copyToProtobufClass(Object from, Descriptor descriptor) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
   {
      List<ProtobufProvider.AssignTo> assigns = ProtobufProvider.getAssignToMap().get(from.getClass());
      if (assigns != null)
      {
         DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
         for (ProtobufProvider.AssignTo assign : assigns)
         {
            assign.assign(from, builder);
         }
         return builder.build();
      }
      else
      {
         List<FieldDescriptor> list = descriptor.getFields();
         final DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
         List<ProtobufProvider.AssignTo> assignList = new ArrayList<ProtobufProvider.AssignTo>();
         for (final FieldDescriptor fd : list)
         {
            final Method m = getGetMethod(from.getClass(), fd.getName());
            ProtobufProvider.AssignTo assign = (obj, messageBuilder) ->
            {
               try
               {
                  messageBuilder.setField(fd, m.invoke(obj));
               }
               catch (Exception e)
               {
                  //
               }
            };
            assign.assign(from, builder);
            assignList.add(assign);
         }
         ProtobufProvider.getAssignToMap().put(from.getClass(), assignList);
         DynamicMessage dm = builder.build();
         return dm;
      }
   }

   private static String classnameToFilename(String classname)
   {
      return classname.replace(".", "/");
   }

   private static String classToProtoClassname(Class<?> clazz)
   {
      String classname = clazz.getName();
      String simpleName = clazz.getSimpleName();
      int n = classname.lastIndexOf(".");
      String prefix = n > -1 ? classname.substring(0, n) : "";
      return prefix + "." + simpleName + "_proto" + "$" + simpleName;
   }

   private static Method getGetMethod(Class<?> clazz, String fieldName) throws NoSuchMethodException
   {
      StringBuilder sb = new StringBuilder("get");
      sb.append(Character.toUpperCase(fieldName.charAt(0)));
      if (fieldName.length() > 1)
      {
         sb.append(fieldName.substring(1));
      }
      return clazz.getMethod(sb.toString());
   }
}
