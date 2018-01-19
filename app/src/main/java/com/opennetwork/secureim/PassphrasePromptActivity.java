package com.opennetwork.secureim;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.opennetwork.secureim.components.AnimatingToggle;
import com.opennetwork.secureim.crypto.InvalidPassphraseException;
import com.opennetwork.secureim.crypto.MasterSecretUtil;
import com.opennetwork.secureim.util.DynamicIntroTheme;
import com.opennetwork.secureim.util.DynamicLanguage;
import com.opennetwork.secureim.crypto.MasterSecret;

/**
 * Activity that prompts for a user's passphrase.
 */
public class PassphrasePromptActivity extends PassphraseActivity {

  private DynamicIntroTheme dynamicTheme    = new DynamicIntroTheme();
  private DynamicLanguage   dynamicLanguage = new DynamicLanguage();

  private EditText        passphraseText;
  private ImageButton     showButton;
  private ImageButton     hideButton;
  private AnimatingToggle visibilityToggle;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.prompt_passphrase_activity);
    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.log_submit, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
    case R.id.menu_submit_debug_logs: handleLogSubmit(); return true;
    }

    return false;
  }

  private void handleLogSubmit() {
    Intent intent = new Intent(this, LogSubmitActivity.class);
    startActivity(intent);
  }

  private void handlePassphrase() {
    try {
      Editable text             = passphraseText.getText();
      String passphrase         = (text == null ? "" : text.toString());
      MasterSecret masterSecret = MasterSecretUtil.getMasterSecret(this, passphrase);

      setMasterSecret(masterSecret);
    } catch (InvalidPassphraseException ipe) {
      passphraseText.setText("");
      passphraseText.setError(
              getString(R.string.PassphrasePromptActivity_invalid_passphrase_exclamation));
    }
  }

  private void setPassphraseVisibility(boolean visibility) {
    int cursorPosition = passphraseText.getSelectionStart();
    if (visibility) {
      passphraseText.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    } else {
      passphraseText.setInputType(InputType.TYPE_CLASS_TEXT |
                                  InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }
    passphraseText.setSelection(cursorPosition);
  }

  private void initializeResources() {
    getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
    getSupportActionBar().setCustomView(R.layout.centered_app_title);

    ImageButton okButton = (ImageButton) findViewById(R.id.ok_button);

    showButton       = (ImageButton)     findViewById(R.id.passphrase_visibility);
    hideButton       = (ImageButton)     findViewById(R.id.passphrase_visibility_off);
    visibilityToggle = (AnimatingToggle) findViewById(R.id.button_toggle);
    passphraseText   = (EditText)        findViewById(R.id.passphrase_edit);
    SpannableString hint = new SpannableString("  " + getString(R.string.PassphrasePromptActivity_enter_passphrase));
    hint.setSpan(new RelativeSizeSpan(0.9f), 0, hint.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    hint.setSpan(new TypefaceSpan("sans-serif"), 0, hint.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

    passphraseText.setHint(hint);
    okButton.setOnClickListener(new OkButtonClickListener());
    showButton.setOnClickListener(new ShowButtonOnClickListener());
    hideButton.setOnClickListener(new HideButtonOnClickListener());
    passphraseText.setOnEditorActionListener(new PassphraseActionListener());
    passphraseText.setImeActionLabel(getString(R.string.prompt_passphrase_activity__unlock),
                                     EditorInfo.IME_ACTION_DONE);
  }

  private class PassphraseActionListener implements TextView.OnEditorActionListener {
    @Override
    public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent keyEvent) {
      if ((keyEvent == null && actionId == EditorInfo.IME_ACTION_DONE) ||
          (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
              (actionId == EditorInfo.IME_NULL)))
      {
        handlePassphrase();
        return true;
      } else if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_UP &&
                 actionId == EditorInfo.IME_NULL)
      {
        return true;
      }

      return false;
    }
  }

  private class OkButtonClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      handlePassphrase();
    }
  }

  private class ShowButtonOnClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      visibilityToggle.display(hideButton);
      setPassphraseVisibility(true);
    }
  }

  private class HideButtonOnClickListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      visibilityToggle.display(showButton);
      setPassphraseVisibility(false);
    }
  }

  @Override
  protected void cleanup() {
    this.passphraseText.setText("");
    System.gc();
  }
}
