package app.morphe.extension.vk.layout;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
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

            // 1. Mini widgets (Keep only Weather)
            if (isMiniWidgetsItem(item)) {
                if (processMiniWidgetsItem(item)) {
                    result.add(item);
                }
                continue;
            }

            // 2. Expandable service menu (SuperAppExpandableMenuItem)
            if (isExpandableMenuItem(item)) {
                if (processExpandableMenuItem(item)) {
                    result.add(item);
                }
                continue;
            }

            // 3. Showcase menu item (SuperAppShowcaseMenuItem / CustomMenuInfo)
            if (isShowcaseMenuItem(item)) {
                if (isAllowedShowcaseItem(item)) {
                    result.add(item);
                }
                continue;
            }

            // 4. Standalone menu item check
            if (isAllowedMenuItem(item)) {
                result.add(item);
                continue;
            }

            // All other items (promo banners, posters, scrolls, tiles, etc.) are DROPPED.
        }

        // Safety fallback: if whitelist filtered out all items, NEVER leave the user with an empty screen!
        if (result.isEmpty() && !items.isEmpty()) {
            Log.w(TAG, "SuperAppFilter whitelist produced empty result from " + items.size() + " items! Falling back to blacklist filter.");
            for (Object item : items) {
                if (item == null || isSuperAppAdOrPromoItem(item)) {
                    continue;
                }
                result.add(item);
            }
        }

        return result;
    }

    private static boolean isMiniWidgetsItem(Object item) {
        String className = item.getClass().getName();
        if (className.endsWith(".dxu0") || className.endsWith(".myx0") || className.contains("MiniWidgets")) {
            return true;
        }
        for (Field f : item.getClass().getDeclaredFields()) {
            if (f.getType().getName().contains("SuperAppMiniWidget")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExpandableMenuItem(Object item) {
        String className = item.getClass().getName();
        if (className.endsWith(".zuu0") || className.endsWith(".jwx0") || className.contains("ExpandableMenu")) {
            return true;
        }
        for (Field f : item.getClass().getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object listObj = f.get(item);
                    if (listObj instanceof List && !((List<?>) listObj).isEmpty()) {
                        Object first = ((List<?>) listObj).get(0);
                        if (first != null) {
                            String fName = first.getClass().getName();
                            if (fName.contains("Expandable") || hasFieldOfType(first.getClass(), "MenuItem")) {
                                return true;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
        return false;
    }

    private static boolean isShowcaseMenuItem(Object item) {
        String className = item.getClass().getName();
        if (className.endsWith(".lyu0") || className.endsWith(".rzx0") || className.endsWith(".tzx0")
                || className.endsWith(".wzx0") || className.contains("ShowcaseMenuItem")) {
            return true;
        }
        for (Field f : item.getClass().getDeclaredFields()) {
            if (f.getType().getName().contains("CustomMenuInfo")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFieldOfType(Class<?> clazz, String typeSubstring) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getType().getName().contains(typeSubstring)) {
                return true;
            }
        }
        return false;
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
                if (!List.class.isAssignableFrom(f.getType())) continue;
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
                        if (listObj instanceof ArrayList) {
                            ((ArrayList<?>) listObj).clear();
                            //noinspection unchecked
                            ((ArrayList<Object>) listObj).addAll(filtered);
                        } else {
                            f.set(item, filtered);
                        }
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
            StringBuilder sb = new StringBuilder();
            collectStrings(item, sb, 0);
            String all = sb.toString().toLowerCase(Locale.ROOT);
            return all.contains("weather") || all.contains("погод");
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isAllowedMenuItem(Object item) {
        if (item == null) return false;
        try {
            // Check for MORE_ITEM enum to exclude "More" / "Ещё" button
            if (isMoreItem(item)) {
                return false;
            }

            StringBuilder sb = new StringBuilder();
            collectStrings(item, sb, 0);
            String all = sb.toString().toLowerCase(Locale.ROOT);

            // 1. Communities / Groups
            if (all.contains("communities") || all.contains("groups") || all.contains("group")
                    || all.contains("сообществ") || all.contains("групп")) {
                return true;
            }
            // 2. Video
            if (all.contains("video") || all.contains("videos") || all.contains("видео")) {
                return true;
            }
            // 3. Clips
            if (all.contains("clip") || all.contains("clips") || all.contains("клип")) {
                return true;
            }
            // 4. Settings
            if (all.contains("setting") || all.contains("settings") || all.contains("настройк")) {
                return true;
            }
            // 5. Bookmarks / Fave
            if (all.contains("bookmark") || all.contains("bookmarks") || all.contains("fave") || all.contains("закладк")) {
                return true;
            }
            // 6. Music / Audio
            if (all.contains("music") || all.contains("audio") || all.contains("музык")) {
                return true;
            }
            // 7. Friends
            if (all.contains("friend") || all.contains("friends") || all.contains("друзь")) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isMoreItem(Object item) {
        try {
            for (Field f : item.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(item);
                if (val instanceof Enum && "MORE_ITEM".equals(((Enum<?>) val).name())) {
                    return true;
                }
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
        return isAllowedMenuItem(item);
    }

    private static void collectStrings(Object obj, StringBuilder sb, int depth) {
        if (obj == null || depth > 2) return;
        Class<?> clazz = obj.getClass();
        String cName = clazz.getName();
        if (cName.startsWith("java.lang.") && !(obj instanceof String)) return;
        if (cName.startsWith("android.graphics.")) return;

        if (obj instanceof String) {
            sb.append(' ').append(obj);
            return;
        }

        try {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val == null) continue;
                if (val instanceof String) {
                    sb.append(' ').append(val);
                } else if (val instanceof Enum) {
                    sb.append(' ').append(((Enum<?>) val).name());
                } else if (depth < 2 && !f.getType().isPrimitive() && !Collection.class.isAssignableFrom(f.getType())) {
                    collectStrings(val, sb, depth + 1);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isSuperAppAdOrPromoItem(Object item) {
        if (item == null) return false;
        String name = item.getClass().getName();
        if (name.contains("Promo") || name.contains("Banner") || name.contains("Ad")
                || name.contains("MarketProduct") || name.contains("Poster")
                || name.contains("SubscribeTile") || name.contains("Onboarding")) {
            return true;
        }
        try {
            StringBuilder sb = new StringBuilder();
            collectStrings(item, sb, 0);
            String all = sb.toString().toLowerCase(Locale.ROOT);
            if (all.contains("promo") || all.contains("реклам") || all.contains("акция") || all.contains("скидк")) {
                return true;
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
