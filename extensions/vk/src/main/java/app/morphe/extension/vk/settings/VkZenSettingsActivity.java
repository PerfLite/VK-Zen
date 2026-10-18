package app.morphe.extension.vk.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import app.morphe.extension.vk.layout.NavigationFilter;

public class VkZenSettingsActivity extends Activity {

    private boolean isDark;
    private int colorBg;
    private int colorSurface;
    private int colorStroke;
    private int colorTextPrimary;
    private int colorTextSecondary;
    private int colorAccent;

    private LinearLayout slotsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Detect dark mode
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;

        // VK color palette
        colorBg = isDark ? Color.parseColor("#19191a") : Color.parseColor("#edeef0");
        colorSurface = isDark ? Color.parseColor("#232324") : Color.parseColor("#ffffff");
        colorStroke = isDark ? Color.parseColor("#333335") : Color.parseColor("#dce1e6");
        colorTextPrimary = isDark ? Color.parseColor("#ffffff") : Color.parseColor("#000000");
        colorTextSecondary = isDark ? Color.parseColor("#909499") : Color.parseColor("#818c99");
        colorAccent = Color.parseColor("#2688eb");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(isDark ? Color.parseColor("#1f1f20") : Color.parseColor("#ffffff"));
            if (!isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        // Root container
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(colorBg);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Top Toolbar with status bar insets
        root.addView(createTopBar());

        // Scrollable content
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        scrollView.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(32));

        // 5 Slots Tabbar Customization Card
        content.addView(createTabbarSlotsCard());

        scrollView.addView(content);
        root.addView(scrollView);

