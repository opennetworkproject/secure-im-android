package com.opennetwork.secureim.mms;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.opennetwork.secureim.components.location.OpenNetworkPlace;
import com.opennetwork.libim.util.guava.Optional;

public class LocationSlide extends ImageSlide {

  @NonNull
  private final OpenNetworkPlace place;

  public LocationSlide(@NonNull  Context context, @NonNull  Uri uri, long size, @NonNull OpenNetworkPlace place)
  {
    super(context, uri, size);
    this.place = place;
  }

  @Override
  @NonNull
  public Optional<String> getBody() {
    return Optional.of(place.getDescription());
  }

  @NonNull
  public OpenNetworkPlace getPlace() {
    return place;
  }

  @Override
  public boolean hasLocation() {
    return true;
  }

}
