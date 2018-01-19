package com.opennetwork.secureim;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import com.opennetwork.secureim.components.ContactFilterToolbar;
import com.opennetwork.secureim.components.ContactFilterToolbar.OnFilterChangedListener;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.util.DirectoryHelper;
import com.opennetwork.secureim.util.DynamicLanguage;
import com.opennetwork.secureim.util.DynamicNoActionBarTheme;
import com.opennetwork.secureim.util.DynamicTheme;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.secureim.util.ViewUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Base activity container for selecting a list of contacts.
 *
 */
public abstract class ContactSelectionActivity extends PassphraseRequiredActionBarActivity
                                               implements SwipeRefreshLayout.OnRefreshListener,
                                                          ContactSelectionListFragment.OnContactSelectedListener
{
  private static final String TAG = ContactSelectionActivity.class.getSimpleName();

  private final DynamicTheme    dynamicTheme    = new DynamicNoActionBarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  protected ContactSelectionListFragment contactsFragment;

  private   MasterSecret         masterSecret;
  private   ContactFilterToolbar toolbar;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, @NonNull MasterSecret masterSecret) {
    this.masterSecret = masterSecret;
    if (!getIntent().hasExtra(ContactSelectionListFragment.DISPLAY_MODE)) {
      getIntent().putExtra(ContactSelectionListFragment.DISPLAY_MODE,
                           TextSecurePreferences.isSmsEnabled(this)
                           ? ContactSelectionListFragment.DISPLAY_MODE_ALL
                           : ContactSelectionListFragment.DISPLAY_MODE_PUSH_ONLY);
    }

    setContentView(R.layout.contact_selection_activity);

    initializeToolbar();
    initializeResources();
    initializeSearch();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  protected ContactFilterToolbar getToolbar() {
    return toolbar;
  }

  private void initializeToolbar() {
    this.toolbar = ViewUtil.findById(this, R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    getSupportActionBar().setIcon(null);
    getSupportActionBar().setLogo(null);
  }

  private void initializeResources() {
    contactsFragment = (ContactSelectionListFragment) getSupportFragmentManager().findFragmentById(R.id.contact_selection_list_fragment);
    contactsFragment.setOnContactSelectedListener(this);
    contactsFragment.setOnRefreshListener(this);
  }

  private void initializeSearch() {
    toolbar.setOnFilterChangedListener(new OnFilterChangedListener() {
      @Override public void onFilterChanged(String filter) {
        contactsFragment.setQueryFilter(filter);
      }
    });
  }

  @Override
  public void onRefresh() {
    new RefreshDirectoryTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getApplicationContext());
  }

  @Override
  public void onContactSelected(String number) {}

  @Override
  public void onContactDeselected(String number) {}

  private static class RefreshDirectoryTask extends AsyncTask<Context, Void, Void> {

    private final WeakReference<ContactSelectionActivity> activity;
    private final MasterSecret masterSecret;

    private RefreshDirectoryTask(ContactSelectionActivity activity) {
      this.activity     = new WeakReference<>(activity);
      this.masterSecret = activity.masterSecret;
    }


    @Override
    protected Void doInBackground(Context... params) {

      try {
        DirectoryHelper.refreshDirectory(params[0], masterSecret, true);
      } catch (IOException e) {
        Log.w(TAG, e);
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      ContactSelectionActivity activity = this.activity.get();

      if (activity != null && !activity.isFinishing()) {
        activity.toolbar.clear();
        activity.contactsFragment.resetQueryFilter();
      }
    }
  }
}
