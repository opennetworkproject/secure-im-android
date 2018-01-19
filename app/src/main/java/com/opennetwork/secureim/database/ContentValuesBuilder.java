package com.opennetwork.secureim.database;

import android.content.ContentValues;

import com.google.android.mms.pdu_alt.EncodedStringValue;

import com.opennetwork.secureim.util.Util;

public class ContentValuesBuilder {

  private final ContentValues contentValues;

  public ContentValuesBuilder(ContentValues contentValues) {
    this.contentValues = contentValues;
  }

  public void add(String key, String charsetKey, EncodedStringValue value) {
    if (value != null) {
      contentValues.put(key, Util.toIsoString(value.getTextString()));
      contentValues.put(charsetKey, value.getCharacterSet());
    }
  }

  public void add(String contentKey, byte[] value) {
    if (value != null) {
      contentValues.put(contentKey, Util.toIsoString(value));
    }
  }

  public void add(String contentKey, int b) {
    if (b != 0)
      contentValues.put(contentKey, b);
  }

  public void add(String contentKey, long value) {
    if (value != -1L)
      contentValues.put(contentKey, value);
  }

  public ContentValues getContentValues() {
    return contentValues;
  }
}
