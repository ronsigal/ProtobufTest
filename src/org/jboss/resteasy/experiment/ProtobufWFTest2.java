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
public class ProtobufWFTest2 {

   private static int count = 1000;

   private static Client httpClient;
   private static Client httpClientAsync;
   private static Client httpClientZip;
   private static Client httpClientAsyncZip;
   
   private static Client vertxClient;
   private static Client nettyClient;
   private static ScheduledExecutorService executorService;
   private static Vertx vertx;
   private static final MediaType PROTOBUF_MEDIA_TYPE = new MediaType("application", "protobuf");
   private static final Variant PROTOBUF_VARIANT = new Variant(PROTOBUF_MEDIA_TYPE, (String) null, (String) null);
   private static final Variant JSON_VARIANT = new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, (String) null);
   private static final Variant PROTOBUF_ZIP_VARIANT = new Variant(PROTOBUF_MEDIA_TYPE, (String) null, "gzip");
   private static final Variant JSON_ZIP_VARIANT = new Variant(MediaType.APPLICATION_JSON_TYPE, (String) null, "gzip");
   private static Person ron = new Person(1, "ron", "ron@jboss.org");
   private static Person tanicka = new Person(3, "tanicka", "a@b");
   private static Person_proto.Person ron_proto = Person_proto.Person.newBuilder().setId(1).setName("ron").setEmail("ron@jboss.org").build();
   private static VeryBigPerson veryBigRon = PersonUtil.getVeryBigPerson("ron");
   private static VeryBigPerson_proto.VeryBigPerson veryBigRon_proto = PersonUtil.getVeryBigPerson_proto("ron");

   private String generateURL(String path) {
      //            return PortProviderUtil.generateURL(path, "ProtobufTest-0.0.1-SNAPSHOT");
      //      return PortProviderUtil.generateURL(path, "eap-app-protobuf-test.6923.rh-us-east-1.openshiftapps.com/ProtobufTest-0.0.1-SNAPSHOT");
      return "https://eap-app-protobuf-test.6923.rh-us-east-1.openshiftapps.com:443/ProtobufTest-0.0.1-SNAPSHOT" + path;
   }

   @BeforeClass
   public static void setup() {
      httpClient = ClientBuilder.newClient();
      httpClient.register(ProtobufProvider.class);
      httpClient.register(JsonBindingProviderExp.class);

      httpClientAsync = ((ResteasyClientBuilder)ClientBuilder.newBuilder()).useAsyncHttpEngine().build();
      httpClientAsync.register(ProtobufProvider.class);
      httpClientAsync.register(JsonBindingProviderExp.class);

      httpClientZip = ClientBuilder.newClient();
      httpClientZip.register(ProtobufProvider.class);
      httpClientZip.register(JsonBindingProviderExp.class);
      httpClientZip.register(GZIPEncodingInterceptor.class);
      httpClientZip.register(GZIPDecodingInterceptor.class);

      httpClientAsyncZip = ((ResteasyClientBuilder)ClientBuilder.newBuilder()).useAsyncHttpEngine().build();
      httpClientAsyncZip.register(ProtobufProvider.class);
      httpClientAsyncZip.register(JsonBindingProviderExp.class);
      httpClientAsyncZip.register(GZIPEncodingInterceptor.class);
      httpClientAsyncZip.register(GZIPDecodingInterceptor.class);
      
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
      if (httpClient != null) {
         httpClient.close();
      }
      CountDownLatch latch = new CountDownLatch(1);
      vertx.close(ar -> latch.countDown());
      latch.await(2, TimeUnit.MINUTES);
      executorService.shutdownNow();
   }

   @Before
   public void before() {
      JsonBindingProviderExp.setSize(0);
      GZIPEncodingInterceptor.count = 0;
      ProtobufProvider.setSize(0);
      ProtobufProvider.getAssignFromMap().clear();
      ProtobufProvider.getAssignToMap().clear();
      ProtobufProvider.getMap().clear();
   }

   ////////////////////////////////////////////////////////////////////////////////////////////////////
   @Test
   public void testHttpJSON() throws Exception {
      doTest(httpClient, "HttpClient", JSON_VARIANT, "json", false);
   }
   
   @Test
   public void testHttpJSONAsync() throws Exception {
      doTest(httpClientAsync, "HttpClient", JSON_VARIANT, "json", true);
   }
   
   @Test
   public void testHttpJSONZip() throws Exception {
      doTest(httpClientZip, "HttpClient", JSON_ZIP_VARIANT, "json", false);
   }
   
   @Test
   public void testHttpJSONAsyncZip() throws Exception {
      doTest(httpClientAsyncZip, "HttpClient", JSON_ZIP_VARIANT, "json", true);
   }
   
   @Test
   public void testHttpProto() throws Exception {
      doTest(httpClient, "HttpClient", PROTOBUF_VARIANT, "protobuf/proto", false);
   }
   
   @Test
   public void testHttpProtoAsync() throws Exception {
      doTest(httpClientAsync, "HttpClient", PROTOBUF_VARIANT, "protobuf/proto", true);
   }
   
   @Test
   public void testHttpProtoZip() throws Exception {
      doTest(httpClientZip, "HttpClient", PROTOBUF_ZIP_VARIANT, "protobuf/proto", false);
   }
   
   @Test
   public void testHttpProtoAsyncZip() throws Exception {
      doTest(httpClientAsyncZip, "HttpClient", PROTOBUF_ZIP_VARIANT, "protobuf/proto", true);
   }
   
   ////////////////////////////////////////////////////////////////////////////////////////////////////
   @Test
   public void testNettyJSON() throws Exception {
      doTest(nettyClient, "NettyClient", JSON_VARIANT, "json", false);
   }
   
   @Test
   public void testNettyJSONAsync() throws Exception {
      doTest(nettyClient, "NettyClient", JSON_VARIANT, "json", true);
   }
   
   @Test
   public void testNettyJSONZip() throws Exception {
      doTest(nettyClient, "NettyClient", JSON_ZIP_VARIANT, "json", false);
   }
   
   @Test
   public void testNettyJSONAsyncZip() throws Exception {
      doTest(nettyClient, "NettyClient", JSON_ZIP_VARIANT, "json", true);
   }
   
   @Test
   public void testNettyProto() throws Exception {
      doTest(nettyClient, "NettyClient", PROTOBUF_VARIANT, "protobuf/proto", false);
   }
   
   @Test
   public void testNettyProtoAsync() throws Exception {
      doTest(nettyClient, "NettyClient", PROTOBUF_VARIANT, "protobuf/proto", true);
   }
   
   @Test
   public void testNettyProtoZip() throws Exception {
      doTest(nettyClient, "NettyClient", PROTOBUF_ZIP_VARIANT, "protobuf/proto", false);
   }
   
   @Test
   public void testNettyProtoAsyncZip() throws Exception {
      doTest(nettyClient, "NettyClient", PROTOBUF_ZIP_VARIANT, "protobuf/proto", true);
   }

   private void doTest(Client client, String clientName, Variant variant, String transport, boolean async) throws Exception {
      long start = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
         doTestOnce(client, variant, transport, async);
      }
      boolean isGzip = "gzip".equals(variant.getEncoding());
      String asyncTag = async ? "/async" : "";
      String encodingTag = isGzip ? "/gzip" : "";
      long bytes = "json".equals(transport) ? JsonBindingProviderExp.getSize() : ProtobufProvider.getSize();
      System.out.println(clientName + "/" + transport + asyncTag + encodingTag + ": time:   " + (System.currentTimeMillis() - start));
