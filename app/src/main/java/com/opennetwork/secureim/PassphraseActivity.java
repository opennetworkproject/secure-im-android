package com.opennetwork.secureim;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.service.KeyCachingService;


/**
 * Base Activity for changing/prompting local encryption passphrase.
 */
public abstract class PassphraseActivity extends BaseActionBarActivity {

  private KeyCachingService keyCachingService;
  private MasterSecret masterSecret;

  protected void setMasterSecret(MasterSecret masterSecret) {
    this.masterSecret = masterSecret;
    Intent bindIntent = new Intent(this, KeyCachingService.class);
    startService(bindIntent);
    bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
  }

  protected abstract void cleanup();

  private ServiceConnection serviceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName className, IBinder service) {
        keyCachingService = ((KeyCachingService.KeySetBinder)service).getService();
        keyCachingService.setMasterSecret(masterSecret);

        PassphraseActivity.this.unbindService(PassphraseActivity.this.serviceConnection);

        masterSecret = null;
        cleanup();

        Intent nextIntent = getIntent().getParcelableExtra("next_intent");
        if (nextIntent != null) {
            try {
                startActivity(nextIntent);
            } catch (java.lang.SecurityException e) {
                Log.w("PassphraseActivity",
                        "Access permission not passed from PassphraseActivity, retry sharing.");
            }
        }
        finish();
      }
      @Override
      public void onServiceDisconnected(ComponentName name) {
        keyCachingService = null;
      }
  };
}
