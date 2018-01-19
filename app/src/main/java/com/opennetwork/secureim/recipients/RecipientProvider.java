package com.opennetwork.secureim.recipients;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.opennetwork.secureim.R;
import com.opennetwork.secureim.color.MaterialColor;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.GroupDatabase.GroupRecord;
import com.opennetwork.secureim.database.RecipientDatabase.RecipientSettings;
import com.opennetwork.secureim.database.RecipientDatabase.RegisteredState;
import com.opennetwork.secureim.database.RecipientDatabase.VibrateState;
import com.opennetwork.secureim.util.ListenableFutureTask;
import com.opennetwork.secureim.util.SoftHashMap;
import com.opennetwork.secureim.util.Util;
import com.opennetwork.libim.util.guava.Optional;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

class RecipientProvider {

  @SuppressWarnings("unused")
  private static final String TAG = RecipientProvider.class.getSimpleName();

  private static final RecipientCache  recipientCache         = new RecipientCache();
  private static final ExecutorService asyncRecipientResolver = Util.newSingleThreadedLifoExecutor();

  private static final Map<String, RecipientDetails> STATIC_DETAILS = new HashMap<String, RecipientDetails>() {{
    put("262966", new RecipientDetails("Amazon", null, false, null, null));
  }};

  @NonNull Recipient getRecipient(@NonNull Context context, @NonNull Address address, @NonNull Optional<RecipientSettings> settings, @NonNull Optional<GroupRecord> groupRecord, boolean asynchronous) {
    Recipient cachedRecipient = recipientCache.get(address);

    if (cachedRecipient != null && (asynchronous || !cachedRecipient.isResolving()) && ((!groupRecord.isPresent() && !settings.isPresent()) || !cachedRecipient.isResolving() || cachedRecipient.getName() != null)) {
      return cachedRecipient;
    }

    Optional<RecipientDetails> prefetchedRecipientDetails = createPrefetchedRecipientDetails(context, address, settings, groupRecord);

    if (asynchronous) {
      cachedRecipient = new Recipient(address, cachedRecipient, prefetchedRecipientDetails, getRecipientDetailsAsync(context, address, settings, groupRecord));
    } else {
      cachedRecipient = new Recipient(address, getRecipientDetailsSync(context, address, settings, groupRecord, false));
    }

    recipientCache.set(address, cachedRecipient);
    return cachedRecipient;
  }

  @NonNull Optional<Recipient> getCached(@NonNull Address address) {
    return Optional.fromNullable(recipientCache.get(address));
  }

  private @NonNull Optional<RecipientDetails> createPrefetchedRecipientDetails(@NonNull Context context, @NonNull Address address,
                                                                               @NonNull Optional<RecipientSettings> settings,
                                                                               @NonNull Optional<GroupRecord> groupRecord)
  {
    if (address.isGroup() && settings.isPresent() && groupRecord.isPresent()) {
      return Optional.of(getGroupRecipientDetails(context, address, groupRecord, settings, true));
    } else if (!address.isGroup() && settings.isPresent()) {
      return Optional.of(new RecipientDetails(null, null, !TextUtils.isEmpty(settings.get().getSystemDisplayName()), settings.get(), null));
    }

    return Optional.absent();
  }

  private @NonNull ListenableFutureTask<RecipientDetails> getRecipientDetailsAsync(final Context context, final @NonNull Address address, final @NonNull Optional<RecipientSettings> settings, final @NonNull Optional<GroupRecord> groupRecord)
  {
    Callable<RecipientDetails> task = () -> getRecipientDetailsSync(context, address, settings, groupRecord, true);

    ListenableFutureTask<RecipientDetails> future = new ListenableFutureTask<>(task);
    asyncRecipientResolver.submit(future);
    return future;
  }

  private @NonNull RecipientDetails getRecipientDetailsSync(Context context, @NonNull Address address, Optional<RecipientSettings> settings, Optional<GroupRecord> groupRecord, boolean nestedAsynchronous) {
    if (address.isGroup()) return getGroupRecipientDetails(context, address, groupRecord, settings, nestedAsynchronous);
    else                   return getIndividualRecipientDetails(context, address, settings);
  }

  private @NonNull RecipientDetails getIndividualRecipientDetails(Context context, @NonNull Address address, Optional<RecipientSettings> settings) {
    if (!settings.isPresent()) {
      settings = DatabaseFactory.getRecipientDatabase(context).getRecipientSettings(address);
    }

    if (!settings.isPresent() && STATIC_DETAILS.containsKey(address.serialize())) {
      return STATIC_DETAILS.get(address.serialize());
    } else {
      boolean systemContact = settings.isPresent() && !TextUtils.isEmpty(settings.get().getSystemDisplayName());
      return new RecipientDetails(null, null, systemContact, settings.orNull(), null);
    }
  }

