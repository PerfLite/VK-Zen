package app.morphe.extension.vk.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NavigationFilter {

    private static final String PREF_NAME = "morphe_tabbar_prefs";
    private static final String KEY_TABS = "user_tabs";
    private static final String KEY_TABS_VERSION = "tabs_version";
    private static final int TABS_VERSION = 2;
    private static final String DEFAULT_TABS = "home,friends,im,music,hub";

    public static final String[] ALL_TAB_KEYS = {
        "home", "friends", "im", "music", "hub", "groups", "profile", "clips", "video"
    };

    public static final String[] ALL_TAB_LABELS = {
        "Главная (Новости)",
        "Друзья",
        "Сообщения (Чаты)",
        "Музыка",
        "Сервисы",
        "Сообщества (Группы)",
        "Профиль",
        "Клипы",
        "Видео"
    };

    /**
     * Filters and customizes TabbarState items according to user preferences.
     * Called whenever TabbarState.c() (getItems) is queried.
     */
    public static List<?> filterTabbarStateItems(Object tabbarState) {
        if (tabbarState == null) {
            return null;
        }
        try {
            Field f = tabbarState.getClass().getDeclaredField("items");
            f.setAccessible(true);
            Object itemsObj = f.get(tabbarState);
            if (itemsObj instanceof List) {
                return filterTabbarItems((List<?>) itemsObj);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Filters and customizes TabbarState items according to user preferences.
     */
    public static List<?> filterTabbarItems(List<?> items) {
        if (items == null) {
            return null;
        }

        SharedPreferences prefs = getPrefs(null);
        String savedTabs = getEffectiveTabs(prefs);

        String[] requestedOrder = savedTabs.split(",");
        ArrayList<Object> result = new ArrayList<>();
        Set<String> addedKeys = new HashSet<>();

        Class<?> itemClass = !items.isEmpty() ? items.get(0).getClass() : null;
        if (itemClass == null) {
            try {
                itemClass = Class.forName("com.vk.tabbar.core.api.domain.TabbarItem");
            } catch (Throwable ignored) {}
        }

        for (String reqKey : requestedOrder) {
            String key = reqKey.trim().toLowerCase();
            if (key.isEmpty() || addedKeys.contains(key)) {
                continue;
            }

            // 1. Look for existing item in original list
            Object matchingItem = null;
            for (Object it : items) {
                if (it != null && key.equals(getItemName(it))) {
                    matchingItem = it;
                    break;
                }
            }

            if (matchingItem != null) {
                result.add(matchingItem);
                addedKeys.add(key);
            } else if (itemClass != null) {
                // 2. Synthesize TabbarItem if requested (e.g. music, groups, video)
                Object synthesized = createTabbarItem(itemClass, key);
                if (synthesized != null) {
                    result.add(synthesized);
                    addedKeys.add(key);
                }
            }

            if (result.size() >= 5) {
                break;
            }
        }

        if (result.isEmpty()) {
            return items;
        }

        return result;
    }

    private static String getItemName(Object item) {
        try {
            Method m = item.getClass().getMethod("c");
            Object val = m.invoke(item);
            if (val instanceof String) {
                return ((String) val).toLowerCase();
            }
        } catch (Throwable ignored) {}

        try {
            Field f = item.getClass().getDeclaredField("name");
            f.setAccessible(true);
            Object val = f.get(item);
            if (val instanceof String) {
                return ((String) val).toLowerCase();
            }
        } catch (Throwable ignored) {}

        String repr = item.toString().toLowerCase();
        for (String key : ALL_TAB_KEYS) {
            if (repr.contains("name=" + key) || repr.contains("id=" + key)) {
                return key;
            }
        }
        return "";
    }

    private static Object createTabbarItem(Class<?> itemClass, String name) {
        try {
            if (itemClass == null) {
                try {
                    itemClass = Class.forName("com.vk.tabbar.core.api.domain.TabbarItem");
                } catch (Throwable ignored) {}
            }
            if (itemClass == null) return null;
            String title = getTabTitle(name);
            for (Constructor<?> ctor : itemClass.getConstructors()) {
                Class<?>[] pTypes = ctor.getParameterTypes();
                if (pTypes.length == 5 && pTypes[0] == String.class && pTypes[3] == boolean.class) {
                    return ctor.newInstance(name, null, title, true, null);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static String getEffectiveTabs(SharedPreferences prefs) {
        if (prefs == null) {
            return DEFAULT_TABS;
        }
        int ver = prefs.getInt(KEY_TABS_VERSION, 0);
        if (ver < TABS_VERSION) {
            prefs.edit().putString(KEY_TABS, DEFAULT_TABS).putInt(KEY_TABS_VERSION, TABS_VERSION).apply();
            return DEFAULT_TABS;
        }
        String saved = prefs.getString(KEY_TABS, null);
        if (saved == null || saved.trim().isEmpty()) {
            return DEFAULT_TABS;
        }
        return saved;
    }

    public static String getTabTitle(String key) {
        if ("home".equalsIgnoreCase(key)) return "Главная";
        if ("friends".equalsIgnoreCase(key)) return "Друзья";
        if ("im".equalsIgnoreCase(key)) return "Сообщения";
        if ("music".equalsIgnoreCase(key)) return "Музыка";
        if ("hub".equalsIgnoreCase(key)) return "Сервисы";
        if ("groups".equalsIgnoreCase(key)) return "Сообщества";
        if ("profile".equalsIgnoreCase(key)) return "Профиль";
        if ("clips".equalsIgnoreCase(key)) return "Клипы";
        if ("video".equalsIgnoreCase(key)) return "Видео";
        return key;
    }

    /**
     * Intercepts VK TabbarSettingsRouter to show full VK Zen settings page.
     */
    public static void openSettings(final Context context) {
        if (context == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = new Intent(context, app.morphe.extension.vk.settings.VkZenSettingsActivity.class);
                    if (!(context instanceof Activity)) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(intent);
                    return;
                } catch (Throwable t) {
                    android.util.Log.e("MorpheNav", "Failed to launch VkZenSettingsActivity, fallback to dialog", t);
                }
                Activity activity = findActivity(context);
                if (activity != null) {
                    showTabbarSettingsDialog(activity);
                }
            }
        });
    }

    public static void onTabbarSettingsFragmentCreated(final Object fragment) {
        if (fragment == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Activity activity = null;
                try {
                    Method getActivity = fragment.getClass().getMethod("getActivity");
                    activity = (Activity) getActivity.invoke(fragment);
                } catch (Throwable ignored) {}
                try {
                    Method dismiss = fragment.getClass().getMethod("dismiss");
                    dismiss.invoke(fragment);
                } catch (Throwable ignored) {
                    try {
                        Method dismiss = fragment.getClass().getMethod("dismissAllowingStateLoss");
                        dismiss.invoke(fragment);
                    } catch (Throwable ignored2) {}
                }
                if (activity != null) {
                    openSettings(activity);
                }
            }
        });
    }

    private static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof android.content.ContextWrapper) {
            Context base = ((android.content.ContextWrapper) context).getBaseContext();
            if (base instanceof Activity) {
                return (Activity) base;
            }
            if (base != null && base != context) {
                return findActivity(base);
            }
        }
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            java.util.Map<?, ?> activities = (java.util.Map<?, ?>) activitiesField.get(activityThread);
            if (activities != null) {
                for (Object record : activities.values()) {
                    Field activityField = record.getClass().getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity act = (Activity) activityField.get(record);
                    if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                        return act;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Called from MainActivity.onResume to attach long click listener to tabbar.
     */
    public static void onMainActivityResume(final Activity activity) {
        if (activity == null) return;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    setupTabbarLongClick(activity);
                } catch (Throwable ignored) {}
            }
        }, 400);
    }

    private static void setupTabbarLongClick(final Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        View tabbar = null;
        try {
            int id = activity.getResources().getIdentifier("tabbar", "id", activity.getPackageName());
            if (id != 0) {
                tabbar = activity.findViewById(id);
            }
        } catch (Throwable ignored) {}

        if (tabbar == null) {
            tabbar = findTabbarView(activity.getWindow().getDecorView());
        }

        if (tabbar != null) {
            View.OnLongClickListener listener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    openSettings(activity);
                    return true;
                }
            };
            tabbar.setOnLongClickListener(listener);
            attachLongClickListenerToChildren(tabbar, listener);
        }
    }

    private static View findTabbarView(View root) {
        if (root == null) return null;
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            int childCount = vg.getChildCount();
            if (childCount >= 3 && childCount <= 6) {
                int height = vg.getHeight();
                if (height > 50 && height < 300) {
                    return vg;
                }
            }
            for (int i = 0; i < childCount; i++) {
                View found = findTabbarView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void attachLongClickListenerToChildren(View v, View.OnLongClickListener listener) {
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                child.setOnLongClickListener(listener);
                attachLongClickListenerToChildren(child, listener);
            }
        }
    }

    public static void showTabbarSettingsDialog(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        boolean hideFriends = app.morphe.extension.vk.friends.FriendsFilter.isHideFriendSuggestionsEnabled();
        String[] menuItems = new String[] {
            "Настройка нижней панели (вкладки 1-5)",
            "Скрыть «Возможные друзья»: " + (hideFriends ? "ВКЛ (скрыты)" : "ВЫКЛ (показаны)")
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Настройки Morphe");
        builder.setItems(menuItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    openTabbarSlotsPicker(activity);
                } else if (which == 1) {
                    boolean newVal = app.morphe.extension.vk.friends.FriendsFilter.toggleHideFriendSuggestions();
                    Toast.makeText(activity,
                            newVal ? "«Возможные друзья» отключены!" : "«Возможные друзья» включены!",
                            Toast.LENGTH_SHORT).show();
                    showTabbarSettingsDialog(activity);
                }
            }
        });
        builder.setNegativeButton("Закрыть", null);
        builder.show();
    }

    private static void openTabbarSlotsPicker(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        SharedPreferences prefs = getPrefs(activity);
        String current = getEffectiveTabs(prefs);
        String[] parts = current.split(",");
        final String[] slots = new String[5];
        String[] defaults = DEFAULT_TABS.split(",");
        for (int i = 0; i < 5; i++) {
            if (i < parts.length && !parts[i].trim().isEmpty()) {
                slots[i] = parts[i].trim().toLowerCase();
            } else if (i < defaults.length) {
                slots[i] = defaults[i];
            } else {
                slots[i] = "home";
            }
        }

        showSlotsDialog(activity, slots);
    }

    private static void showSlotsDialog(final Activity activity, final String[] slots) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        String[] itemTexts = new String[5];
        for (int i = 0; i < 5; i++) {
            itemTexts[i] = "Вкладка " + (i + 1) + ": " + getTabLabelByKey(slots[i]);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Настройка нижней панели (все 5)");
        builder.setItems(itemTexts, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int whichSlot) {
                int selectedIdx = -1;
                for (int j = 0; j < ALL_TAB_KEYS.length; j++) {
                    if (ALL_TAB_KEYS[j].equalsIgnoreCase(slots[whichSlot])) {
                        selectedIdx = j;
                        break;
                    }
                }

                AlertDialog.Builder slotPicker = new AlertDialog.Builder(activity);
                slotPicker.setTitle("Выберите вкладку " + (whichSlot + 1));
                slotPicker.setSingleChoiceItems(ALL_TAB_LABELS, selectedIdx, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface pickerDialog, int whichOption) {
                        slots[whichSlot] = ALL_TAB_KEYS[whichOption];
                        pickerDialog.dismiss();
                        showSlotsDialog(activity, slots);
                    }
                });
                slotPicker.setNegativeButton("Назад", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface pickerDialog, int w) {
                        showSlotsDialog(activity, slots);
                    }
                });
                slotPicker.show();
            }
        });

        builder.setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(slots[i]);
                }
                SharedPreferences p = getPrefs(activity);
                if (p != null) {
                    p.edit().putString(KEY_TABS, sb.toString()).putInt(KEY_TABS_VERSION, TABS_VERSION).commit();
                }
                Toast.makeText(activity, "Вкладки сохранены! Перезапуск...", Toast.LENGTH_SHORT).show();
                restartApp(activity);
            }
        });

        builder.setNeutralButton("Сброс", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences p = getPrefs(activity);
                if (p != null) {
                    p.edit().putString(KEY_TABS, DEFAULT_TABS).putInt(KEY_TABS_VERSION, TABS_VERSION).commit();
                }
                Toast.makeText(activity, "Сброшено по умолчанию! Перезапуск...", Toast.LENGTH_SHORT).show();
                restartApp(activity);
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    public static void saveTabs(Context context, String tabs) {
        SharedPreferences p = getPrefs(context);
        if (p != null) {
            p.edit().putString(KEY_TABS, tabs).putInt(KEY_TABS_VERSION, TABS_VERSION).commit();
        }
    }

    public static void restartApp(final Activity activity) {
        if (activity == null) return;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        Runtime.getRuntime().exit(0);
                    } else {
                        activity.recreate();
                    }
                } catch (Throwable t) {
                    try {
                        activity.recreate();
                    } catch (Throwable ignored) {}
                }
            }
        }, 300);
    }

    public static String getTabLabelByKey(String key) {
        for (int i = 0; i < ALL_TAB_KEYS.length; i++) {
            if (ALL_TAB_KEYS[i].equalsIgnoreCase(key)) {
                return ALL_TAB_LABELS[i];
            }
        }
        return key;
    }

    public static SharedPreferences getPrefs(Context context) {
        if (context == null) {
            try {
                context = (Context) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null);
            } catch (Throwable ignored) {}
        }
        if (context == null) {
            try {
                context = (Context) Class.forName("android.app.AppGlobals")
                        .getMethod("getInitialApplication")
                        .invoke(null);
            } catch (Throwable ignored) {}
        }
        if (context == null) {
            try {
                Class<?> holder = Class.forName("xsna.bf3");
                Field f = holder.getDeclaredField("a");
                f.setAccessible(true);
                context = (Context) f.get(null);
            } catch (Throwable ignored) {}
        }
        if (context != null) {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        return null;
    }
}
