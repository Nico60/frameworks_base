/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.content.Context;
import android.content.ContentResolver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.PorterDuff.Mode;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.UserHandle;
import android.graphics.PorterDuff;
import android.telephony.MSimTelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.internal.util.omni.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public final class PhoneStatusBarTransitions extends BarTransitions {
    private static final float ICON_ALPHA_WHEN_NOT_OPAQUE = 1;
    private static final float ICON_ALPHA_WHEN_LIGHTS_OUT_BATTERY_CLOCK = 0.5f;
    private static final float ICON_ALPHA_WHEN_LIGHTS_OUT_NON_BATTERY_CLOCK = 0;

    private final PhoneStatusBarView mView;
    private final float mIconAlphaWhenOpaque;

    private List<ImageView> mIcons = new ArrayList<ImageView>();
    private List<ImageView> mIconsReverse = new ArrayList<ImageView>();
    private List<ImageView> mNotificationIcons = new ArrayList<ImageView>();
    private List<TextView> mNotificationTexts = new ArrayList<TextView>();

    private View mLeftSide, mStatusIcons, mSignalCluster;
    private View mBattery, mClock, mCenterClock, mNetworkTraffic;

    private Animator mCurrentAnimation;
    private int mCurrentColor = -3;
    private int mCurrentBg;
    private String mFullColor = "fullcolor";
    private String mNonFullColor = "nonfullcolor";

    private boolean mCustomColor;
    private boolean mCustomColorNotification;
    private int notificationColor;
    private int systemColor;

    public PhoneStatusBarTransitions(PhoneStatusBarView view) {
        super(view, R.drawable.status_background, R.color.status_bar_background_opaque,
                R.color.status_bar_background_semi_transparent);
        mView = view;
        final Resources res = mView.getContext().getResources();
        final ContentResolver resolver = mView.getContext().getContentResolver();
        mIconAlphaWhenOpaque = res.getFraction(R.dimen.status_bar_icon_drawing_alpha, 1, 1);
        if (Settings.System.getInt(resolver,
              Settings.System.CUSTOM_SYSTEM_ICON_COLOR, 0) == 1) {
                    systemColor = Settings.System.getInt(resolver,
                                Settings.System.SYSTEM_ICON_COLOR, 0xffffffff);
                    mCustomColor=true;
        } else {
            mCustomColor=false;
        }
        if (Settings.System.getInt(resolver,
              Settings.System.CUSTOM_NOTIFICATION_ICON_COLOR, 0) == 1) {
                    notificationColor = Settings.System.getInt(resolver,
                                Settings.System.NOTIFICATION_ICON_COLOR, 0xffffffff);
                    mCustomColorNotification=true;
        } else {
            mCustomColorNotification=false;
        }
    }

    public void init() {
        mLeftSide = mView.findViewById(R.id.notification_icon_area);
        mStatusIcons = mView.findViewById(R.id.statusIcons);
        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
            mSignalCluster = mView.findViewById(R.id.msim_signal_cluster);
        } else {
            mSignalCluster = mView.findViewById(R.id.signal_cluster);
        }
        mBattery = mView.findViewById(R.id.battery);
        mCenterClock = mView.findViewById(R.id.center_clock);
        mNetworkTraffic = mView.findViewById(R.id.networkTraffic);
        mClock = mView.findViewById(R.id.clock);
        applyModeBackground(-1, getMode(), false /*animate*/);
        applyMode(getMode(), false /*animate*/);
    }

    public ObjectAnimator animateTransitionTo(View v, float toAlpha) {
        return ObjectAnimator.ofFloat(v, "alpha", v.getAlpha(), toAlpha);
    }

    private static Drawable GrayscaleDrawable (Context context, Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap bitmap_gray = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Canvas canvas_gray = new Canvas(bitmap_gray);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        Paint paint = new Paint();
        ColorMatrix colormatrix = new ColorMatrix();
        colormatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colormatrix);
        paint.setColorFilter(filter);
        canvas_gray.drawBitmap(bitmap, 0, 0, paint);
        Drawable drawable_gray = new BitmapDrawable(context.getResources(), bitmap_gray);
        return drawable_gray;
    }

    private float getNonBatteryClockAlphaFor(int mode) {
        return mode == MODE_LIGHTS_OUT ? ICON_ALPHA_WHEN_LIGHTS_OUT_NON_BATTERY_CLOCK
                : !isOpaque(mode) ? ICON_ALPHA_WHEN_NOT_OPAQUE
                : mIconAlphaWhenOpaque;
    }

    private float getBatteryClockAlpha(int mode) {
        return mode == MODE_LIGHTS_OUT ? ICON_ALPHA_WHEN_LIGHTS_OUT_BATTERY_CLOCK
                : getNonBatteryClockAlphaFor(mode);
    }

    @Override
    protected void onTransition(int oldMode, int newMode, boolean animate) {
        super.onTransition(oldMode, newMode, animate);
        applyMode(newMode, animate);
    }

    public void addIcon(ImageView iv) {
        if (!mIcons.contains(iv)) {
            mIcons.add(iv);
        }
    }

    public void removeIcon(ImageView iv) {
        if (mIcons.contains(iv)) {
            mIcons.remove(iv);
        }
    }

    public void addIconReverse(ImageView iv) {
        if (!mIconsReverse.contains(iv)) {
            mIconsReverse.add(iv);
        }
    }

    public void addNotificationIcon(ImageView iv) {
        if (!mNotificationIcons.contains(iv)) {
            boolean isNotFullColor = ColorUtils.getIconWhiteBlackTransparent(iv.getDrawable());
            if (isNotFullColor) {
                iv.setTag(mNonFullColor);
            } else {
                iv.setTag(mFullColor);
            }
            mNotificationIcons.add(iv);
        }
    }

    public void removeNotificationIcon(ImageView iv) {
        if (mNotificationIcons.contains(iv)) {
            mNotificationIcons.remove(iv);
        }
    }

    public void addNotificationText(TextView tv) {
        if (!mNotificationTexts.contains(tv)) {
            mNotificationTexts.add(tv);
        }
    }

    public void removeNotificationText(TextView tv) {
        if (mNotificationTexts.contains(tv)) {
            mNotificationTexts.remove(tv);
        }
    }

    @Override
    public void finishAnimations() {
        setColorChangeIcon(-3);
        setColorChangeNotificationIcon(-3);
        super.finishAnimations();
    }

    @Override
    public void changeColorIconBackground(int bg_color, int ic_color) {
        if (mCurrentBg == bg_color) {
            return;
        }
        mCurrentBg = bg_color;
        if (ColorUtils.isBrightColor(bg_color)) {
            ic_color = Color.BLACK;
        }
        mCurrentColor = ic_color;
        setColorChangeIcon(ic_color);
        setColorChangeNotificationIcon(ic_color);
        super.changeColorIconBackground(bg_color, ic_color);
    }

    public int getCurrentIconColor() {
        return mCurrentColor;
    }

    public void updateNotificationIconColor() {
        setColorChangeNotificationIcon(mCurrentColor);
    }

    private void setColorChangeIcon(int ic_color) {
       if (mCustomColor) {
                /*for (ImageView iv : mIcons) {
                        if (iv != null) {
                                iv.clearColorFilter();
                        } else {
                                mIcons.remove(iv);
                            }
                }
                for (ImageView ivr : mIcons) {
                        if (ivr != null) {
                                ivr.clearColorFilter();
                        } else {
                            mIcons.remove(ivr);
                            }
                }*/
                return;
        }
        for (ImageView iv : mIcons) {
             if (iv != null) {
                 if (ic_color == -3) {
                     iv.clearColorFilter();
                 } else {
                     iv.setColorFilter(ic_color, PorterDuff.Mode.SRC_ATOP);
                 }
             } else {
                 mIcons.remove(iv);
             }
        }
        for (ImageView ivr : mIconsReverse) {
             if (ivr != null) {
                 if (ic_color == -3) {
                     ivr.clearColorFilter();
                 } else {
                     if (ColorUtils.isBrightColor(ic_color)) {
                         ivr.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                     } else {
                         ivr.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                     }
                 }
             } else {
                 mIconsReverse.remove(ivr);
             }
        }
    }

    private void setColorChangeNotificationIcon(int ic_color) {
        if (mCustomColor) {
                /*for (ImageView notifiv : mNotificationIcons) {
                            if (notifiv != null) {
                                notifiv.clearColorFilter();
                            } else {
                                mNotificationIcons.remove(notifiv);
                            }
                }*/
                return;
        }
        for (ImageView notifiv : mNotificationIcons) {
             if (notifiv != null) {
                 if (ic_color == -3) {
                     notifiv.clearColorFilter();
                 } else {
                     String colors = (String) notifiv.getTag();
                     if (TextUtils.equals(colors, mNonFullColor)) {
                         notifiv.setColorFilter(ic_color, PorterDuff.Mode.MULTIPLY);
                     } else {
                         notifiv.clearColorFilter();
                     }
                 }
             } else {
                 mNotificationIcons.remove(notifiv);
             }
        }
        for (TextView notiftv : mNotificationTexts) {
             if (notiftv != null) {
                 if (ic_color == -3) {
                     notiftv.setTextColor(Color.WHITE);
                 } else {
                     notiftv.setTextColor(ic_color);
                 }
             } else {
                 mNotificationTexts.remove(notiftv);
             }
        }
    }

    private void applyMode(int mode, boolean animate) {
        if (mLeftSide == null) return; // pre-init
        float newAlpha = getNonBatteryClockAlphaFor(mode);
        float newAlphaBC = getBatteryClockAlpha(mode);
        if (mCurrentAnimation != null) {
            mCurrentAnimation.cancel();
        }
        if (animate) {
            AnimatorSet anims = new AnimatorSet();
            anims.playTogether(
                    animateTransitionTo(mLeftSide, newAlpha),
                    animateTransitionTo(mStatusIcons, newAlpha),
                    animateTransitionTo(mSignalCluster, newAlpha),
                    animateTransitionTo(mNetworkTraffic, newAlpha),
                    animateTransitionTo(mBattery, newAlphaBC),
                    animateTransitionTo(mClock, newAlphaBC),
                    animateTransitionTo(mCenterClock, newAlphaBC)
                    );
            if (mode == MODE_LIGHTS_OUT) {
                anims.setDuration(LIGHTS_OUT_DURATION);
            }
            anims.start();
            mCurrentAnimation = anims;
        } else {
            mLeftSide.setAlpha(newAlpha);
            mStatusIcons.setAlpha(newAlpha);
            mSignalCluster.setAlpha(newAlpha);
            mNetworkTraffic.setAlpha(newAlpha);
            mBattery.setAlpha(newAlphaBC);
            mClock.setAlpha(newAlphaBC);
            mCenterClock.setAlpha(newAlphaBC);
        }
    }
}
