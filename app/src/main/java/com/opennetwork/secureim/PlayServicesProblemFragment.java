package com.opennetwork.secureim;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.common.GooglePlayServicesUtil;

public class PlayServicesProblemFragment extends DialogFragment {

  @Override
  public @NonNull Dialog onCreateDialog(@NonNull Bundle bundle) {
    int    code   = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(code, getActivity(), 9111);

    if (dialog == null) {
      return new AlertDialog.Builder(getActivity())
              .setNegativeButton(android.R.string.ok, null)
              .setMessage(R.string.PlayServicesProblemFragment_the_version_of_google_play_services_you_have_installed_is_not_functioning)
              .create();
    } else {
      return dialog;
    }
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    super.onCancel(dialog);
    finish();
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    super.onDismiss(dialog);
    finish();
  }

  private void finish() {
    Activity activity = getActivity();
    if (activity != null) activity.finish();
  }
  
}
