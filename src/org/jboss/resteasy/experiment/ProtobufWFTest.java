package org.jboss.resteasy.experiment;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ReactorNettyClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.vertx.VertxClientHttpEngine;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.encoding.GZIPEncodingInterceptor;
import org.jboss.resteasy.plugins.protobuf.ProtobufProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.vertx.core.Vertx;
import reactor.netty.http.HttpResources;
import reactor.netty.http.client.HttpClient;

/**
 * @tpSubChapter
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.x.y
 * @tpTestCaseDetails Regression test for RESTEASY-zzz
 */
public class ProtobufWFTest {

   private static int count = 1000;

   private static Client client;
   private static Client asyncClient;
   private static Client zipClient;
   private static Client vertxClient;
   private static Client nettyClient;
   private static ScheduledExecutorService executorService;
   private static Vertx vertx;
   private static final MediaType PROTOBUF_MEDIA_TYPE = new MediaType("application", "protobuf");
   private static final Variant PROTOBUF_ZIP_VARIANT = new Variant(PROTOBUF_MEDIA_TYPE, (String) null, "gzip");
   private static final Variant JSON_ZIP_VARIANT = new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, "gzip");
   private static Person ron = new Person(1, "ron", "ron@jboss.org");
   private static Person tanicka = new Person(3, "tanicka", "a@b");
   private static Person_proto.Person ron_proto = Person_proto.Person.newBuilder().setId(1).setName("ron").setEmail("ron@jboss.org").build();
   private static VeryBigPerson veryBigRon = PersonUtil.getVeryBigPerson("ron");
   private static VeryBigPerson_proto.VeryBigPerson veryBigRon_proto = PersonUtil.getVeryBigPerson_proto();
   private static VeryBigPersonNumeric veryBigRonNumeric = PersonUtil.getVeryBigPersonNumeric();

   private String generateURL(String path) {
      //            return PortProviderUtil.generateURL(path, "ProtobufTest-0.0.1-SNAPSHOT");
      //      return PortProviderUtil.generateURL(path, "eap-app-protobuf-test.6923.rh-us-east-1.openshiftapps.com/ProtobufTest-0.0.1-SNAPSHOT");
      return "https://eap-app-protobuf-test.6923.rh-us-east-1.openshiftapps.com:443/ProtobufTest-0.0.1-SNAPSHOT" + path;
   }

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
      client.register(ProtobufProvider.class);
      client.register(JsonBindingProviderExp.class);

      asyncClient = ((ResteasyClientBuilder)ClientBuilder.newBuilder()).useAsyncHttpEngine().build();
      asyncClient.register(ProtobufProvider.class);
      asyncClient.register(JsonBindingProviderExp.class);
      //      vertxClient.register(GZIPEncodingInterceptor.class);
      //      vertxClient.register(GZIPDecodingInterceptor.class);

      zipClient = ClientBuilder.newClient();
      zipClient.register(ProtobufProvider.class);
      zipClient.register(JsonBindingProviderExp.class);
      zipClient.register(GZIPEncodingInterceptor.class);
      zipClient.register(GZIPDecodingInterceptor.class);

      vertx = Vertx.vertx();
      executorService = Executors.newSingleThreadScheduledExecutor();
      vertxClient = ((ResteasyClientBuilder)ClientBuilder
            .newBuilder()
            .scheduledExecutorService(executorService))
            .httpEngine(new VertxClientHttpEngine(vertx)).build();
      vertxClient.register(ProtobufProvider.class);
      vertxClient.register(JsonBindingProviderExp.class);
      vertxClient.register(GZIPEncodingInterceptor.class);
      vertxClient.register(GZIPDecodingInterceptor.class);

      final ReactorNettyClientHttpEngine engine =
            new ReactorNettyClientHttpEngine(
                  HttpClient.create(),
                  new DefaultChannelGroup(new DefaultEventExecutor()),
                  HttpResources.get());

      final ClientBuilder builder = ClientBuilder.newBuilder();
      final ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder)builder;
      clientBuilder.httpEngine(engine);
      clientBuilder.register(ProtobufProvider.class);
      clientBuilder.register(JsonBindingProviderExp.class);
      clientBuilder.register(GZIPEncodingInterceptor.class);
      clientBuilder.register(GZIPDecodingInterceptor.class);
      nettyClient = builder.build();
