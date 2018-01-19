package com.opennetwork.secureim.jobs;

import android.content.Context;
import android.util.Log;

import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.jobqueue.Job;
import com.opennetwork.jobqueue.JobParameters;

public class TrimThreadJob extends Job {

  private static final String TAG = TrimThreadJob.class.getSimpleName();

  private final Context context;
  private final long    threadId;

  public TrimThreadJob(Context context, long threadId) {
    super(JobParameters.newBuilder().withGroupId(TrimThreadJob.class.getSimpleName()).create());
    this.context  = context;
    this.threadId = threadId;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onRun() {
    boolean trimmingEnabled   = TextSecurePreferences.isThreadLengthTrimmingEnabled(context);
    int     threadLengthLimit = TextSecurePreferences.getThreadTrimLength(context);

    if (!trimmingEnabled)
      return;

    DatabaseFactory.getThreadDatabase(context).trimThread(threadId, threadLengthLimit);
  }

  @Override
  public boolean onShouldRetry(Exception exception) {
    return false;
  }

  @Override
  public void onCanceled() {
    Log.w(TAG, "Canceling trim attempt: " + threadId);
  }
}
