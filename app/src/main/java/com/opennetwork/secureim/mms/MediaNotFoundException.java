package com.opennetwork.secureim.mms;

public class MediaNotFoundException extends Exception {

  public MediaNotFoundException() {
  }

  public MediaNotFoundException(String detailMessage) {
    super(detailMessage);
  }

  public MediaNotFoundException(Throwable throwable) {
    super(throwable);
  }

  public MediaNotFoundException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

}