//      nettyClient.register(ProtobufProvider.class);
//      nettyClient.register(JsonBindingProviderExp.class);
//      nettyClient.register(GZIPEncodingInterceptor.class);
//      nettyClient.register(GZIPDecodingInterceptor.class);
   }

   @AfterClass
   public static void cleanup() throws Exception {
      if (client != null) {
         client.close();
      }
      CountDownLatch latch = new CountDownLatch(1);
      vertx.close(ar -> latch.countDown());
      latch.await(2, TimeUnit.MINUTES);
      executorService.shutdownNow();
   }

   @Before
   public void before() {
//      System.out.println("JsonBindingProviderExp [before]: " + JsonBindingProviderExp.getSize());
      JsonBindingProviderExp.setSize(0);
//      System.out.println("JsonBindingProviderExp [after]: " + JsonBindingProviderExp.getSize());
      GZIPEncodingInterceptor.count = 0;
      ProtobufProvider.setSize(0);
      ProtobufProvider.getAssignFromMap().clear();
      ProtobufProvider.getAssignToMap().clear();
      ProtobufProvider.getMap().clear();
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //   ////@Test
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

   //   //@Test
   public void testPersonJSON() {
      //      doTest("json");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doTest("json");
      }
      System.out.println("Person (JSON) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (JSON) bytes: " + JsonBindingProviderExp.getSize());
      //      Assert.fail("ok");
   }

   //   //@Test
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
      //      Assert.fail("ok");
   }

   public void doTest(String transport)
   {
      Builder request = client.target(generateURL("/" + transport)).request();
      Response response = request.post(Entity.entity(ron, "application/" + transport));
      //    System.out.println("status: " + response.getStatus());
      Person person = response.readEntity(Person.class);
      Assert.assertEquals(tanicka, person);
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //   //@Test
   public void testPersonProtoAsync() throws Exception {
      //      doTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doAsyncTest("protobuf");
      }
      System.out.println("Person (protobuf/async) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (protobuf/async) bytes: " + ProtobufProvider.getSize());
      Assert.fail("ok"); // Get details
   }

   //   //@Test
   public void testPersonJSONAsync() throws Exception {
      //      doTest("json");
      //      System.out.println("url: " + generateURL("/json"));
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doAsyncTest("json");
      }
      System.out.println("Person (JSON/async) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (JSON/async) bytes: " + JsonBindingProviderExp.getSize());
      //      Assert.fail("ok");
   }

   //   //@Test
   public void testPerson_protoAsync() throws Exception {
      Builder request = asyncClient.target(generateURL("/protobuf/proto")).request();
      //      Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
      //      Person_proto.Person person = response.readEntity(Person_proto.Person.class);
      //      Assert.assertTrue(tanicka_proto.getEmail().equals(person.getEmail()));
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++)
      {
         Future<Response> future = request.async().post(Entity.entity(ron_proto, "application/protobuf"));
         //    System.out.println("status: " + response.getStatus());
         Response response = future.get();
         Person_proto.Person person = response.readEntity(Person_proto.Person.class);
         Assert.assertTrue("tanicka".equals(person.getName()));
      }
      System.out.println("Person_proto/async time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person_proto/async bytes: " + ProtobufProvider.getSize());
   }

   public void doAsyncTest(String transport) throws Exception
   {
      Future<Response> future = asyncClient
            .target(generateURL("/" + transport)).request()
            .async()
            .post(Entity.entity(ron, "application/" + transport));
      Response response = future.get();
      Person person = response.readEntity(Person.class);
      Assert.assertEquals(tanicka, person);
   }


   ////////////////////////////////////////////////////////////////////////////////////////////////////
