package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Bing's photo of the day as Home's wallpaper (Launcher settings, Wallpaper, Bing photo of the day).
 * One small JSON request and one JPEG a day, kept in the launcher's own files: the picture is a few
 * hundred KB on disk and decoded by {@link WallpaperPlayer} like any wallpaper, so it costs no more
 * memory than a theme's picture. The caption Bing sends for it is shown by WallpaperPlayer.
 */
final class BingWallpaper {
    static final String ENABLED_KEY = "bing_wallpaper";
    private static final String CAPTION_KEY = "bing_wallpaper_caption";
    private static final String DATE_KEY = "bing_wallpaper_date";
    /** The one file kept on the TV: a new day overwrites it, so the wallpaper never grows. */
    static final String FILE_NAME = "bing.jpg";
    private static final String FOLDER = "bing";
    private static final String API =
            "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-IN";
    private static final String TAG = "TvHome.Bing";
    private static final int MAX_IMAGE = 8 * 1024 * 1024;
    private static final int MAX_JSON = 256 * 1024;

    private BingWallpaper() {}

    static boolean enabled(Context ctx) {
        return prefs(ctx).getBoolean(ENABLED_KEY, false);
    }

    static void setEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(ENABLED_KEY, enabled).apply();
    }

    /** The sentence Bing sent for the picture on screen; empty when it has none. */
    static String caption(Context ctx) {
        return prefs(ctx).getString(CAPTION_KEY, "");
    }

    static File file(Context ctx) {
        return new File(new File(ctx.getFilesDir(), FOLDER), FILE_NAME);
    }

    /** Today's picture is on the TV already: no request, no wait. */
    private static boolean current(Context ctx) {
        SharedPreferences prefs = prefs(ctx);
        File file = file(ctx);
        return LocalDate.now().toString().equals(prefs.getString(DATE_KEY, ""))
                && file.isFile() && file.length() > 0;
    }

    /**
     * Downloads today's picture in the background when the one on the TV is not from today, then runs
     * {@code done} on the main thread. Home starts on the cached picture and swaps it when this lands.
     */
    static void refreshAsync(Context ctx, Runnable done) {
        if (!enabled(ctx) || current(ctx)) return;
        new Thread(() -> {
            refreshNow(ctx);
            if (done != null) new Handler(Looper.getMainLooper()).post(done);
        }, "bing-wallpaper").start();
    }

    /**
     * Downloads today's picture if needed. Returns true when a usable picture is on disk afterwards.
     * Called off the UI thread (Launcher settings waits for it before switching the wallpaper on).
     */
    static boolean refreshNow(Context ctx) {
        if (current(ctx)) return true;
        File out = file(ctx);
        try {
            JSONObject image = image(ctx);
            if (image == null) return false;
            String url = image.optString("url", "");
            if (url.isEmpty()) return false;
            if (!url.startsWith("http")) url = "https://www.bing.com" + url;
            byte[] bytes = get(url, MAX_IMAGE);
            if (bytes == null || bytes.length < 1024) return false;
            // Refuse anything that is not a picture: a captive portal's HTML page must not become the wallpaper.
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth < 320 || bounds.outHeight < 180) return false;
            File folder = out.getParentFile();
            if (folder != null && !folder.isDirectory() && !folder.mkdirs()) return false;
            File tmp = new File(folder, FILE_NAME + ".tmp");
            try (FileOutputStream stream = new FileOutputStream(tmp)) {
                stream.write(bytes);
            }
            if (!tmp.renameTo(out)) {
                tmp.delete();
                return false;
            }
            // The sentence under the picture: Bing's description ("Aerial view of Rose Island, The Bahamas"),
            // with the photo credit that trails it in brackets dropped; its headline is the fallback.
            String caption = image.optString("copyright", "").trim();
            int credit = caption.lastIndexOf('(');
            if (credit > 0 && caption.endsWith(")")) caption = caption.substring(0, credit).trim();
            if (caption.isEmpty()) caption = image.optString("title", "").trim();
            prefs(ctx).edit()
                    .putString(CAPTION_KEY, caption)
                    .putString(DATE_KEY, LocalDate.now().toString())
                    .apply();
            return true;
        } catch (Exception | OutOfMemoryError e) {
            Log.w(TAG, "refresh: " + e);
            return false;
        }
    }

    private static JSONObject image(Context ctx) throws Exception {
        byte[] body = get(API, MAX_JSON);
        if (body == null) return null;
        JSONArray images = new JSONObject(new String(body, StandardCharsets.UTF_8)).optJSONArray("images");
        return images == null || images.length() == 0 ? null : images.optJSONObject(0);
    }

    /** One HTTPS GET with a short timeout and a browser user agent (Bing answers 403 without one). */
    private static byte[] get(String address, int limit) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                Log.w(TAG, "HTTP " + status + " for " + address);
                return null;
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int total = 0, count;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > limit) return null;
                    out.write(buffer, 0, count);
                }
                return out.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }
}
