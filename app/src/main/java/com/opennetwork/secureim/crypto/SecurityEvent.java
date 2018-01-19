package com.opennetwork.secureim.crypto;

import android.content.Context;
import android.content.Intent;

import com.opennetwork.secureim.service.KeyCachingService;

/**
 * This class processes key exchange interactions.
 */

public class SecurityEvent {

  public static final String SECURITY_UPDATE_EVENT = "com.opennetwork.securesms.KEY_EXCHANGE_UPDATE";

  public static void broadcastSecurityUpdateEvent(Context context) {
    Intent intent = new Intent(SECURITY_UPDATE_EVENT);
    intent.setPackage(context.getPackageName());
    context.sendBroadcast(intent, KeyCachingService.KEY_PERMISSION);
  }

}
