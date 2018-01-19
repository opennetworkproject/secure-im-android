package com.opennetwork.secureim.jobs.requirements;

import android.content.Context;

import com.opennetwork.secureim.service.KeyCachingService;
import com.opennetwork.jobqueue.dependencies.ContextDependent;
import com.opennetwork.jobqueue.requirements.Requirement;

public class MasterSecretRequirement implements Requirement, ContextDependent {

  private transient Context context;

  public MasterSecretRequirement(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    return KeyCachingService.getMasterSecret(context) != null;
  }

  @Override
  public void setContext(Context context) {
    this.context = context;
  }
}
