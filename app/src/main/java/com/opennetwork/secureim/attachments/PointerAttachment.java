package com.opennetwork.secureim.attachments;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.opennetwork.secureim.crypto.MasterSecretUnion;
import com.opennetwork.secureim.crypto.MediaKey;
import com.opennetwork.secureim.database.AttachmentDatabase;
import com.opennetwork.libim.util.guava.Optional;
import com.opennetwork.imservice.api.messages.OpenNetworkServiceAttachment;

import java.util.LinkedList;
import java.util.List;

public class PointerAttachment extends Attachment {

  public PointerAttachment(@NonNull String contentType, int transferState, long size,
                           @Nullable String fileName,  @NonNull String location,
                           @NonNull String key, @NonNull String relay,
                           @Nullable byte[] digest, boolean voiceNote)
  {
    super(contentType, transferState, size, fileName, location, key, relay, digest, null, voiceNote);
  }

  @Nullable
  @Override
  public Uri getDataUri() {
    return null;
  }

  @Nullable
  @Override
  public Uri getThumbnailUri() {
    return null;
  }


  public static List<Attachment> forPointers(@NonNull MasterSecretUnion masterSecret, Optional<List<OpenNetworkServiceAttachment>> pointers) {
    List<Attachment> results = new LinkedList<>();

    if (pointers.isPresent()) {
      for (OpenNetworkServiceAttachment pointer : pointers.get()) {
        if (pointer.isPointer()) {
          String encryptedKey = MediaKey.getEncrypted(masterSecret, pointer.asPointer().getKey());
          results.add(new PointerAttachment(pointer.getContentType(),
                                            AttachmentDatabase.TRANSFER_PROGRESS_PENDING,
                                            pointer.asPointer().getSize().or(0),
                                            pointer.asPointer().getFileName().orNull(),
                                            String.valueOf(pointer.asPointer().getId()),
                                            encryptedKey, pointer.asPointer().getRelay().orNull(),
                                            pointer.asPointer().getDigest().orNull(),
                                            pointer.asPointer().getVoiceNote()));
        }
      }
    }

    return results;
  }
}
