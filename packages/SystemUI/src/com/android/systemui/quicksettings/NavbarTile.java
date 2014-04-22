package com.android.systemui.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;

public class NavbarTile extends QuickSettingsTile {
    private boolean mEnabled = false;

    public NavbarTile(Context context, QuickSettingsController qsc) {
        super(context, qsc);

        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.ENABLE_NAVIGATION_BAR, mEnabled ? 0 : 1);
            }
        };

        mOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.settings",
                    "com.android.settings.Settings$NavbarSettingsActivity");
                startSettingsActivity(intent);
                return true;
            }
        };

        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.ENABLE_NAVIGATION_BAR)
                , this);

    }

    @Override
    void onPostCreate() {
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        mEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ENABLE_NAVIGATION_BAR, 0) == 1;
        if(mEnabled){
            mDrawable = R.drawable.ic_qs_navbar_on;
            mLabel = mContext.getString(R.string.quick_settings_navbar_on);
        }else{
            mDrawable = R.drawable.ic_qs_navbar_off;
            mLabel = mContext.getString(R.string.quick_settings_navbar_off);
        }
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        updateResources();
    }
}
