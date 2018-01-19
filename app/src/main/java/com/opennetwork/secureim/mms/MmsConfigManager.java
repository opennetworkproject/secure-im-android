package com.opennetwork.secureim.mms;


import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.android.mms.service_alt.MmsConfig;

import com.opennetwork.secureim.util.dualsim.SubscriptionInfoCompat;
import com.opennetwork.secureim.util.dualsim.SubscriptionManagerCompat;
import com.opennetwork.libim.util.guava.Optional;

import java.util.HashMap;
import java.util.Map;

public class MmsConfigManager {

  private static Map<Integer, MmsConfig> mmsConfigMap = new HashMap<>();

  @WorkerThread
  public synchronized static @Nullable MmsConfig getMmsConfig(Context context, int subscriptionId) {
    if (mmsConfigMap.containsKey(subscriptionId)) {
      return mmsConfigMap.get(subscriptionId);
    }

    MmsConfig loadedConfig = loadMmsConfig(context, subscriptionId);

    if (loadedConfig != null) mmsConfigMap.put(subscriptionId, loadedConfig);

    return loadedConfig;
  }

  private static MmsConfig loadMmsConfig(Context context, int subscriptionId) {
    if (subscriptionId != -1 && Build.VERSION.SDK_INT >= 24) {
      Optional<SubscriptionInfoCompat> subscriptionInfo = new SubscriptionManagerCompat(context).getActiveSubscriptionInfo(subscriptionId);

      if (subscriptionInfo.isPresent()) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.mcc = subscriptionInfo.get().getMcc();
        configuration.mnc = subscriptionInfo.get().getMnc();

        Context subcontext = context.createConfigurationContext(configuration);
        return new MmsConfig(subcontext, subscriptionId);
      }
    }

    return new MmsConfig(context, subscriptionId);
  }

}
