package com.opennetwork.secureim;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.util.DynamicLanguage;
import com.opennetwork.secureim.util.DynamicTheme;


public class ImportExportActivity extends PassphraseRequiredActionBarActivity {

  private DynamicTheme    dynamicTheme    = new DynamicTheme();
  private DynamicLanguage dynamicLanguage = new DynamicLanguage();

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState, @NonNull MasterSecret masterSecret) {
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    initFragment(android.R.id.content, new ImportExportFragment(),
                 masterSecret, dynamicLanguage.getCurrentLocale());
  }

  @Override
  public void onResume() {
    dynamicTheme.onResume(this);
    super.onResume();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case android.R.id.home:  finish();  return true;
    }

    return false;
  }
}
