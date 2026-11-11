package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The pictures shipped in the launcher's {@code assets/icons/} and one app's pick among them.
 *
 * <p>This build carries the owner's own icon set only: the ADW/Nova icon pack reader is gone, so
 * there is nothing to install and nothing to parse. A per-app pick is {@code icon.<package> =
 * asset:<file>}; no entry (or {@link #OWN}) keeps the app's own artwork.
 */
final class IconPacks {
    private static final String TAG = "TvHome.Icons";

    /** Prefix of a per-app choice. */
    static final String CHOSEN = "icon.";
    /** Picked "App's own picture": no bundled picture replaces this app's art. */
    static final String OWN = "own";
    /** A picture shipped inside the launcher, in {@code assets/icons/}: {@code icon.<package> = asset:<file>}. */
    static final String ASSET = "asset:";

    private IconPacks() {}

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    /** The bundled picture chosen for one app, or null when it keeps its own artwork. */
    static Drawable iconFor(Context ctx, String pkg) {
        String chosen = prefs(ctx).getString(CHOSEN + pkg, null);
        if (chosen == null || OWN.equals(chosen) || !chosen.startsWith(ASSET)) return null;
        return fromAsset(ctx, chosen.substring(ASSET.length()));
    }

    /** The picture files shipped in the launcher's assets/icons/, sorted by name. */
    static List<String> bundled(Context ctx) {
        List<String> names = new ArrayList<>();
        try {
            String[] files = ctx.getAssets().list("icons");
            if (files != null) {
                for (String file : files) {
                    String lower = file.toLowerCase(Locale.ROOT);
                    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                            || lower.endsWith(".webp")) {
                        names.add(file);
                    }
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "bundled icons: " + e);
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** A picture shipped in the launcher; decoded at its own size (the tiles fit it themselves). */
    static Drawable fromAsset(Context ctx, String file) {
        try (InputStream in = ctx.getAssets().open("icons/" + file)) {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            return bitmap == null ? null : new BitmapDrawable(ctx.getResources(), bitmap);
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.w(TAG, "bundled icon " + file + ": " + e);
            return null;
        }
    }

    /** "icon-01.png" as a readable grid label: "ICON-01". */
    static String label(String file) {
        return file.replaceFirst("(?i)\\.(png|jpe?g|webp)$", "").trim().toUpperCase(Locale.ROOT);
    }
}
