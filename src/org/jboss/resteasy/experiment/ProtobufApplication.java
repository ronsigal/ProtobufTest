package org.jboss.resteasy.experiment;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class ProtobufApplication extends Application
{
   public Set<Class<?>> getClasses() {
      Set<Class<?>> classes = new HashSet<Class<?>>();
      classes.add(ProtobufWFResource.class);
      classes.add(JsonBindingProviderExp.class);
      return classes;
  }
}
