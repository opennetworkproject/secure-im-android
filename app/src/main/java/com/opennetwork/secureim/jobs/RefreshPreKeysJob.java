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
import com.opennetwork.libim.state.PreKeyRecord;
import com.opennetwork.libim.state.SignedPreKeyRecord;
import com.opennetwork.imservice.api.OpenNetworkServiceAccountManager;
import com.opennetwork.imservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

public class RefreshPreKeysJob extends MasterSecretJob implements InjectableType {

  private static final String TAG = RefreshPreKeysJob.class.getSimpleName();

  private static final int PREKEY_MINIMUM = 10;

  @Inject transient OpenNetworkServiceAccountManager accountManager;

  public RefreshPreKeysJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withGroupId(RefreshPreKeysJob.class.getSimpleName())
                                .withRequirement(new NetworkRequirement(context))
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRetryCount(5)
                                .create());
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException {
    if (!TextSecurePreferences.isPushRegistered(context)) return;

    int availableKeys = accountManager.getPreKeysCount();

    if (availableKeys >= PREKEY_MINIMUM && TextSecurePreferences.isSignedPreKeyRegistered(context)) {
      Log.w(TAG, "Available keys sufficient: " + availableKeys);
      return;
    }

    List<PreKeyRecord> preKeyRecords       = PreKeyUtil.generatePreKeys(context);
    IdentityKeyPair    identityKey         = IdentityKeyUtil.getIdentityKeyPair(context);
    SignedPreKeyRecord signedPreKeyRecord  = PreKeyUtil.generateSignedPreKey(context, identityKey, false);

    Log.w(TAG, "Registering new prekeys...");

    accountManager.setPreKeys(identityKey.getPublicKey(), signedPreKeyRecord, preKeyRecords);

    PreKeyUtil.setActiveSignedPreKeyId(context, signedPreKeyRecord.getId());
    TextSecurePreferences.setSignedPreKeyRegistered(context, true);

    ApplicationContext.getInstance(context)
                      .getJobManager()
                      .add(new CleanPreKeysJob(context));
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof NonSuccessfulResponseCodeException) return false;
    if (exception instanceof PushNetworkException)               return true;

    return false;
  }

  @Override
  public void onCanceled() {

  }

}
