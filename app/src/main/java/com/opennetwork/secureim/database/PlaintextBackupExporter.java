package com.opennetwork.secureim.database;


import android.content.Context;

import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.model.SmsMessageRecord;
import com.opennetwork.secureim.util.StorageUtil;

import java.io.File;
import java.io.IOException;

public class PlaintextBackupExporter {

  private static final String FILENAME = "OpenNetworkPlaintextBackup.xml";

  public static void exportPlaintextToSd(Context context, MasterSecret masterSecret)
      throws NoExternalStorageException, IOException
  {
    exportPlaintext(context, masterSecret);
  }

  public static File getPlaintextExportFile() throws NoExternalStorageException {
    return new File(StorageUtil.getBackupDir(), FILENAME);
  }

  private static void exportPlaintext(Context context, MasterSecret masterSecret)
      throws NoExternalStorageException, IOException
  {
    int count               = DatabaseFactory.getSmsDatabase(context).getMessageCount();
    XmlBackup.Writer writer = new XmlBackup.Writer(getPlaintextExportFile().getAbsolutePath(), count);


    SmsMessageRecord record;
    EncryptingSmsDatabase.Reader reader = null;
    int skip                            = 0;
    int ROW_LIMIT                       = 500;

    do {
      if (reader != null)
        reader.close();

      reader = DatabaseFactory.getEncryptingSmsDatabase(context).getMessages(masterSecret, skip, ROW_LIMIT);

      while ((record = reader.getNext()) != null) {
        XmlBackup.XmlBackupItem item =
            new XmlBackup.XmlBackupItem(0, record.getIndividualRecipient().getAddress().serialize(),
                                        record.getIndividualRecipient().getName(),
                                        record.getDateReceived(),
                                        MmsSmsColumns.Types.translateToSystemBaseType(record.getType()),
                                        null, record.getDisplayBody().toString(), null,
                                        1, record.getDeliveryStatus());

        writer.writeItem(item);
      }

      skip += ROW_LIMIT;
    } while (reader.getCount() > 0);

    writer.close();
  }
}
