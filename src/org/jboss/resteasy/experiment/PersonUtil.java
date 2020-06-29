package org.jboss.resteasy.experiment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

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

   public static VeryBigPersonNumeric getVeryBigPersonNumeric()
   {
      Random rand = new Random(); 
      try
      {
         VeryBigPersonNumeric vbp = new VeryBigPersonNumeric();
         for (int i = 0; i < VeryBigPersonNumeric.class.getDeclaredFields().length; i++)
         {
            Field f = VeryBigPersonNumeric.class.getDeclaredFields()[i];
            f.setAccessible(true);
            Method m = VeryBigPersonNumeric.class.getMethod("setS" + f.getName().substring(1), int.class);
            m.invoke(vbp, rand.nextInt());
         }
         return vbp;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static VeryBigPerson_proto.VeryBigPerson getVeryBigPerson_proto()
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
               m.invoke(builder, f);  
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
   
   public static VeryBigPersonNumeric_proto.VeryBigPersonNumeric getVeryBigPersonNumeric_proto()
   {
      Random rand = new Random(); 
      try
      {
         VeryBigPersonNumeric_proto.VeryBigPersonNumeric.Builder builder = VeryBigPersonNumeric_proto.VeryBigPersonNumeric.newBuilder();
         for (int i = 0; i < VeryBigPersonNumeric.class.getDeclaredFields().length; i++)
         {
            Field f = VeryBigPersonNumeric.class.getDeclaredFields()[i];
            f.setAccessible(true);
            Method m = builder.getClass().getMethod("setS" + f.getName().substring(1), int.class);
            m.invoke(builder, rand.nextInt());
         }
         return builder.build();
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }
}
