/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.systemui.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.telephony.SignalStrength;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;

public class SignalClusterTextView extends LinearLayout implements
        NetworkController.NetworkSignalChangedCallback,
        NetworkController.SignalStrengthChangedCallback {

    private boolean mAirplaneMode;
    private int mDBm = 0;
    private int mSignalClusterStyle = SignalClusterView.STYLE_NORMAL;

    private int mCurrentColor = -3;
    private boolean mCustomColor;
    private int systemColor;

    private TextView mMobileSignalText;
    private ImageView mMobileIcon;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.CUSTOM_SYSTEM_ICON_COLOR), false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SYSTEM_ICON_COLOR), false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public SignalClusterTextView(Context context) {
        this(context, null);
    }

    public SignalClusterTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mMobileSignalText = (TextView) findViewById(R.id.mobile_signal_text);
        mMobileIcon       = (ImageView) findViewById(R.id.mobile_signal_text_icon);
        updateSignalText();
    }

    public void setStyle(int style) {
        mSignalClusterStyle = style;
        updateSignalText();
    }

    private String getSignalLevelString(int dBm) {
        if (dBm == 0 || dBm == SignalStrength.INVALID) {
            return "-\u221e"; // -oo ('minus infinity')
        }
        return Integer.toString(dBm);
    }

    private void updateSignalText() {
        int nowColor;
        if (mMobileSignalText == null) {
            return;
        }
        if (mAirplaneMode || mDBm == 0) {
            setVisibility(View.GONE);
        } else if (mSignalClusterStyle == SignalClusterView.STYLE_TEXT) {
            if (mCustomColor) {
                nowColor=systemColor;
            } else {
                if (mCurrentColor != -3) {
                    nowColor=mCurrentColor;
                } else {
                    nowColor=0xFFFFFFFF;
                }
            }
            setVisibility(View.VISIBLE);
            Drawable drawable = getResources().getDrawable( R.drawable.stat_sys_signal_min ); 
            if (drawable != null && mMobileIcon != null) {
                drawable.setColorFilter(nowColor, Mode.MULTIPLY);
                mMobileIcon.setImageDrawable(drawable);
            }
            mMobileSignalText.setText(getSignalLevelString(mDBm));
            mMobileSignalText.setTextColor(nowColor);
        } else {
            setVisibility(View.GONE);
        }
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, int wifiSignalIconId,
            boolean activityIn, boolean activityOut,
            String wifiSignalContentDescriptionId, String description) {
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId,
            String mobileSignalContentDescriptionId, int dataTypeIconId,
            boolean activityIn, boolean activityOut,
            String dataTypeContentDescriptionId, String description) {
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
        mAirplaneMode = enabled;
        updateSignalText();
    }

    @Override
    public void onPhoneSignalStrengthChanged(int dbm) {
        mDBm = dbm;
        updateSignalText();
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mCustomColor = Settings.System.getIntForUser(resolver,
                Settings.System.CUSTOM_SYSTEM_ICON_COLOR, 0, UserHandle.USER_CURRENT) == 1;
        systemColor = Settings.System.getIntForUser(resolver,
                Settings.System.SYSTEM_ICON_COLOR, -2, UserHandle.USER_CURRENT);
        updateSignalText();
    }
}
