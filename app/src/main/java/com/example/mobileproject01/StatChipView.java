package com.example.mobileproject01;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

final class StatChipView extends LinearLayout {
    private final TextView valueView;
    private final TextView labelView;

    StatChipView(Context context, String value, String label) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(Styles.dp(context, 14), Styles.dp(context, 12), Styles.dp(context, 14), Styles.dp(context, 12));
        setBackground(Styles.cardBackground(0x1F2A4062, 0x1A36537C));

        valueView = new TextView(context);
        valueView.setTextColor(0xFFF4F8FF);
        valueView.setTextSize(16f);
        valueView.setTypeface(valueView.getTypeface(), android.graphics.Typeface.BOLD);
        valueView.setText(value);

        labelView = new TextView(context);
        labelView.setTextColor(0xFFA7B7D1);
        labelView.setTextSize(11f);
        labelView.setText(label);
        labelView.setPadding(0, Styles.dp(context, 3), 0, 0);

        addView(valueView);
        addView(labelView);
    }

    void setValue(String value) {
        valueView.setText(value);
    }

    void setLabel(String label) {
        labelView.setText(label);
    }
}
