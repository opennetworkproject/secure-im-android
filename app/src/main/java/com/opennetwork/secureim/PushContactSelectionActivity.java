package com.opennetwork.secureim;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.opennetwork.secureim.crypto.MasterSecret;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity container for selecting a list of contacts.
 *
 */
public class PushContactSelectionActivity extends ContactSelectionActivity {

  private final static String TAG = PushContactSelectionActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle icicle, @NonNull MasterSecret masterSecret) {
    getIntent().putExtra(ContactSelectionListFragment.MULTI_SELECT, true);
    super.onCreate(icicle, masterSecret);

    getToolbar().setNavigationIcon(R.drawable.ic_check_white_24dp);
    getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent resultIntent = getIntent();
        List<String> selectedContacts = contactsFragment.getSelectedContacts();

        if (selectedContacts != null) {
          resultIntent.putStringArrayListExtra("contacts", new ArrayList<>(selectedContacts));
        }

        setResult(RESULT_OK, resultIntent);
        finish();
      }
    });
  }
}
