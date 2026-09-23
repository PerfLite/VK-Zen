package app.morphe.extension.vk.ota;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Over-the-air self-update for VK Zen clones, backed by GitHub Releases.
 *
 * Auto-check is throttled (once per CHECK_INTERVAL_MS) and silent: it only
 * surfaces a dialog when a strictly newer release exists. Manual check from the
 * settings screen always runs and reports the outcome.
 *
 * Installation is delegated to DownloadManager: it downloads the APK and hands
 * its content:// URI to the system installer via ACTION_VIEW. The PackageInstaller
 * session API is deliberately avoided — in a Morphe clone commit() throws "status
 * receiver should come from the same package as that of the installer", because
 * getNameForUid() resolves to the original VK package, not the clone's.
 */
public class OtaUpdater {

    private static final String TAG = "VKZenOTA";

    private static final String OWNER = "PerfLite";
    private static final String REPO = "VK-Zen";

    private static final String PREF_NAME = "morphe_ota_prefs";
    private static final String KEY_LAST_CHECK = "last_check_ms";
    private static final long CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L; // 12 hours

    private static final String INSTALL_ACTION = "app.morphe.extension.ota.INSTALL_RESULT";

    /** OTA version of this build. Bump in lockstep with the GitHub release tag. */
    public static final String CURRENT_VERSION = "1.0.5";

    private static volatile boolean autoChecking = false;
    private static volatile boolean manualChecking = false;
    private static volatile boolean downloadInProgress = false;

    private static final int ACCENT = Color.parseColor("#2688eb");

    // Palette resolved per dialog from the current night mode.
    private int colorSurface;
    private int colorStroke;
    private int colorTextPrimary;
    private int colorTextSecondary;

    private void resolvePalette(Context ctx) {
        int night = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean dark = night == Configuration.UI_MODE_NIGHT_YES;
        colorSurface = Color.parseColor(dark ? "#232324" : "#ffffff");
        colorStroke = Color.parseColor(dark ? "#333335" : "#dce1e6");
        colorTextPrimary = Color.parseColor(dark ? "#ffffff" : "#000000");
        colorTextSecondary = Color.parseColor(dark ? "#909499" : "#818c99");
    }

