package com.sysdig.jenkins.plugins.sysdig.json;

import com.google.gson.Gson;
import com.sysdig.jenkins.plugins.sysdig.uireport.SysdigSecureGatesSerializer;
import com.sysdig.jenkins.plugins.sysdig.uireport.SysdigSecureGates;

public class GsonBuilder {
  public static Gson build() {
    return new com.google.gson.GsonBuilder()
      .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY)
      .registerTypeAdapter(SysdigSecureGates.class, new SysdigSecureGatesSerializer())
      .create();
  }
}
