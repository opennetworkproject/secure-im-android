package com.opennetwork.secureim;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.opennetwork.secureim.crypto.IdentityKeyUtil;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.crypto.MasterSecretUtil;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.secureim.util.Util;
import com.opennetwork.secureim.util.VersionTracker;

/**
 * Activity for creating a user's local encryption passphrase.
 */

public class PassphraseCreateActivity extends PassphraseActivity {

  public PassphraseCreateActivity() { }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.create_passphrase_activity);

    initializeResources();
  }

  private void initializeResources() {
    new SecretGenerator().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
  }

  private class SecretGenerator extends AsyncTask<String, Void, Void> {
    private MasterSecret   masterSecret;

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Void doInBackground(String... params) {
      String passphrase = params[0];
      masterSecret      = MasterSecretUtil.generateMasterSecret(PassphraseCreateActivity.this,
                                                                passphrase);

      MasterSecretUtil.generateAsymmetricMasterSecret(PassphraseCreateActivity.this, masterSecret);
      IdentityKeyUtil.generateIdentityKeys(PassphraseCreateActivity.this);
      VersionTracker.updateLastSeenVersion(PassphraseCreateActivity.this);
      TextSecurePreferences.setLastExperienceVersionCode(PassphraseCreateActivity.this, Util.getCurrentApkReleaseVersion(PassphraseCreateActivity.this));
      TextSecurePreferences.setPasswordDisabled(PassphraseCreateActivity.this, true);
      TextSecurePreferences.setReadReceiptsEnabled(PassphraseCreateActivity.this, true);

      return null;
    }

    @Override
    protected void onPostExecute(Void param) {
      setMasterSecret(masterSecret);
    }
  }

  @Override
  protected void cleanup() {
    System.gc();
  }
}