//      System.out.println(clientName + "/" + transport + asyncTag + encodingTag + ": bytes:  " + bytes);
//      if (isGzip) {
//         System.out.println(clientName + "/" + transport + asyncTag + encodingTag + ": compressed bytes:  " + GZIPEncodingInterceptor.count);
//      }
   }
   
   private void doTestOnce(Client client, Variant variant, String transport, boolean async) throws Exception
   {
      Builder request = client.target(generateURL("/big/" + transport)).request();
      Object entity = "json".equals(transport) ? veryBigRon : veryBigRon_proto;
      Response response = null;
      if (async) {
         Future<Response> future = request.async().post(Entity.entity(entity, variant));
         response = future.get();
      } else {
         response = request.post(Entity.entity(entity, variant));
      }
//      System.out.println("status: " + response.getStatus());
//      System.out.println("result: " + response.readEntity(String.class));
      if ("json".equals(transport)) {
         VeryBigPerson person = response.readEntity(VeryBigPerson.class);
         Assert.assertEquals("tanicka", person.getS0());
      } else {
         VeryBigPerson_proto.VeryBigPerson person = response.readEntity(VeryBigPerson_proto.VeryBigPerson .class);
         Assert.assertTrue("tanicka".equals(person.getS0()));
      }
   }
}
