package org.jboss.resteasy.experiment;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;

@Path("")
public class ProtobufWFResource
{
   private static Person tanicka = new Person(3, "tanicka", "a@b");
   private static Person_proto.Person tanicka_proto = Person_proto.Person.newBuilder().setId(3).setName("tanicka").setEmail("a@b.c").build();
   private static VeryBigPerson veryBigTanicka = PersonUtil.getVeryBigPerson("tanicka");
   private static VeryBigPerson_proto.VeryBigPerson veryBigTanicka_proto = PersonUtil.getVeryBigPerson_proto("tanicka");
   
   @POST
   @Path("json")
   @Consumes("application/json")
   @Produces("application/json")
   public Person json(Person person) {
//      System.out.println("json: " + person);
      return tanicka;
   }
   
   @POST
   @Path("protobuf")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   public Person protobuf(Person person) {
      return tanicka;
   }
   
   @POST
   @Path("protobuf/proto")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   public Person_proto.Person proto(Person_proto.Person person) {
      return tanicka_proto;
   }
   
   @POST
   @Path("json/zip")
   @Consumes("application/json")
   @Produces("application/json")
   @GZIP
   public Response jsonZip(@GZIP Person person) {
//      System.out.println("json: " + person);
      return Response.ok(tanicka).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
   }
   
   @POST
   @Path("protobuf/zip")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   @GZIP
   public Response protobufZip(@GZIP Person person) {
      return Response.ok(tanicka).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
   }
   
   @POST
   @Path("protobuf/proto/zip")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   @GZIP
   public Response protoZip(@GZIP Person_proto.Person person) {
      return Response.ok(tanicka_proto).header(HttpHeaders.CONTENT_ENCODING, "gzip").build();
   }
   
   @POST
   @Path("big/json")
   @Consumes("application/json")
   @Produces("application/json")
   public VeryBigPerson veryBigJson(VeryBigPerson person) throws Exception {
      return veryBigTanicka;
   }

   @POST
   @Path("big/protobuf")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   public VeryBigPerson veryBigProto(VeryBigPerson person) throws Exception {
      return veryBigTanicka;
   }
   
   @POST
   @Path("big/protobuf/proto")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   public VeryBigPerson_proto.VeryBigPerson proto(VeryBigPerson_proto.VeryBigPerson person) {
      return veryBigTanicka_proto;
   }
   

   @POST
   @Path("string/string")
   @Consumes("text/plain")
   @Produces("text/plain")
   public String stringString(String s) {
//      System.out.println("returning: tanicka" + s);
      return "tanicka" + s;
   }
   
   @GET
   @Path("string")
   @Produces("text/plain")
   public String string() {
//      System.out.println("returning: tanicka");
      return "tanicka";
   }
}
