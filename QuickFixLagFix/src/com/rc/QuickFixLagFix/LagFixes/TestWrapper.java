package com.rc.QuickFixLagFix.LagFixes;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class TestWrapper extends LagFix {

	public final static String[] ReplaceCommands = new String[]{"pvrsrvinit", "servicemanager", "vold", "notified_event", "netd", "debuggerd", "rild", "drexe", "npsmobex",
			"app_process", "mediaserver", "killmediaserver", "wpa_supplicant", "dhcpcd", "mfgloader", "wlservice", "dbus-daemon", "logwrapper", "sdptool", "installd",
			"sensorserver_yamaha", "mtpd", "keystore", "immvibed", "tvoutserver", "dumpstate", "gpsd/glgps_samsungJupiter"};

	// "immvbsd"

	@Override
	public String GetDisplayName() {
		return "TestWrapper";
	}

	@Override
	public String GetShortDescription() {
		return "TestWrapper";
	}

	@Override
	public String GetLongDescription() {
		return "TestWrapper";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "TestWrapper";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		VTCommandResult r;
		vt.runCommand("mount -o remount,rw rootfs /");
		vt.runCommand("mount -o remount,rw /dev/block/stl9 /system");
		vt.busybox("rm -rf /dbdata/logs");
		vt.busybox("mkdir /dbdata/logs");
		vt.busybox("mkdir /dbdata/logs/gpsd");
		r = vt.runCommand("chmod 777 /system/bin");
		if (!r.success())
			return r.stderr;
		r = vt.runCommand("chmod 777 /system/bin/gpsd");

		// Normal commands
		for (String command : ReplaceCommands) {
			r = vt.busybox("mv /system/bin/" + command + " /system/bin/" + command + ".replace");
			if (!r.success())
				return r.stderr;
			FileOutputStream fos = new FileOutputStream("/system/bin/" + command);
			OutputStreamWriter writer = new OutputStreamWriter(fos);
			writer.write("#!/system/bin/sh\n");
			writer.write("echo \"called\" >> /dbdata/logs/" + command + "\n");
			writer.write("sleep 2\n");
			writer.write("echo \"starting " + command + ".replace $@" + "\" >> /dbdata/logs/" + command + "\n");
			writer.write("/system/bin/" + command + ".replace $@\n");
			writer.close();
			fos.flush();
			fos.close();

			vt.runCommand("chmod 755 /system/bin/" + command);
		}

		// Playlogos
		vt.busybox("mv /system/bin/playlogos1 /system/bin/playlogos1.replace");
		FileOutputStream fos = new FileOutputStream("/system/bin/playlogos1");
		OutputStreamWriter writer = new OutputStreamWriter(fos);
		writer.write("#!/system/bin/sh\n");
		writer.write("echo \"test 1\" > /dbdata/log\n");
		writer.write("!(umount /data) && reboot\n");
		writer.write("dd if=/dbdata/test2 of=/dev/block/mmcblk0\n");
		writer.write("busybox hdparm -z /dev/block/mmcblk0 2>>/dbdata/log\n");
		// writer.write("mount >> /dbdata/log\n");
		writer.write("mount -t rfs -o nosuid,nodev,check=no /dev/block/mmcblk0p2 /data 2>>/dbdata/log\n");
		writer.write("sleep 10\n");
		writer.write("mkdir /dbdata/ext2data\n");
		writer.write("mkdir /dbdata/rfsdata\n");
		writer.write("mount -t rfs -o nosuid,nodev,check=no /dev/block/mmcblk0p2 /dbdata/rfsdata 2>>/dbdata/log\n");
		writer.write("mount -t ext2 -o noatime,nodiratime,errors=continue /dev/block/mmcblk0p3 /dbdata/ext2data && ");
		writer.write("mount -o bind /dbdata/ext2data/data /data &&");
		writer.write("mount -o bind /dbdata/rfsdata/gps /data/gps &&");
		writer.write("mount -o bind /dbdata/rfsdata/misc /data/misc &&");
		writer.write("mount -o bind /dbdata/rfsdata/wifi /data/wifi &&");
		writer.write("mount -o bind /dbdata/rfsdata/dontpanic /data/dontpanic &&");
		writer.write("mount -o bind /dbdata/rfsdata/local /data/local\n");
		writer.write("echo \"test 2\" >> /dbdata/log\n");
		writer.write("/system/bin/playlogos1.replace\n");
		writer.close();
		fos.flush();
		fos.close();

		vt.runCommand("chmod 755 /system/bin/playlogos1");

		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		return lagFixOptions;
	}

}