//   //@Test
   public void testPersonProtoVertx() throws Exception {
      //      doTest("protobuf");
      CompletionStageRxInvoker invoker = vertxClient.target(generateURL("/protobuf")).request().rx(CompletionStageRxInvoker.class);
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVertxTest(invoker, "application/protobuf");
      }
      System.out.println("Person (protobuf) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (protobuf) bytes: " + ProtobufProvider.getSize());
      Assert.fail("ok"); // Get details
   }

   //   //@Test
   public void testPersonJSONVertx() throws Exception {
      //      doTest("json");
      System.out.println("url: " + generateURL("/json"));
      CompletionStageRxInvoker invoker = vertxClient.target(generateURL("/json")).request().rx(CompletionStageRxInvoker.class);
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVertxTest(invoker, "application/json");
      }
      System.out.println("Person (JSON) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (JSON) bytes: " + JsonBindingProviderExp.getSize());
      //      Assert.fail("ok");
   }

   //   //@Test
   public void testPerson_protoVertx() throws Exception {
      CompletionStageRxInvoker invoker = vertxClient.target(generateURL("/protobuf/proto")).request().rx(CompletionStageRxInvoker.class);
      //      CompletionStage<Response> cs = invoker.post(Entity.entity(ron_proto, "application/protobuf"));
      //      Response response = cs.toCompletableFuture().get();

      //      Builder request = vertxClient.target(generateURL("/protobuf/proto")).request();
      //      Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
      //      Person_proto.Person person = response.readEntity(Person_proto.Person.class);
      //      Assert.assertTrue(tanicka_proto.getEmail().equals(person.getEmail()));
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++)
      {
         CompletionStage<Response> cs = invoker.post(Entity.entity(ron_proto, "application/protobuf"));
         Response response = cs.toCompletableFuture().get();

         //         Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
         //    System.out.println("status: " + response.getStatus());
         Person_proto.Person person = response.readEntity(Person_proto.Person.class);
         Assert.assertTrue("tanicka".equals(person.getName()));
      }
      System.out.println("Person_proto time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person_proto bytes: " + ProtobufProvider.getSize());
      //      Assert.fail("ok");
   }

   public void doVertxTest(CompletionStageRxInvoker invoker, String mediaType) throws Exception
   {
      CompletionStage<Response> cs = invoker.post(Entity.entity(ron, mediaType));
      Response response = cs.toCompletableFuture().get();
      //    System.out.println("status: " + response.getStatus());
      Person person = response.readEntity(Person.class);
      Assert.assertEquals(tanicka, person);
   }


   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //    ////@Test
   public void testPersonProtoZip() {
      //    doTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doZipTest("protobuf");
      }
      System.out.println("Person (protobuf/zip) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (protobuf/zip) bytes: " + ProtobufProvider.getSize());
      Assert.fail("ok"); // Get details
   }

   //   //@Test
   public void testPersonJSONZip() {
      //    doTest("json");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doZipTest("json");
      }
      System.out.println("Person (JSON/zip) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (JSON/zip) bytes: " + JsonBindingProviderExp.getSize());
      System.out.println("Person (JSON/zip) compressed bytes: " + GZIPEncodingInterceptor.count);

      //    Assert.fail("ok");
   }

