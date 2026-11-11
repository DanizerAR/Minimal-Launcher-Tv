package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks one app's picture from the set shipped in the launcher ({@code assets/icons/}); "App's own
 * picture" clears the pick. The choice is saved as {@code icon.<package> = asset:<file>} and Home
 * rebuilds from the change count when this closes.
 */
public final class IconPickerActivity extends Activity {
    static final String EXTRA_PACKAGE = "com.gathin.tvhome.extra.ICON_PACKAGE";
    static final String EXTRA_ACTIVITY = "com.gathin.tvhome.extra.ICON_ACTIVITY";
    static final String EXTRA_LABEL = "com.gathin.tvhome.extra.ICON_LABEL";

    private SharedPreferences choices;
    private String pkg, label;
    private final List<String> files = new ArrayList<>();
    private TextView status;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        pkg = getIntent().getStringExtra(EXTRA_PACKAGE);
        label = getIntent().getStringExtra(EXTRA_LABEL);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        if (pkg == null) {
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF101419);
        int pad = dp(72);
        root.setPadding(pad, dp(34), pad, dp(28));

        TextView title = new TextView(this);
        title.setText(getString(R.string.icon_pick_title, label == null ? pkg : label));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(30);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText(R.string.icon_pick_hint);
        hint.setTextColor(0xFFACB8C1);
        hint.setTextSize(14);
        hint.setPadding(0, dp(6), 0, dp(14));
        root.addView(hint);

        root.addView(row(getString(R.string.icon_pick_own), v -> save(null)));

        status = new TextView(this);
        status.setText(R.string.icon_pick_loading);
        status.setTextColor(0xFFACB8C1);
        status.setTextSize(14);
        status.setPadding(0, dp(14), 0, dp(8));
        root.addView(status);

        files.addAll(IconPacks.bundled(this));
        if (files.isEmpty()) {
            status.setText(R.string.icon_pick_none);
        } else {
            status.setText(getResources().getQuantityString(R.plurals.icon_pick_count, files.size(), files.size()));
        }

        RecyclerView grid = new RecyclerView(this);
        grid.setLayoutManager(new GridLayoutManager(this, 8));
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(4), 0, 0);
        grid.setAdapter(new IconAdapter());
        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        if (!files.isEmpty()) grid.requestFocus();
    }

    private TextView row(String text, View.OnClickListener click) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextColor(0xFFFFFFFF);
        row.setTextSize(18);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(18), 0);
        row.setBackgroundResource(R.drawable.settings_choice_bg);
        row.setFocusable(true);
        row.setClickable(true);
        row.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);
        return row;
    }

    /** null clears the pick; otherwise {@code asset:<file>}. */
    private void save(String value) {
        SharedPreferences.Editor editor = choices.edit();
        if (value == null) editor.remove(IconPacks.CHOSEN + pkg);
        else editor.putString(IconPacks.CHOSEN + pkg, value);
        editor.putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
        android.util.Log.d("TvHome.IconPicker", "save " + pkg + " = " + value);
        setResult(RESULT_OK);
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class IconAdapter extends RecyclerView.Adapter<IconAdapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout frame = new FrameLayout(parent.getContext());
            frame.setBackgroundResource(R.drawable.settings_choice_bg);
            frame.setFocusable(true);
            frame.setClickable(true);
            int size = dp(72);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(size, size);
            int margin = dp(5);
            lp.setMargins(margin, margin, margin, margin);
            frame.setLayoutParams(lp);
            ImageView image = new ImageView(parent.getContext());
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int pad = dp(9);
            frame.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) {{
                setMargins(pad, pad, pad, pad);
            }});
            return new Holder(frame, image);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String file = files.get(position);
            Drawable picture = IconPacks.fromAsset(IconPickerActivity.this, file);
            holder.image.setImageDrawable(picture);
            holder.itemView.setContentDescription(IconPacks.label(file));
            holder.itemView.setOnClickListener(v -> save(IconPacks.ASSET + file));
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            Holder(View item, ImageView image) {
                super(item);
                this.image = image;
            }
        }
    }
}
