package com.tremendo.uitweakforgboard;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.view.*;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.*;
import static de.robv.android.xposed.XposedHelpers.*;
import android.widget.*;


public class UITweakForGboard implements IXposedHookInitPackageResources, IXposedHookLoadPackage {

	final String MODULE_PACKAGE = MainActivity.class.getPackage().getName();
	final String MODULE_NAME = "UI Tweak For Gboard";
	final String LOG_TAG = MODULE_NAME+": ";
	final String GBOARD_PACKAGE = "com.google.android.inputmethod.latin";

	BroadcastReceiver mBroadcastReceiver;

	@Override
    public void handleInitPackageResources(final InitPackageResourcesParam resparam) throws Throwable {
		if (!GBOARD_PACKAGE.equals(resparam.packageName)) {
			return;
		}

		try {
			resparam.res.hookLayout(resparam.packageName, "layout", "ims_input_view", new XC_LayoutInflated() {
				@Override
				public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {

					final XSharedPreferences prefs = new XSharedPreferences(MODULE_PACKAGE);
					final View headerGroupView = liparam.view.findViewById(getId("header_group_view"));
					final LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) headerGroupView.getLayoutParams();

					mBroadcastReceiver = new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							if (intent.getBooleanExtra("hide", false)) {
								layoutParams.height = 0;
							} else layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
							headerGroupView.setLayoutParams(layoutParams);
						}
					};

					prefs.reload();
					if (prefs.getBoolean("pref_hide_search_bar", false)) {
						layoutParams.height = 0;
						headerGroupView.setLayoutParams(layoutParams);
					}

					try {
						AndroidAppHelper.currentApplication().unregisterReceiver(mBroadcastReceiver);
					} catch (IllegalArgumentException e) { }

					AndroidAppHelper.currentApplication().registerReceiver(mBroadcastReceiver, new IntentFilter(MODULE_PACKAGE+".TOGGLE_SEARCH_BAR"));
				}
			});
		} catch (Resources.NotFoundException e) {
			XposedBridge.log(LOG_TAG + "Tried hooking layout 'ims_input_view'; resource not found");
		}

	}


	@Override
	public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

		if (MODULE_PACKAGE.equals(lpparam.packageName)) {
			findAndHookMethod(MODULE_PACKAGE+".MainActivity$PrefsFragment", lpparam.classLoader,
			"onResume", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
					setObjectField(param.thisObject, "mXposedActivated", true);
				}
			});
		}


		if (!GBOARD_PACKAGE.equals(lpparam.packageName)) {
			return;
		}

		final String classNameLatinIME = "com.android.inputmethod.latin.LatinIME";
		try {
			findAndHookMethod(findClass(classNameLatinIME, lpparam.classLoader), "onDestroy", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
					try {
						AndroidAppHelper.currentApplication().unregisterReceiver(mBroadcastReceiver);
					} catch (IllegalArgumentException e) { }
				}
			});
		} catch (ClassNotFoundError e) {
			XposedBridge.log(LOG_TAG + String.format("Class not found in Gboard: '%s'; app version may be incompatible with module.", classNameLatinIME));
		}
	}


	private static int getId(String idName) {
		return AndroidAppHelper.currentApplication().getApplicationContext().getResources()
							   .getIdentifier(idName, "id", AndroidAppHelper.currentApplication().getPackageName());
	}

}
