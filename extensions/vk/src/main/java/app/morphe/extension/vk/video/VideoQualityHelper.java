package app.morphe.extension.vk.video;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.vk.layout.NavigationFilter;

public class VideoQualityHelper {

    private static final String TAG = "VideoQualityHelper";
    public static final String KEY_VIDEO_QUALITY = "pref_video_quality";
    public static final String DEFAULT_QUALITY = "auto";

    public static final String[] QUALITY_VALUES = {
        "auto", "1080", "720", "480", "360"
    };

    public static final String[] QUALITY_TITLES = {
        "Авто (по умолчанию)",
        "1080p Full HD",
        "720p HD",
        "480p",
        "360"
    };

    public static final String[] QUALITY_SUBTITLES = {
        "Стандартное поведение VK (адаптивный битрейт)",
        "Максимальная четкость",
        "Оптимальный баланс качества и трафика",
        "Среднее качество",
        "Базовое качество (экономия трафика)"
    };

    private static final Pattern HEIGHT_PATTERN = Pattern.compile("height:\\s*(\\d+)");

    public static String getPreferredQualitySetting() {
        try {
            SharedPreferences prefs = NavigationFilter.getPrefs(null);
            if (prefs != null) {
                return prefs.getString(KEY_VIDEO_QUALITY, DEFAULT_QUALITY);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error getting preferred quality setting", t);
        }
        return DEFAULT_QUALITY;
    }

    public static void savePreferredQuality(Context context, String quality) {
        try {
            SharedPreferences prefs = NavigationFilter.getPrefs(context);
            if (prefs != null) {
                prefs.edit().putString(KEY_VIDEO_QUALITY, quality).commit();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error saving preferred quality setting", t);
        }
    }

    public static String getQualityTitle(String value) {
        for (int i = 0; i < QUALITY_VALUES.length; i++) {
            if (QUALITY_VALUES[i].equalsIgnoreCase(value)) {
                return QUALITY_TITLES[i];
            }
        }
        return QUALITY_TITLES[0];
    }

    public static String getQualitySubtitle(String value) {
        for (int i = 0; i < QUALITY_VALUES.length; i++) {
            if (QUALITY_VALUES[i].equalsIgnoreCase(value)) {
                return QUALITY_SUBTITLES[i];
            }
        }
        return QUALITY_SUBTITLES[0];
    }

    /**
     * Intercepts player quality selection in xsna.soe0.c(OneVideoPlayer, int).
     * If originalQuality <= 0 (i.e. -2 or -4 representing auto / initial preparation),
     * it resolves the best matching track resolution according to user preference.
     * If user explicitly selected a positive resolution, it is preserved.
     */
    public static int getPreferredQuality(Object player, int originalQuality) {
        try {
            // Do not override explicit user selection during active playback
            if (originalQuality > 0) {
                return originalQuality;
            }

            String pref = getPreferredQualitySetting();
            if (DEFAULT_QUALITY.equals(pref) || pref == null || pref.isEmpty()) {
                return originalQuality;
            }

            int targetHeight;
            try {
                targetHeight = Integer.parseInt(pref);
            } catch (NumberFormatException e) {
                return originalQuality;
            }

            if (player == null) {
                return originalQuality;
            }

            // Retrieve available tracks: try player.e0() or player.z()
            List<?> tracks = null;
            try {
                Method e0 = player.getClass().getMethod("e0");
                tracks = (List<?>) e0.invoke(player);
            } catch (Throwable ignored) {}

            if (tracks == null || tracks.isEmpty()) {
                try {
                    Method z = player.getClass().getMethod("z");
                    tracks = (List<?>) z.invoke(player);
                } catch (Throwable ignored) {}
            }

            if (tracks == null || tracks.isEmpty()) {
                return originalQuality;
            }

            List<Integer> availableHeights = new ArrayList<>();
            for (Object track : tracks) {
                int h = extractTrackHeight(track);
                if (h > 0 && !availableHeights.contains(h)) {
                    availableHeights.add(h);
                }
            }

            if (availableHeights.isEmpty()) {
                return originalQuality;
            }

            // 1. Exact match available
            if (availableHeights.contains(targetHeight)) {
                Log.d(TAG, "Exact preferred quality match: " + targetHeight);
                return targetHeight;
            }

            // 2. Best available <= targetHeight
            int best = -1;
            for (int h : availableHeights) {
                if (h <= targetHeight && h > best) {
                    best = h;
                }
            }

            // 3. If target is lower than all available, pick lowest available
            if (best == -1) {
                best = Collections.min(availableHeights);
            }

            if (best > 0) {
                Log.d(TAG, "Applying preferred quality: " + best + " (target was: " + targetHeight + ")");
                return best;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in getPreferredQuality", t);
        }
        return originalQuality;
    }

    public static int extractTrackHeight(Object track) {
        if (track == null) return -1;
        try {
            Field cField = null;
            Class<?> clazz = track.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    cField = clazz.getDeclaredField("c");
                    break;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (cField != null) {
                cField.setAccessible(true);
                Object format = cField.get(track);
                if (format != null) {
                    // Try method b() -> FrameSize -> getHeight()
                    try {
                        Method bMethod = format.getClass().getMethod("b");
                        Object frameSize = bMethod.invoke(format);
                        if (frameSize != null) {
                            Method getHeight = frameSize.getClass().getMethod("getHeight");
                            return (Integer) getHeight.invoke(frameSize);
                        }
                    } catch (Throwable ignored) {}

                    // Try method c() -> android.util.Size -> getHeight()
                    try {
                        Method cMethod = format.getClass().getMethod("c");
                        Object size = cMethod.invoke(format);
                        if (size instanceof android.util.Size) {
                            return ((android.util.Size) size).getHeight();
                        }
                    } catch (Throwable ignored) {}

                    // Try regex from format.toString()
                    try {
                        String s = format.toString();
                        Matcher m = HEIGHT_PATTERN.matcher(s);
                        if (m.find()) {
                            return Integer.parseInt(m.group(1));
                        }
                    } catch (Throwable ignored) {}
                }
            }

            // Fallback: check track.toString()
            String ts = track.toString();
            Matcher m = HEIGHT_PATTERN.matcher(ts);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to extract track height from " + track, t);
        }
        return -1;
    }
}