//   //@Test
   public void testPerson_protoZip()
   {         
      Builder request = zipClient.target(generateURL("/protobuf/proto/zip")).request();
      request.header(HttpHeaders.CONTENT_ENCODING, "gzip");
      //      Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
      //      Person_proto.Person person = response.readEntity(Person_proto.Person.class);
      //      Assert.assertTrue(tanicka_proto.getEmail().equals(person.getEmail()));
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++)
      {
         Response response = request.post(Entity.entity(ron_proto, PROTOBUF_ZIP_VARIANT));
         //    System.out.println("status: " + response.getStatus());
         Person_proto.Person person = response.readEntity(Person_proto.Person.class);
         Assert.assertTrue("tanicka".equals(person.getName()));
      }
      System.out.println("Person_proto/zip time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person_proto/zip bytes: " + ProtobufProvider.getSize());
      System.out.println("Person_proto/zip compressed bytes: " + GZIPEncodingInterceptor.count);
      //      Assert.fail("ok");
   }

   protected void doZipTest(String transport)
   {
      Builder request = zipClient.target(generateURL("/" + transport + "/zip")).request();
      //      request.header(HttpHeaders.CONTENT_ENCODING, "zip");
      Variant variant = "json".equals(transport) ? JSON_ZIP_VARIANT : PROTOBUF_ZIP_VARIANT;
      Response response = request.post(Entity.entity(ron, variant));
      //    System.out.println("status: " + response.getStatus());
      Person person = response.readEntity(Person.class);
      Assert.assertEquals(tanicka, person);
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //   ////@Test
   public void testPersonProtoNetty() {
      //      doTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doNettyTest("protobuf");
      }
      System.out.println("Person (protobuf) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (protobuf) bytes: " + ProtobufProvider.getSize());
      Assert.fail("ok"); // Get details
   }

//   //@Test
   public void testPersonJSONNetty() {
      //      doTest("json");
//      System.out.println("Person (JSON/netty) bytes [before]: " + JsonBindingProviderExp.getSize());
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doNettyTest("json");
      }
      System.out.println("Person (JSON/netty) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (JSON/netty) bytes: " + JsonBindingProviderExp.getSize());
      //      Assert.fail("ok");
   }

