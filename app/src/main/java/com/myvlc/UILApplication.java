/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.myvlc;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import org.videolan.vlc.util.BitmapCache;

import java.util.Locale;


/**
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class UILApplication extends Application {

	public final static String TAG = "VLC/VLCApplication";
	private static UILApplication instance;

	public final static String SLEEP_INTENT = "org.videolan.vlc.SleepIntent";
	public final static String INCOMING_CALL_INTENT = "org.videolan.vlc.IncomingCallIntent";
	public final static String CALL_ENDED_INTENT = "org.videolan.vlc.CallEndedIntent";

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	@SuppressWarnings("unused")
	@Override
	public void onCreate() {
		if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyDialog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyDeath().build());
		}
		super.onCreate();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String p = pref.getString("set_locale", "");
		if (p != null && !p.equals("")) {
			Locale locale;
			// workaround due to region code
			if(p.equals("zh-TW")) {
				locale = Locale.TRADITIONAL_CHINESE;
			} else if(p.startsWith("zh")) {
				locale = Locale.CHINA;
			} else if(p.equals("pt-BR")) {
				locale = new Locale("pt", "BR");
			} else if(p.equals("bn-IN") || p.startsWith("bn")) {
				locale = new Locale("bn", "IN");
			} else {
				/**
				 * Avoid a crash of
				 * java.lang.AssertionError: couldn't initialize LocaleData for locale
				 * if the user enters nonsensical region codes.
				 */
				if(p.contains("-"))
					p = p.substring(0, p.indexOf('-'));
				locale = new Locale(p);
			}
			Locale.setDefault(locale);
			Configuration config = new Configuration();
			config.locale = locale;
			getBaseContext().getResources().updateConfiguration(config,
					getBaseContext().getResources().getDisplayMetrics());
		}

		instance = this;

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "System is running low on memory");

		BitmapCache.getInstance().clear();
	}

	/**
	 * @return the main context of the Application
	 */
	public static Context getAppContext()
	{
		return instance;
	}

	/**
	 * @return the main resources from the Application
	 */
	public static Resources getAppResources()
	{
		if(instance == null) return null;
		return instance.getResources();
	}
}