package com.opennetwork.secureim.scribbles;


import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.opennetwork.secureim.util.AsyncLoader;

import java.io.IOException;

class StickerLoader extends AsyncLoader<String[]> {

  private static final String TAG = StickerLoader.class.getName();

  private final String assetDirectory;

  StickerLoader(Context context, String assetDirectory) {
    super(context);
    this.assetDirectory = assetDirectory;
  }

  @Override
  public @NonNull
  String[] loadInBackground() {
    try {
      String[] files = getContext().getAssets().list(assetDirectory);

      for (int i=0;i<files.length;i++) {
        files[i] = assetDirectory + "/" + files[i];
      }

      return files;
    } catch (IOException e) {
      Log.w(TAG, e);
    }

    return new String[0];
  }
}
