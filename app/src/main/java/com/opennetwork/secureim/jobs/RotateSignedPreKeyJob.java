package com.opennetwork.secureim.jobs;


import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.ApplicationContext;
import com.opennetwork.secureim.crypto.IdentityKeyUtil;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.crypto.PreKeyUtil;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.jobs.requirements.MasterSecretRequirement;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.IdentityKeyPair;
import com.opennetwork.libim.state.SignedPreKeyRecord;
import com.opennetwork.imservice.api.OpenNetworkServiceAccountManager;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import javax.inject.Inject;

public class RotateSignedPreKeyJob extends MasterSecretJob implements InjectableType {

  private static final String TAG = RotateSignedPreKeyJob.class.getName();

  @Inject transient OpenNetworkServiceAccountManager accountManager;

  public RotateSignedPreKeyJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRetryCount(5)
                                .create());
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onRun(MasterSecret masterSecret) throws Exception {
    Log.w(TAG, "Rotating signed prekey...");

    IdentityKeyPair    identityKey        = IdentityKeyUtil.getIdentityKeyPair(context);
    SignedPreKeyRecord signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(context, identityKey, false);

    accountManager.setSignedPreKey(signedPreKeyRecord);

    PreKeyUtil.setActiveSignedPreKeyId(context, signedPreKeyRecord.getId());
    TextSecurePreferences.setSignedPreKeyRegistered(context, true);
    TextSecurePreferences.setSignedPreKeyFailureCount(context, 0);

    ApplicationContext.getInstance(context)
                      .getJobManager()
                      .add(new CleanPreKeysJob(context));
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    return exception instanceof PushNetworkException;
  }

  @Override
  public void onCanceled() {
    TextSecurePreferences.setSignedPreKeyFailureCount(context, TextSecurePreferences.getSignedPreKeyFailureCount(context) + 1);
  }
}
