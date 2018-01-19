package com.opennetwork.secureim.push;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;

import com.opennetwork.secureim.BuildConfig;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.imservice.api.OpenNetworkServiceAccountManager;

public class AccountManagerFactory {

  private static final String TAG = AccountManagerFactory.class.getName();

  public static OpenNetworkServiceAccountManager createManager(Context context) {
    return new OpenNetworkServiceAccountManager(new OpenNetworkServiceNetworkAccess(context).getConfiguration(context),
                                           TextSecurePreferences.getLocalNumber(context),
                                           TextSecurePreferences.getPushServerPassword(context),
                                           BuildConfig.USER_AGENT);
  }

  public static OpenNetworkServiceAccountManager createManager(final Context context, String number, String password) {
    if (new OpenNetworkServiceNetworkAccess(context).isCensored(number)) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          try {
            ProviderInstaller.installIfNeeded(context);
          } catch (Throwable t) {
            Log.w(TAG, t);
          }
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    return new OpenNetworkServiceAccountManager(new OpenNetworkServiceNetworkAccess(context).getConfiguration(number),
                                           number, password, BuildConfig.USER_AGENT);
  }

}
