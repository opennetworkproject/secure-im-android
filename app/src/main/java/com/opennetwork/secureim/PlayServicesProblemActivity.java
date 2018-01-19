package com.opennetwork.secureim;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class PlayServicesProblemActivity extends FragmentActivity {

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    PlayServicesProblemFragment fragment = new PlayServicesProblemFragment();
    fragment.show(getSupportFragmentManager(), "dialog");
  }
}