    /** Throttled, silent check meant to be fired from the app startup hook. */
    public static void autoCheck(final Context context) {
        if (context == null || autoChecking) return;
        final Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long last = prefs.getLong(KEY_LAST_CHECK, 0L);
        long now = System.currentTimeMillis();
        if (now - last < CHECK_INTERVAL_MS) return;
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply();

        autoChecking = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ReleaseInfo info = fetchLatest();
                    if (info != null && isNewer(info.version, CURRENT_VERSION)) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                new OtaUpdater().showUpdateDialog(topActivity(app), info);
                            }
                        });
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "autoCheck failed", t);
                } finally {
                    autoChecking = false;
                }
            }
        }, "VKZenOTA-auto").start();
    }

    /** Manual check from the settings screen; always runs and reports to the user. */
    public static void manualCheck(final Activity activity) {
        if (activity == null || manualChecking) return;
        manualChecking = true;
        Toast.makeText(activity, "Проверяю обновления…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ReleaseInfo info = null;
                Throwable error = null;
                try {
                    info = fetchLatest();
                } catch (Throwable t) {
                    error = t;
                    Log.e(TAG, "manualCheck failed", t);
                }
                final ReleaseInfo result = info;
                final Throwable err = error;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        manualChecking = false;
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (err != null || result == null) {
                            Toast.makeText(activity, "Не удалось проверить обновления", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (isNewer(result.version, CURRENT_VERSION)) {
                            new OtaUpdater().showUpdateDialog(activity, result);
                        } else {
                            Toast.makeText(activity,
                                    "У вас последняя версия (" + CURRENT_VERSION + ")",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }, "VKZenOTA-manual").start();
    }

    // ------------------------------------------------------------------
    // UI: VK-styled update dialog with Markdown changelog
    // ------------------------------------------------------------------

    private void showUpdateDialog(Activity activity, final ReleaseInfo info) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        resolvePalette(activity);

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorSurface);
        bg.setCornerRadius(dp(activity, 16));
        bg.setStroke(dp(activity, 1), colorStroke);
        root.setBackground(bg);

        TextView title = new TextView(activity);
        title.setText("Доступно обновление " + info.version);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(colorTextPrimary);
        title.setPadding(0, 0, 0, dp(activity, 12));
        root.addView(title);

        ScrollView sv = new ScrollView(activity);
        sv.setVerticalScrollBarEnabled(false);
        TextView body = new TextView(activity);
        body.setText(renderMarkdown(activity, info.changelog));
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        body.setTextColor(colorTextSecondary);
        body.setLineSpacing(dp(activity, 2), 1.0f);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, dp(activity, 16), 0, 0);

        TextView btnLater = makeTextButton(activity, "ПОЗЖЕ", colorTextSecondary);
        btnLater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        buttons.addView(btnLater);

        TextView btnInstall = makeTextButton(activity, "СКАЧАТЬ И УСТАНОВИТЬ", ACCENT);
        LinearLayout.LayoutParams lpInstall = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpInstall.setMargins(dp(activity, 8), 0, 0, 0);
        btnInstall.setLayoutParams(lpInstall);
        btnInstall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startDownload(activity, info);
            }
        });
        buttons.addView(btnInstall);

        root.addView(buttons);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f);
            int height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.60f);
            dialog.getWindow().setLayout(width, height);
        }
        dialog.show();
    }

    private TextView makeTextButton(Context ctx, String text, int color) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
        TypedValue outValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            tv.setBackgroundResource(outValue.resourceId);
        }
        return tv;
    }

    // ------------------------------------------------------------------
    // Download with a VK-styled progress dialog
    // ------------------------------------------------------------------

    private void startDownload(final Activity activity, final ReleaseInfo info) {
        if (downloadInProgress) return;
        final Context ctx = activity.getApplicationContext();

        // Android 8+ requires the user to grant "install unknown apps" to us.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (!ctx.getPackageManager().canRequestPackageInstalls()) {
                    Toast.makeText(activity, "Разрешите установку из неизвестных источников", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + ctx.getPackageName()));
                    activity.startActivity(intent);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }

        downloadInProgress = true;
        resolvePalette(activity);
        final long total = info.size;
        final ProgressUi ui = createProgressDialog(activity, total);
        ui.dialog.show();

        final DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId;
        try {
            DownloadManager.Request req =
                    new DownloadManager.Request(Uri.parse(info.downloadUrl));
            req.setMimeType("application/vnd.android.package-archive");
            req.setTitle("VK Zen " + info.version);
            req.setDescription("Скачивание обновления…");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            downloadId = dm.enqueue(req);
        } catch (Throwable t) {
            downloadInProgress = false;
            try { ui.dialog.dismiss(); } catch (Throwable ignored) {}
            Log.e(TAG, "enqueue failed", t);
            if (!activity.isFinishing()) {
                Toast.makeText(activity, "Не удалось начать загрузку: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
            return;
        }

        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri localUri = null;
                Throwable error = null;
                int lastPct = -1;
                try {
                    while (true) {
                        DownloadManager.Query q = new DownloadManager.Query().setFilterById(downloadId);
                        Cursor c = dm.query(q);
                        int stCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
                        int byCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int toCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        int urCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
                        int reCol = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON);
                        boolean done = false;
                        while (c.moveToNext()) {
                            int status = c.getInt(stCol);
                            long bytes = c.getLong(byCol);
                            long tot = c.getLong(toCol);
                            if (tot > 0) {
                                int pct = (int) (bytes * 100 / tot);
                                if (pct != lastPct) {
                                    lastPct = pct;
                                    final int fp = pct;
                                    final long fb = bytes, ft = tot;
                                    main.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (ui.pb != null) ui.pb.setProgress(fp);
                                            String mb = String.format("%.1f / %.1f МБ", fb / 1048576.0, ft / 1048576.0);
                                            ui.percent.setText(fp + "%  ·  " + mb);
                                        }
                                    });
                                }
                            }
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                String u = c.getString(urCol);
                                if (u != null) localUri = Uri.parse(u);
                                done = true;
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                error = new RuntimeException("код " + c.getInt(reCol));
                                done = true;
                            }
                        }
                        c.close();
                        if (done) break;
                        Thread.sleep(400);
                    }
                } catch (Throwable t) {
                    error = t;
                    Log.e(TAG, "download poll failed", t);
                }
                final Uri uri = localUri;
                final Throwable err = error;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        downloadInProgress = false;
                        try { ui.dialog.dismiss(); } catch (Throwable ignored) {}
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (err != null || uri == null) {
                            Toast.makeText(activity, "Скачивание не удалось", Toast.LENGTH_LONG).show();
                            return;
                        }
                        launchInstaller(activity, uri);
                    }
                });
            }
        }, "VKZenOTA-dm").start();
    }

    private static final class ProgressUi {
        Dialog dialog;
        ProgressBar pb;
        TextView percent;
    }

    private ProgressUi createProgressDialog(Activity activity, long total) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 22), dp(activity, 22), dp(activity, 22), dp(activity, 22));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorSurface);
        bg.setCornerRadius(dp(activity, 16));
        bg.setStroke(dp(activity, 1), colorStroke);
        root.setBackground(bg);

        TextView title = new TextView(activity);
        title.setText("Скачивание обновления…");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(colorTextPrimary);
        root.addView(title);

        ProgressBar pb;
        if (total > 0) {
            pb = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        } else {
            pb = new ProgressBar(activity);
            pb.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        }
        LinearLayout.LayoutParams lpPb = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpPb.topMargin = dp(activity, 16);
        pb.setLayoutParams(lpPb);
        root.addView(pb);

        TextView percent = new TextView(activity);
        percent.setText(total > 0 ? "0%" : "подготовка…");
        percent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        percent.setTextColor(colorTextSecondary);
        LinearLayout.LayoutParams lpPct = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpPct.topMargin = dp(activity, 8);
        percent.setLayoutParams(lpPct);
        root.addView(percent);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.86f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ProgressUi ui = new ProgressUi();
        ui.dialog = dialog;
        ui.pb = pb;
        ui.percent = percent;
        return ui;
    }

    // ------------------------------------------------------------------
    // Hand the downloaded APK to the system installer
    // ------------------------------------------------------------------

    private void launchInstaller(Activity activity, Uri contentUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable t) {
            Log.e(TAG, "failed to launch installer", t);
            if (!activity.isFinishing()) {
                Toast.makeText(activity,
                        "Не удалось открыть установщик: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ------------------------------------------------------------------
    // Minimal Markdown renderer (headings, bold, bullet lists)
    // ------------------------------------------------------------------

    static CharSequence renderMarkdown(Context ctx, String md) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        if (md == null) return out;
        String[] lines = md.replace("\r\n", "\n").split("\n");
        boolean first = true;
        for (String raw : lines) {
            String text = raw;
            if (text.trim().isEmpty()) {
                if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append("\n");
                continue;
            }
            if (!first) out.append("\n");
            first = false;

            int start = out.length();
            boolean heading = false;
            if (text.startsWith("### ")) {
                text = text.substring(4);
                heading = true;
            } else if (text.startsWith("## ")) {
                text = text.substring(3);
                heading = true;
            } else if (text.startsWith("# ")) {
                text = text.substring(2);
                heading = true;
            }

            boolean bullet = false;
            if (text.startsWith("- ") || text.startsWith("* ")) {
                text = text.substring(2);
                bullet = true;
            }

            if (bullet) out.append("•  ");
            int contentStart = out.length();
            appendInlineBold(out, text);
            int contentEnd = out.length();

            if (heading) {
                out.setSpan(new StyleSpan(Typeface.BOLD), contentStart, contentEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(1.12f), contentStart, contentEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (bullet) {
                out.setSpan(new BulletSpan(dp(ctx, 14)), start, contentEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return out;
    }

    private static void appendInlineBold(SpannableStringBuilder out, String text) {
        int i = 0;
        while (i < text.length()) {
            int b = text.indexOf("**", i);
            if (b < 0) {
                out.append(text.substring(i));
                break;
            }
            out.append(text, i, b);
            int e = text.indexOf("**", b + 2);
            if (e < 0) {
                out.append(text.substring(b));
                break;
            }
            int s = out.length();
            out.append(text, b + 2, e);
            out.setSpan(new StyleSpan(Typeface.BOLD), s, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            i = e + 2;
        }
    }

    // ------------------------------------------------------------------
    // Network / version helpers
    // ------------------------------------------------------------------

    private static ReleaseInfo fetchLatest() throws Exception {
        String api = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";
        HttpURLConnection conn = (HttpURLConnection) new URL(api).openConnection();
        conn.setRequestProperty("User-Agent", "VK-Zen-Updater");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        try {
            InputStream in = conn.getInputStream();
            String json = readAll(in);
            in.close();
            JSONObject obj = new JSONObject(json);
            ReleaseInfo info = new ReleaseInfo();
            info.version = normalizeVersion(obj.optString("tag_name", ""));
            info.changelog = obj.optString("body", "");
            JSONArray assets = obj.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.optJSONObject(i);
                    if (a == null) continue;
                    String name = a.optString("name", "");
                    if (name.toLowerCase().endsWith(".apk")) {
                        info.downloadUrl = a.optString("browser_download_url", "");
                        info.size = a.optLong("size", 0L);
                        break;
                    }
                }
            }
            if (info.version.isEmpty() || info.downloadUrl.isEmpty()) return null;
            return info;
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), "UTF-8");
    }

    private static String normalizeVersion(String tag) {
        if (tag == null) return "";
        String t = tag.trim();
        if (t.startsWith("v") || t.startsWith("V")) t = t.substring(1);
        return t;
    }

    /** Returns true when {@code remote} is a strictly newer semver than {@code local}. */
    static boolean isNewer(String remote, String local) {
        int[] r = parseVersion(remote);
        int[] l = parseVersion(local);
        int len = Math.max(r.length, l.length);
        for (int i = 0; i < len; i++) {
            int rv = i < r.length ? r[i] : 0;
            int lv = i < l.length ? l[i] : 0;
            if (rv != lv) return rv > lv;
        }
        return false;
    }

    private static int[] parseVersion(String v) {
        String[] parts = normalizeVersion(v).split("[^0-9]+");
        int[] out = new int[parts.length == 1 && parts[0].isEmpty() ? 1 : parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                out[i] = 0;
            }
        }
        return out;
    }

    private static Activity topActivity(Context ctx) {
        try {
            Object thread = Class.forName("android.app.ActivityThread")
                    .getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field f = thread.getClass().getDeclaredField("mActivities");
            f.setAccessible(true);
            Object mapObj = f.get(thread);
            if (mapObj instanceof java.util.Map) {
                for (Object record : ((java.util.Map<?, ?>) mapObj).values()) {
                    java.lang.reflect.Field af = record.getClass().getDeclaredField("activity");
                    af.setAccessible(true);
                    Object a = af.get(record);
                    if (a instanceof Activity) {
                        Activity act = (Activity) a;
                        if (!act.isFinishing() && !act.isDestroyed()) return act;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return ctx instanceof Activity ? (Activity) ctx : null;
    }

    private static int dp(Context ctx, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }

    private static final class ReleaseInfo {
        String version = "";
        String changelog = "";
        String downloadUrl = "";
        long size = 0L;
    }
}
