package com.opennetwork.secureim;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.soundcloud.android.crop.Crop;

import com.opennetwork.secureim.components.PushRecipientsPanel;
import com.opennetwork.secureim.components.PushRecipientsPanel.RecipientsPanelChangedListener;
import com.opennetwork.secureim.contacts.RecipientsEditor;
import com.opennetwork.secureim.contacts.avatars.ContactColors;
import com.opennetwork.secureim.contacts.avatars.ResourceContactPhoto;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.GroupDatabase;
import com.opennetwork.secureim.database.GroupDatabase.GroupRecord;
import com.opennetwork.secureim.database.RecipientDatabase;
import com.opennetwork.secureim.database.ThreadDatabase;
import com.opennetwork.secureim.groups.GroupManager;
import com.opennetwork.secureim.groups.GroupManager.GroupActionResult;
import com.opennetwork.secureim.mms.GlideApp;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.util.BitmapUtil;
import com.opennetwork.secureim.util.DynamicLanguage;
import com.opennetwork.secureim.util.DynamicTheme;
import com.opennetwork.secureim.util.SelectedRecipientsAdapter;
import com.opennetwork.secureim.util.SelectedRecipientsAdapter.OnRecipientDeletedListener;
import com.opennetwork.secureim.util.TextSecurePreferences;
import com.opennetwork.secureim.util.ViewUtil;
import com.opennetwork.secureim.util.task.ProgressDialogAsyncTask;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.util.InvalidNumberException;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GroupCreateActivity extends PassphraseRequiredActionBarActivity
                                 implements OnRecipientDeletedListener,
                                            RecipientsPanelChangedListener
{

  private final static String TAG = GroupCreateActivity.class.getSimpleName();

  public static final String GROUP_ADDRESS_EXTRA = "group_recipient";
  public static final String GROUP_THREAD_EXTRA  = "group_thread";

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private static final int PICK_CONTACT = 1;
  public static final  int AVATAR_SIZE  = 210;

  private EditText     groupName;
  private ListView     lv;
  private ImageView    avatar;
  private TextView     creatingText;
  private MasterSecret masterSecret;
  private Bitmap       avatarBmp;

  @NonNull private Optional<GroupData> groupToUpdate = Optional.absent();

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle state, @NonNull MasterSecret masterSecret) {
    this.masterSecret = masterSecret;

    setContentView(R.layout.group_create_activity);
    //noinspection ConstantConditions
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    initializeResources();
    initializeExistingGroup();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
    updateViewState();
  }

  private boolean isOpenNetworkGroup() {
    return TextSecurePreferences.isPushRegistered(this) && !getAdapter().hasNonPushMembers();
  }

  private void disableOpenNetworkGroupViews(int reasonResId) {
    View pushDisabled = findViewById(R.id.push_disabled);
    pushDisabled.setVisibility(View.VISIBLE);
    ((TextView) findViewById(R.id.push_disabled_reason)).setText(reasonResId);
    avatar.setEnabled(false);
    groupName.setEnabled(false);
  }

  private void enableOpenNetworkGroupViews() {
    findViewById(R.id.push_disabled).setVisibility(View.GONE);
    avatar.setEnabled(true);
    groupName.setEnabled(true);
  }

  @SuppressWarnings("ConstantConditions")
  private void updateViewState() {
    if (!TextSecurePreferences.isPushRegistered(this)) {
      disableOpenNetworkGroupViews(R.string.GroupCreateActivity_youre_not_registered_for_opennetwork);
      getSupportActionBar().setTitle(R.string.GroupCreateActivity_actionbar_mms_title);
    } else if (getAdapter().hasNonPushMembers()) {
      disableOpenNetworkGroupViews(R.string.GroupCreateActivity_contacts_dont_support_push);
      getSupportActionBar().setTitle(R.string.GroupCreateActivity_actionbar_mms_title);
    } else {
      enableOpenNetworkGroupViews();
      getSupportActionBar().setTitle(groupToUpdate.isPresent()
                                     ? R.string.GroupCreateActivity_actionbar_edit_title
                                     : R.string.GroupCreateActivity_actionbar_title);
    }
  }

  private static boolean isActiveInDirectory(Recipient recipient) {
    return recipient.resolve().getRegistered() == RecipientDatabase.RegisteredState.REGISTERED;
  }

  private void addSelectedContacts(@NonNull Recipient... recipients) {
    new AddMembersTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipients);
  }

  private void addSelectedContacts(@NonNull Collection<Recipient> recipients) {
    addSelectedContacts(recipients.toArray(new Recipient[recipients.size()]));
  }

  private void initializeResources() {
    RecipientsEditor    recipientsEditor = ViewUtil.findById(this, R.id.recipients_text);
    PushRecipientsPanel recipientsPanel  = ViewUtil.findById(this, R.id.recipients);
    lv           = ViewUtil.findById(this, R.id.selected_contacts_list);
    avatar       = ViewUtil.findById(this, R.id.avatar);
    groupName    = ViewUtil.findById(this, R.id.group_name);
    creatingText = ViewUtil.findById(this, R.id.creating_group_text);
    SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(this);
    adapter.setOnRecipientDeletedListener(this);
    lv.setAdapter(adapter);
    recipientsEditor.setHint(R.string.recipients_panel__add_members);
    recipientsPanel.setPanelChangeListener(this);
    findViewById(R.id.contacts_button).setOnClickListener(new AddRecipientButtonListener());
    avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_group_white_24dp).asDrawable(this, ContactColors.UNKNOWN_COLOR.toConversationColor(this)));
    avatar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Crop.pickImage(GroupCreateActivity.this);
      }
    });
  }

  private void initializeExistingGroup() {
    final Address groupAddress = getIntent().getParcelableExtra(GROUP_ADDRESS_EXTRA);

    if (groupAddress != null) {
      new FillExistingGroupInfoAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, groupAddress.toGroupString());
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    inflater.inflate(R.menu.group_create, menu);
    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      case R.id.menu_create_group:
        if (groupToUpdate.isPresent()) handleGroupUpdate();
        else                           handleGroupCreate();
        return true;
    }

    return false;
  }

  @Override
  public void onRecipientDeleted(Recipient recipient) {
    getAdapter().remove(recipient);
    updateViewState();
  }

  @Override
  public void onRecipientsPanelUpdate(List<Recipient> recipients) {
    if (recipients != null && !recipients.isEmpty()) addSelectedContacts(recipients);
  }

  private void handleGroupCreate() {
    if (getAdapter().getCount() < 1) {
      Log.i(TAG, getString(R.string.GroupCreateActivity_contacts_no_members));
      Toast.makeText(getApplicationContext(), R.string.GroupCreateActivity_contacts_no_members, Toast.LENGTH_SHORT).show();
      return;
    }
    if (isOpenNetworkGroup()) {
      new CreateOpenNetworkGroupTask(this, masterSecret, avatarBmp, getGroupName(), getAdapter().getRecipients()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    } else {
      new CreateMmsGroupTask(this, masterSecret, getAdapter().getRecipients()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private void handleGroupUpdate() {
    new UpdateOpenNetworkGroupTask(this, masterSecret, groupToUpdate.get().id, avatarBmp,
                              getGroupName(), getAdapter().getRecipients()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void handleOpenConversation(long threadId, Recipient recipient) {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
    intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
    intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
    startActivity(intent);
    finish();
  }

  private SelectedRecipientsAdapter getAdapter() {
    return (SelectedRecipientsAdapter)lv.getAdapter();
  }

  private @Nullable String getGroupName() {
    return groupName.getText() != null ? groupName.getText().toString() : null;
  }

  @Override
  public void onActivityResult(int reqCode, int resultCode, final Intent data) {
    super.onActivityResult(reqCode, resultCode, data);
    Uri outputFile = Uri.fromFile(new File(getCacheDir(), "cropped"));

    if (data == null || resultCode != Activity.RESULT_OK)
      return;

    switch (reqCode) {
      case PICK_CONTACT:
        List<String> selected = data.getStringArrayListExtra("contacts");

        for (String contact : selected) {
          Address   address   = Address.fromExternal(this, contact);
          Recipient recipient = Recipient.from(this, address, false);

          addSelectedContacts(recipient);
        }
        break;

      case Crop.REQUEST_PICK:
        new Crop(data.getData()).output(outputFile).asSquare().start(this);
        break;
      case Crop.REQUEST_CROP:
        GlideApp.with(this)
                .asBitmap()
                .load(Crop.getOutput(data))
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .override(AVATAR_SIZE, AVATAR_SIZE)
                .into(new SimpleTarget<Bitmap>() {
                  @Override
                  public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    setAvatar(Crop.getOutput(data), resource);
                  }
                });
    }
  }

  private class AddRecipientButtonListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(GroupCreateActivity.this, PushContactSelectionActivity.class);
      if (groupToUpdate.isPresent()) intent.putExtra(ContactSelectionListFragment.DISPLAY_MODE,
                                                     ContactSelectionListFragment.DISPLAY_MODE_PUSH_ONLY);
      startActivityForResult(intent, PICK_CONTACT);
    }
  }

  private static class CreateMmsGroupTask extends AsyncTask<Void,Void,GroupActionResult> {
    private final GroupCreateActivity activity;
    private final MasterSecret        masterSecret;
    private final Set<Recipient>      members;

    public CreateMmsGroupTask(GroupCreateActivity activity, MasterSecret masterSecret, Set<Recipient> members) {
      this.activity     = activity;
      this.masterSecret = masterSecret;
      this.members      = members;
    }

    @Override
    protected GroupActionResult doInBackground(Void... avoid) {
      List<Address> memberAddresses = new LinkedList<>();

      for (Recipient recipient : members) {
        memberAddresses.add(recipient.getAddress());
      }

      String    groupId        = DatabaseFactory.getGroupDatabase(activity).getOrCreateGroupForMembers(memberAddresses, true);
      Recipient groupRecipient = Recipient.from(activity, Address.fromSerialized(groupId), true);
      long      threadId       = DatabaseFactory.getThreadDatabase(activity).getThreadIdFor(groupRecipient, ThreadDatabase.DistributionTypes.DEFAULT);

      return new GroupActionResult(groupRecipient, threadId);
    }

    @Override
    protected void onPostExecute(GroupActionResult result) {
      activity.handleOpenConversation(result.getThreadId(), result.getGroupRecipient());
    }

    @Override
    protected void onProgressUpdate(Void... values) {
      super.onProgressUpdate(values);
    }
  }

  private abstract static class OpenNetworkGroupTask extends AsyncTask<Void,Void,Optional<GroupActionResult>> {
    protected GroupCreateActivity activity;
    protected MasterSecret        masterSecret;
    protected Bitmap              avatar;
    protected Set<Recipient>      members;
    protected String              name;

    public OpenNetworkGroupTask(GroupCreateActivity activity,
                           MasterSecret        masterSecret,
                           Bitmap              avatar,
                           String              name,
                           Set<Recipient>      members)
    {
      this.activity     = activity;
      this.masterSecret = masterSecret;
      this.avatar       = avatar;
      this.name         = name;
      this.members      = members;
    }

    @Override
    protected void onPreExecute() {
      activity.findViewById(R.id.group_details_layout).setVisibility(View.GONE);
      activity.findViewById(R.id.creating_group_layout).setVisibility(View.VISIBLE);
      activity.findViewById(R.id.menu_create_group).setVisibility(View.GONE);
        final int titleResId = activity.groupToUpdate.isPresent()
                             ? R.string.GroupCreateActivity_updating_group
                             : R.string.GroupCreateActivity_creating_group;
        activity.creatingText.setText(activity.getString(titleResId, activity.getGroupName()));
      }

    @Override
    protected void onPostExecute(Optional<GroupActionResult> groupActionResultOptional) {
      if (activity.isFinishing()) return;
      activity.findViewById(R.id.group_details_layout).setVisibility(View.VISIBLE);
      activity.findViewById(R.id.creating_group_layout).setVisibility(View.GONE);
      activity.findViewById(R.id.menu_create_group).setVisibility(View.VISIBLE);
    }
  }

  private static class CreateOpenNetworkGroupTask extends OpenNetworkGroupTask {
    public CreateOpenNetworkGroupTask(GroupCreateActivity activity, MasterSecret masterSecret, Bitmap avatar, String name, Set<Recipient> members) {
      super(activity, masterSecret, avatar, name, members);
    }

    @Override
    protected Optional<GroupActionResult> doInBackground(Void... aVoid) {
      return Optional.of(GroupManager.createGroup(activity, masterSecret, members, avatar, name, false));
    }

    @Override
    protected void onPostExecute(Optional<GroupActionResult> result) {
      if (result.isPresent() && result.get().getThreadId() > -1) {
        if (!activity.isFinishing()) {
          activity.handleOpenConversation(result.get().getThreadId(), result.get().getGroupRecipient());
        }
      } else {
        super.onPostExecute(result);
        Toast.makeText(activity.getApplicationContext(),
                       R.string.GroupCreateActivity_contacts_invalid_number, Toast.LENGTH_LONG).show();
      }
    }
  }

  private static class UpdateOpenNetworkGroupTask extends OpenNetworkGroupTask {
    private String groupId;

    public UpdateOpenNetworkGroupTask(GroupCreateActivity activity,
                                 MasterSecret masterSecret, String groupId, Bitmap avatar, String name,
                                 Set<Recipient> members)
    {
      super(activity, masterSecret, avatar, name, members);
      this.groupId = groupId;
    }

    @Override
    protected Optional<GroupActionResult> doInBackground(Void... aVoid) {
      try {
        return Optional.of(GroupManager.updateGroup(activity, masterSecret, groupId, members, avatar, name));
      } catch (InvalidNumberException e) {
        return Optional.absent();
      }
    }

    @Override
    protected void onPostExecute(Optional<GroupActionResult> result) {
      if (result.isPresent() && result.get().getThreadId() > -1) {
        if (!activity.isFinishing()) {
          Intent intent = activity.getIntent();
          intent.putExtra(GROUP_THREAD_EXTRA, result.get().getThreadId());
          intent.putExtra(GROUP_ADDRESS_EXTRA, result.get().getGroupRecipient().getAddress());
          activity.setResult(RESULT_OK, intent);
          activity.finish();
        }
      } else {
        super.onPostExecute(result);
        Toast.makeText(activity.getApplicationContext(),
                       R.string.GroupCreateActivity_contacts_invalid_number, Toast.LENGTH_LONG).show();
      }
    }
  }

  private static class AddMembersTask extends AsyncTask<Recipient,Void,List<AddMembersTask.Result>> {
    static class Result {
      Optional<Recipient> recipient;
      boolean             isPush;
      String              reason;

      public Result(@Nullable Recipient recipient, boolean isPush, @Nullable String reason) {
        this.recipient = Optional.fromNullable(recipient);
        this.isPush    = isPush;
        this.reason    = reason;
      }
    }

    private GroupCreateActivity activity;
    private boolean             failIfNotPush;

    public AddMembersTask(@NonNull GroupCreateActivity activity) {
      this.activity      = activity;
      this.failIfNotPush = activity.groupToUpdate.isPresent();
    }

    @Override
    protected List<Result> doInBackground(Recipient... recipients) {
      final List<Result> results = new LinkedList<>();

      for (Recipient recipient : recipients) {
        boolean isPush = isActiveInDirectory(recipient);

        if (failIfNotPush && !isPush) {
          results.add(new Result(null, false, activity.getString(R.string.GroupCreateActivity_cannot_add_non_push_to_existing_group,
                                                                 recipient.toShortString())));
        } else if (TextUtils.equals(TextSecurePreferences.getLocalNumber(activity), recipient.getAddress().serialize())) {
          results.add(new Result(null, false, activity.getString(R.string.GroupCreateActivity_youre_already_in_the_group)));
        } else {
          results.add(new Result(recipient, isPush, null));
        }
      }
      return results;
    }

    @Override
    protected void onPostExecute(List<Result> results) {
      if (activity.isFinishing()) return;

      for (Result result : results) {
        if (result.recipient.isPresent()) {
          activity.getAdapter().add(result.recipient.get(), result.isPush);
        } else {
          Toast.makeText(activity, result.reason, Toast.LENGTH_SHORT).show();
        }
      }
      activity.updateViewState();
    }
  }

  private static class FillExistingGroupInfoAsyncTask extends ProgressDialogAsyncTask<String,Void,Optional<GroupData>> {
    private GroupCreateActivity activity;

    public FillExistingGroupInfoAsyncTask(GroupCreateActivity activity) {
      super(activity,
            R.string.GroupCreateActivity_loading_group_details,
            R.string.please_wait);
      this.activity = activity;
    }

    @Override
    protected Optional<GroupData> doInBackground(String... groupIds) {
      final GroupDatabase         db               = DatabaseFactory.getGroupDatabase(activity);
      final List<Recipient>       recipients       = db.getGroupMembers(groupIds[0], false);
      final Optional<GroupRecord> group            = db.getGroup(groupIds[0]);
      final Set<Recipient>        existingContacts = new HashSet<>(recipients.size());
      existingContacts.addAll(recipients);

      if (group.isPresent()) {
        return Optional.of(new GroupData(groupIds[0],
                                         existingContacts,
                                         BitmapUtil.fromByteArray(group.get().getAvatar()),
                                         group.get().getAvatar(),
                                         group.get().getTitle()));
      } else {
        return Optional.absent();
      }
    }

    @Override
    protected void onPostExecute(Optional<GroupData> group) {
      super.onPostExecute(group);

      if (group.isPresent() && !activity.isFinishing()) {
        activity.groupToUpdate = group;

        activity.groupName.setText(group.get().name);
        if (group.get().avatarBmp != null) {
          activity.setAvatar(group.get().avatarBytes, group.get().avatarBmp);
        }
        SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(activity, group.get().recipients);
        adapter.setOnRecipientDeletedListener(activity);
        activity.lv.setAdapter(adapter);
        activity.updateViewState();
      }
    }
  }

  private <T> void setAvatar(T model, Bitmap bitmap) {
    avatarBmp = bitmap;
    GlideApp.with(this)
            .load(model)
            .circleCrop()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(avatar);
  }

  private static class GroupData {
    String         id;
    Set<Recipient> recipients;
    Bitmap         avatarBmp;
    byte[]         avatarBytes;
    String         name;

    public GroupData(String id, Set<Recipient> recipients, Bitmap avatarBmp, byte[] avatarBytes, String name) {
      this.id          = id;
      this.recipients  = recipients;
      this.avatarBmp   = avatarBmp;
      this.avatarBytes = avatarBytes;
      this.name        = name;
    }
  }
}
