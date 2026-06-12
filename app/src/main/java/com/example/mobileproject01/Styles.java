package com.example.mobileproject01;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

public final class Styles {
    static final int INK = 0xFF111111;
    static final int PAPER = 0xFFFFF8ED;
    static final int PANEL = 0xFFFFFFFF;
    static final int MUTED = 0xFF5F5F66;
    static final int LINE = 0xFF191919;
    static final int PINK = 0xFFFF5DA8;
    static final int GREEN = 0xFFB9FF3D;
    static final int BLUE = 0xFF5E7BFF;
    static final int YELLOW = 0xFFFFD84D;
    static final int RED = 0xFFFF624D;

    private Styles() {
    }

    static GradientDrawable cardBackground(int startColor, int endColor) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor});
        drawable.setCornerRadius(18f);
        drawable.setStroke(dpRadius(1), LINE);
        return drawable;
    }

    static GradientDrawable panelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(PANEL);
        drawable.setCornerRadius(18f);
        drawable.setStroke(dpRadius(1), LINE);
        return drawable;
    }

    static GradientDrawable previewBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF4F4F4, 0xFFE8ECFF});
        drawable.setCornerRadius(14f);
        drawable.setStroke(dpRadius(1), LINE);
        return drawable;
    }

    static GradientDrawable inputBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xFFFFFFFF);
        drawable.setCornerRadius(14f);
        drawable.setStroke(dpRadius(1), LINE);
        return drawable;
    }

    static GradientDrawable chipBackground(boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(999f);
        drawable.setStroke(dpRadius(1), LINE);
        drawable.setColor(selected ? GREEN : 0xFFFFFFFF);
        return drawable;
    }

    static TextView title(Context context, String text) {
        TextView view = baseText(context, text, 28f, INK, true);
        view.setLetterSpacing(0f);
        return view;
    }

    static TextView sectionTitle(Context context, String text) {
        return baseText(context, text, 18f, INK, true);
    }

    static TextView body(Context context, String text) {
        return baseText(context, text, 14f, MUTED, false);
    }

    static TextView miniText(Context context, String text) {
        return baseText(context, text, 12f, MUTED, false);
    }

    static TextView badge(Context context, String text) {
        TextView view = baseText(context, text, 11f, INK, true);
        view.setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5));
        view.setBackground(chipBackground(false));
        return view;
    }

    static StatChipView statChip(Context context, String value, String label) {
        return new StatChipView(context, value, label);
    }

    static Button primaryButton(Context context, String text) {
        Button button = baseButton(context, text, INK);
        styleButton(button, GREEN);
        return button;
    }

    static Button secondaryButton(Context context, String text) {
        Button button = baseButton(context, text, INK);
        styleButton(button, 0xFFFFFFFF);
        return button;
    }

    static Button accentButton(Context context, String text, int color) {
        Button button = baseButton(context, text, INK);
        styleButton(button, color);
        return button;
    }

    static Button chipButton(Context context, String text) {
        Button button = baseButton(context, text, INK);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        button.setMinHeight(dp(context, 38));
        button.setPadding(dp(context, 12), dp(context, 7), dp(context, 12), dp(context, 7));
        styleButton(button, 0xFFFFFFFF);
        return button;
    }

    static void setChipSelected(Button button, boolean selected) {
        styleButton(button, selected ? GREEN : 0xFFFFFFFF);
    }

    static Button dangerButton(Context context, String text) {
        Button button = baseButton(context, text, INK);
        styleButton(button, RED);
        return button;
    }

    static EditText inputField(Context context, String hint, boolean multiLine) {
        EditText editText = new EditText(context);
        editText.setHint(hint);
        editText.setTextColor(INK);
        editText.setHintTextColor(0xFF8A8A91);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        editText.setBackground(inputBackground());
        editText.setPadding(dp(context, 12), dp(context, 9), dp(context, 12), dp(context, 9));
        editText.setSingleLine(!multiLine);
        editText.setTextIsSelectable(true);
        editText.setLineSpacing(0f, 1.08f);
        if (multiLine) {
            editText.setMinLines(2);
            editText.setGravity(Gravity.TOP | Gravity.START);
        }
        return editText;
    }

    static TextView itemTitle(Context context, String text) {
        TextView view = baseText(context, text, 16f, INK, true);
        view.setMaxLines(2);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return view;
    }

    static TextView itemBody(Context context, String text) {
        return baseText(context, text, 13f, MUTED, false);
    }

    static TextView itemSnippet(Context context, String text) {
        return baseText(context, text, 12f, MUTED, false);
    }

    static void applyPressFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().translationY(dp(v.getContext(), 2)).setDuration(80).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().translationY(0).setDuration(120).start();
            }
            return false;
        });
    }

    static void applyClickableSurface(View view) {
        view.setClickable(true);
        view.setFocusable(true);
        applyPressFeedback(view);
    }

    static LinearLayout divider(Context context) {
        LinearLayout divider = new LinearLayout(context);
        divider.setBackgroundColor(LINE);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 1)));
        return divider;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static int dpRadius(int value) {
        return value;
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
        Button button = new MaterialButton(context);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        button.setMinHeight(dp(context, 42));
        button.setPadding(dp(context, 14), dp(context, 9), dp(context, 14), dp(context, 9));
        button.setTransformationMethod(null);
        button.setGravity(Gravity.CENTER);
        applyPressFeedback(button);
        return button;
    }

    private static void styleButton(Button button, int color) {
        if (button instanceof MaterialButton) {
            MaterialButton materialButton = (MaterialButton) button;
            materialButton.setBackgroundTintList(ColorStateList.valueOf(color));
            materialButton.setStrokeColor(ColorStateList.valueOf(LINE));
            materialButton.setStrokeWidth(dp(button.getContext(), 1));
            materialButton.setCornerRadius(dp(button.getContext(), 8));
            materialButton.setRippleColor(ColorStateList.valueOf(0x22111111));
            materialButton.setTextColor(INK);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setBackgroundTintList(ColorStateList.valueOf(color));
            button.setTextColor(INK);
        } else {
            button.setBackground(cardBackground(color, color));
            button.setTextColor(INK);
        }
    }
}
