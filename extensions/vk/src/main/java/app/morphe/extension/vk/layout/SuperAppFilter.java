package app.morphe.extension.vk.layout;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SuperAppFilter {
    private static final String TAG = "MorpheSuperApp";

    /**
     * Intercepts ListData passed to SuperAppFragment.Sq(ListData).
     * Filters items to keep only Weather mini-widget and
     * Communities, Video, Clips, Settings menu items.
     * All other widgets (ads, promo banners, games, posters, etc.) are removed.
     */
    public static Object filterListData(Object listData) {
        if (listData == null) return null;
        try {
            Field listField = findFieldByType(listData.getClass(), List.class);
            if (listField == null) {
                listField = listData.getClass().getDeclaredField("a");
            }
            listField.setAccessible(true);
            Object listObj = listField.get(listData);
            if (listObj instanceof List) {
                List<?> original = (List<?>) listObj;
                List<?> filtered = filterItems(original);
                listField.set(listData, filtered);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to filter ListData", t);
        }
        return listData;
    }

    public static List<?> filterItems(List<?> items) {
        if (items == null) return null;
        List<Object> result = new ArrayList<>();

        for (Object item : items) {
            if (item == null) continue;
            String className = item.getClass().getName();

            // 1. Mini widgets (Keep only Weather)
            if (className.endsWith(".dxu0") || className.contains("MiniWidgets")) {
                if (processMiniWidgetsItem(item)) {
                    result.add(item);
                }
                continue;
            }

            // 2. Expandable service menu (SuperAppExpandableMenuItem)
            if (className.endsWith(".zuu0") || className.contains("ExpandableMenu")) {
                if (processExpandableMenuItem(item)) {
                    result.add(item);
                }
                continue;
            }

            // 3. Showcase menu item (SuperAppShowcaseMenuItem / CustomMenuInfo)
            if (className.endsWith(".lyu0") || className.contains("ShowcaseMenuItem")) {
                if (isAllowedShowcaseItem(item)) {
                    result.add(item);
                }
                continue;
            }

            // All other items (promo banners, posters, scrolls, tiles, etc.) are DROPPED.
        }

        return result;
    }

    private static boolean processMiniWidgetsItem(Object item) {
        try {
            for (Field f : item.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object widget = f.get(item);
                if (widget == null) continue;
                String wClass = widget.getClass().getName();
                if (wClass.contains("SuperAppMiniWidget")) {
                    for (Field pf : widget.getClass().getDeclaredFields()) {
                        pf.setAccessible(true);
                        Object payload = pf.get(widget);
                        if (payload == null) continue;
                        if (payload.getClass().getName().contains("Payload")) {
                            for (Field itemsField : payload.getClass().getDeclaredFields()) {
                                itemsField.setAccessible(true);
                                Object miniListObj = itemsField.get(payload);
                                if (miniListObj instanceof List) {
                                    List<?> miniList = (List<?>) miniListObj;
                                    List<Object> filteredMini = new ArrayList<>();
                                    for (Object mItem : miniList) {
                                        if (isWeatherMiniWidget(mItem)) {
                                            filteredMini.add(mItem);
                                        }
                                    }
                                    if (!filteredMini.isEmpty()) {
                                        if (miniListObj instanceof ArrayList) {
                                            ((ArrayList<?>) miniListObj).clear();
                                            //noinspection unchecked
                                            ((ArrayList<Object>) miniListObj).addAll(filteredMini);
                                        } else {
                                            itemsField.set(payload, filteredMini);
                                        }
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error filtering mini widgets", t);
        }
        return false;
    }

    private static boolean processExpandableMenuItem(Object item) {
        try {
            for (Field f : item.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object listObj = f.get(item);
                if (listObj instanceof List) {
                    List<?> menuItems = (List<?>) listObj;
                    List<Object> filtered = new ArrayList<>();
                    for (Object mItem : menuItems) {
                        if (isAllowedMenuItem(mItem)) {
                            filtered.add(mItem);
                        }
                    }
                    if (!filtered.isEmpty()) {
                        f.set(item, filtered);
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error filtering expandable menu", t);
        }
        return false;
    }

    private static boolean isWeatherMiniWidget(Object item) {
        if (item == null) return false;
        try {
            for (Field f : item.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(item);
                if (val instanceof String) {
                    String s = ((String) val).toLowerCase(Locale.ROOT);
                    if (s.contains("weather") || s.contains("погод")) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isAllowedMenuItem(Object item) {
        if (item == null) return false;
        try {
            String title = "";
            String name = "";
            String trackCode = "";

            for (Field f : item.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(item);
                if (val instanceof String) {
                    String str = (String) val;
                    String fName = f.getName();
                    if ("f".equals(fName) || "title".equalsIgnoreCase(fName)) title = str;
                    else if ("d".equals(fName) || "name".equalsIgnoreCase(fName)) name = str;
                    else if ("e".equals(fName) || "trackCode".equalsIgnoreCase(fName)) trackCode = str;
                } else if (val instanceof Enum) {
                    if ("MORE_ITEM".equals(((Enum<?>) val).name())) {
                        return false;
                    }
                }
            }

            String all = (title + " " + name + " " + trackCode).toLowerCase(Locale.ROOT);

            // 1. Communities / Groups
            if (all.contains("communities") || all.contains("groups") || all.contains("сообществ") || all.contains("групп")) {
                return true;
            }
            // 2. Video
            if (all.contains("video") || all.contains("видео")) {
                return true;
            }
            // 3. Clips
            if (all.contains("clips") || all.contains("клип")) {
                return true;
            }
            // 4. Settings
            if (all.contains("setting") || all.contains("настройк")) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isAllowedShowcaseItem(Object item) {
        if (item == null) return false;
        try {
            for (Field f : item.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object info = f.get(item);
                if (info != null && info.getClass().getName().contains("CustomMenuInfo")) {
                    return isAllowedMenuItem(info);
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static Field findFieldByType(Class<?> clazz, Class<?> type) {
        for (Field f : clazz.getDeclaredFields()) {
            if (type.isAssignableFrom(f.getType())) {
                return f;
            }
        }
        return null;
    }
}