  private @NonNull RecipientDetails getGroupRecipientDetails(Context context, Address groupId, Optional<GroupRecord> groupRecord, Optional<RecipientSettings> settings, boolean asynchronous) {

    if (!groupRecord.isPresent()) {
      groupRecord = DatabaseFactory.getGroupDatabase(context).getGroup(groupId.toGroupString());
    }

    if (!settings.isPresent()) {
      settings = DatabaseFactory.getRecipientDatabase(context).getRecipientSettings(groupId);
    }

    if (groupRecord.isPresent()) {
      String          title           = groupRecord.get().getTitle();
      List<Address>   memberAddresses = groupRecord.get().getMembers();
      List<Recipient> members         = new LinkedList<>();
      Long            avatarId        = null;

      for (Address memberAddress : memberAddresses) {
        members.add(getRecipient(context, memberAddress, Optional.absent(), Optional.absent(), asynchronous));
      }

      if (!groupId.isMmsGroup() && title == null) {
        title = context.getString(R.string.RecipientProvider_unnamed_group);
      }

      if (groupRecord.get().getAvatar() != null && groupRecord.get().getAvatar().length > 0) {
        avatarId = groupRecord.get().getAvatarId();
      }

      return new RecipientDetails(title, avatarId, false, settings.orNull(), members);
    }

    return new RecipientDetails(context.getString(R.string.RecipientProvider_unnamed_group), null, false, settings.orNull(), null);
  }

  static class RecipientDetails {
    @Nullable final String               name;
    @Nullable final String               customLabel;
    @Nullable final Uri                  systemContactPhoto;
    @Nullable final Uri                  contactUri;
    @Nullable final Long                 groupAvatarId;
    @Nullable final MaterialColor        color;
    @Nullable final Uri                  ringtone;
              final long                 mutedUntil;
    @Nullable final VibrateState         vibrateState;
              final boolean              blocked;
              final int                  expireMessages;
    @NonNull  final List<Recipient>      participants;
    @Nullable final String               profileName;
              final boolean              seenInviteReminder;
              final Optional<Integer>    defaultSubscriptionId;
    @NonNull  final RegisteredState      registered;
    @Nullable final byte[]               profileKey;
    @Nullable final String               profileAvatar;
              final boolean              profileSharing;
              final boolean              systemContact;

    RecipientDetails(@Nullable String name, @Nullable Long groupAvatarId,
                     boolean systemContact, @Nullable RecipientSettings settings,
                     @Nullable List<Recipient> participants)
    {
      this.groupAvatarId         = groupAvatarId;
      this.systemContactPhoto    = settings     != null ? Util.uri(settings.getSystemContactPhotoUri()) : null;
      this.customLabel           = settings     != null ? settings.getSystemPhoneLabel() : null;
      this.contactUri            = settings     != null ? Util.uri(settings.getSystemContactUri()) : null;
      this.color                 = settings     != null ? settings.getColor() : null;
      this.ringtone              = settings     != null ? settings.getRingtone() : null;
      this.mutedUntil            = settings     != null ? settings.getMuteUntil() : 0;
      this.vibrateState          = settings     != null ? settings.getVibrateState() : null;
      this.blocked               = settings     != null && settings.isBlocked();
      this.expireMessages        = settings     != null ? settings.getExpireMessages() : 0;
      this.participants          = participants == null ? new LinkedList<>() : participants;
      this.profileName           = settings     != null ? settings.getProfileName() : null;
      this.seenInviteReminder    = settings     != null && settings.hasSeenInviteReminder();
      this.defaultSubscriptionId = settings     != null ? settings.getDefaultSubscriptionId() : Optional.absent();
      this.registered            = settings     != null ? settings.getRegistered() : RegisteredState.UNKNOWN;
      this.profileKey            = settings     != null ? settings.getProfileKey() : null;
      this.profileAvatar         = settings     != null ? settings.getProfileAvatar() : null;
      this.profileSharing        = settings     != null && settings.isProfileSharing();
      this.systemContact         = systemContact;

      if (name == null && settings != null) this.name = settings.getSystemDisplayName();
      else                                  this.name = name;
    }
  }

  private static class RecipientCache {

    private final Map<Address,Recipient> cache = new SoftHashMap<>(1000);

    public synchronized Recipient get(Address address) {
      return cache.get(address);
    }

    public synchronized void set(Address address, Recipient recipient) {
      cache.put(address, recipient);
    }

  }

}