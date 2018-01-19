package com.opennetwork.secureim.mms;

import com.opennetwork.secureim.util.Base64;

import java.io.IOException;


public class TextTransport {

  public byte[] getDecodedMessage(byte[] encodedMessageBytes) throws IOException {
    return Base64.decode(encodedMessageBytes);
  }

  public byte[] getEncodedMessage(byte[] messageWithMac) {
    return Base64.encodeBytes(messageWithMac).getBytes();
  }
}
