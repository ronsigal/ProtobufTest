package org.jboss.resteasy.experiment;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.protobuf.ProtobufProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @tpSubChapter
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.x.y
 * @tpTestCaseDetails Regression test for RESTEASY-zzz
 */
public class ProtobufWFTest {

   private static int count = 1000;
   
   private static Client client;
   private static final MediaType PROTOBUF_MEDIA_TYPE = new MediaType("application", "protobuf");
   private static Person ron = new Person(1, "ron", "ron@jboss.org");
   private static Person tanicka = new Person(3, "tanicka", "a@b");
   private static Person_proto.Person ron_proto = Person_proto.Person.newBuilder().setId(1).setName("ron").setEmail("ron@jboss.org").build();
   private static VeryBigPerson veryBigRon = PersonUtil.getVeryBigPerson("ron");
   private static VeryBigPerson_proto.VeryBigPerson veryBigRon_proto = PersonUtil.getVeryBigPerson_proto("ron");

   private String generateURL(String path) {
//      return PortProviderUtil.generateURL(path, "ProtobufTest-0.0.1-SNAPSHOT");
      return PortProviderUtil.generateURL(path, "https://eap-app-protobuf-test.6923.rh-us-east-1.openshiftapps.com/");
   }

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
      client.register(ProtobufProvider.class);
      client.register(JsonBindingProviderExp.class);
   }

   @AfterClass
   public static void cleanup() {
      client.close();
   }
   
   @Before
   public void before() {
      JsonBindingProviderExp.setSize(0);
      ProtobufProvider.setSize(0);
      ProtobufProvider.getAssignFromMap().clear();
      ProtobufProvider.getAssignToMap().clear();
      ProtobufProvider.getMap().clear();
   }

   //@Test
   public void testPersonProto() {
//      doTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doTest("protobuf");
      }
      System.out.println("Person (protobuf) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (protobuf) bytes: " + ProtobufProvider.getSize());
      Assert.fail("ok"); // Get details
   }

   //@Test
   public void testPersonJSON() {
//      doTest("json");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doTest("json");
      }
      System.out.println("Person (JSON) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (JSON) bytes: " + JsonBindingProviderExp.getSize());
      Assert.fail("ok");
   }

   public void doTest(String transport)
   {
      Builder request = client.target(generateURL("/" + transport)).request();
      Response response = request.post(Entity.entity(ron, "application/" + transport));
      //    System.out.println("status: " + response.getStatus());
      Person person = response.readEntity(Person.class);
      Assert.assertEquals(tanicka, person);
   }

   //@Test
   public void testPerson_proto()
   {         
      Builder request = client.target(generateURL("/protobuf/proto")).request();
//      Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
//      Person_proto.Person person = response.readEntity(Person_proto.Person.class);
//      Assert.assertTrue(tanicka_proto.getEmail().equals(person.getEmail()));
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++)
      {
         Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
         //    System.out.println("status: " + response.getStatus());
         Person_proto.Person person = response.readEntity(Person_proto.Person.class);
         Assert.assertTrue("tanicka".equals(person.getName()));
      }
      System.out.println("Person_proto time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person_proto bytes: " + ProtobufProvider.getSize());
      Assert.fail("ok");
   }

   @Test
   public void testVeryBigProto() throws Exception {
//      doVeryBigTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigTest("protobuf");
      }
      System.out.println("VeryBigPerson (proto) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (proto) bytes: " + ProtobufProvider.getSize());
//      Assert.fail("ok");
   }

   @Test
   public void testVeryBigJSON() throws Exception {
//      doVeryBigTest("json");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigTest("json");
      }
      System.out.println("VeryBigPerson (JSON) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (JSON) bytes: " + JsonBindingProviderExp.getSize());
//      Assert.fail("ok");
   }

   private void doVeryBigTest(String transport) throws Exception
   {
//      System.out.println("url: " + generateURL("/big/" + transport));
      Builder request = client.target(generateURL("/big/" + transport)).request();
      Response response = request.post(Entity.entity(veryBigRon, "application/" + transport));
      VeryBigPerson person = response.readEntity(VeryBigPerson.class);
      Assert.assertEquals("tanicka", person.getS0());
   }

   @Test
   public void testVeryBigPerson_proto()
   {
      Builder request = client.target(generateURL("/big/protobuf/proto")).request();
//      Response response = request.post(Entity.entity(veryBigRon_proto, "application/protobuf"));
//      VeryBigPerson_proto.VeryBigPerson person = response.readEntity(VeryBigPerson_proto.VeryBigPerson.class);
//      Assert.assertTrue("tanicka".equals(person.getS0()));
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++)
      {
         Response response = request.post(Entity.entity(veryBigRon_proto, "application/protobuf"));
         //    System.out.println("status: " + response.getStatus());
         VeryBigPerson_proto.VeryBigPerson person = response.readEntity(VeryBigPerson_proto.VeryBigPerson .class);
         Assert.assertTrue("tanicka".equals(person.getS0()));
      }
      System.out.println("VeryBigPerson_proto time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson_proto bytes: " + ProtobufProvider.getSize());
//      Assert.fail("ok");
   }
}
