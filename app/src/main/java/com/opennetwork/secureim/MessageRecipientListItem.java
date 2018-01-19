package com.opennetwork.secureim;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.opennetwork.secureim.MessageDetailsRecipientAdapter.RecipientDeliveryStatus;
import com.opennetwork.secureim.components.AvatarImageView;
import com.opennetwork.secureim.components.DeliveryStatusView;
import com.opennetwork.secureim.components.FromTextView;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.DatabaseFactory;
import com.opennetwork.secureim.database.MmsDatabase;
import com.opennetwork.secureim.database.documents.IdentityKeyMismatch;
import com.opennetwork.secureim.database.documents.NetworkFailure;
import com.opennetwork.secureim.database.model.MessageRecord;
import com.opennetwork.secureim.mms.GlideRequests;
import com.opennetwork.secureim.recipients.Recipient;
import com.opennetwork.secureim.recipients.RecipientModifiedListener;
import com.opennetwork.secureim.sms.MessageSender;
import com.opennetwork.secureim.util.Util;

/**
 * A simple view to show the recipients of a message
 */
public class MessageRecipientListItem extends RelativeLayout
    implements RecipientModifiedListener
{
  private final static String TAG = MessageRecipientListItem.class.getSimpleName();

  private RecipientDeliveryStatus member;
  private GlideRequests           glideRequests;
  private FromTextView            fromView;
  private TextView                errorDescription;
  private TextView                actionDescription;
  private Button                  conflictButton;
  private Button                  resendButton;
  private AvatarImageView         contactPhotoImage;
  private DeliveryStatusView      deliveryStatusView;

  public MessageRecipientListItem(Context context) {
    super(context);
  }

  public MessageRecipientListItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.fromView           = findViewById(R.id.from);
    this.errorDescription   = findViewById(R.id.error_description);
    this.actionDescription  = findViewById(R.id.action_description);
    this.contactPhotoImage  = findViewById(R.id.contact_photo_image);
    this.conflictButton     = findViewById(R.id.conflict_button);
    this.resendButton       = findViewById(R.id.resend_button);
    this.deliveryStatusView = findViewById(R.id.delivery_status);
  }

  public void set(final MasterSecret masterSecret,
                  final GlideRequests glideRequests,
                  final MessageRecord record,
                  final RecipientDeliveryStatus member,
                  final boolean isPushGroup)
  {
    this.glideRequests = glideRequests;
    this.member        = member;

    member.getRecipient().addListener(this);
    fromView.setText(member.getRecipient());
    contactPhotoImage.setAvatar(glideRequests, member.getRecipient(), false);
    setIssueIndicators(masterSecret, record, isPushGroup);
  }

  private void setIssueIndicators(final MasterSecret masterSecret,
                                  final MessageRecord record,
                                  final boolean isPushGroup)
  {
    final NetworkFailure      networkFailure = getNetworkFailure(record);
    final IdentityKeyMismatch keyMismatch    = networkFailure == null ? getKeyMismatch(record) : null;

    String errorText = "";

    if (keyMismatch != null) {
      resendButton.setVisibility(View.GONE);
      conflictButton.setVisibility(View.VISIBLE);

      errorText = getContext().getString(R.string.MessageDetailsRecipient_new_safety_number);
      conflictButton.setOnClickListener(v -> new ConfirmIdentityDialog(getContext(), masterSecret, record, keyMismatch).show());
    } else if (networkFailure != null || (!isPushGroup && record.isFailed())) {
      resendButton.setVisibility(View.VISIBLE);
      resendButton.setEnabled(true);
      resendButton.requestFocus();
      conflictButton.setVisibility(View.GONE);

      errorText = getContext().getString(R.string.MessageDetailsRecipient_failed_to_send);
      resendButton.setOnClickListener(v -> {
        resendButton.setVisibility(View.GONE);
        errorDescription.setVisibility(View.GONE);
        actionDescription.setVisibility(View.VISIBLE);
        actionDescription.setText(R.string.message_recipients_list_item__resending);
        new ResendAsyncTask(masterSecret, record, networkFailure).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      });
    } else {
      if (record.isOutgoing()) {
        if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.PENDING || member.getDeliveryStatus() == RecipientDeliveryStatus.Status.UNKNOWN) {
          deliveryStatusView.setVisibility(View.GONE);
        } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.READ) {
          deliveryStatusView.setRead();
          deliveryStatusView.setVisibility(View.VISIBLE);
        } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.DELIVERED) {
          deliveryStatusView.setDelivered();
          deliveryStatusView.setVisibility(View.VISIBLE);
        } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.SENT) {
          deliveryStatusView.setSent();
          deliveryStatusView.setVisibility(View.VISIBLE);
        }
      } else {
        deliveryStatusView.setVisibility(View.GONE);
      }

      resendButton.setVisibility(View.GONE);
      conflictButton.setVisibility(View.GONE);
    }

    errorDescription.setText(errorText);
    errorDescription.setVisibility(TextUtils.isEmpty(errorText) ? View.GONE : View.VISIBLE);
  }

  private NetworkFailure getNetworkFailure(final MessageRecord record) {
    if (record.hasNetworkFailures()) {
      for (final NetworkFailure failure : record.getNetworkFailures()) {
        if (failure.getAddress().equals(member.getRecipient().getAddress())) {
          return failure;
        }
      }
    }
    return null;
  }

  private IdentityKeyMismatch getKeyMismatch(final MessageRecord record) {
    if (record.isIdentityMismatchFailure()) {
      for (final IdentityKeyMismatch mismatch : record.getIdentityKeyMismatches()) {
        if (mismatch.getAddress().equals(member.getRecipient().getAddress())) {
          return mismatch;
        }
      }
    }
    return null;
  }

  public void unbind() {
    if (this.member != null && this.member.getRecipient() != null) this.member.getRecipient().removeListener(this);
  }

  @Override
  public void onModified(final Recipient recipient) {
    Util.runOnMain(() -> {
      fromView.setText(recipient);
      contactPhotoImage.setAvatar(glideRequests, recipient, false);
    });
  }

  private class ResendAsyncTask extends AsyncTask<Void,Void,Void> {
    private final Context        context;
    private final MasterSecret   masterSecret;
    private final MessageRecord  record;
    private final NetworkFailure failure;

    ResendAsyncTask(MasterSecret masterSecret, MessageRecord record, NetworkFailure failure) {
      this.context      = getContext().getApplicationContext();
      this.masterSecret = masterSecret;
      this.record       = record;
      this.failure      = failure;
    }

    @Override
    protected Void doInBackground(Void... params) {
      MmsDatabase mmsDatabase = DatabaseFactory.getMmsDatabase(context);
      mmsDatabase.removeFailure(record.getId(), failure);

      if (record.getRecipient().isPushGroupRecipient()) {
        MessageSender.resendGroupMessage(context, record, failure.getAddress());
      } else {
        MessageSender.resend(context, masterSecret, record);
      }
      return null;
    }
  }

}
