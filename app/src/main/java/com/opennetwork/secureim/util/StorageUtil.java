package com.opennetwork.secureim.util;

import android.os.Environment;

import com.opennetwork.secureim.database.NoExternalStorageException;

import java.io.File;

public class StorageUtil
{
  private static File getOpenNetworkStorageDir() throws NoExternalStorageException {
    final File storage = Environment.getExternalStorageDirectory();

    if (!storage.canWrite()) {
      throw new NoExternalStorageException();
    }

    return storage;
  }

  public static boolean canWriteInOpenNetworkStorageDir() {
    File storage;

    try {
      storage = getOpenNetworkStorageDir();
    } catch (NoExternalStorageException e) {
      return false;
    }

    return storage.canWrite();
  }

  public static File getBackupDir() throws NoExternalStorageException {
    return getOpenNetworkStorageDir();
  }

  public static File getVideoDir() throws NoExternalStorageException {
    return new File(getOpenNetworkStorageDir(), Environment.DIRECTORY_MOVIES);
  }

  public static File getAudioDir() throws NoExternalStorageException {
    return new File(getOpenNetworkStorageDir(), Environment.DIRECTORY_MUSIC);
  }

  public static File getImageDir() throws NoExternalStorageException {
    return new File(getOpenNetworkStorageDir(), Environment.DIRECTORY_PICTURES);
  }

  public static File getDownloadDir() throws NoExternalStorageException {
    return new File(getOpenNetworkStorageDir(), Environment.DIRECTORY_DOWNLOADS);
  }
}
