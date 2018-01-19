package com.opennetwork.secureim.contacts;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.opennetwork.secureim.R;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.util.Util;
import com.opennetwork.libim.util.guava.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Database to supply all types of contacts that TextSecure needs to know about
 */
public class ContactsDatabase {

  private static final String TAG              = ContactsDatabase.class.getSimpleName();
  private static final String CONTACT_MIMETYPE = "vnd.android.cursor.item/vnd.com.opennetwork.securesms.contact";
  private static final String CALL_MIMETYPE    = "vnd.android.cursor.item/vnd.com.opennetwork.securesms.call";
  private static final String SYNC             = "__TS";

  static final String NAME_COLUMN         = "name";
  static final String NUMBER_COLUMN       = "number";
  static final String NUMBER_TYPE_COLUMN  = "number_type";
  static final String LABEL_COLUMN        = "label";
  static final String CONTACT_TYPE_COLUMN = "contact_type";

  static final int NORMAL_TYPE  = 0;
  static final int PUSH_TYPE    = 1;
  static final int NEW_TYPE     = 2;
  static final int RECENT_TYPE  = 3;
  static final int DIVIDER_TYPE = 4;

  private final Context context;

  public ContactsDatabase(Context context) {
    this.context  = context;
  }

  public synchronized void setRegisteredUsers(@NonNull Account account,
                                              @NonNull List<Address> registeredAddressList,
                                              boolean remove)
      throws RemoteException, OperationApplicationException
  {
    Set<Address>                        registeredAddressSet = new HashSet<>();
    ArrayList<ContentProviderOperation> operations           = new ArrayList<>();
    Map<Address, OpenNetworkContact>         currentContacts      = getOpenNetworkRawContacts(account);

    for (Address registeredAddress : registeredAddressList) {
      registeredAddressSet.add(registeredAddress);

      if (!currentContacts.containsKey(registeredAddress)) {
        Optional<SystemContactInfo> systemContactInfo = getSystemContactInfo(registeredAddress);

        if (systemContactInfo.isPresent()) {
          Log.w(TAG, "Adding number: " + registeredAddress);
          addTextSecureRawContact(operations, account, systemContactInfo.get().number,
                                  systemContactInfo.get().name, systemContactInfo.get().id,
                                  true);
        }
      }
    }

    for (Map.Entry<Address, OpenNetworkContact> currentContactEntry : currentContacts.entrySet()) {
      if (!registeredAddressSet.contains(currentContactEntry.getKey())) {
        if (remove) {
          Log.w(TAG, "Removing number: " + currentContactEntry.getKey());
          removeTextSecureRawContact(operations, account, currentContactEntry.getValue().getId());
        }
      } else if (!currentContactEntry.getValue().isVoiceSupported()) {
        Log.w(TAG, "Adding voice support: " + currentContactEntry.getKey());
        addContactVoiceSupport(operations, currentContactEntry.getKey(), currentContactEntry.getValue().getId());
      } else if (!Util.isStringEquals(currentContactEntry.getValue().getRawDisplayName(),
                                      currentContactEntry.getValue().getAggregateDisplayName()))
      {
        Log.w(TAG, "Updating display name: " + currentContactEntry.getKey());
        updateDisplayName(operations, currentContactEntry.getValue().getAggregateDisplayName(), currentContactEntry.getValue().getId(), currentContactEntry.getValue().getDisplayNameSource());
      }
    }

    if (!operations.isEmpty()) {
      context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
    }
  }

