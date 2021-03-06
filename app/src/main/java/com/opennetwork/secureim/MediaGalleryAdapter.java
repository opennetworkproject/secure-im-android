package com.opennetwork.secureim;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.codewaves.stickyheadergrid.StickyHeaderGridAdapter;

import com.opennetwork.secureim.components.ThumbnailView;
import com.opennetwork.secureim.crypto.MasterSecret;
import com.opennetwork.secureim.database.Address;
import com.opennetwork.secureim.database.MediaDatabase.MediaRecord;
import com.opennetwork.secureim.database.loaders.BucketedThreadMediaLoader.BucketedThreadMedia;
import com.opennetwork.secureim.mms.GlideRequests;
import com.opennetwork.secureim.mms.Slide;
import com.opennetwork.secureim.util.MediaUtil;

import java.util.Locale;

class MediaGalleryAdapter extends StickyHeaderGridAdapter {

  private static final String TAG = MediaGalleryAdapter.class.getSimpleName();

  private final Context             context;
  private final MasterSecret        masterSecret;
  private final GlideRequests       glideRequests;
  private final Locale              locale;
  private final Address             address;

  private  BucketedThreadMedia media;

  private static class ViewHolder extends StickyHeaderGridAdapter.ItemViewHolder {
    ThumbnailView imageView;

    ViewHolder(View v) {
      super(v);
      imageView = v.findViewById(R.id.image);
    }
  }

  private static class HeaderHolder extends StickyHeaderGridAdapter.HeaderViewHolder {
    TextView textView;

    HeaderHolder(View itemView) {
      super(itemView);
      textView = itemView.findViewById(R.id.text);
    }
  }

  MediaGalleryAdapter(@NonNull Context context, @NonNull MasterSecret masterSecret, @NonNull GlideRequests glideRequests,
                      BucketedThreadMedia media, Locale locale, Address address)
  {
    this.context       = context;
    this.masterSecret  = masterSecret;
    this.glideRequests = glideRequests;
    this.locale        = locale;
    this.media         = media;
    this.address       = address;
  }

  public void setMedia(BucketedThreadMedia media) {
    this.media = media;
  }

  @Override
  public StickyHeaderGridAdapter.HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
    return new HeaderHolder(LayoutInflater.from(context).inflate(R.layout.media_overview_gallery_item_header, parent, false));
  }

  @Override
  public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
    return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.media_overview_gallery_item, parent, false));
  }

  @Override
  public void onBindHeaderViewHolder(StickyHeaderGridAdapter.HeaderViewHolder viewHolder, int section) {
    ((HeaderHolder)viewHolder).textView.setText(media.getName(section, locale));
  }

  @Override
  public void onBindItemViewHolder(ItemViewHolder viewHolder, int section, int offset) {
    MediaRecord   mediaRecord   = media.get(section, offset);
    ThumbnailView thumbnailView = ((ViewHolder)viewHolder).imageView;

    Slide slide = MediaUtil.getSlideForAttachment(context, mediaRecord.getAttachment());

    if (slide != null) {
      thumbnailView.setImageResource(masterSecret, glideRequests, slide, false, false);
    }

    thumbnailView.setOnClickListener(new OnMediaClickListener(mediaRecord));
  }

  @Override
  public int getSectionCount() {
    return media.getSectionCount();
  }

  @Override
  public int getSectionItemCount(int section) {
    return media.getSectionItemCount(section);
  }

  private class OnMediaClickListener implements View.OnClickListener {

    private final MediaRecord mediaRecord;

    private OnMediaClickListener(MediaRecord mediaRecord) {
      this.mediaRecord = mediaRecord;
    }

    @Override
    public void onClick(View v) {
      if (mediaRecord.getAttachment().getDataUri() != null) {
        Intent intent = new Intent(context, MediaPreviewActivity.class);
        intent.putExtra(MediaPreviewActivity.DATE_EXTRA, mediaRecord.getDate());
        intent.putExtra(MediaPreviewActivity.SIZE_EXTRA, mediaRecord.getAttachment().getSize());
        intent.putExtra(MediaPreviewActivity.ADDRESS_EXTRA, address);
        intent.putExtra(MediaPreviewActivity.OUTGOING_EXTRA, mediaRecord.isOutgoing());

        if (mediaRecord.getAddress() != null) {
          intent.putExtra(MediaPreviewActivity.ADDRESS_EXTRA, mediaRecord.getAddress());
        }

        intent.setDataAndType(mediaRecord.getAttachment().getDataUri(), mediaRecord.getContentType());
        context.startActivity(intent);
      }
    }
  }

}
