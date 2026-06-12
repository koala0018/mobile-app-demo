package com.example.mobileproject01;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

final class StatChipView extends LinearLayout {
    private final TextView valueView;
    private final TextView labelView;

    StatChipView(Context context, String value, String label) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(Styles.dp(context, 12), Styles.dp(context, 8), Styles.dp(context, 12), Styles.dp(context, 8));
        setBackground(Styles.cardBackground(0xFFFFFFFF, 0xFFFFFFFF));

        valueView = new TextView(context);
        valueView.setTextColor(Styles.INK);
        valueView.setTextSize(15f);
        valueView.setTypeface(valueView.getTypeface(), Typeface.BOLD);
        valueView.setText(value);
        valueView.setSingleLine(true);

        labelView = new TextView(context);
        labelView.setTextColor(Styles.MUTED);
        labelView.setTextSize(10.5f);
        labelView.setText(label);
        labelView.setPadding(0, Styles.dp(context, 2), 0, 0);
        labelView.setSingleLine(true);

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
