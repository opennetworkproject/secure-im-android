package com.opennetwork.secureim.util;

import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Rfc5724Uri {

  private final String              uri;
  private final String              schema;
  private final String              path;
  private final Map<String, String> queryParams;

  public Rfc5724Uri(String uri) throws URISyntaxException {
    this.uri         = uri;
    this.schema      = parseSchema();
    this.path        = parsePath();
    this.queryParams = parseQueryParams();
  }

  private String parseSchema() throws URISyntaxException {
    String[] parts = uri.split(":");

    if (parts.length < 1 || parts[0].isEmpty()) throw new URISyntaxException(uri, "invalid schema");
    else                                        return parts[0];
  }

  private String parsePath() throws URISyntaxException {
    String[] parts = uri.split("\\?")[0].split(":", 2);

    if (parts.length < 2 || parts[1].isEmpty()) throw new URISyntaxException(uri, "invalid path");
    else                                        return parts[1];
  }

  private Map<String, String> parseQueryParams() throws URISyntaxException {
    Map<String, String> queryParams = new HashMap<>();
    if (uri.split("\\?").length < 2) {
      return queryParams;
    }

    for (String keyValue : uri.split("\\?")[1].split("&")) {
      String[] parts = keyValue.split("=");

      if (parts.length == 1) queryParams.put(parts[0], "");
      else                  queryParams.put(parts[0], URLDecoder.decode(parts[1]));
    }

    return queryParams;
  }

  public String getSchema() {
    return schema;
  }

  public String getPath() {
    return path;
  }

  public Map<String, String> getQueryParams() {
    return queryParams;
  }
}
