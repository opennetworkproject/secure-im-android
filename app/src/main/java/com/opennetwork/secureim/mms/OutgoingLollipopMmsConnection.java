package com.opennetwork.secureim.mms;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.mms.service_alt.MmsConfig;
import com.google.android.mms.pdu_alt.PduParser;
import com.google.android.mms.pdu_alt.SendConf;

import com.opennetwork.secureim.providers.MmsBodyProvider;
import com.opennetwork.secureim.transport.UndeliverableMessageException;
import com.opennetwork.secureim.util.Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class OutgoingLollipopMmsConnection extends LollipopMmsConnection implements OutgoingMmsConnection {
  private static final String TAG    = OutgoingLollipopMmsConnection.class.getSimpleName();
  private static final String ACTION = OutgoingLollipopMmsConnection.class.getCanonicalName() + "MMS_SENT_ACTION";

  private byte[] response;

  public OutgoingLollipopMmsConnection(Context context) {
    super(context, ACTION);
  }

  @TargetApi(VERSION_CODES.LOLLIPOP_MR1)
  @Override
  public synchronized void onResult(Context context, Intent intent) {
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP_MR1) {
      Log.w(TAG, "HTTP status: " + intent.getIntExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, -1));
    }

    response = intent.getByteArrayExtra(SmsManager.EXTRA_MMS_DATA);
  }

  @Override
  @TargetApi(VERSION_CODES.LOLLIPOP)
  public @Nullable synchronized SendConf send(@NonNull byte[] pduBytes, int subscriptionId)
      throws UndeliverableMessageException
  {
    beginTransaction();
    try {
      MmsBodyProvider.Pointer pointer = MmsBodyProvider.makeTemporaryPointer(getContext());
      Util.copy(new ByteArrayInputStream(pduBytes), pointer.getOutputStream());

      SmsManager smsManager;

      if (VERSION.SDK_INT >= 22 && subscriptionId != -1) {
        smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
      } else {
        smsManager = SmsManager.getDefault();
      }

      Bundle configOverrides = new Bundle();
      configOverrides.putBoolean(SmsManager.MMS_CONFIG_GROUP_MMS_ENABLED, true);

      MmsConfig mmsConfig = MmsConfigManager.getMmsConfig(getContext(), subscriptionId);

      if (mmsConfig != null) {
        MmsConfig.Overridden overridden = new MmsConfig.Overridden(mmsConfig, new Bundle());
        configOverrides.putString(SmsManager.MMS_CONFIG_HTTP_PARAMS, overridden.getHttpParams());
        configOverrides.putInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, overridden.getMaxMessageSize());
      }

      smsManager.sendMultimediaMessage(getContext(),
                                       pointer.getUri(),
                                       null,
                                       configOverrides,
                                       getPendingIntent());

      waitForResult();

      Log.w(TAG, "MMS broadcast received and processed.");
      pointer.close();

      if (response == null) {
        throw new UndeliverableMessageException("Null response.");
      }

      return (SendConf) new PduParser(response).parse();
    } catch (IOException | TimeoutException e) {
      throw new UndeliverableMessageException(e);
    } finally {
      endTransaction();
    }
  }
}