        setContentView(root);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            result = getResources().getDimensionPixelSize(resId);
        }
        if (result <= 0) {
            result = dp(28);
        }
        return result;
    }

    private View createTopBar() {
        int sbHeight = getStatusBarHeight();

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(isDark ? Color.parseColor("#1f1f20") : Color.parseColor("#ffffff"));
        bar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56) + sbHeight));
        bar.setPadding(dp(4), sbHeight, dp(16), 0);

        // Native VK Back button
        ImageView btnBack = new ImageView(this);
        int iconId = getResources().getIdentifier("vk_icon_arrow_left_outline_28", "drawable", getPackageName());
        if (iconId == 0) {
            iconId = getResources().getIdentifier("ic_arrow_back", "drawable", getPackageName());
        }
        if (iconId == 0) {
            iconId = getResources().getIdentifier("abc_ic_ab_back_material", "drawable", getPackageName());
        }
        if (iconId != 0) {
            btnBack.setImageResource(iconId);
        } else {
            btnBack.setImageDrawable(createBackArrowDrawable(colorTextPrimary));
        }
        btnBack.setColorFilter(colorTextPrimary);
        btnBack.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        btnBack.setPadding(dp(10), dp(10), dp(10), dp(10));

        TypedValue outValue = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)) {
            btnBack.setBackgroundResource(outValue.resourceId);
        } else {
            btnBack.setBackground(null);
        }

        LinearLayout.LayoutParams lpBack = new LinearLayout.LayoutParams(dp(48), dp(48));
        btnBack.setLayoutParams(lpBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(btnBack);

        // Title
        TextView title = new TextView(this);
        title.setText("Настройки");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(colorTextPrimary);
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpTitle.setMargins(dp(8), 0, 0, 0);
        title.setLayoutParams(lpTitle);
        bar.addView(title);

        return bar;
    }

    private android.graphics.drawable.Drawable createBackArrowDrawable(final int color) {
        return new android.graphics.drawable.Drawable() {
            private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) {{
                setColor(color);
                setStyle(android.graphics.Paint.Style.STROKE);
                setStrokeWidth(dp(2.4f));
                setStrokeCap(android.graphics.Paint.Cap.ROUND);
                setStrokeJoin(android.graphics.Paint.Join.ROUND);
            }};

            @Override
            public void draw(android.graphics.Canvas canvas) {
                android.graphics.Rect b = getBounds();
                float cx = b.exactCenterX();
                float cy = b.exactCenterY();
                float w = dp(16);
                float h = dp(14);
                canvas.drawLine(cx - w / 2, cy, cx + w / 2, cy, paint);
                canvas.drawLine(cx - w / 2, cy, cx - w / 2 + h / 2, cy - h / 2, paint);
                canvas.drawLine(cx - w / 2, cy, cx - w / 2 + h / 2, cy + h / 2, paint);
            }

            @Override
            public void setAlpha(int alpha) { paint.setAlpha(alpha); }

            @Override
            public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }

            @Override
            public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        };
    }

    private View createTabbarSlotsCard() {
        LinearLayout card = createCardLayout();

        TextView title = new TextView(this);
        title.setText("Нижняя панель (5 вкладок)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(colorTextPrimary);
        card.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Нажмите на слот, чтобы выбрать для него любую вкладку:");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        desc.setTextColor(colorTextSecondary);
        desc.setPadding(0, dp(2), 0, dp(12));
        card.addView(desc);

        slotsContainer = new LinearLayout(this);
        slotsContainer.setOrientation(LinearLayout.VERTICAL);
        refreshSlotsList();
        card.addView(slotsContainer);

        // Apply & Restart button
        TextView btnRestart = new TextView(this);
        btnRestart.setText("Применить и перезапустить VK");
        btnRestart.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnRestart.setTypeface(Typeface.DEFAULT_BOLD);
        btnRestart.setTextColor(Color.WHITE);
        btnRestart.setGravity(Gravity.CENTER);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(colorAccent);
        btnBg.setCornerRadius(dp(8));
        btnRestart.setBackground(btnBg);
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        lpBtn.setMargins(0, dp(16), 0, dp(8));
        btnRestart.setLayoutParams(lpBtn);
        btnRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigationFilter.restartApp(VkZenSettingsActivity.this);
            }
        });
        card.addView(btnRestart);

        // Reset button
        TextView btnReset = new TextView(this);
        btnReset.setText("Сбросить вкладки по умолчанию");
        btnReset.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnReset.setTextColor(colorAccent);
        btnReset.setGravity(Gravity.CENTER);
        btnReset.setPadding(0, dp(8), 0, dp(4));
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigationFilter.saveTabs(VkZenSettingsActivity.this, "home,friends,im,music,hub");
                refreshSlotsList();
                Toast.makeText(VkZenSettingsActivity.this, "Вкладки сброшены по умолчанию!", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnReset);

        return card;
    }

    private void refreshSlotsList() {
        if (slotsContainer == null) return;
        slotsContainer.removeAllViews();

        final String[] slots = getCurrentSlots();
        for (int i = 0; i < 5; i++) {
            final int slotIndex = i;
            String key = slots[i];
            String label = NavigationFilter.getTabLabelByKey(key);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setBackground(createItemSelectorDrawable());
            row.setClickable(true);

            // Slot badge
            TextView badge = new TextView(this);
            badge.setText(String.valueOf(i + 1));
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setTextColor(Color.WHITE);
            badge.setGravity(Gravity.CENTER);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.OVAL);
            badgeBg.setColor(colorAccent);
            badge.setBackground(badgeBg);
            LinearLayout.LayoutParams lpBadge = new LinearLayout.LayoutParams(dp(24), dp(24));
            lpBadge.setMargins(0, 0, dp(12), 0);
            badge.setLayoutParams(lpBadge);
            row.addView(badge);

            // Slot text
            TextView text = new TextView(this);
            text.setText(label);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            text.setTextColor(colorTextPrimary);
            LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            text.setLayoutParams(lpText);
            row.addView(text);

            // Arrow
            TextView arrow = new TextView(this);
            arrow.setText("›");
            arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            arrow.setTextColor(colorTextSecondary);
            row.addView(arrow);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSlotPicker(slotIndex, slots);
                }
            });

            slotsContainer.addView(row);

            if (i < 4) {
                View divider = new View(this);
                divider.setBackgroundColor(colorStroke);
                LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                lpDiv.setMargins(dp(48), 0, 0, 0);
                divider.setLayoutParams(lpDiv);
                slotsContainer.addView(divider);
            }
        }
    }

    private void showSlotPicker(final int slotIndex, final String[] slots) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout dialogRoot = new LinearLayout(this);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        dialogRoot.setPadding(dp(20), dp(20), dp(20), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorSurface);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), colorStroke);
        dialogRoot.setBackground(bg);

        // Title
        TextView title = new TextView(this);
        title.setText("Выберите вкладку для слота " + (slotIndex + 1));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(colorTextPrimary);
        title.setPadding(0, 0, 0, dp(14));
        dialogRoot.addView(title);

        // Scrollable list of options
        ScrollView sv = new ScrollView(this);
        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);

        for (int j = 0; j < NavigationFilter.ALL_TAB_KEYS.length; j++) {
            final String key = NavigationFilter.ALL_TAB_KEYS[j];
            final String label = NavigationFilter.ALL_TAB_LABELS[j];
            final boolean isSelected = key.equalsIgnoreCase(slots[slotIndex]);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(12), dp(8), dp(12));
            row.setBackground(createItemSelectorDrawable());
            row.setClickable(true);

            // Radio circle
            TextView radio = new TextView(this);
            radio.setGravity(Gravity.CENTER);
            GradientDrawable circleBg = new GradientDrawable();
            circleBg.setShape(GradientDrawable.OVAL);
            if (isSelected) {
                circleBg.setColor(colorAccent);
                circleBg.setStroke(dp(2), colorAccent);
                radio.setText("✓");
                radio.setTextColor(Color.WHITE);
                radio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                radio.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                circleBg.setColor(Color.TRANSPARENT);
                circleBg.setStroke(dp(2), colorTextSecondary);
            }
            radio.setBackground(circleBg);
            LinearLayout.LayoutParams lpRadio = new LinearLayout.LayoutParams(dp(20), dp(20));
            lpRadio.setMargins(0, 0, dp(14), 0);
            radio.setLayoutParams(lpRadio);
            row.addView(radio);

            // Text
            TextView itemText = new TextView(this);
            itemText.setText(label);
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            itemText.setTextColor(isSelected ? colorAccent : colorTextPrimary);
            if (isSelected) {
                itemText.setTypeface(Typeface.DEFAULT_BOLD);
            }
            row.addView(itemText);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    slots[slotIndex] = key;
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < 5; k++) {
                        if (k > 0) sb.append(",");
                        sb.append(slots[k]);
                    }
                    NavigationFilter.saveTabs(VkZenSettingsActivity.this, sb.toString());
                    refreshSlotsList();
                    dialog.dismiss();
                    Toast.makeText(VkZenSettingsActivity.this, "Слот " + (slotIndex + 1) + ": " + label, Toast.LENGTH_SHORT).show();
                }
            });

            listLayout.addView(row);
        }

        sv.addView(listLayout);
        dialogRoot.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // Cancel button
        TextView btnCancel = new TextView(this);
        btnCancel.setText("ОТМЕНА");
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setTextColor(colorAccent);
        btnCancel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        btnCancel.setPadding(dp(12), dp(16), dp(8), dp(4));
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialogRoot.addView(btnCancel);

        dialog.setContentView(dialogRoot);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90f);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.65f);
            dialog.getWindow().setLayout(width, height);
        }
        dialog.show();
    }

    private String[] getCurrentSlots() {
        SharedPreferences prefs = NavigationFilter.getPrefs(this);
        String current = NavigationFilter.getEffectiveTabs(prefs);
        String[] parts = current.split(",");
        String[] slots = new String[5];
        String[] defaults = "home,friends,im,music,hub".split(",");
        for (int i = 0; i < 5; i++) {
            if (i < parts.length && !parts[i].trim().isEmpty()) {
                slots[i] = parts[i].trim().toLowerCase();
            } else if (i < defaults.length) {
                slots[i] = defaults[i];
            } else {
                slots[i] = "home";
            }
        }
        return slots;
    }

    private LinearLayout createCardLayout() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorSurface);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), colorStroke);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        return card;
    }

    private GradientDrawable createItemSelectorDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(8));
        gd.setColor(isDark ? Color.parseColor("#2a2a2c") : Color.parseColor("#f5f6f8"));
        return gd;
    }

    private int dp(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
