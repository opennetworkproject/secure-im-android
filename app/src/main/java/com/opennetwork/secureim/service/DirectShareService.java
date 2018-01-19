package com.opennetwork.secureim.service;


import android.content.ComponentName;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.support.annotation.RequiresApi;

import com.opennetwork.secureim.ShareActivity;
import com.opennetwork.secureim.crypto.MasterCipher;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.ThreadDatabase;
import com.opennetwork.secureim.database.model.ThreadRecord;
import com.opennetwork.secureim.mms.GlideApp;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.util.BitmapUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiresApi(api = Build.VERSION_CODES.M)
public class DirectShareService extends ChooserTargetService {
  @Override
  public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
                                                 IntentFilter matchedFilter)
  {
    List<ChooserTarget> results        = new LinkedList<>();
    MasterSecret        masterSecret   = KeyCachingService.getMasterSecret(this);

    if (masterSecret == null) {
      return results;
    }

    ComponentName  componentName  = new ComponentName(this, ShareActivity.class);
    ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(this);
    Cursor         cursor         = threadDatabase.getDirectShareList();

    try {
      ThreadDatabase.Reader reader = threadDatabase.readerFor(cursor, new MasterCipher(masterSecret));
      ThreadRecord record;

      while ((record = reader.getNext()) != null && results.size() < 10) {
        try {
          Recipient recipient = Recipient.from(this, record.getRecipient().getAddress(), false);
          String    name      = recipient.toShortString();

          Bitmap avatar;

          if (recipient.getContactPhoto() != null) {
            avatar = GlideApp.with(this)
                             .asBitmap()
                             .load(recipient.getContactPhoto())
                             .circleCrop()
                             .submit(getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                     getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width))
                             .get();
          } else {
            avatar = BitmapUtil.createFromDrawable(recipient.getFallbackContactPhotoDrawable(this, false),
                                                   getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                                                   getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height));
          }

          Parcel parcel = Parcel.obtain();
          parcel.writeParcelable(recipient.getAddress(), 0);

          Bundle bundle = new Bundle();
          bundle.putLong(ShareActivity.EXTRA_THREAD_ID, record.getThreadId());
          bundle.putByteArray(ShareActivity.EXTRA_ADDRESS_MARSHALLED, parcel.marshall());
          bundle.putInt(ShareActivity.EXTRA_DISTRIBUTION_TYPE, record.getDistributionType());
          bundle.setClassLoader(getClassLoader());

          results.add(new ChooserTarget(name, Icon.createWithBitmap(avatar), 1.0f, componentName, bundle));
          parcel.recycle();
        } catch (InterruptedException | ExecutionException e) {
          throw new AssertionError(e);
        }
      }

      return results;
    } finally {
      if (cursor != null) cursor.close();
    }
  }
}
