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
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
     * Injects the horizontal flip / mirror button into the player controls container (VideoShortActions).
     */
    public static void injectMirrorButton(ViewGroup container) {
        if (container == null) return;
        try {
            // Prevent duplicate buttons
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

            // Configure layout params for ConstraintLayout
            ViewGroup.LayoutParams lp = createConstraintLayoutParams(container, context, btnSize);
            mirrorButton.setLayoutParams(lp);

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

                            // Update button icon tint
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

            container.addView(mirrorButton);
            Log.d(TAG, "Successfully injected video mirror button");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to inject video mirror button", t);
        }
    }

    private static ViewGroup.LayoutParams createConstraintLayoutParams(ViewGroup container, Context context, int btnSize) {
        try {
            Class<?> lpClass = Class.forName("androidx.constraintlayout.widget.ConstraintLayout$LayoutParams");
            Constructor<?> ctor = lpClass.getConstructor(int.class, int.class);
            Object lp = ctor.newInstance(btnSize, btnSize);

            // Search for enter_pip view in container to anchor to its left
            View anchorView = null;
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child != null && child.getId() != View.NO_ID) {
                    try {
                        String entryName = context.getResources().getResourceEntryName(child.getId());
                        if ("enter_pip".equalsIgnoreCase(entryName) || "more".equalsIgnoreCase(entryName)) {
                            anchorView = child;
                            if ("enter_pip".equalsIgnoreCase(entryName)) break;
                        }
                    } catch (Throwable ignored) {}
                }
            }

            Field topToTop = lpClass.getField("topToTop");
            Field bottomToBottom = lpClass.getField("bottomToBottom");
            Field endToStart = lpClass.getField("endToStart");
            Field endToEnd = lpClass.getField("endToEnd");
            Field rightMargin = lpClass.getField("rightMargin");
            Field leftMargin = lpClass.getField("leftMargin");

            if (anchorView != null) {
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

            // Or views named Surface / VideoView / AspectRatio
            String className = current.getClass().getName();
            if (className.contains("Texture") || className.contains("SurfaceView") || className.contains("AspectRatioFrameLayout")) {
                if (candidate == null) candidate = current;
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
            float width = getBounds().width();
            float height = getBounds().height();
            if (width <= 0 || height <= 0) return;

            float padX = width * 0.15f;
            float left = padX;
            float right = width - padX;
            float arrowHeadSize = width * 0.22f;

            float yTop = height * 0.36f;
            float yBottom = height * 0.64f;

            // Top arrow: pointing right ──►
            pathTop.reset();
            pathTop.moveTo(left, yTop);
            pathTop.lineTo(right, yTop);
            pathTop.lineTo(right - arrowHeadSize, yTop - arrowHeadSize * 0.7f);
            pathTop.moveTo(right, yTop);
            pathTop.lineTo(right - arrowHeadSize, yTop + arrowHeadSize * 0.7f);

            // Bottom arrow: pointing left ◄──
            pathBottom.reset();
            pathBottom.moveTo(right, yBottom);
            pathBottom.lineTo(left, yBottom);
            pathBottom.lineTo(left + arrowHeadSize, yBottom - arrowHeadSize * 0.7f);
            pathBottom.moveTo(left, yBottom);
            pathBottom.lineTo(left + arrowHeadSize, yBottom + arrowHeadSize * 0.7f);

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
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
