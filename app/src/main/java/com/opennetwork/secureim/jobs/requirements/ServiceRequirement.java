package com.opennetwork.secureim.jobs.requirements;

import android.content.Context;

import com.opennetwork.secureim.sms.TelephonyServiceState;
import com.opennetwork.jobqueue.dependencies.ContextDependent;
import com.opennetwork.jobqueue.requirements.Requirement;

public class ServiceRequirement implements Requirement, ContextDependent {

  private static final String TAG = ServiceRequirement.class.getSimpleName();

  private transient Context context;

  public ServiceRequirement(Context context) {
    this.context  = context;
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    TelephonyServiceState telephonyServiceState = new TelephonyServiceState();
    return telephonyServiceState.isConnected(context);
  }
}
