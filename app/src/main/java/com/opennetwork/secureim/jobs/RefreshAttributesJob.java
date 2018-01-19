package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.imservice.api.OpenNetworkServiceAccountManager;
import com.opennetwork.imservice.api.push.exceptions.NetworkFailureException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class RefreshAttributesJob extends ContextJob implements InjectableType {

  public static final long serialVersionUID = 1L;

  private static final String TAG = RefreshAttributesJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceAccountManager opennetworkAccountManager;

  public RefreshAttributesJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withRequirement(new NetworkRequirement(context))
                                .withWakeLock(true, 30, TimeUnit.SECONDS)
                                .withGroupId(RefreshAttributesJob.class.getName())
                                .create());
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException {
    String  opennetworkingKey      = TextSecurePreferences.getOpenNetworkingKey(context);
    int     registrationId    = TextSecurePreferences.getLocalRegistrationId(context);
    boolean fetchesMessages   = TextSecurePreferences.isGcmDisabled(context);

    opennetworkAccountManager.setAccountAttributes(opennetworkingKey, registrationId, fetchesMessages);
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    return e instanceof NetworkFailureException;
  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "Failed to update account attributes!");
  }
}
