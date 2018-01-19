package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.service.KeyCachingService;
import com.opennetwork.secureim.util.DirectoryHelper;
import com.opennetwork.jobqueue.JobParameters;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.imservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

public class DirectoryRefreshJob extends ContextJob {

  @Nullable private transient Recipient    recipient;
  @Nullable private transient MasterSecret masterSecret;
            private transient boolean      notifyOfNewUsers;

  public DirectoryRefreshJob(@NonNull Context context, boolean notifyOfNewUsers) {
    this(context, null, null, notifyOfNewUsers);
  }

  public DirectoryRefreshJob(@NonNull Context context,
                             @Nullable MasterSecret masterSecret,
                             @Nullable Recipient recipient,
                                       boolean notifyOfNewUsers)
  {
    super(context, JobParameters.newBuilder()
                                .withGroupId(DirectoryRefreshJob.class.getSimpleName())
                                .withRequirement(new NetworkRequirement(context))
                                .create());

    this.recipient        = recipient;
    this.masterSecret     = masterSecret;
    this.notifyOfNewUsers = notifyOfNewUsers;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws IOException {
    Log.w("DirectoryRefreshJob", "DirectoryRefreshJob.onRun()");
    PowerManager          powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    PowerManager.WakeLock wakeLock     = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Directory Refresh");

    try {
      wakeLock.acquire();
      if (recipient == null) {
        DirectoryHelper.refreshDirectory(context, KeyCachingService.getMasterSecret(context), notifyOfNewUsers);
      } else {
        DirectoryHelper.refreshDirectoryFor(context, masterSecret, recipient);
      }
    } finally {
      if (wakeLock.isHeld()) wakeLock.release();
    }
  }

  @Override
  public boolean onShouldRetry(Exception exception) {
    if (exception instanceof PushNetworkException) return true;
    return false;
  }

  @Override
  public void onCanceled() {}
}
