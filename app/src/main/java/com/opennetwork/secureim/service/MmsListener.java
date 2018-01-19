package com.opennetwork.secureim.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

import com.opennetwork.secureim.ApplicationContext;
import com.opennetwork.secureim.jobs.MmsReceiveJob;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.secureim.util.Util;

public class MmsListener extends BroadcastReceiver {

  private static final String TAG = MmsListener.class.getSimpleName();

  private boolean isRelevant(Context context, Intent intent) {
    if (!ApplicationMigrationService.isDatabaseImported(context)) {
      return false;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
        Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction()) &&
        Util.isDefaultSmsProvider(context))
    {
      return false;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT &&
        TextSecurePreferences.isInterceptAllMmsEnabled(context))
    {
      return true;
    }

    return false;
  }

  @Override
    public void onReceive(Context context, Intent intent) {
    Log.w(TAG, "Got MMS broadcast..." + intent.getAction());

    if ((Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION.equals(intent.getAction())  &&
        Util.isDefaultSmsProvider(context))                                        ||
        (Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction()) &&
         isRelevant(context, intent)))
    {
      Log.w(TAG, "Relevant!");
      int subscriptionId = intent.getExtras().getInt("subscription", -1);

      ApplicationContext.getInstance(context)
                        .getJobManager()
                        .add(new MmsReceiveJob(context, intent.getByteArrayExtra("data"), subscriptionId));

      abortBroadcast();
    }
  }



}
