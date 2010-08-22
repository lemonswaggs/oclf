package com.rc.QuickFixLagFix.LagFixes;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.os.StatFs;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixSeekOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixSeekOption.SeekOptionUpdate;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.OptionListener;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class OneClickLagFixV1_3 extends LagFix {

	String[] dataDirectories = new String[]{"data", "app", "app-private", "dalvik-cache", "system"};

	@Override
	public String GetDisplayName() {
		return "OneClickLagFix V1-3";
	}

	@Override
	public String GetShortDescription() {
		return "Java version of OneClickLagFix V2-3 with size limitations.";
	}

	@Override
	public String GetLongDescription() {
		return "This is a clone of the RyanZA One Click Lag Fix 2-3 found on XDA Developers. It is implemented using Java instead of an 'sh' script. It contains a lot of checks. This version has size limitations compared to the stand-alone version. This will hopefully be fixed soon...";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		r = cmd.su.busyboxWaitFor("");
		if (!r.success() || !r.stdout.contains("BusyBox v1.17.1"))
			return "Packaged BusyBox v1.17.1 is required for this fix. Install it from the menu.";

		r = cmd.su.busyboxWaitFor("mkfs.ext2");
		if (r.stderr.contains("applet not found"))
			return "BusyBox v1.17.1 is required for this fix. Your busybox does not have mkfs.ext2.";
		
		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools to use this lag fix.";

		StatFs statfs = new StatFs("/data/");
		long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		long bytecountused = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree;

		final long minsize = bytecountused + 1024L * 1024L * 10;
		final long maxsize = bytecountfree - 1024L * 1024L * 200;

		if (minsize > maxsize)
			return "You do not have enough free space in /data to use this fix. Please free up space before using this fix.";

		if (Utils.GetBatteryLevel(ApplicationContext) < 40) {
			return "You need at least 40% battery charge to use this fix.";
		}

		RandomAccessFile rfile = null;
		try {
			rfile = new RandomAccessFile("/system/bin/playlogos1", "r");
			if (rfile.getChannel().size() < 5000)
				return "You already appear to have another lag fix installed! (/system/bin/playlogos1 less than 5KB)";
			statfs.restat("/data/");
			long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			statfs.restat("/data/data/");
			long datadata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			if (data_total != datadata_total)
				return "You already appear to have another lag fix installed! (free space on /data does not match /data/data)";
		} catch (FileNotFoundException ex) {
			return "You do not have a /system/bin/playlogos1 file!";
		} finally {
			Utils.CloseFile(rfile);
		}

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Enabling Flight Mode and killing all running apps.");
		Utils.EnableFlightMode(ApplicationContext);
		try {
			Utils.KillAllRunningApps(ApplicationContext);

			UpdateStatus("Initializing boot support");
			Utils.InitializeBootSupport(R.raw.oclfv2_3, "oclfv2_3.sh", ApplicationContext, vt);

			UpdateStatus("Calculating sizes...");

			StatFs statfs = new StatFs("/data/");
			long progress = Long.parseLong(options.get("ext2size"));
			long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
			long bytecountused = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree;

			final long minsize = bytecountused + 1024L * 1024L * 10;
			final long maxsize = bytecountfree - 1024L * 1024L * 200;
			long curval = minsize + progress * (maxsize - minsize) / 10000;

			UpdateStatus("Creating " + Utils.FormatByte(curval) + " file to store data inside. This could take a long time...");
			curval /= 1024L;
			curval /= 1024L;

			VTCommandResult r = vt.busybox("rm /data/linux.ex2");
			String err;
			UpdateStatus("Running command: " + "dd if=/dev/zero of=/data/linux.ex2 bs=1M count=0 seek=" + curval);
			r = vt.busybox("dd if=/dev/zero of=/data/linux.ex2 bs=1M count=0 seek=" + curval);
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("rm /data/linux.ex2");
				return "Could not create /data/linux.ex2 - " + err;
			}

			UpdateStatus("Creating loopback device");
			r = vt.busybox("mknod /dev/loop0 b 7 0");
			UpdateStatus("Linking loopback to the 1GB file store");
			r = vt.busybox("losetup /dev/loop0 /data/linux.ex2");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("rm /data/linux.ex2");
				return "Could not link loopback device /dev/loop0 to /data/linux.ex2! " + err;
			}
			UpdateStatus("Creating the EXT2 filesystem");
			r = vt.busybox("mkfs.ext2 -b 4096 /dev/loop0");
			vt.busybox("rm -rf /data/ext2data");
			vt.busybox("mkdir /data/ext2data");
			UpdateStatus("Mounting Device");
			r = vt.runCommand("mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop0 /data/ext2data");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("losetup -d /dev/loop0");
				r = vt.busybox("rm /data/linux.ex2");
				return "Could not mount loopback device /dev/loop0! " + err;
			}

			for (String dir : dataDirectories) {
				UpdateStatus("Copying over folder " + dir + " to EXT2");
				r = vt.busybox("cp -rp /data/" + dir + " /data/ext2data/");
				if (!r.success()) {
					UpdateStatus("Hit an error, undoing lagfix!");
					err = r.stderr;
					r = vt.busybox("umount /data/ext2data");
					r = vt.busybox("losetup -d /dev/loop0");
					r = vt.busybox("rm /data/linux.ex2");
					return "Could not copy over " + dir + ": " + err;
				}
			}
			for (String dir : dataDirectories) {
				r = vt.busybox("rm -rf /data/" + dir + ".bak");
			}
			r = vt.busybox("rm -rf /data/bak");
			r = vt.busybox("mkdir /data/bak");
			
			for (String dir : dataDirectories) {				
				UpdateStatus("Renaming old " + dir + " folder to " + dir + ".bak");
				r = vt.busybox("mv /data/" + dir + " /data/bak/" + dir);
				if (!r.success()) {
					UpdateStatus("Hit an error, undoing lagfix!");
					err = r.stderr;
					for (String dir2 : dataDirectories) {
						UpdateStatus("Renaming " + dir2 + ".bak folder back to to " + dir2);
						r = vt.busybox("mv /data/bak/" + dir2 + " /data/" + dir2);
					}
					r = vt.busybox("umount /data/ext2data");
					r = vt.busybox("losetup -d /dev/loop0");
					r = vt.busybox("rm /data/linux.ex2");
					return "Could not rename folder " + dir + "! " + err;
				}
			}
			
			
			for (String dir : dataDirectories) {
				if (!dir.equals("app-private")) {
					UpdateStatus("Mounting loopback for " + dir);
					vt.busybox("mkdir /data/"+dir);
					r = vt.busybox("mount -o bind /data/ext2data/" + dir + " /data/" + dir);
					if (!r.success()) {
						return "Could not mount " + dir + ": " + r.stderr;
					}
				}
			}

			UpdateStatus("Creating symlink for app-private into app");
			r = vt.busybox("ln -s /data/app/app-private /data/app-private");
			if (!r.success()) {
				return "Could not create symlink for /app-private: " + r.stderr;
			}
			
			UpdateStatus("Setting up boot support");
			try {
				Utils.SetupBootSupport("oclfv2_3.sh", vt);
			} catch (Exception ex) {
				UpdateStatus("Error while setting up boot support!");
				return ex.getLocalizedMessage();
			}

			UpdateStatus("Removing temporary backup files off RFS. It is possible to see apps FC at this point.");
			r = vt.busybox("rm -rf /data/bak");
			
			UpdateStatus("System will reboot in 10 seconds, to ensure everything works properly.");
			Thread.sleep(10000);
			vt.FNF("reboot");
		} finally {
			Utils.DisableFlightMode(ApplicationContext);
		}

		return SUCCESS;
	}

	@Override
	public void GetOptions(OptionListener listener) throws Exception {
		StatFs statfs = new StatFs("/data/");
		long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		long bytecountused = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree;

		final long minsize = bytecountused + 1024L * 1024L * 10;
		final long maxsize = bytecountfree - 1024L * 1024L * 200;

		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		LagFixSeekOption lagFixSeekOption = new LagFixSeekOption("ext2size", "EXT Size", "Size of EXT2 partition", 10000);
		lagFixSeekOption.setUpdatehandler(new SeekOptionUpdate(lagFixSeekOption) {

			@Override
			public String DisplayValue(int progress) {
				long curval = minsize + progress * (maxsize - minsize) / 10000;
				return Utils.FormatByte(curval);
			}
		});
		lagFixOptions.add(lagFixSeekOption);

		listener.LagFixOptionListCompleted(lagFixOptions);
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

}