//   //@Test
   public void testPerson_protoNetty()
   {         
      Builder request = nettyClient.target(generateURL("/protobuf/proto")).request();
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
      System.out.println("Person_proto (netty) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person_proto (netty) bytes: " + ProtobufProvider.getSize());
      //      Assert.fail("ok");
   }

   public void doNettyTest(String transport)
   {
      Builder request = nettyClient.target(generateURL("/" + transport)).request();
      Response response = request.post(Entity.entity(ron, "application/" + transport));
      //    System.out.println("status: " + response.getStatus());
      Person person = response.readEntity(Person.class);
      Assert.assertEquals(tanicka, person);
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //   //@Test
   public void testVeryBigPersonProtoAsync() throws Exception {
      //      doVeryBigTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonTestAsync("protobuf");
      }
      System.out.println("VeryBigPerson (proto/async) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (proto/async) bytes: " + ProtobufProvider.getSize());
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPersonJSONAsync() throws Exception {
      //      doVeryBigTest("json");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonTestAsync("json");
      }
      System.out.println("VeryBigPerson (JSON/async) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (JSON/async) bytes: " + JsonBindingProviderExp.getSize());
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPerson_protoAsync()
   {
      Builder request = asyncClient.target(generateURL("/big/protobuf/proto")).request();
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
      System.out.println("VeryBigPerson_proto/async time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson_proto/async bytes: " + ProtobufProvider.getSize());

      //      Assert.fail("ok");
   }

   private void doVeryBigPersonTestAsync(String transport) throws Exception
   {
      //      System.out.println("url: " + generateURL("/big/" + transport));
      Builder request = asyncClient.target(generateURL("/big/" + transport)).request();
      Response response = request.post(Entity.entity(veryBigRon, "application/" + transport));
      VeryBigPerson person = response.readEntity(VeryBigPerson.class);
      Assert.assertEquals("tanicka", person.getS0());
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //   ////@Test
   public void testVeryBigPersonProto() throws Exception {
      //      doVeryBigTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonTest("protobuf");
      }
      System.out.println("VeryBigPerson (proto) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (proto) bytes: " + ProtobufProvider.getSize());
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPersonJSON() throws Exception {
      //      doVeryBigTest("json");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonTest("json");
      }
      System.out.println("VeryBigPerson (JSON) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (JSON) bytes: " + JsonBindingProviderExp.getSize());
      //      Assert.fail("ok");
   }

   //@Test
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

   //   public void testPerson_protoZip()
   //   {         
   //      Builder request = zipClient.target(generateURL("/protobuf/proto/zip")).request();
   //      request.header(HttpHeaders.CONTENT_ENCODING, "gzip");
   //      //      Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
   //      //      Person_proto.Person person = response.readEntity(Person_proto.Person.class);
   //      //      Assert.assertTrue(tanicka_proto.getEmail().equals(person.getEmail()));
   //      long start = System.currentTimeMillis();
   //      for (int i = 0; i < count; i++)
   //      {
   //         Response response = request.post(Entity.entity(ron_proto, PROTOBUF_ZIP_VARIANT));
   //         //    System.out.println("status: " + response.getStatus());
   //         Person_proto.Person person = response.readEntity(Person_proto.Person.class);
   //         Assert.assertTrue("tanicka".equals(person.getName()));
   //      }
   //      System.out.println("Person_proto/zip time:  " + (System.currentTimeMillis() - start));
   //      System.out.println("Person_proto/zip bytes: " + ProtobufProvider.getSize());
   //      //      Assert.fail("ok");
   //   }

   private void doVeryBigPersonTest(String transport) throws Exception
   {
      //      System.out.println("url: " + generateURL("/big/" + transport));
      Builder request = client.target(generateURL("/big/" + transport)).request();
      Response response = request.post(Entity.entity(veryBigRon, "application/" + transport));
      VeryBigPerson person = response.readEntity(VeryBigPerson.class);
      Assert.assertEquals("tanicka", person.getS0());
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //   ////@Test
   public void testVeryBigPersonProtoZip() throws Exception {
      //      doVeryBigTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonZipTest("protobuf");
      }
      System.out.println("VeryBigPerson (proto) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (proto) bytes: " + ProtobufProvider.getSize());
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPersonJSONZip() throws Exception {
      //      doVeryBigTest("json");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonZipTest("json");
      }
      System.out.println("VeryBigPerson (JSON/zip) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (JSON/zip) bytes: " + JsonBindingProviderExp.getSize());
      System.out.println("VeryBigPerson (JSON/zip) compressed bytes: " + GZIPEncodingInterceptor.count);
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPerson_protoZip()
   {
      Builder request = zipClient.target(generateURL("/big/protobuf/proto/zip")).request();
      //      Response response = request.post(Entity.entity(veryBigRon_proto, "application/protobuf"));
      //      VeryBigPerson_proto.VeryBigPerson person = response.readEntity(VeryBigPerson_proto.VeryBigPerson.class);
      //      Assert.assertTrue("tanicka".equals(person.getS0()));
      request.header(HttpHeaders.CONTENT_ENCODING, "gzip");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++)
      {
         Response response = request.post(Entity.entity(veryBigRon_proto, PROTOBUF_ZIP_VARIANT));
         //    System.out.println("status: " + response.getStatus());
         VeryBigPerson_proto.VeryBigPerson person = response.readEntity(VeryBigPerson_proto.VeryBigPerson .class);
         Assert.assertTrue("tanicka".equals(person.getS0()));
      }
      System.out.println("VeryBigPerson_proto (zip) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson_proto (zip) bytes: " + ProtobufProvider.getSize());
      System.out.println("VeryBigPerson_proto (zip) compressed bytes: " + GZIPEncodingInterceptor.count);

      //      Assert.fail("ok");
   }

   //   public void testPerson_protoZip()
   //   {         
   //      Builder request = zipClient.target(generateURL("/protobuf/proto/zip")).request();
   //      request.header(HttpHeaders.CONTENT_ENCODING, "gzip");
   //      //      Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
   //      //      Person_proto.Person person = response.readEntity(Person_proto.Person.class);
   //      //      Assert.assertTrue(tanicka_proto.getEmail().equals(person.getEmail()));
   //      long start = System.currentTimeMillis();
   //      for (int i = 0; i < count; i++)
   //      {
   //         Response response = request.post(Entity.entity(ron_proto, PROTOBUF_ZIP_VARIANT));
   //         //    System.out.println("status: " + response.getStatus());
   //         Person_proto.Person person = response.readEntity(Person_proto.Person.class);
   //         Assert.assertTrue("tanicka".equals(person.getName()));
   //      }
   //      System.out.println("Person_proto/zip time:  " + (System.currentTimeMillis() - start));
   //      System.out.println("Person_proto/zip bytes: " + ProtobufProvider.getSize());
   //      //      Assert.fail("ok");
   //   }

   private void doVeryBigPersonZipTest(String transport) throws Exception
   {
      //      System.out.println("url: " + generateURL("/big/" + transport));
      Builder request = zipClient.target(generateURL("/big/" + transport + "/zip")).request();
      Variant variant = "json".equals(transport) ? JSON_ZIP_VARIANT : PROTOBUF_ZIP_VARIANT;
      Response response = request.post(Entity.entity(veryBigRon, variant));
      VeryBigPerson person = response.readEntity(VeryBigPerson.class);
      Assert.assertEquals("tanicka", person.getS0());
   }
   
   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //   ////@Test
   public void testVeryBigPersonProtoNetty() throws Exception {
      //      doVeryBigTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonNettyTest("protobuf");
      }
      System.out.println("VeryBigPerson (proto/netty) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (proto/netty) bytes: " + ProtobufProvider.getSize());
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPersonJSONNetty() throws Exception {
      //      doVeryBigTest("json");
//      System.out.println("VeryBigPerson (JSON/netty) bytes [before]: " + JsonBindingProviderExp.getSize());
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonNettyTest("json");
      }
      System.out.println("VeryBigPerson (JSON/netty) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (JSON/netty) bytes: " + JsonBindingProviderExp.getSize());
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPerson_protoNetty()
   {
      Builder request = nettyClient.target(generateURL("/big/protobuf/proto")).request();
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
      System.out.println("VeryBigPerson_proto (netty) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson_proto (netty) bytes: " + ProtobufProvider.getSize());

      //      Assert.fail("ok");
   }

   private void doVeryBigPersonNettyTest(String transport) throws Exception
   {
      //      System.out.println("url: " + generateURL("/big/" + transport));
      Builder request = nettyClient.target(generateURL("/big/" + transport)).request();
      Response response = request.post(Entity.entity(veryBigRon, "application/" + transport));
      VeryBigPerson person = response.readEntity(VeryBigPerson.class);
      Assert.assertEquals("tanicka", person.getS0());
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   //   ////@Test
   public void testVeryBigPersonProtoNettyAsync() throws Exception {
      //      doVeryBigTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonNettyAsyncTest("protobuf");
      }
      System.out.println("VeryBigPerson (proto/netty/async) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (proto/netty/async) bytes: " + ProtobufProvider.getSize());
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPersonJSONNettyAsync() throws Exception {
      //      doVeryBigTest("json");
//      System.out.println("VeryBigPerson (JSON/netty) bytes [before]: " + JsonBindingProviderExp.getSize());
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonNettyAsyncTest("json");
      }
      System.out.println("VeryBigPerson (JSON/netty/async) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson (JSON/netty/async) bytes: " + JsonBindingProviderExp.getSize());
      //      Assert.fail("ok");
   }

   //@Test
   public void testVeryBigPerson_protoNettyAsync() throws Exception
   {
      Builder request = nettyClient.target(generateURL("/big/protobuf/proto")).request();
      //      Response response = request.post(Entity.entity(veryBigRon_proto, "application/protobuf"));
      //      VeryBigPerson_proto.VeryBigPerson person = response.readEntity(VeryBigPerson_proto.VeryBigPerson.class);
      //      Assert.assertTrue("tanicka".equals(person.getS0()));
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++)
      {
         Future<Response> future = request.async().post(Entity.entity(veryBigRon_proto, "application/protobuf"));
         Response response = future.get();
         //    System.out.println("status: " + response.getStatus());
         VeryBigPerson_proto.VeryBigPerson person = response.readEntity(VeryBigPerson_proto.VeryBigPerson .class);
         Assert.assertTrue("tanicka".equals(person.getS0()));
      }
      System.out.println("VeryBigPerson_proto (netty/async) time:  " + (System.currentTimeMillis() - start));
      System.out.println("VeryBigPerson_proto (netty/async) bytes: " + ProtobufProvider.getSize());

      //      Assert.fail("ok");
   }

   private void doVeryBigPersonNettyAsyncTest(String transport) throws Exception
   {
      //      System.out.println("url: " + generateURL("/big/" + transport));
      Builder request = nettyClient.target(generateURL("/big/" + transport)).request();
      Future<Response> future = request.async().post(Entity.entity(veryBigRon, "application/" + transport));
      Response response = future.get();
      VeryBigPerson person = response.readEntity(VeryBigPerson.class);
      Assert.assertEquals("tanicka", person.getS0());
   }

   
   //   ////@Test
   public void testString() throws Exception {
      System.out.println("url: " + generateURL("/string/string"));
      Builder request = client.target(generateURL("/string/string")).request();
      Response response = request.post(Entity.entity(ron, "text/plain"));
      System.out.println("status: " + response.getStatus());
      String s = response.readEntity(String.class);
      System.out.println("s: " + s);
      System.out.println(response.getHeaderString("Location"));
   }
   
   ////////////////////////////////////////////////////////////////////////////////////////////////////
   @Test
   public void testVeryBigPersonNumericProto() {
      //      doTest("protobuf");
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doVeryBigPersonNumericTest("protobuf");
      }
      System.out.println("Person (protobuf) time:  " + (System.currentTimeMillis() - start));
      System.out.println("Person (protobuf) bytes: " + ProtobufProvider.getSize());
      Assert.fail("ok"); // Get details
   }

