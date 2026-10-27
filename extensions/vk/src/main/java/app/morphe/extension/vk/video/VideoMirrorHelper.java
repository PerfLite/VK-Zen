package app.morphe.extension.vk.video;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Queue;

public class VideoMirrorHelper {

    private static final String TAG = "VideoMirrorHelper";
    private static final String TAG_MIRROR_BUTTON = "morphe_video_mirror_button";
    private static final int COLOR_ACTIVE = Color.parseColor("#4BB34B");
    private static final int COLOR_NORMAL = Color.WHITE;

    /**
     * Injects the horizontal flip / mirror button into the player controls container.
     * Supports VideoToolbarView, VideoButtonsView, and VideoShortActions.
     */
    public static void injectMirrorButton(final ViewGroup container) {
        if (container == null) return;
        try {
            // Attempt immediate injection
            injectMirrorButtonInternal(container);

            // Also post to UI thread in case child views (action buttons) are populated/inflated asynchronously
            container.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        injectMirrorButtonInternal(container);
                    } catch (Throwable t) {
                        Log.e(TAG, "post inject error", t);
                    }
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "injectMirrorButton error", t);
        }
    }

    private static synchronized void injectMirrorButtonInternal(final ViewGroup container) {
        if (container == null) return;

        // Check if button is already injected in this container or its parent hierarchy
        if (container.findViewWithTag(TAG_MIRROR_BUTTON) != null) {
            return;
        }

        final Context context = container.getContext();
        final ImageView mirrorButton = new ImageView(context);
        mirrorButton.setTag(TAG_MIRROR_BUTTON);
        mirrorButton.setContentDescription("Отзеркалить видео");

        int btnSize = dp(context, 44);
        int iconPadding = dp(context, 10);
        mirrorButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);

        // Click ripple effect
        try {
            TypedValue outValue = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)) {
                mirrorButton.setBackgroundResource(outValue.resourceId);
            }
        } catch (Throwable ignored) {}

        final MirrorIconDrawable iconDrawable = new MirrorIconDrawable(context);
        mirrorButton.setImageDrawable(iconDrawable);

        mirrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    View surface = findVideoSurface(container);
                    if (surface != null) {
                        float curScale = surface.getScaleX();
                        boolean activateMirror = (curScale > 0f);
                        float newScale = activateMirror ? -1.0f : 1.0f;
                        surface.setScaleX(newScale);

                        mirrorButton.setColorFilter(activateMirror ? COLOR_ACTIVE : COLOR_NORMAL);
                        Toast.makeText(
                            context,
                            activateMirror ? "Отзеркаливание включено ↔" : "Отзеркаливание выключено",
                            Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(context, "Видеопоток не найден", Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Error toggling video mirror", t);
                }
            }
        });

        // Search for existing action buttons (more, cast, settings, rate, enter_pip)
        View anchorView = findAnchorView(container, context);

        if (anchorView != null && anchorView.getParent() instanceof ViewGroup) {
            ViewGroup anchorParent = (ViewGroup) anchorView.getParent();

            if (anchorParent instanceof LinearLayout) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(btnSize, btnSize);
                lp.gravity = Gravity.CENTER_VERTICAL;
                int insertIdx = anchorParent.indexOfChild(anchorView);
                anchorParent.addView(mirrorButton, insertIdx, lp);
                Log.d(TAG, "Injected mirror button into LinearLayout next to " + anchorView);
                return;
            }

            if (anchorParent.getClass().getName().contains("ConstraintLayout")) {
                ViewGroup.LayoutParams lp = createConstraintLayoutParams(anchorParent, context, btnSize, anchorView);
                anchorParent.addView(mirrorButton, lp);
                Log.d(TAG, "Injected mirror button into ConstraintLayout next to " + anchorView);
                return;
            }

            if (anchorParent instanceof RelativeLayout) {
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(btnSize, btnSize);
                lp.addRule(RelativeLayout.START_OF, anchorView.getId());
                lp.addRule(RelativeLayout.ALIGN_TOP, anchorView.getId());
                anchorParent.addView(mirrorButton, lp);
                Log.d(TAG, "Injected mirror button into RelativeLayout next to " + anchorView);
                return;
            }
        }

        // Direct container fallback
        if (container instanceof LinearLayout) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(btnSize, btnSize);
            lp.gravity = Gravity.CENTER_VERTICAL;
            container.addView(mirrorButton, lp);
            Log.d(TAG, "Injected mirror button as direct child of LinearLayout");
            return;
        }

        if (container.getClass().getName().contains("ConstraintLayout")) {
            ViewGroup.LayoutParams lp = createConstraintLayoutParams(container, context, btnSize, null);
            container.addView(mirrorButton, lp);
            Log.d(TAG, "Injected mirror button as direct child of ConstraintLayout");
            return;
        }

        if (container instanceof RelativeLayout) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(btnSize, btnSize);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.addRule(RelativeLayout.ALIGN_PARENT_END);
            lp.topMargin = dp(context, 8);
            lp.rightMargin = dp(context, 52);
            container.addView(mirrorButton, lp);
            Log.d(TAG, "Injected mirror button as direct child of RelativeLayout");
            return;
        }

        // Generic fallback
        ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(btnSize, btnSize);
        mlp.rightMargin = dp(context, 52);
        container.addView(mirrorButton, mlp);
        Log.d(TAG, "Injected mirror button using generic MarginLayoutParams");
    }

    private static View findAnchorView(ViewGroup container, Context context) {
        Queue<View> queue = new LinkedList<>();
        queue.add(container);

        View candidate = null;

        while (!queue.isEmpty()) {
            View current = queue.poll();
            if (current == null) continue;

            if (current != container && current.getId() != View.NO_ID) {
                try {
                    String entryName = context.getResources().getResourceEntryName(current.getId());
                    if ("more".equalsIgnoreCase(entryName) || "settings".equalsIgnoreCase(entryName)) {
                        return current;
                    }
                    if ("cast".equalsIgnoreCase(entryName) || "enter_pip".equalsIgnoreCase(entryName) || "cancel".equalsIgnoreCase(entryName)) {
                        if (candidate == null) candidate = current;
                    }
                } catch (Throwable ignored) {}
            }

            if (current instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) current;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    queue.add(vg.getChildAt(i));
                }
            }
        }

        return candidate;
    }

    private static ViewGroup.LayoutParams createConstraintLayoutParams(ViewGroup container, Context context, int btnSize, View anchorView) {
        try {
            Class<?> lpClass = Class.forName("androidx.constraintlayout.widget.ConstraintLayout$LayoutParams");
            Constructor<?> ctor = lpClass.getConstructor(int.class, int.class);
            Object lp = ctor.newInstance(btnSize, btnSize);

            Field topToTop = lpClass.getField("topToTop");
            Field bottomToBottom = lpClass.getField("bottomToBottom");
            Field endToStart = lpClass.getField("endToStart");
            Field endToEnd = lpClass.getField("endToEnd");
            Field rightMargin = lpClass.getField("rightMargin");
            Field leftMargin = lpClass.getField("leftMargin");

            if (anchorView != null && anchorView.getId() != View.NO_ID) {
                endToStart.setInt(lp, anchorView.getId());
                topToTop.setInt(lp, anchorView.getId());
                bottomToBottom.setInt(lp, anchorView.getId());
                rightMargin.setInt(lp, dp(context, 4));
                leftMargin.setInt(lp, dp(context, 4));
            } else {
                int parentId = 0; // ConstraintLayout.LayoutParams.PARENT_ID = 0
                endToEnd.setInt(lp, parentId);
                topToTop.setInt(lp, parentId);
                rightMargin.setInt(lp, dp(context, 48));
            }

            return (ViewGroup.LayoutParams) lp;
        } catch (Throwable t) {
            Log.w(TAG, "ConstraintLayout.LayoutParams reflection fallback: " + t);
            ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(btnSize, btnSize);
            mlp.rightMargin = dp(context, 48);
            return mlp;
        }
    }

    /**
     * Recursively searches for the active video rendering surface (TextureView, SurfaceView, or video player frame)
     * in the view hierarchy.
     */
    public static View findVideoSurface(View view) {
        if (view == null) return null;
        View root = view.getRootView();
        if (root == null) root = view;

        Queue<View> queue = new LinkedList<>();
        queue.add(root);

        View candidate = null;

        while (!queue.isEmpty()) {
            View current = queue.poll();
            if (current == null) continue;

            // Prioritize TextureView
            if (current instanceof TextureView) {
                return current;
            }

            // Next prioritize SurfaceView
            if (current instanceof SurfaceView) {
                if (candidate == null) candidate = current;
            }

            // Check class name / ID for VideoView / AspectRatioFrameLayout / Surface
            String className = current.getClass().getName();
            if (className.contains("Texture") || className.contains("SurfaceView") || className.contains("AspectRatioFrameLayout")) {
                if (candidate == null) candidate = current;
            }
            if (current.getId() != View.NO_ID) {
                try {
                    String entry = view.getContext().getResources().getResourceEntryName(current.getId());
                    if ("video_view".equalsIgnoreCase(entry)) {
                        if (candidate == null) candidate = current;
                    }
                } catch (Throwable ignored) {}
            }

            if (current instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) current;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    queue.add(vg.getChildAt(i));
                }
            }
        }

        return candidate;
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.getResources().getDisplayMetrics()
        );
    }

    /**
     * Crisp, resolution-independent horizontal flip icon drawable (two opposing horizontal arrows ⇄).
     */
    public static class MirrorIconDrawable extends Drawable {
        private final Paint paint;
        private final Path pathTop;
        private final Path pathBottom;
        private final float strokeWidth;

        public MirrorIconDrawable(Context context) {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(COLOR_NORMAL);
            paint.setStyle(Paint.Style.STROKE);
            strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2.2f,
                context.getResources().getDisplayMetrics()
            );
            paint.setStrokeWidth(strokeWidth);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            pathTop = new Path();
            pathBottom = new Path();
        }

        @Override
        public void draw(Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            int w = bounds.width();
            int h = bounds.height();
            if (w <= 0 || h <= 0) return;

            float padX = w * 0.22f;
            float left = bounds.left + padX;
            float right = bounds.right - padX;

            float arrowLen = (right - left) * 0.28f;
            float arrowSpread = h * 0.14f;

            float topY = bounds.top + h * 0.38f;
            float bottomY = bounds.top + h * 0.62f;

            // Top arrow: pointing right ->
            pathTop.reset();
            pathTop.moveTo(left, topY);
            pathTop.lineTo(right, topY);
            pathTop.moveTo(right - arrowLen, topY - arrowSpread);
            pathTop.lineTo(right, topY);
            pathTop.lineTo(right - arrowLen, topY + arrowSpread);

            // Bottom arrow: pointing left <-
            pathBottom.reset();
            pathBottom.moveTo(right, bottomY);
            pathBottom.lineTo(left, bottomY);
            pathBottom.moveTo(left + arrowLen, bottomY - arrowSpread);
            pathBottom.lineTo(left, bottomY);
            pathBottom.lineTo(left + arrowLen, bottomY + arrowSpread);

            canvas.drawPath(pathTop, paint);
            canvas.drawPath(pathBottom, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
