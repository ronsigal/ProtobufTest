package org.jboss.resteasy.experiment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PersonUtil
{

   private static String abc = "abcdefghijklmnopqrstuvwxyz";

   public static VeryBigPerson getVeryBigPerson(String first)
   {  
      try
      {
         VeryBigPerson vbp = new VeryBigPerson();
         for (int i = 0; i < VeryBigPerson.class.getDeclaredFields().length; i++)
         {
            Field f = VeryBigPerson.class.getDeclaredFields()[i];
            f.setAccessible(true);
            Method m = VeryBigPerson.class.getMethod("setS" + f.getName().substring(1), String.class);
            if (i == 0) {
               m.invoke(vbp, first);
            } else {
               m.invoke(vbp, abc.substring(i % abc.length()) + abc.substring(0, i % abc.length()));  
            }
         }
         return vbp;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }
   
   public static VeryBigPerson_proto.VeryBigPerson getVeryBigPerson_proto(String first)
   {  
      try
      {
         VeryBigPerson_proto.VeryBigPerson.Builder builder = VeryBigPerson_proto.VeryBigPerson.newBuilder();
         for (int i = 0; i < VeryBigPerson.class.getDeclaredFields().length; i++)
         {
            Field f = VeryBigPerson.class.getDeclaredFields()[i];
            f.setAccessible(true);
            Method m = builder.getClass().getMethod("setS" + f.getName().substring(1), String.class);
            if (i == 0) {
               m.invoke(builder, first);  
            } else {
               m.invoke(builder, abc.substring(i % abc.length()) + abc.substring(0, i % abc.length()));
            }
         }
         return builder.build();
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }
}
