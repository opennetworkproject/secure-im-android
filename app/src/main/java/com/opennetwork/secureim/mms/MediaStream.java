package com.opennetwork.secureim.mms;

import java.io.InputStream;

public class MediaStream {
  private final InputStream stream;
  private final String      mimeType;

  public MediaStream(InputStream stream, String mimeType) {
    this.stream   = stream;
    this.mimeType = mimeType;
  }

  public InputStream getStream() {
    return stream;
  }

  public String getMimeType() {
    return mimeType;
  }
}
