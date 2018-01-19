package com.opennetwork.secureim.preferences.widgets;


import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

import com.opennetwork.secureim.R;

public class OpenNetworkPreference extends Preference {

  private TextView rightSummary;
  private CharSequence summary;

  public OpenNetworkPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize();
  }

  public OpenNetworkPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  public OpenNetworkPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public OpenNetworkPreference(Context context) {
    super(context);
    initialize();
  }

  private void initialize() {
    setWidgetLayoutResource(R.layout.preference_right_summary_widget);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder view) {
    super.onBindViewHolder(view);

    this.rightSummary = (TextView)view.findViewById(R.id.right_summary);
    setSummary(this.summary);
  }

  @Override
  public void setSummary(CharSequence summary) {
    super.setSummary(null);

    this.summary = summary;

    if (this.rightSummary != null) {
      this.rightSummary.setText(summary);
    }
  }

}
