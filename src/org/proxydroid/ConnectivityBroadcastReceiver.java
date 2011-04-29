package org.proxydroid;

import java.util.ArrayList;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "ConnectivityBroadcastReceiver";

	public boolean isWorked(Context context, String service) {
		ActivityManager myManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		ArrayList<RunningServiceInfo> runningService = (ArrayList<RunningServiceInfo>) myManager
				.getRunningServices(30);
		for (int i = 0; i < runningService.size(); i++) {
			if (runningService.get(i).service.getClassName().toString()
					.equals(service)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			Log.w(TAG, "onReceived() called uncorrectly");
			return;
		}

		Log.e(TAG, "Connection Test");

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		
		String[] profileValues = settings.getString("profileValues", "").split(
		"\\|");
		
		for (String profile : profileValues) {
			String profileString = settings.getString(profile, "");
			String[] st = profileString.split("\\|");
			if (st.length >= 8 && isOnline(context, st[6])) {
				Editor ed = settings.edit();
				ed.putString("host", st[0].equals("null") ? "" : st[0]);
				ed.putString("port", st[1]);
				ed.putString("user", st[2].equals("null") ? "" : st[2]);
				ed.putString("password", st[3].equals("null") ? "" : st[3]);
				ed.putBoolean("isSocks", st[4].equals("true") ? true : false);
				ed.putString("proxyType", st[5]);
				ed.putString("ssid", st[6]);
				ed.putBoolean("isAutoConnect", st[7].equals("true") ? true : false);
				ed.commit();
				break;
			}
		}

		String ssid = settings.getString("ssid", "");
		if (isOnline(context, ssid)) {
			if (!isWorked(context, ProxyDroid.SERVICE_NAME)) {
				ProxyDroidReceiver pdr = new ProxyDroidReceiver();
				pdr.onReceive(context, intent);
			}
		}

	}

	public boolean isOnline(Context context, String ssid) {
		String ssids[] = ssid.split(" ");
		if (ssids.length < 1)
			return false;
		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		if (networkInfo == null)
			return false;
		if (!networkInfo.getTypeName().equals("WIFI"))
			if (ssid.equals("2G/3G"))
				return true;
			else
				return false;
		WifiManager wm = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wm.getConnectionInfo();
		if (wInfo == null)
			return false;
		String current = wInfo.getSSID();
		if (current == null || current.equals(""))
			return false;
		for (String item : ssids) {
			if (item.equals(current))
				return true;
		}
		return false;
	}

}