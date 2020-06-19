package org.jboss.resteasy.experiment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Priority;
import javax.json.bind.Jsonb;
import javax.ws.rs.Consumes;
import javax.ws.rs.Priorities;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.providers.jsonb.JsonBindingProvider;
import org.jboss.resteasy.plugins.providers.jsonb.i18n.Messages;
import org.jboss.resteasy.util.DelegatingOutputStream;

/**
 * Created by rsearls on 6/26/17.
 */
@Provider
@Produces({"application/json", "application/*+json", "text/json"})
@Consumes({"application/json", "application/*+json", "text/json"})
@Priority(Priorities.USER-500)
public class JsonBindingProviderExp extends JsonBindingProvider
      implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

   private static long size;

   public static long getSize()
   {
      return size;
   }
   
   public static void setSize(long size) {
      JsonBindingProviderExp.size = size;
   }

   @Override
   public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations,
                       MediaType mediaType,
                       MultivaluedMap<String, Object> httpHeaders,
                       OutputStream entityStream)
         throws java.io.IOException, javax.ws.rs.WebApplicationException {
      Jsonb jsonb = getJsonb(type);
      try
      {
         entityStream = new DelegatingOutputStream(entityStream) {
            @Override
            public void flush() throws IOException {
               // don't flush as this is a performance hit on Undertow.
               // and causes chunked encoding to happen.
            }
         };
         entityStream.write(jsonb.toJson(t).getBytes(getCharset(mediaType)));
         entityStream.flush();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         baos.write(jsonb.toJson(t).getBytes(getCharset(mediaType)));
         size += baos.toByteArray().length;
      } catch (Throwable e)
      {
         throw new ProcessingException(Messages.MESSAGES.jsonBSerializationError(e.toString()), e);
      }
   }
//
//   @Override
//   public CompletionStage<Void> asyncWriteTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
//                                             MultivaluedMap<String, Object> httpHeaders, AsyncOutputStream entityStream) {
//      Jsonb jsonb = getJsonb(type);
//      try
//      {
//         return entityStream.asyncWrite(jsonb.toJson(t).getBytes(getCharset(mediaType)));
//      } catch (Throwable e)
//      {
//         CompletableFuture<Void> ret = new CompletableFuture();
//         ret.completeExceptionally(new ProcessingException(Messages.MESSAGES.jsonBSerializationError(e.toString()), e));
//         return ret;
//      }
//   }
}
