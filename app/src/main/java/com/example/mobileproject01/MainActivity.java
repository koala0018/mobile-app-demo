package com.example.mobileproject01;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28f);
        title.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tap the button to count clicks.");
        subtitle.setTextSize(18f);
        subtitle.setPadding(0, 24, 0, 24);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView counterView = new TextView(this);
        counterView.setText("Count: 0");
        counterView.setTextSize(22f);
        counterView.setPadding(0, 12, 0, 12);
        counterView.setGravity(Gravity.CENTER_HORIZONTAL);

        Button button = new Button(this);
        button.setText("Tap me");
        button.setOnClickListener(v -> {
            counter++;
            counterView.setText("Count: " + counter);
        });

        root.addView(title);
        root.addView(subtitle);
        root.addView(counterView);
        root.addView(button);

        setContentView(root);
    }
}

