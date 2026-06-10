package com.example.mobileproject01;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Styles {
    private Styles() {
    }

    static GradientDrawable cardBackground(int startColor, int endColor) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor});
        drawable.setCornerRadius(28f);
        drawable.setStroke(1, 0x22FFFFFF);
        return drawable;
    }

    static GradientDrawable previewBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF20385B, 0xFF12223B});
        drawable.setCornerRadius(18f);
        drawable.setStroke(1, 0x1FFFFFFF);
        return drawable;
    }

    static TextView title(Context context, String text) {
        TextView view = baseText(context, text, 28f, 0xFFF4F8FF, true);
        view.setLetterSpacing(0.02f);
        return view;
    }

    static TextView sectionTitle(Context context, String text) {
        TextView view = baseText(context, text, 18f, 0xFFF4F8FF, true);
        return view;
    }

    static TextView body(Context context, String text) {
        return baseText(context, text, 14f, 0xFFA7B7D1, false);
    }

    static TextView miniText(Context context, String text) {
        TextView view = baseText(context, text, 12f, 0xFF91A6C6, false);
        view.setAlpha(0.95f);
        return view;
    }

    static StatChipView statChip(Context context, String value, String label) {
        return new StatChipView(context, value, label);
    }

    static Button primaryButton(Context context, String text) {
        Button button = baseButton(context, text, 0xFF0F1E3A);
        button.setBackground(cardBackground(0xFF6EE7FF, 0xFF84F0FF));
        return button;
    }

    static Button secondaryButton(Context context, String text) {
        Button button = baseButton(context, text, 0xFFF4F8FF);
        button.setBackground(cardBackground(0x1F203A60, 0x1B294561));
        return button;
    }

    static TextView itemTitle(Context context, String text) {
        return baseText(context, text, 18f, 0xFFF4F8FF, true);
    }

    static TextView itemBody(Context context, String text) {
        TextView view = baseText(context, text, 13f, 0xFFBDD0EE, false);
        view.setLineSpacing(0f, 1.08f);
        return view;
    }

    static TextView itemSnippet(Context context, String text) {
        TextView view = baseText(context, text, 12f, 0xFF91A6C6, false);
        view.setLineSpacing(0f, 1.05f);
        return view;
    }

    private static TextView baseText(Context context, String text, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setLineSpacing(0f, 1.15f);
        if (bold) {
            view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return view;
    }

    private static Button baseButton(Context context, String text, int textColor) {
        Button button = new Button(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        button.setPadding(dp(context, 16), dp(context, 13), dp(context, 16), dp(context, 13));
        button.setTransformationMethod(null);
        button.setGravity(Gravity.CENTER);
        return button;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

}
