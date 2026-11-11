package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Names given to apps on Home; they override config.xml app_labels and survive restarts. */
final class AppNames {
    private static final String PREFIX = "app_name:";

    private AppNames() {}

    /** Package -> name for every renamed app. */
    static Map<String, String> load(Context ctx) {
        SharedPreferences prefs = prefs(ctx);
        Map<String, String> names = new HashMap<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith(PREFIX) || !(entry.getValue() instanceof String)) continue;
            String name = ((String) entry.getValue()).trim();
            if (!name.isEmpty()) names.put(entry.getKey().substring(PREFIX.length()), name);
        }
        return names.isEmpty() ? Collections.emptyMap() : names;
    }

    /** Saves a name; an empty one removes the override so the app's own label comes back. */
    static void save(Context ctx, String pkg, String name) {
        String clean = name == null ? "" : name.trim();
        SharedPreferences.Editor edit = prefs(ctx).edit();
        if (clean.isEmpty()) edit.remove(PREFIX + pkg);
        else edit.putString(PREFIX + pkg, clean);
        edit.apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }
}
