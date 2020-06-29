package org.jboss.resteasy.experiment;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.resteasy.annotations.GZIP;

@Path("numeric")
public class ProtobufWFResourceNumeric
{
   private static VeryBigPersonNumeric veryBigTanickaNumeric = PersonUtil.getVeryBigPersonNumeric();
   private static VeryBigPersonNumeric_proto.VeryBigPersonNumeric veryBigTanicka_proto = PersonUtil.getVeryBigPersonNumeric_proto();
   
   @POST
   @Path("big/json")
   @Consumes("application/json")
   @Produces("application/json")
   public VeryBigPersonNumeric veryBigJson(VeryBigPersonNumeric person) throws Exception {
      return veryBigTanickaNumeric;
   }

   @POST
   @Path("big/protobuf")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   public VeryBigPersonNumeric veryBigProto(VeryBigPersonNumeric person) throws Exception {
      return veryBigTanickaNumeric;
   }
   
   @POST
   @Path("big/protobuf/proto")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   public VeryBigPersonNumeric_proto.VeryBigPersonNumeric proto(VeryBigPersonNumeric_proto.VeryBigPersonNumeric person) {
      return veryBigTanicka_proto;
   }
   
   @POST
   @Path("big/json/zip")
   @Consumes("application/json")
   @Produces("application/json")
   @GZIP
   public VeryBigPersonNumeric veryBigJsonZip(@GZIP VeryBigPersonNumeric person) throws Exception {
      return veryBigTanickaNumeric;
   }

   @POST
   @Path("big/protobuf/zip")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   @GZIP
   public VeryBigPersonNumeric veryBigProtoZip(@GZIP VeryBigPersonNumeric person) throws Exception {
      return veryBigTanickaNumeric;
   }
   
   @POST
   @Path("big/protobuf/proto/zip")
   @Consumes("application/protobuf")
   @Produces("application/protobuf")
   @GZIP
   public VeryBigPersonNumeric_proto.VeryBigPersonNumeric protoZip(@GZIP VeryBigPersonNumeric_proto.VeryBigPersonNumeric person) {
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
