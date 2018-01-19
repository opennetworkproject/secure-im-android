package com.opennetwork.secureim.preferences;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

import com.opennetwork.secureim.PassphraseRequiredActionBarActivity;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.util.DynamicLanguage;
import com.opennetwork.secureim.util.DynamicTheme;

public class MmsPreferencesActivity extends PassphraseRequiredActionBarActivity {

  private final DynamicTheme dynamicTheme       = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, @NonNull MasterSecret masterSecret) {
    this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    Fragment fragment = new MmsPreferencesFragment();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.replace(android.R.id.content, fragment);
    fragmentTransaction.commit();

  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
    }

    return false;
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
  }

}
