package org.jboss.resteasy.experiment;


public class Person
{
   private int id;
   private String name;
   private String email;
   
   public Person(int id, String name, String email)
   {
      this.id = id;
      this.name = name;
      this.email = email;
   }
   
   public Person()
   {
   }
   
   public int getId()
   {
      return id;
   }
   public void setId(int id)
   {
      this.id = id;
   }
   public String getName()
   {
      return name;
   }
   public void setName(String name)
   {
      this.name = name;
   }
   public String getEmail()
   {
      return email;
   }
   public void setEmail(String email)
   {
      this.email = email;
   }
   public String toString()
   {
      StringBuilder sb = new StringBuilder();
      sb.append("id:    " + id + "\r");
      sb.append("name:  " + name + "\r");
      sb.append("email: " + email + "\r");
      return sb.toString();
   }
   public boolean equals(Object other)
   {
      if (!(other instanceof Person))
      {
         return false;
      }
      Person otherPerson = (Person) other;
      return id == otherPerson.id && name.equals(otherPerson.getName()) && email.equals(otherPerson.getEmail());
   }
}
