package com.opennetwork.secureim.jobs;


import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.crypto.ProfileKeyUtil;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.crypto.UntrustedIdentityException;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachment;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachmentStream;
import com.opennetwork.imservice.api.messages.multidevice.ContactsMessage;
import com.opennetwork.imservice.api.messages.multidevice.DeviceContact;
import com.opennetwork.imservice.api.messages.multidevice.DeviceContactsOutputStream;
import com.opennetwork.imservice.api.messages.multidevice.OpenNetworkServiceSyncMessage;
import com.opennetwork.imservice.api.messages.multidevice.VerifiedMessage;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;

public class MultiDeviceProfileKeyUpdateJob extends MasterSecretJob implements InjectableType {

  private static final long serialVersionUID = 1L;
  private static final String TAG = MultiDeviceProfileKeyUpdateJob.class.getSimpleName();

  @Inject transient OpenNetworkServiceMessageSender messageSender;

  public MultiDeviceProfileKeyUpdateJob(Context context) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .withGroupId(MultiDeviceProfileKeyUpdateJob.class.getSimpleName())
                                .create());
  }

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException, UntrustedIdentityException {
    if (!TextSecurePreferences.isMultiDevice(getContext())) {
      Log.w(TAG, "Not multi device...");
      return;
    }

    Optional<byte[]>           profileKey = Optional.of(ProfileKeyUtil.getProfileKey(getContext()));
    ByteArrayOutputStream      baos       = new ByteArrayOutputStream();
    DeviceContactsOutputStream out        = new DeviceContactsOutputStream(baos);

    out.write(new DeviceContact(TextSecurePreferences.getLocalNumber(getContext()),
                                Optional.<String>absent(),
                                Optional.<OpenNetworkServiceAttachmentStream>absent(),
                                Optional.<String>absent(),
                                Optional.<VerifiedMessage>absent(),
                                profileKey));

    out.close();

    OpenNetworkServiceAttachmentStream attachmentStream = OpenNetworkServiceAttachment.newStreamBuilder()
                                                                            .withStream(new ByteArrayInputStream(baos.toByteArray()))
                                                                            .withContentType("application/octet-stream")
                                                                            .withLength(baos.toByteArray().length)
                                                                            .build();

    OpenNetworkServiceSyncMessage      syncMessage      = OpenNetworkServiceSyncMessage.forContacts(new ContactsMessage(attachmentStream, false));

    messageSender.sendMessage(syncMessage);
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof PushNetworkException) return true;
    return false;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "Profile key sync failed!");
  }
}
