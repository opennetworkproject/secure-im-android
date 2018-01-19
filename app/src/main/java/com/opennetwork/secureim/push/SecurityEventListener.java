package com.opennetwork.secureim.push;

import android.content.Context;

import com.opennetwork.secureim.crypto.SecurityEvent;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.push.OpenNetworkServiceAddress;

public class SecurityEventListener implements OpenNetworkServiceMessageSender.EventListener {

  private static final String TAG = SecurityEventListener.class.getSimpleName();

  private final Context context;

  public SecurityEventListener(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public void onSecurityEvent(OpenNetworkServiceAddress textSecureAddress) {
    SecurityEvent.broadcastSecurityUpdateEvent(context);
  }
}
