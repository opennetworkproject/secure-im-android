package com.opennetwork.secureim.jobs.requirements;

import android.content.Context;

import com.opennetwork.jobqueue.dependencies.ContextDependent;
import com.opennetwork.jobqueue.requirements.NetworkRequirement;
import com.opennetwork.jobqueue.requirements.Requirement;

public class NetworkOrServiceRequirement implements Requirement, ContextDependent {

  private transient Context context;

  public NetworkOrServiceRequirement(Context context) {
    this.context = context;
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    NetworkRequirement networkRequirement = new NetworkRequirement(context);
    ServiceRequirement serviceRequirement = new ServiceRequirement(context);

    return networkRequirement.isPresent() || serviceRequirement.isPresent();
  }
}
