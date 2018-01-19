package com.opennetwork.secureim.jobs;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.RecipientDatabase;
import com.opennetwork.secureim.dependencies.InjectableType;
import com.opennetwork.secureim.profiles.AvatarHelper;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.util.Util;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageReceiver;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

public class RetrieveProfileAvatarJob extends ContextJob implements InjectableType {

  private static final String TAG = RetrieveProfileAvatarJob.class.getSimpleName();

  private static final int MAX_PROFILE_SIZE_BYTES = 20 * 1024 * 1024;

  @Inject OpenNetworkServiceMessageReceiver receiver;

  private final String    profileAvatar;
  private final Recipient recipient;

  public RetrieveProfileAvatarJob(Context context, Recipient recipient, String profileAvatar) {
    super(context, JobParameters.newBuilder()
                                .withGroupId(RetrieveProfileAvatarJob.class.getSimpleName() + recipient.getAddress().serialize())
                                .withRequirement(new NetworkRequirement(context))
                                .create());

    this.recipient     = recipient;
    this.profileAvatar = profileAvatar;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException {
    RecipientDatabase database   = DatabaseFactory.getRecipientDatabase(context);
    byte[]            profileKey = recipient.resolve().getProfileKey();

    if (profileKey == null) {
      Log.w(TAG, "Recipient profile key is gone!");
      return;
    }

    if (Util.equals(profileAvatar, recipient.resolve().getProfileAvatar())) {
      Log.w(TAG, "Already retrieved profile avatar: " + profileAvatar);
      return;
    }

    if (TextUtils.isEmpty(profileAvatar)) {
      Log.w(TAG, "Removing profile avatar for: " + recipient.getAddress().serialize());
      AvatarHelper.delete(context, recipient.getAddress());
      database.setProfileAvatar(recipient, profileAvatar);
      return;
    }

    File downloadDestination = File.createTempFile("avatar", "jpg", context.getCacheDir());

    try {
      InputStream avatarStream       = receiver.retrieveProfileAvatar(profileAvatar, downloadDestination, profileKey, MAX_PROFILE_SIZE_BYTES);
      File        decryptDestination = File.createTempFile("avatar", "jpg", context.getCacheDir());

      Util.copy(avatarStream, new FileOutputStream(decryptDestination));
      decryptDestination.renameTo(AvatarHelper.getAvatarFile(context, recipient.getAddress()));
    } finally {
      if (downloadDestination != null) downloadDestination.delete();
    }

    database.setProfileAvatar(recipient, profileAvatar);
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    Log.w(TAG, e);
    if (e instanceof PushNetworkException) return true;
    return false;
  }

  @Override
  public void onCanceled() {

  }
}
