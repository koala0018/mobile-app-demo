package com.example.mobileproject01;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Styles {
    private Styles() {
    }

    static GradientDrawable cardBackground(int startColor, int endColor) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor});
        drawable.setCornerRadius(32f);
        drawable.setStroke(1, 0x22FFFFFF);
        return drawable;
    }

    static GradientDrawable panelBackground() {
        return cardBackground(0xFF101D30, 0xFF172843);
    }

    static GradientDrawable previewBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF243A5B, 0xFF13233B});
        drawable.setCornerRadius(24f);
        drawable.setStroke(1, 0x18FFFFFF);
        return drawable;
    }

    static GradientDrawable inputBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xFF0E1828);
        drawable.setCornerRadius(24f);
        drawable.setStroke(1, 0x20FFFFFF);
        return drawable;
    }

    static GradientDrawable chipBackground(boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(999f);
        drawable.setStroke(1, selected ? 0x66A8F5FF : 0x2EFFFFFF);
        drawable.setColor(selected ? 0xFF203A59 : 0x1E20324E);
        return drawable;
    }

    static TextView title(Context context, String text) {
        TextView view = baseText(context, text, 30f, 0xFFF4F8FF, true);
        view.setLetterSpacing(0.02f);
        return view;
    }

    static TextView sectionTitle(Context context, String text) {
        return baseText(context, text, 18f, 0xFFF4F8FF, true);
    }

    static TextView body(Context context, String text) {
        return baseText(context, text, 14f, 0xFFA7B7D1, false);
    }

    static TextView miniText(Context context, String text) {
        TextView view = baseText(context, text, 12f, 0xFF90A7C7, false);
        view.setAlpha(0.95f);
        return view;
    }

    static TextView badge(Context context, String text) {
        TextView view = baseText(context, text, 11.5f, 0xFFF4F8FF, true);
        view.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        view.setBackground(chipBackground(false));
        return view;
    }

    static StatChipView statChip(Context context, String value, String label) {
        return new StatChipView(context, value, label);
    }

    static Button primaryButton(Context context, String text) {
        Button button = baseButton(context, text, 0xFF07111F);
        button.setBackground(cardBackground(0xFF7EE7FF, 0xFF97F4FF));
        return button;
    }

    static Button secondaryButton(Context context, String text) {
        Button button = baseButton(context, text, 0xFFF4F8FF);
        button.setBackground(cardBackground(0x1F203A60, 0x1B294561));
        return button;
    }

    static Button chipButton(Context context, String text) {
        Button button = baseButton(context, text, 0xFFF4F8FF);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        button.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));
        button.setBackground(chipBackground(false));
        return button;
    }

    static Button dangerButton(Context context, String text) {
        Button button = baseButton(context, text, 0xFFFFECF0);
        button.setBackground(cardBackground(0xFF60263A, 0xFF7B3149));
        return button;
    }

    static EditText inputField(Context context, String hint, boolean multiLine) {
        EditText editText = new EditText(context);
        editText.setHint(hint);
        editText.setTextColor(0xFFF4F8FF);
        editText.setHintTextColor(0xFF8095B6);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        editText.setBackground(inputBackground());
        editText.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        editText.setSingleLine(!multiLine);
        editText.setTextIsSelectable(true);
        editText.setLineSpacing(0f, 1.1f);
        if (multiLine) {
            editText.setMinLines(3);
            editText.setGravity(Gravity.TOP | Gravity.START);
        }
        return editText;
    }

    static TextView itemTitle(Context context, String text) {
        TextView view = baseText(context, text, 18f, 0xFFF4F8FF, true);
        view.setMaxLines(2);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return view;
    }

    static TextView itemBody(Context context, String text) {
        TextView view = baseText(context, text, 13f, 0xFFBDD0EE, false);
        view.setLineSpacing(0f, 1.08f);
        return view;
    }

    static TextView itemSnippet(Context context, String text) {
        TextView view = baseText(context, text, 12f, 0xFF90A7C7, false);
        view.setLineSpacing(0f, 1.05f);
        return view;
    }

    static void applyPressFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            float scale = event.getActionMasked() == MotionEvent.ACTION_DOWN ? 0.985f : 1f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
            return false;
        });
    }

    static void applyClickableSurface(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClickable(true);
            view.setFocusable(true);
            view.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(0x22FFFFFF),
                    view.getBackground(),
                    null));
        }
        applyPressFeedback(view);
    }

    static LinearLayout divider(Context context) {
        LinearLayout divider = new LinearLayout(context);
        divider.setBackgroundColor(0x14FFFFFF);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 1)));
        return divider;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static TextView baseText(Context context, String text, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setLineSpacing(0f, 1.12f);
        if (bold) {
            view.setTypeface(Typeface.create(view.getTypeface(), Typeface.BOLD));
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
        applyPressFeedback(button);
        return button;
    }
}
