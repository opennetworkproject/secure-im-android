package com.opennetwork.secureim.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.MessagingDatabase.MarkedMessageInfo;
import com.opennetwork.libim.logging.Log;

import java.util.LinkedList;
import java.util.List;

public class AndroidAutoHeardReceiver extends MasterSecretBroadcastReceiver {

  public static final String TAG                   = AndroidAutoHeardReceiver.class.getSimpleName();
  public static final String HEARD_ACTION          = "com.opennetwork.securesms.notifications.ANDROID_AUTO_HEARD";
  public static final String THREAD_IDS_EXTRA      = "car_heard_thread_ids";
  public static final String NOTIFICATION_ID_EXTRA = "car_notification_id";

  @Override
  protected void onReceive(final Context context, Intent intent,
                           @Nullable final MasterSecret masterSecret)
  {
    if (!HEARD_ACTION.equals(intent.getAction()))
      return;

    final long[] threadIds = intent.getLongArrayExtra(THREAD_IDS_EXTRA);

    if (threadIds != null) {
      int notificationId = intent.getIntExtra(NOTIFICATION_ID_EXTRA, -1);
      NotificationManagerCompat.from(context).cancel(notificationId);

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          List<MarkedMessageInfo> messageIdsCollection = new LinkedList<>();

          for (long threadId : threadIds) {
            Log.i(TAG, "Marking meassage as read: " + threadId);
            List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(threadId, true);

            messageIdsCollection.addAll(messageIds);
          }

          MessageNotifier.updateNotification(context, masterSecret);
          MarkReadReceiver.process(context, messageIdsCollection);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