//   //   //@Test
//   public void testPersonJSON() {
//      //      doTest("json");
//      long start = System.currentTimeMillis();
//      for (int i = 0; i < count; i++) {
//         doTest("json");
//      }
//      System.out.println("Person (JSON) time:  " + (System.currentTimeMillis() - start));
//      System.out.println("Person (JSON) bytes: " + JsonBindingProviderExp.getSize());
//      //      Assert.fail("ok");
//   }

//   //   //@Test
//   public void testPerson_proto()
//   {         
//      Builder request = client.target(generateURL("/protobuf/proto")).request();
//      //      Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
//      //      Person_proto.Person person = response.readEntity(Person_proto.Person.class);
//      //      Assert.assertTrue(tanicka_proto.getEmail().equals(person.getEmail()));
//      long start = System.currentTimeMillis();
//      for (int i = 0; i < count; i++)
//      {
//         Response response = request.post(Entity.entity(ron_proto, "application/protobuf"));
//         //    System.out.println("status: " + response.getStatus());
//         Person_proto.Person person = response.readEntity(Person_proto.Person.class);
//         Assert.assertTrue("tanicka".equals(person.getName()));
//      }
//      System.out.println("Person_proto time:  " + (System.currentTimeMillis() - start));
//      System.out.println("Person_proto bytes: " + ProtobufProvider.getSize());
//      //      Assert.fail("ok");
//   }

   public void doVeryBigPersonNumericTest(String transport)
   {
      Builder request = client.target(generateURL("/" + transport)).request();
      Response response = request.post(Entity.entity(veryBigRonNumeric, "application/" + transport));
      //    System.out.println("status: " + response.getStatus());
      Person person = response.readEntity(Person.class);
      Assert.assertEquals(tanicka, person);
   }
}
