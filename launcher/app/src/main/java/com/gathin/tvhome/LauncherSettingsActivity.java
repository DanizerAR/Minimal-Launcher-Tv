package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[6];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private static final String[] CLOCK_FORMATS = {"system", "12", "24"};
    private final TextView[] clockChoices = new TextView[CLOCK_FORMATS.length];
    private static final int[] CLOCK_SIZES = {40, 52, 64, 78};
    private final TextView[] clockSizeChoices = new TextView[CLOCK_SIZES.length];
    private static final String[] DOCK_BACKGROUNDS = {Config.DOCK_CLEAR, Config.DOCK_TINT, Config.DOCK_GLASS};
    private final TextView[] dockBackgroundChoices = new TextView[DOCK_BACKGROUNDS.length];
    private static final String[] GLOW_STYLES = {Config.GLOW_OFF, Config.GLOW_RADIAL, Config.GLOW_HORIZON};
    private final TextView[] glowStyleChoices = new TextView[GLOW_STYLES.length];
    private static final int[] GLOW_INTENSITIES = {50, 100, 150};
    private final TextView[] glowIntensityChoices = new TextView[GLOW_INTENSITIES.length];
    private final TextView[] glowXChoices = new TextView[3];
    private final TextView[] glowYChoices = new TextView[3];
    private static final String[] FOCUS_TINTS = {Config.FOCUS_AUTO, Config.FOCUS_WHITE,
            Config.FOCUS_BLUE, Config.FOCUS_TEAL, Config.FOCUS_GREEN, Config.FOCUS_AMBER,
            Config.FOCUS_ROSE, Config.FOCUS_VIOLET, Config.FOCUS_OFF};
    private static final int[] FOCUS_TINT_LABELS = {R.string.focus_auto, R.string.focus_white,
            R.string.focus_blue, R.string.focus_teal, R.string.focus_green, R.string.focus_amber,
            R.string.focus_rose, R.string.focus_violet, R.string.focus_off};
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView openOnStartChoice;
    private TextView focusTintChoice;
    private android.widget.SeekBar wallpaperDimBar;
    private TextView wallpaperDimValue;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private String dockBackground;
    private String clockFormat;
    private int clockSize;
    private String glowStyle;
    private int glowIntensity;
    private int glowX, glowY;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(View.VISIBLE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        View sortAppsChoice = findViewById(R.id.sortAppsChoice);
        sortAppsChoice.setOnClickListener(v -> {
            Order.file(this).delete();
            changed();
            Toast.makeText(this, R.string.sort_apps_done, Toast.LENGTH_SHORT).show();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        clockFormat = config.clockFormat;
        clockSize = choices.getInt(Config.CLOCK_SIZE_KEY, 52);
        if (clockSize < 30 || clockSize > 100) clockSize = 52;
        dockBackground = choices.getString(Config.DOCK_BACKGROUND_KEY,
                config.dockGlass ? Config.DOCK_GLASS : config.dockBand ? Config.DOCK_TINT : Config.DOCK_CLEAR);
        if (!Config.DOCK_TINT.equals(dockBackground) && !Config.DOCK_GLASS.equals(dockBackground))
            dockBackground = Config.DOCK_CLEAR;
        glowStyle = choices.getString(Config.GLOW_STYLE_KEY,
                config.ambientGlow ? config.glowStyle : Config.GLOW_OFF);
        if (!Config.GLOW_OFF.equals(glowStyle) && !Config.GLOW_HORIZON.equals(glowStyle))
            glowStyle = Config.GLOW_RADIAL;
        glowIntensity = choices.getInt(Config.GLOW_INTENSITY_KEY, 100);
        if (glowIntensity != 50 && glowIntensity != 150) glowIntensity = 100;
        glowX = choices.getInt(Config.GLOW_POSITION_X_KEY, 0);
        if (glowX < 0 || glowX > 2) glowX = 0;
        glowY = choices.getInt(Config.GLOW_POSITION_Y_KEY, 1);
        if (glowY < 0 || glowY > 2) glowY = 1;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns3, R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7, R.id.columns8};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 3;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] clockIds = {R.id.clockSystem, R.id.clock12, R.id.clock24};
        for (int i = 0; i < clockIds.length; i++) {
            final String value = CLOCK_FORMATS[i];
            clockChoices[i] = findViewById(clockIds[i]);
            clockChoices[i].setOnClickListener(v -> {
                if (value.equals(clockFormat)) return;
                clockFormat = value;
                choices.edit().putString(Config.CLOCK_FORMAT_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] sizeIds = {R.id.clockSizeSmall, R.id.clockSizeMedium, R.id.clockSizeLarge, R.id.clockSizeHuge};
        for (int i = 0; i < sizeIds.length; i++) {
            final int value = CLOCK_SIZES[i];
            clockSizeChoices[i] = findViewById(sizeIds[i]);
            clockSizeChoices[i].setOnClickListener(v -> {
                if (clockSize == value) return;
                clockSize = value;
                choices.edit().putInt(Config.CLOCK_SIZE_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockBackgroundIds = {R.id.dockClear, R.id.dockTint, R.id.dockGlass};
        for (int i = 0; i < dockBackgroundIds.length; i++) {
            final String value = DOCK_BACKGROUNDS[i];
            dockBackgroundChoices[i] = findViewById(dockBackgroundIds[i]);
            dockBackgroundChoices[i].setOnClickListener(v -> {
                if (value.equals(dockBackground)) return;
                dockBackground = value;
                choices.edit().putString(Config.DOCK_BACKGROUND_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] glowStyleIds = {R.id.glowOff, R.id.glowRadial, R.id.glowHorizon};
        for (int i = 0; i < glowStyleIds.length; i++) {
            final String value = GLOW_STYLES[i];
            glowStyleChoices[i] = findViewById(glowStyleIds[i]);
            glowStyleChoices[i].setOnClickListener(v -> {
                if (value.equals(glowStyle)) return;
                glowStyle = value;
                choices.edit().putString(Config.GLOW_STYLE_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] glowIntensityIds = {R.id.glowSubtle, R.id.glowNormal, R.id.glowStrong};
        for (int i = 0; i < glowIntensityIds.length; i++) {
            final int value = GLOW_INTENSITIES[i];
            glowIntensityChoices[i] = findViewById(glowIntensityIds[i]);
            glowIntensityChoices[i].setOnClickListener(v -> {
                if (glowIntensity == value) return;
                glowIntensity = value;
                choices.edit().putInt(Config.GLOW_INTENSITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] glowXIds = {R.id.glowLeft, R.id.glowCenter, R.id.glowRight};
        for (int i = 0; i < glowXIds.length; i++) {
            final int value = i;
            glowXChoices[i] = findViewById(glowXIds[i]);
            glowXChoices[i].setOnClickListener(v -> {
                if (glowX == value) return;
                glowX = value;
                choices.edit().putInt(Config.GLOW_POSITION_X_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] glowYIds = {R.id.glowTop, R.id.glowMiddle, R.id.glowBottom};
        for (int i = 0; i < glowYIds.length; i++) {
            final int value = i;
            glowYChoices[i] = findViewById(glowYIds[i]);
            glowYChoices[i].setOnClickListener(v -> {
                if (glowY == value) return;
                glowY = value;
                choices.edit().putInt(Config.GLOW_POSITION_Y_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Mirror text alignment without globally opting the launcher into RTL. Numeric rows
        // still need explicit ordering in Android's no-supportsRtl compatibility mode.
        if (getResources().getBoolean(R.bool.rtl_menu_bar))
            findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
            reverseRow(clockChoices);
            reverseRow(clockSizeChoices);
            reverseRow(dockBackgroundChoices);
            reverseRow(glowStyleChoices);
            reverseRow(glowIntensityChoices);
            reverseRow(glowXChoices);
            reverseRow(glowYChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        wallpaperDimBar = findViewById(R.id.wallpaperDimBar);
        wallpaperDimValue = findViewById(R.id.wallpaperDimValue);
        wallpaperDimBar.setProgress(choices.getInt(Config.WALLPAPER_DIM_KEY, config.wallpaperDim));
        wallpaperDimBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar bar, int progress, boolean fromUser) {
                wallpaperDimValue.setText(getString(R.string.wallpaper_dim_value, progress));
                // A TV remote moves a SeekBar with left/right, which does not start or stop a touch
                // drag; save on every step instead so the change is not lost.
                if (fromUser) {
                    choices.edit().putInt(Config.WALLPAPER_DIM_KEY, progress).apply();
                    changed();
                }
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar bar) { }
            @Override public void onStopTrackingTouch(android.widget.SeekBar bar) {
                choices.edit().putInt(Config.WALLPAPER_DIM_KEY, bar.getProgress()).apply();
                changed();
            }
        });
        wallpaperDimValue.setText(getString(R.string.wallpaper_dim_value, wallpaperDimBar.getProgress()));
        focusTintChoice = findViewById(R.id.focusTintChoice);
        focusTintChoice.setOnClickListener(v -> showFocusTintPicker());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v ->
                open(new Intent(this, HomeSettingsActivity.class)));
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 2];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_bing);
        labels[images.size() + 1] = getString(R.string.wallpaper_none);
        boolean bing = BingWallpaper.enabled(this);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = bing ? images.size()
                : "none".equals(current) ? images.size() + 1
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        chooseBing();
                        return;
                    }
                    if (which == images.size() + 1) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    BingWallpaper.setEnabled(this, false);
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    BingWallpaper.setEnabled(this, false);
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Fetches today's Bing picture first, so Home opens on it instead of an empty screen. */
    private void chooseBing() {
        Toast.makeText(this, R.string.wallpaper_bing_loading, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            boolean ok = BingWallpaper.refreshNow(this);
            runOnUiThread(() -> {
                if (ok) {
                    BingWallpaper.setEnabled(this, true);
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.wallpaper_bing_failed, Toast.LENGTH_LONG).show();
                }
                refresh();
            });
        }, "bing-wallpaper").start();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 3);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        for (int i = 0; i < clockChoices.length; i++) {
            clockChoices[i].setSelected(CLOCK_FORMATS[i].equals(clockFormat));
        }
        for (int i = 0; i < clockSizeChoices.length; i++) {
            clockSizeChoices[i].setSelected(clockSize == CLOCK_SIZES[i]);
        }
        for (int i = 0; i < dockBackgroundChoices.length; i++) {
            dockBackgroundChoices[i].setSelected(DOCK_BACKGROUNDS[i].equals(dockBackground));
        }
        for (int i = 0; i < glowStyleChoices.length; i++) {
            glowStyleChoices[i].setSelected(GLOW_STYLES[i].equals(glowStyle));
        }
        for (int i = 0; i < glowIntensityChoices.length; i++) {
            glowIntensityChoices[i].setSelected(glowIntensity == GLOW_INTENSITIES[i]);
        }
        for (int i = 0; i < glowXChoices.length; i++) {
            glowXChoices[i].setSelected(glowX == i);
        }
        for (int i = 0; i < glowYChoices.length; i++) {
            glowYChoices[i].setSelected(glowY == i);
        }
        // The opacity only tunes the tint band; Clear and Glass have their own look.
        boolean tinted = Config.DOCK_TINT.equals(dockBackground);
        for (TextView choice : dockChoices) {
            choice.setEnabled(tinted);
            choice.setAlpha(tinted ? 1f : 0.5f);
        }
        // Strength and centre do nothing while the glow is off.
        boolean glowing = !Config.GLOW_OFF.equals(glowStyle);
        for (TextView choice : glowIntensityChoices) {
            choice.setEnabled(glowing);
            choice.setAlpha(glowing ? 1f : 0.5f);
        }
        for (TextView choice : glowXChoices) {
            choice.setEnabled(glowing);
            choice.setAlpha(glowing ? 1f : 0.5f);
        }
        for (TextView choice : glowYChoices) {
            choice.setEnabled(glowing);
            choice.setAlpha(glowing ? 1f : 0.5f);
        }
        wallpaperDimBar.setProgress(choices.getInt(Config.WALLPAPER_DIM_KEY, wallpaperDimBar.getProgress()));
        String focusTint = choices.getString(Config.FOCUS_TINT_KEY, Config.FOCUS_AUTO);
        focusTintChoice.setText(getString(R.string.focus_tint_state,
                getString(focusTintLabel(focusTint))));
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        String wallpaperState = BingWallpaper.enabled(this)
                ? getString(R.string.wallpaper_bing_state)
                : "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                : wallpaperCurrent != null ? wallpaperCurrent
                : getString(R.string.wallpaper_theme_default);
        wallpaperChoice.setText(getString(R.string.wallpaper_state, wallpaperState));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
    }

    private int focusTintLabel(String value) {
        for (int i = 0; i < FOCUS_TINTS.length; i++) {
            if (FOCUS_TINTS[i].equals(value)) return FOCUS_TINT_LABELS[i];
        }
        return R.string.focus_auto;
    }

    /** One short list, like the wallpaper picker; the names say which colour each row picks. */
    private void showFocusTintPicker() {
        String chosen = choices.getString(Config.FOCUS_TINT_KEY, Config.FOCUS_AUTO);
        int checked = 0;
        String[] labels = new String[FOCUS_TINTS.length];
        for (int i = 0; i < FOCUS_TINTS.length; i++) {
            labels[i] = getString(FOCUS_TINT_LABELS[i]);
            if (FOCUS_TINTS[i].equals(chosen)) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.focus_tint_label)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    choices.edit().putString(Config.FOCUS_TINT_KEY, FOCUS_TINTS[which]).apply();
                    changed();
                    refresh();
                    d.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
