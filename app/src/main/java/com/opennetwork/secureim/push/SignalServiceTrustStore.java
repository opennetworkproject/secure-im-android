package com.opennetwork.secureim.push;

import android.content.Context;

import com.opennetwork.secureim.R;
import com.opennetwork.imservice.api.push.TrustStore;

import java.io.InputStream;

public class OpenNetworkServiceTrustStore implements TrustStore {

  private final Context context;

  public OpenNetworkServiceTrustStore(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public InputStream getKeyStoreInputStream() {
    return context.getResources().openRawResource(R.raw.whisper);
  }

  @Override
  public String getKeyStorePassword() {
    return "opennetwork";
  }
}
