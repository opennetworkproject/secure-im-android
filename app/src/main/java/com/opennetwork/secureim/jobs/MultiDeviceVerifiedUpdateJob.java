package com.opennetwork.secureim.jobs;


import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.IdentityDatabase.VerifiedStatus;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.IdentityKey;
import com.opennetwork.libim.InvalidKeyException;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.multidevice.OpenNetworkServiceSyncMessage;
import com.opennetwork.imservice.api.messages.multidevice.VerifiedMessage;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

import javax.inject.Inject;

public class MultiDeviceVerifiedUpdateJob extends ContextJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = MultiDeviceVerifiedUpdateJob.class.getSimpleName();

  @Inject
  transient OpenNetworkServiceMessageSender messageSender;

  private final String         destination;
  private final byte[]         identityKey;
  private final VerifiedStatus verifiedStatus;
  private final long           timestamp;

  public MultiDeviceVerifiedUpdateJob(Context context, Address destination, IdentityKey identityKey, VerifiedStatus verifiedStatus) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .withGroupId("__MULTI_DEVICE_VERIFIED_UPDATE__")
                                .create());

    this.destination    = destination.serialize();
    this.identityKey    = identityKey.serialize();
    this.verifiedStatus = verifiedStatus;
    this.timestamp      = System.currentTimeMillis();
  }

  @Override
  public void onRun() throws IOException, UntrustedIdentityException {
    try {
      if (!TextSecurePreferences.isMultiDevice(context)) {
        Log.w(TAG, "Not multi device...");
        return;
      }

      if (destination == null) {
        Log.w(TAG, "No destination...");
        return;
      }

      Address                       canonicalDestination = Address.fromSerialized(destination);
      VerifiedMessage.VerifiedState verifiedState        = getVerifiedState(verifiedStatus);
      VerifiedMessage               verifiedMessage      = new VerifiedMessage(canonicalDestination.toPhoneString(), new IdentityKey(identityKey, 0), verifiedState, timestamp);

      messageSender.sendMessage(OpenNetworkServiceSyncMessage.forVerified(verifiedMessage));
    } catch (InvalidKeyException e) {
      throw new IOException(e);
    }
  }

  private VerifiedMessage.VerifiedState getVerifiedState(VerifiedStatus status) {
    VerifiedMessage.VerifiedState verifiedState;

    switch (status) {
      case DEFAULT:    verifiedState = VerifiedMessage.VerifiedState.DEFAULT;    break;
      case VERIFIED:   verifiedState = VerifiedMessage.VerifiedState.VERIFIED;   break;
      case UNVERIFIED: verifiedState = VerifiedMessage.VerifiedState.UNVERIFIED; break;
      default: throw new AssertionError("Unknown status: " + verifiedStatus);
    }

    return verifiedState;
  }

  @Override
  public boolean onShouldRetry(Exception exception) {
    return exception instanceof PushNetworkException;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onCanceled() {

  }
}
