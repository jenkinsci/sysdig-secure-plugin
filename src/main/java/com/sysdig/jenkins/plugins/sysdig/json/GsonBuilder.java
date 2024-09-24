package com.sysdig.jenkins.plugins.sysdig.json;

import com.google.gson.Gson;

public class GsonBuilder {
  public static Gson build() {
    return new com.google.gson.GsonBuilder()
      .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY)
      .create();
  }
}
