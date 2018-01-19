package com.opennetwork.secureim.dependencies;

import android.content.Context;

import com.opennetwork.secureim.crypto.storage.OpenNetworkProtocolStoreImpl;
import com.opennetwork.secureim.jobs.CleanPreKeysJob;
import com.opennetwork.libim.state.SignedPreKeyStore;

import dagger.Module;
import dagger.Provides;

@Module (complete = false, injects = {CleanPreKeysJob.class})
public class AxolotlStorageModule {

  private final Context context;

  public AxolotlStorageModule(Context context) {
    this.context = context;
  }

  @Provides SignedPreKeyStoreFactory provideSignedPreKeyStoreFactory() {
    return new SignedPreKeyStoreFactory() {
      @Override
      public SignedPreKeyStore create() {
        return new OpenNetworkProtocolStoreImpl(context);
      }
    };
  }

  public static interface SignedPreKeyStoreFactory {
    public SignedPreKeyStore create();
  }
}