  @NonNull Cursor querySystemContacts(@Nullable String filter) {
    Uri uri;

    if (!TextUtils.isEmpty(filter)) {
      uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(filter));
    } else {
      uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
    }

    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      uri = uri.buildUpon().appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true").build();
    }

    String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                       ContactsContract.CommonDataKinds.Phone.NUMBER,
                                       ContactsContract.CommonDataKinds.Phone.TYPE,
                                       ContactsContract.CommonDataKinds.Phone.LABEL};

    String sort = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

    Map<String, String> projectionMap = new HashMap<String, String>() {{
      put(NAME_COLUMN, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
      put(NUMBER_COLUMN, ContactsContract.CommonDataKinds.Phone.NUMBER);
      put(NUMBER_TYPE_COLUMN, ContactsContract.CommonDataKinds.Phone.TYPE);
      put(LABEL_COLUMN, ContactsContract.CommonDataKinds.Phone.LABEL);
    }};

    String formattedNumber = "REPLACE(REPLACE(REPLACE(REPLACE(data1,' ',''),'-',''),'(',''),')','')";
    String excludeSelection = "(" + formattedNumber +" NOT IN " +
            "(SELECT data1 FROM view_data WHERE "+formattedNumber+" = data1) " +
            "OR "+formattedNumber+" = data1)" +
            "AND " + formattedNumber + "NOT IN (SELECT "+formattedNumber+" FROM view_data where mimetype = '"+CONTACT_MIMETYPE+"')" ;

    String fallbackSelection = ContactsContract.Data.SYNC2 + " IS NULL OR " + ContactsContract.Data.SYNC2 + " != '" + SYNC + "'";

    Cursor cursor;

    try {
      cursor = context.getContentResolver().query(uri, projection, excludeSelection, null, sort);
    } catch (Exception e) {
      Log.w(TAG, e);
      cursor = context.getContentResolver().query(uri, projection, fallbackSelection, null, sort);
    }

    return new ProjectionMappingCursor(cursor, projectionMap,
                                       new Pair<String, Object>(CONTACT_TYPE_COLUMN, NORMAL_TYPE));
  }

  @NonNull Cursor queryTextSecureContacts(String filter) {
    String[] projection = new String[] {ContactsContract.Contacts.DISPLAY_NAME,
                                        ContactsContract.Data.DATA1};

    String  sort = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

    Map<String, String> projectionMap = new HashMap<String, String>(){{
      put(NAME_COLUMN, ContactsContract.Contacts.DISPLAY_NAME);
      put(NUMBER_COLUMN, ContactsContract.Data.DATA1);
    }};

    Cursor cursor;

    if (TextUtils.isEmpty(filter)) {
      cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                                                  projection,
                                                  ContactsContract.Data.MIMETYPE + " = ?",
                                                  new String[] {CONTACT_MIMETYPE},
                                                  sort);
    } else {
      cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                                                  projection,
                                                  ContactsContract.Data.MIMETYPE + " = ? AND (" + ContactsContract.Contacts.DISPLAY_NAME + " LIKE ? OR " + ContactsContract.Data.DATA1 + " LIKE ?)",
                                                  new String[] {CONTACT_MIMETYPE,
                                                                "%" + filter + "%", "%" + filter + "%"},
                                                  sort);
    }

    return new ProjectionMappingCursor(cursor, projectionMap,
                                       new Pair<String, Object>(LABEL_COLUMN, "TextSecure"),
                                       new Pair<String, Object>(NUMBER_TYPE_COLUMN, 0),
                                       new Pair<String, Object>(CONTACT_TYPE_COLUMN, PUSH_TYPE));

  }

  private void addContactVoiceSupport(List<ContentProviderOperation> operations,
                                      @NonNull Address address, long rawContactId)
  {
    operations.add(ContentProviderOperation.newUpdate(RawContacts.CONTENT_URI)
                                           .withSelection(RawContacts._ID + " = ?", new String[] {String.valueOf(rawContactId)})
                                           .withValue(RawContacts.SYNC4, "true")
                                           .build());

    operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build())
                                           .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                           .withValue(ContactsContract.Data.MIMETYPE, CALL_MIMETYPE)
                                           .withValue(ContactsContract.Data.DATA1, address.toPhoneString())
                                           .withValue(ContactsContract.Data.DATA2, context.getString(R.string.app_name))
                                           .withValue(ContactsContract.Data.DATA3, context.getString(R.string.ContactsDatabase_opennetwork_call_s, address.toPhoneString()))
                                           .withYieldAllowed(true)
                                           .build());
  }

  private void updateDisplayName(List<ContentProviderOperation> operations,
                                 @Nullable String displayName,
                                 long rawContactId, int displayNameSource)
  {
    Uri dataUri = ContactsContract.Data.CONTENT_URI.buildUpon()
                                                   .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                                   .build();

    if (displayNameSource != ContactsContract.DisplayNameSources.STRUCTURED_NAME) {
      operations.add(ContentProviderOperation.newInsert(dataUri)
                                             .withValue(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, rawContactId)
                                             .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                                             .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                             .build());
    } else {
      operations.add(ContentProviderOperation.newUpdate(dataUri)
                                             .withSelection(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                                                            new String[] {String.valueOf(rawContactId), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
                                             .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                                             .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                             .build());
    }
  }

  private void removeContactVoiceSupport(List<ContentProviderOperation> operations, long rawContactId) {
    operations.add(ContentProviderOperation.newUpdate(RawContacts.CONTENT_URI)
                                           .withSelection(RawContacts._ID + " = ?", new String[] {String.valueOf(rawContactId)})
                                           .withValue(RawContacts.SYNC4, "false")
                                           .build());

    operations.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build())
                                           .withSelection(ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                                                          new String[] {String.valueOf(rawContactId), CALL_MIMETYPE})
                                           .withYieldAllowed(true)
                                           .build());
  }

  private void addTextSecureRawContact(List<ContentProviderOperation> operations,
                                       Account account, String e164number, String displayName,
                                       long aggregateId, boolean supportsVoice)
  {
    int index   = operations.size();
    Uri dataUri = ContactsContract.Data.CONTENT_URI.buildUpon()
                                                   .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                                   .build();

    operations.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                                           .withValue(RawContacts.ACCOUNT_NAME, account.name)
                                           .withValue(RawContacts.ACCOUNT_TYPE, account.type)
                                           .withValue(RawContacts.SYNC1, e164number)
                                           .withValue(RawContacts.SYNC4, String.valueOf(supportsVoice))
                                           .build());

    operations.add(ContentProviderOperation.newInsert(dataUri)
                                           .withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, index)
                                           .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                                           .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                           .build());

    operations.add(ContentProviderOperation.newInsert(dataUri)
                                           .withValueBackReference(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID, index)
                                           .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                           .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, e164number)
                                           .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_OTHER)
                                           .withValue(ContactsContract.Data.SYNC2, SYNC)
                                           .build());

    operations.add(ContentProviderOperation.newInsert(dataUri)
                                           .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, index)
                                           .withValue(ContactsContract.Data.MIMETYPE, CONTACT_MIMETYPE)
                                           .withValue(ContactsContract.Data.DATA1, e164number)
                                           .withValue(ContactsContract.Data.DATA2, context.getString(R.string.app_name))
                                           .withValue(ContactsContract.Data.DATA3, context.getString(R.string.ContactsDatabase_message_s, e164number))
                                           .withYieldAllowed(true)
                                           .build());

    if (supportsVoice) {
      operations.add(ContentProviderOperation.newInsert(dataUri)
                                             .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, index)
                                             .withValue(ContactsContract.Data.MIMETYPE, CALL_MIMETYPE)
                                             .withValue(ContactsContract.Data.DATA1, e164number)
                                             .withValue(ContactsContract.Data.DATA2, context.getString(R.string.app_name))
                                             .withValue(ContactsContract.Data.DATA3, context.getString(R.string.ContactsDatabase_opennetwork_call_s, e164number))
                                             .withYieldAllowed(true)
                                             .build());
    }


    if (Build.VERSION.SDK_INT >= 11) {
      operations.add(ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                                             .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, aggregateId)
                                             .withValueBackReference(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, index)
                                             .withValue(ContactsContract.AggregationExceptions.TYPE, ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER)
                                             .build());
    }
  }

  private void removeTextSecureRawContact(List<ContentProviderOperation> operations,
                                          Account account, long rowId)
  {
    operations.add(ContentProviderOperation.newDelete(RawContacts.CONTENT_URI.buildUpon()
                                                                             .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                                                                             .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
                                                                             .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build())
                                           .withYieldAllowed(true)
                                           .withSelection(BaseColumns._ID + " = ?", new String[] {String.valueOf(rowId)})
                                           .build());
  }

  private @NonNull Map<Address, OpenNetworkContact> getOpenNetworkRawContacts(@NonNull Account account) {
    Uri currentContactsUri = RawContacts.CONTENT_URI.buildUpon()
                                                    .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
                                                    .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type).build();

    Map<Address, OpenNetworkContact> opennetworkContacts = new HashMap<>();
    Cursor                      cursor         = null;

    try {
      String[] projection;

      if (Build.VERSION.SDK_INT >= 11) {
        projection = new String[] {BaseColumns._ID, RawContacts.SYNC1, RawContacts.SYNC4, RawContacts.CONTACT_ID, RawContacts.DISPLAY_NAME_PRIMARY, RawContacts.DISPLAY_NAME_SOURCE};
      } else{
        projection = new String[] {BaseColumns._ID, RawContacts.SYNC1, RawContacts.SYNC4, RawContacts.CONTACT_ID};
      }

      cursor = context.getContentResolver().query(currentContactsUri, projection, null, null, null);

      while (cursor != null && cursor.moveToNext()) {
        Address currentAddress              = Address.fromExternal(context, cursor.getString(1));
        long    rawContactId                = cursor.getLong(0);
        long    contactId                   = cursor.getLong(3);
        String  supportsVoice               = cursor.getString(2);
        String  rawContactDisplayName       = null;
        String  aggregateDisplayName        = null;
        int     rawContactDisplayNameSource = 0;

        if (Build.VERSION.SDK_INT >= 11) {
          rawContactDisplayName       = cursor.getString(4);
          rawContactDisplayNameSource = cursor.getInt(5);
          aggregateDisplayName        = getDisplayName(contactId);
        }

        opennetworkContacts.put(currentAddress, new OpenNetworkContact(rawContactId, supportsVoice, rawContactDisplayName, aggregateDisplayName, rawContactDisplayNameSource));
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }

    return opennetworkContacts;
  }

  private Optional<SystemContactInfo> getSystemContactInfo(@NonNull Address address)
  {
    if (!address.isPhone()) return Optional.absent();

    Uri      uri          = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address.toPhoneString()));
    String[] projection   = {ContactsContract.PhoneLookup.NUMBER,
                             ContactsContract.PhoneLookup._ID,
                             ContactsContract.PhoneLookup.DISPLAY_NAME};
    Cursor   numberCursor = null;
    Cursor   idCursor     = null;

    try {
      numberCursor = context.getContentResolver().query(uri, projection, null, null, null);

      while (numberCursor != null && numberCursor.moveToNext()) {
        String  systemNumber  = numberCursor.getString(0);
        Address systemAddress = Address.fromExternal(context, systemNumber);

        if (systemAddress.equals(address)) {
          idCursor = context.getContentResolver().query(RawContacts.CONTENT_URI,
                                                        new String[] {RawContacts._ID},
                                                        RawContacts.CONTACT_ID + " = ? ",
                                                        new String[] {String.valueOf(numberCursor.getLong(1))},
                                                        null);

          if (idCursor != null && idCursor.moveToNext()) {
            return Optional.of(new SystemContactInfo(numberCursor.getString(2),
                                                     numberCursor.getString(0),
                                                     idCursor.getLong(0)));
          }
        }
      }
    } finally {
      if (numberCursor != null) numberCursor.close();
      if (idCursor     != null) idCursor.close();
    }

    return Optional.absent();
  }

  private @Nullable String getDisplayName(long contactId) {
    Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                                                       new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                                                       ContactsContract.Contacts._ID + " = ?",
                                                       new String[] {String.valueOf(contactId)},
                                                       null);

    try {
      if (cursor != null && cursor.moveToFirst()) {
        return cursor.getString(0);
      } else {
        return null;
      }
    } finally {
      if (cursor != null) cursor.close();
    }
  }

  private static class ProjectionMappingCursor extends CursorWrapper {

    private final Map<String, String>    projectionMap;
    private final Pair<String, Object>[] extras;

    @SafeVarargs
    ProjectionMappingCursor(Cursor cursor,
                            Map<String, String> projectionMap,
                            Pair<String, Object>... extras)
    {
      super(cursor);
      this.projectionMap = projectionMap;
      this.extras        = extras;
    }

    @Override
    public int getColumnCount() {
      return super.getColumnCount() + extras.length;
    }

    @Override
    public int getColumnIndex(String columnName) {
      for (int i=0;i<extras.length;i++) {
        if (extras[i].first.equals(columnName)) {
          return super.getColumnCount() + i;
        }
      }

      return super.getColumnIndex(projectionMap.get(columnName));
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
      int index = getColumnIndex(columnName);

      if (index == -1) throw new IllegalArgumentException("Bad column name!");
      else             return index;
    }

    @Override
    public String getColumnName(int columnIndex) {
      int baseColumnCount = super.getColumnCount();

      if (columnIndex >= baseColumnCount) {
        int offset = columnIndex - baseColumnCount;
        return extras[offset].first;
      }

      return getReverseProjection(super.getColumnName(columnIndex));
    }

    @Override
    public String[] getColumnNames() {
      String[] names    = super.getColumnNames();
      String[] allNames = new String[names.length + extras.length];

      for (int i=0;i<names.length;i++) {
        allNames[i] = getReverseProjection(names[i]);
      }

      for (int i=0;i<extras.length;i++) {
        allNames[names.length + i] = extras[i].first;
      }

      return allNames;
    }

    @Override
    public int getInt(int columnIndex) {
      if (columnIndex >= super.getColumnCount()) {
        int offset = columnIndex - super.getColumnCount();
        return (Integer)extras[offset].second;
      }

      return super.getInt(columnIndex);
    }

    @Override
    public String getString(int columnIndex) {
      if (columnIndex >= super.getColumnCount()) {
        int offset = columnIndex - super.getColumnCount();
        return (String)extras[offset].second;
      }

      return super.getString(columnIndex);
    }


    private @Nullable String getReverseProjection(String columnName) {
      for (Map.Entry<String, String> entry : projectionMap.entrySet()) {
        if (entry.getValue().equals(columnName)) {
          return entry.getKey();
        }
      }

      return null;
    }
  }

  private static class SystemContactInfo {
    private final String name;
    private final String number;
    private final long   id;

    private SystemContactInfo(String name, String number, long id) {
      this.name   = name;
      this.number = number;
      this.id     = id;
    }
  }

  private static class OpenNetworkContact {

              private final long   id;
    @Nullable private final String supportsVoice;
    @Nullable private final String rawDisplayName;
    @Nullable private final String aggregateDisplayName;
              private final int    displayNameSource;

    OpenNetworkContact(long id,
                  @Nullable String supportsVoice,
                  @Nullable String rawDisplayName,
                  @Nullable String aggregateDisplayName,
                  int displayNameSource)
    {
      this.id                   = id;
      this.supportsVoice        = supportsVoice;
      this.rawDisplayName       = rawDisplayName;
      this.aggregateDisplayName = aggregateDisplayName;
      this.displayNameSource    = displayNameSource;
    }

    public long getId() {
      return id;
    }

    boolean isVoiceSupported() {
      return "true".equals(supportsVoice);
    }

    @Nullable
    String getRawDisplayName() {
      return rawDisplayName;
    }

    @Nullable
    String getAggregateDisplayName() {
      return aggregateDisplayName;
    }

    int getDisplayNameSource() {
      return displayNameSource;
    }
  }
}