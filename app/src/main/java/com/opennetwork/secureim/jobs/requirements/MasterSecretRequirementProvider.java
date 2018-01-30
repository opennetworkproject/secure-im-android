package com.opennetwork.secureim.jobs.requirements;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.opennetwork.secureim.service.KeyCachingService;
import com.opennetwork.jobqueue.requirements.RequirementListener;
import com.opennetwork.jobqueue.requirements.RequirementProvider;

public class MasterSecretRequirementProvider implements RequirementProvider {

  private final BroadcastReceiver newKeyReceiver;

  private RequirementListener listener;

  public MasterSecretRequirementProvider(Context context) {
    this.newKeyReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (listener != null) {
          listener.onRequirementStatusChanged();
        }
      }
    };

    IntentFilter filter = new IntentFilter(KeyCachingService.NEW_KEY_EVENT);
    context.registerReceiver(newKeyReceiver, filter, KeyCachingService.KEY_PERMISSION, null);
  }

  @Override
  public void setListener(RequirementListener listener) {
    this.listener = listener;
  }
}
