package org.jboss.resteasy.plugins.protobuf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

@Provider
@Produces("application/protobuf")
@Consumes("application/protobuf")
public class ProtobufProvider<T> implements MessageBodyReader<T>, MessageBodyWriter<T>
{  
   public static long size;

   public static long getSize()
   {
      return size;
   }

   public static void setSize(int size)
   {
      ProtobufProvider.size = size;
   }

   public interface AssignTo
   {
      public void assign(Object from, DynamicMessage.Builder builder);
   }

   public interface AssignFrom
   {
      public void assign(Message message, Object object);
   }

   private static Map<Class<?>, Method> map = new ConcurrentHashMap<Class<?>, Method>();
   private static Map<Class<?>, List<AssignTo>> assignToMap = new ConcurrentHashMap<Class<?>, List<AssignTo>>();
   private static Map<Class<?>, List<AssignFrom>> assignFromMap = new ConcurrentHashMap<Class<?>, List<AssignFrom>>();
   private String directory = "/tmp";

   @Override
   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return true;
   }

   @Override
   public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
         MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
               throws IOException, WebApplicationException
   {
      Message message = null;
      try
      {
         if (t instanceof Message)
         {
            message = (Message) t;
         }
         else
         {
            message = new ProtobufCompiler().compile(directory, t);
         }
//         ByteArrayOutputStream baos = new ByteArrayOutputStream();
//         message.writeTo(baos);
//         size += baos.toByteArray().length;
         message.writeTo(entityStream);
      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   @Override
   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return true;
   }

   @SuppressWarnings("unchecked")
   @Override
   public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
         MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
               throws IOException, WebApplicationException
   {
      try
      {
         Object obj = ProtobufCompiler.decompile(directory, type, entityStream);
         return (T) obj;
      }
      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
         throw new WebApplicationException(e);
      }
   }

   public static Map<Class<?>, Method> getMap()
   {
      return map;
   }

   public static Map<Class<?>, List<AssignTo>> getAssignToMap()
   {
      return assignToMap;
   }

   public static Map<Class<?>, List<AssignFrom>> getAssignFromMap()
   {
      return assignFromMap;
   }
}
