package com.opennetwork.secureim.dependencies;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import com.opennetwork.secureim.BuildConfig;
import com.opennetwork.secureim.CreateProfileActivity;
import com.opennetwork.secureim.DeviceListFragment;
import com.opennetwork.secureim.crypto.storage.OpenNetworkProtocolStoreImpl;
import com.opennetwork.secureim.events.ReminderUpdateEvent;
import com.opennetwork.secureim.jobs.AttachmentDownloadJob;
import com.opennetwork.secureim.jobs.AvatarDownloadJob;
import com.opennetwork.secureim.jobs.CleanPreKeysJob;
import com.opennetwork.secureim.jobs.CreateSignedPreKeyJob;
import com.opennetwork.secureim.jobs.GcmRefreshJob;
import com.opennetwork.secureim.jobs.MultiDeviceBlockedUpdateJob;
import com.opennetwork.secureim.jobs.MultiDeviceContactUpdateJob;
import com.opennetwork.secureim.jobs.MultiDeviceGroupUpdateJob;
import com.opennetwork.secureim.jobs.MultiDeviceProfileKeyUpdateJob;
import com.opennetwork.secureim.jobs.MultiDeviceReadReceiptUpdateJob;
import com.opennetwork.secureim.jobs.MultiDeviceReadUpdateJob;
import com.opennetwork.secureim.jobs.MultiDeviceVerifiedUpdateJob;
import com.opennetwork.secureim.jobs.PushGroupSendJob;
import com.opennetwork.secureim.jobs.PushGroupUpdateJob;
import com.opennetwork.secureim.jobs.PushMediaSendJob;
import com.opennetwork.secureim.jobs.PushNotificationReceiveJob;
import com.opennetwork.secureim.jobs.PushTextSendJob;
import com.opennetwork.secureim.jobs.RefreshAttributesJob;
import com.opennetwork.secureim.jobs.RefreshPreKeysJob;
import com.opennetwork.secureim.jobs.RequestGroupInfoJob;
import com.opennetwork.secureim.jobs.RetrieveProfileAvatarJob;
import com.opennetwork.secureim.jobs.RetrieveProfileJob;
import com.opennetwork.secureim.jobs.RotateSignedPreKeyJob;
import com.opennetwork.secureim.jobs.SendReadReceiptJob;
import com.opennetwork.secureim.push.SecurityEventListener;
import com.opennetwork.secureim.push.OpenNetworkServiceNetworkAccess;
import com.opennetwork.secureim.service.MessageRetrievalService;
import com.opennetwork.secureim.service.WebRtcCallService;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.OpenNetworkServiceAccountManager;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageReceiver;
import com.opennetwork.imservice.api.OpenNetworkServiceMessageSender;
import com.opennetwork.imservice.api.util.CredentialsProvider;
import com.opennetwork.imservice.api.websocket.ConnectivityListener;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {CleanPreKeysJob.class,
                                     CreateSignedPreKeyJob.class,
                                     PushGroupSendJob.class,
                                     PushTextSendJob.class,
                                     PushMediaSendJob.class,
                                     AttachmentDownloadJob.class,
                                     RefreshPreKeysJob.class,
                                     MessageRetrievalService.class,
                                     PushNotificationReceiveJob.class,
                                     MultiDeviceContactUpdateJob.class,
                                     MultiDeviceGroupUpdateJob.class,
                                     MultiDeviceReadUpdateJob.class,
                                     MultiDeviceBlockedUpdateJob.class,
                                     DeviceListFragment.class,
                                     RefreshAttributesJob.class,
                                     GcmRefreshJob.class,
                                     RequestGroupInfoJob.class,
                                     PushGroupUpdateJob.class,
                                     AvatarDownloadJob.class,
                                     RotateSignedPreKeyJob.class,
                                     WebRtcCallService.class,
                                     RetrieveProfileJob.class,
                                     MultiDeviceVerifiedUpdateJob.class,
                                     CreateProfileActivity.class,
                                     RetrieveProfileAvatarJob.class,
                                     MultiDeviceProfileKeyUpdateJob.class,
                                     SendReadReceiptJob.class,
                                     MultiDeviceReadReceiptUpdateJob.class})
public class OpenNetworkCommunicationModule {

  private static final String TAG = OpenNetworkCommunicationModule.class.getSimpleName();

  private final Context                      context;
  private final OpenNetworkServiceNetworkAccess   networkAccess;

  private OpenNetworkServiceAccountManager  accountManager;
  private OpenNetworkServiceMessageSender   messageSender;
  private OpenNetworkServiceMessageReceiver messageReceiver;

  public OpenNetworkCommunicationModule(Context context, OpenNetworkServiceNetworkAccess networkAccess) {
    this.context       = context;
    this.networkAccess = networkAccess;
  }

  @Provides
  synchronized OpenNetworkServiceAccountManager provideOpenNetworkAccountManager() {
    if (this.accountManager == null) {
      this.accountManager = new OpenNetworkServiceAccountManager(networkAccess.getConfiguration(context),
                                                            new DynamicCredentialsProvider(context),
                                                            BuildConfig.USER_AGENT);
    }

    return this.accountManager;
  }

  @Provides
  synchronized OpenNetworkServiceMessageSender provideOpenNetworkMessageSender() {
    if (this.messageSender == null) {
      this.messageSender = new OpenNetworkServiceMessageSender(networkAccess.getConfiguration(context),
                                                          new DynamicCredentialsProvider(context),
                                                          new OpenNetworkProtocolStoreImpl(context),
                                                          BuildConfig.USER_AGENT,
                                                          Optional.fromNullable(MessageRetrievalService.getPipe()),
                                                          Optional.of(new SecurityEventListener(context)));
    } else {
      this.messageSender.setMessagePipe(MessageRetrievalService.getPipe());
    }

    return this.messageSender;
  }

  @Provides
  synchronized OpenNetworkServiceMessageReceiver provideOpenNetworkMessageReceiver() {
    if (this.messageReceiver == null) {
      this.messageReceiver = new OpenNetworkServiceMessageReceiver(networkAccess.getConfiguration(context),
                                                              new DynamicCredentialsProvider(context),
                                                              BuildConfig.USER_AGENT,
                                                              new PipeConnectivityListener());
    }

    return this.messageReceiver;
  }

  private static class DynamicCredentialsProvider implements CredentialsProvider {

    private final Context context;

    private DynamicCredentialsProvider(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public String getUser() {
      return TextSecurePreferences.getLocalNumber(context);
    }

    @Override
    public String getPassword() {
      return TextSecurePreferences.getPushServerPassword(context);
    }

    @Override
    public String getOpenNetworkingKey() {
      return TextSecurePreferences.getOpenNetworkingKey(context);
    }
  }

  private class PipeConnectivityListener implements ConnectivityListener {

    @Override
    public void onConnected() {
      Log.w(TAG, "onConnected()");
    }

    @Override
    public void onConnecting() {
      Log.w(TAG, "onConnecting()");
    }

    @Override
    public void onDisconnected() {
      Log.w(TAG, "onDisconnected()");
    }

    @Override
    public void onAuthenticationFailure() {
      Log.w(TAG, "onAuthenticationFailure()");
      TextSecurePreferences.setUnauthorizedReceived(context, true);
      EventBus.getDefault().post(new ReminderUpdateEvent());
    }

  }

}
