package com.rc.QuickFixLagFix.lib;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class Utils {

	public static void EnableFlightMode(Context context) {
		Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);

		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", true);
		context.sendBroadcast(intent);
	}

	public static void DisableFlightMode(Context context) {
		Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);

		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", false);
		context.sendBroadcast(intent);
	}

	public static void KillAllRunningApps(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> runningapps = am.getRunningAppProcesses();
		for (RunningAppProcessInfo app : runningapps) {
			if (app.processName.equals("system") || app.processName.equals("com.rc.QuickFixLagFix"))
				continue;
			am.restartPackage(app.processName);
		}
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static int GetBatteryLevel(Context context) {
		Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = intent.getIntExtra("level", 0);
		return level;
	}

	public static void CloseFile(RandomAccessFile rfile) {
		try {
			rfile.close();
		} catch (Exception ex) {
		}
	}

	public static void SaveIncludedFileIntoFilesFolder(int resourceid, String filename, Context ApplicationContext) throws Exception {
		InputStream is = ApplicationContext.getResources().openRawResource(resourceid);
		FileOutputStream fos = ApplicationContext.openFileOutput(filename, Context.MODE_WORLD_READABLE);
		byte[] bytebuf = new byte[1024];
		int read;
		while ((read = is.read(bytebuf)) >= 0) {
			fos.write(bytebuf, 0, read);
		}
		is.close();
		fos.getChannel().force(true);
		fos.flush();
		fos.close();
	}

	public static VTCommandResult CopyIncludedFiletoPath(int resourceid, String filename, String ToPath, Context ApplicationContext, VirtualTerminal vt) throws Exception {
		SaveIncludedFileIntoFilesFolder(resourceid, filename, ApplicationContext);
		String FromPath = ApplicationContext.getFilesDir() + "/" + filename;
		return vt.runCommand("dd if=" + FromPath + " of=" + ToPath);
	}
	
	public static void CopyIncludedFiletoPath(int resourceid, String ToPath, Context ApplicationContext) throws Exception {
//		ShellCommand sh = new ShellCommand();
//		SaveIncludedFileIntoFilesFolder(resourceid, filename, ApplicationContext);
//		String FromPath = ApplicationContext.getFilesDir() + "/" + filename;
//		sh.sh.runWaitFor("rm "+ToPath);
//		return sh.sh.runWaitFor("dd if=" + FromPath + " of=" + ToPath);
		
		InputStream is = ApplicationContext.getResources().openRawResource(resourceid);
		FileOutputStream fos = new FileOutputStream(ToPath);
		byte[] bytebuf = new byte[1024];
		int read;
		while ((read = is.read(bytebuf)) >= 0) {
			fos.write(bytebuf, 0, read);
		}
		is.close();
		fos.getChannel().force(true);
		fos.flush();
		fos.close();
	}
	
	public static void SetupBootSupport(String filename, VirtualTerminal vt) throws Exception {
		VTCommandResult r = vt.busybox("mv /system/bin/playlogos1 /system/bin/playlogosnow");
		if (!r.success()) {
			throw new Exception("Could not mv playlogos1 to playlogosnow - " + r.stderr);
		}
		r = vt.busybox("cp /data/oclf/bootsupport/playlogos1 /system/bin/playlogos1");
		if (!r.success()) {
			vt.busybox("mv /system/bin/playlogosnow /system/bin/playlogos1");
			throw new Exception("Could not copy included playlogos1 script to /system/bin/playlogos1 - " + r.stderr);
		}
		r = vt.busybox("cp /data/oclf/bootsupport/"+filename+" /system/bin/userinit.sh");
		if (!r.success()) {
			vt.busybox("mv /system/bin/playlogosnow /system/bin/playlogos1");
			throw new Exception("Could not copy included " + filename + " script to /system/bin/userinit.sh - " + r.stderr);
		}
		r = vt.busybox("chmod 755 /system/bin/playlogos1");
		if (!r.success()) {
			vt.busybox("mv /system/bin/playlogosnow /system/bin/playlogos1");
			throw new Exception("Could not set up permissions for /system/bin/playlogos1 script - " + r.stderr);
		}
		r = vt.busybox("chmod 755 /system/bin/userinit.sh");
		if (!r.success()) {
			vt.busybox("mv /system/bin/playlogosnow /system/bin/playlogos1");
			throw new Exception("Could not set up permissions for /system/bin/userinit.sh script - " + r.stderr);
		}
	}

	public static void InitializeBootSupport(int resourceid, String filename, Context ApplicationContext, VirtualTerminal vt) throws Exception {
		VTCommandResult r = vt.busybox("rm -rf /data/oclf/bootsupport");
		r = vt.busybox("mkdir -p /data/oclf/bootsupport");
		if (!r.success()) {
			throw new Exception("Could not create bootsupport folder - " + r.stderr);
		}
		r = CopyIncludedFiletoPath(R.raw.playlogos1, "playlogos1", "/data/oclf/bootsupport/playlogos1", ApplicationContext, vt);
		if (!r.success()) {
			throw new Exception("Could not copy included playlogos1 script to /data/oclf/bootsupport/playlogos11 - " + r.stderr);
		}
		r = CopyIncludedFiletoPath(resourceid, filename, "/data/oclf/bootsupport/"+filename, ApplicationContext, vt);
		if (!r.success()) {
			throw new Exception("Could not copy included " + filename + " script to /data/oclf/bootsupport/"+filename+" - " + r.stderr);
		}
	}

	public static void RemoveBootSupport(VirtualTerminal vt) throws Exception {
		vt.busybox("mv /system/bin/playlogosnow /system/bin/playlogos1");
		vt.busybox("rm /system/bin/userinit.sh");
	}

	public static String FormatByte(long bytes) {
		return Long.toString(bytes / 1024L / 1024L) + "MB";
	}

	static final String HEXES = "0123456789ABCDEF";
	public static String getHex(byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length);
		for (final byte b : raw) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}

	public static String GetMD5Hash(String file) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		InputStream fis = new FileInputStream(file);
		byte[] buffer = new byte[1024];

		int numRead;
		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				md.update(buffer, 0, numRead);
			}
		} while (numRead != -1);
		fis.close();

		return getHex(md.digest());
	}
	
	public static long getDirectorySize(String path, VirtualTerminal vt) throws Exception {
		VTCommandResult r = vt.busybox("du -sx "+path+" | awk '{print $1;}'");
		if (!r.success())
			throw new Exception("Could not get size of "+path+": "+r.stderr);
		
		String kbSizeStr = r.stdout.replaceAll("\n:RET=0", "").trim();
		long kbSize = Long.parseLong(kbSizeStr);
		return kbSize*1024L;
	}

}
