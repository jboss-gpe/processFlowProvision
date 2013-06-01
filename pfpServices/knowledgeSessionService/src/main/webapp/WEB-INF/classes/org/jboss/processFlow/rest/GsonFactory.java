package org.jboss.processFlow.rest;

import java.sql.Timestamp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonFactory
{
  public static Gson createInstance()
  {
    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").registerTypeAdapter(Timestamp.class, new SQLDateTypeAdapter()).create();

    return gson;
  }
}
