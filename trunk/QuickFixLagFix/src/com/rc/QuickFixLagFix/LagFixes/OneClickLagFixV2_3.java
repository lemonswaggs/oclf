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

public class OneClickLagFixV2_3 extends LagFix {

	String[] dataDirectories = new String[]{"data", "app", "app-private", "dalvik-cache", "system"};
	String[] dataDirectoriesToMoveNow = new String[]{"data", "app", "app-private"};
	String[] dataDirectoriesToMoveLater = new String[]{"dalvik-cache", "system" };

	@Override
	public String GetDisplayName() {
		return "OneClickLagFix V2-3";
	}

	@Override
	public String GetShortDescription() {
		return "Java version of OneClickLagFix V2-3";
	}

	@Override
	public String GetLongDescription() {
		return "This is a clone of the RyanZA One Click Lag Fix 2-3 found on XDA Developers.";
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

		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools to use this lag fix.";

		StatFs statfs = new StatFs("/data/");
		long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();

		if (bytecountfree < 1024L * 1024L * 200L)
			return "You must have 200MB of free space to use this lag fix. Remove some applications to free up space.";

		statfs = new StatFs("/sdcard/");
		bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();

		if (bytecountfree < 1024L * 1024L * 1024L * 2L)
			return "You must have 2GB of free space on /sdcard/ to use this lag fix.";

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
			final long bytecounttotal = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			final long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();

			long dataDirSize = 0;

			for (String dir : dataDirectoriesToMoveNow) {
				long dirSize = Utils.getDirectorySize("/data/" + dir, vt);
				dataDirSize += dirSize;
			}

			final long minsize = 1024L * 1024L * 100L + dataDirSize;
			final long maxsize = bytecounttotal - 1024L * 1024L * 200L - (bytecounttotal - dataDirSize - bytecountfree);
			long curval = minsize + progress * (maxsize - minsize) / 10000;

			long MBSizeForTempFile = dataDirSize + 1024L * 1024L * 50L;
			UpdateStatus("Creating " + Utils.FormatByte(MBSizeForTempFile) + " temporary file on /sdcard/ to store data inside. This could take a long time...");
			MBSizeForTempFile /= 1024L;
			MBSizeForTempFile /= 1024L;

			VTCommandResult r = vt.busybox("rm /sdcard/linux.ex2");
			String err;
			UpdateStatus("Running command: " + "dd if=/dev/zero of=/sdcard/linux.ex2 bs=1M count=0 seek=" + MBSizeForTempFile);
			r = vt.busybox("dd if=/dev/zero of=/sdcard/linux.ex2 bs=1M count=0 seek=" + MBSizeForTempFile);
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("rm /sdcard/linux.ex2");
				return "Could not create /sdcard/linux.ex2 - " + err;
			}

			UpdateStatus("Creating loopback devices");
			r = vt.busybox("mknod /dev/loop0 b 7 1");
			r = vt.busybox("mknod /dev/loop1 b 7 2");
			UpdateStatus("Linking loopback to the temporary file store");
			r = vt.busybox("losetup /dev/loop0 /sdcard/linux.ex2");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("rm /sdcard/linux.ex2");
				return "Could not link loopback device /dev/loop0 to /sdcard/linux.ex2! " + err;
			}

			UpdateStatus("Creating the EXT2 filesystem");
			r = vt.runCommand("/data/oclf/mke2fs -b 4096 -m 0 /dev/loop0;");
			vt.busybox("rm -rf /data/sddata");
			vt.busybox("mkdir /data/sddata");
			UpdateStatus("Mounting Device");
			r = vt.runCommand("mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop0 /data/sddata");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("losetup -d /dev/loop0");
				r = vt.busybox("rm /sdcard/linux.ex2");
				return "Could not mount loopback device /dev/loop0! " + err;
			}

			for (String dir : dataDirectoriesToMoveNow) {
				UpdateStatus("Copying over folder " + dir + " to temporary filesystem");
				r = vt.busybox("cp -rp /data/" + dir + " /data/sddata/");
				if (!r.success()) {
					UpdateStatus("Hit an error, undoing lagfix!");
					err = r.stderr;
					for (String dir2 : dataDirectoriesToMoveNow) {
						vt.busybox("cp -rp /data/sddata/" + dir2 + " /data/");
					}
					r = vt.busybox("umount /data/sddata");
					r = vt.busybox("losetup -d /dev/loop0");
					r = vt.busybox("rm /sdcard/linux.ex2");
					return "Could not copy over " + dir + ": " + err;
				}
			}
			
//			r = vt.busybox("ls /data/data");
//			String[] files = r.stdout.split("\n");
//			for (String relfile : files) {
//				if (relfile == null || relfile.equals("")) continue;
//				String abspath = "/data/data/"+relfile;
//				UpdateStatus("Removing "+abspath);
//				if (!abspath.startsWith("/data/"))
//					throw new Exception(abspath+" BAD");
//				vt.busybox("rm -rf " + abspath);
//				Thread.sleep(2000);
//				UpdateStatus("No FC on "+abspath);				
//			}
			
//			UpdateStatus("Pulling important files into backup...");
//			vt.busybox("mkdir /data/bak");
//			vt.busybox("mv /data/dalvik-cache/data@app@com.rc.QuickFixLagFix.apk@classes.dex /data/bak/");
//			vt.busybox("mv /data/app/com.rc.QuickFixLagFix.apk /data/bak/");
//			vt.busybox("mv /data/data/com.rc.QuickFixLagFix /data/bak/");
			
			for (String dir : dataDirectoriesToMoveNow) {
				UpdateStatus("Removing contents of /data/" + dir + ". You may see FCs at this point.");
				r = vt.busybox("rm -rf /data/" + dir+".bak/*");
			}
			for (String dir : dataDirectoriesToMoveLater) {
				UpdateStatus("Renaming /data/" + dir + " to .bak - You may see FCs at this point.");
				r = vt.busybox("mv /data/" + dir + " /data/" + dir + ".bak");
			}
			UpdateStatus("Creating mountpoints...");
			for (String dir : dataDirectories) {
				r = vt.busybox("mkdir -p /data/" + dir);
			}
			UpdateStatus("Now creating real " + Utils.FormatByte(curval) + " EXT2 partition in /data. Will take a long time.");
			curval /= 1024L;
			curval /= 1024L;
			UpdateStatus("Running command: " + "dd if=/dev/zero of=/data/linux.ex2 bs=1M count=0 seek=" + curval);
			r = vt.busybox("dd if=/dev/zero of=/data/linux.ex2 bs=1M count=0 seek=" + curval);
			if (!r.success()) {
				err = r.stderr;
				UpdateStatus("Hit an error, undoing lagfix!");
				// TODO
				return "Could not create /data/linux.ex2 - " + err;
			}

			UpdateStatus("Linking loopback to the real file store");
			r = vt.busybox("losetup /dev/loop1 /data/linux.ex2");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				// TODO
				return "Could not link loopback device /dev/loop1 to /data/linux.ex2! " + err;
			}

			UpdateStatus("Creating the EXT2 filesystem");
			r = vt.runCommand("/data/oclf/mke2fs -b 4096 -m 0 /dev/loop1;");
			vt.busybox("rm -rf /data/ext2data");
			vt.busybox("mkdir /data/ext2data");
			UpdateStatus("Mounting Device");
			r = vt.runCommand("mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop1 /data/ext2data");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				// TODO
				return "Could not mount loopback device /dev/loop1! " + err;
			}

			for (String dir : dataDirectoriesToMoveNow) {
				UpdateStatus("Copying over folder " + dir + " from temporary filesystem to EXT2 filesystem");
				r = vt.busybox("cp -rp /data/sddata/" + dir + " /data/ext2data/");
				if (!r.success()) {
					UpdateStatus("Hit an error, undoing lagfix!");
					err = r.stderr;
					// TODO
					return "Could not copy over " + dir + ": " + err;
				}
			}

			for (String dir : dataDirectoriesToMoveLater) {
				UpdateStatus("Copying over folder " + dir + " from .bak to EXT2 filesystem");
				r = vt.busybox("cp -rp /data/" + dir + ".bak /data/ext2data/");
				r = vt.busybox("mv /data/ext2data/" + dir + ".bak /data/ext2data/" + dir);
				if (!r.success()) {
					UpdateStatus("Hit an error, undoing lagfix!");
					err = r.stderr;
					// TODO
					return "Could not copy over " + dir + ": " + err;
				}
			}

			for (String dir : dataDirectories) {
				if (!dir.equals("app-private")) {
					UpdateStatus("Mounting loopback for " + dir);
					r = vt.busybox("mount -o bind /data/ext2data/" + dir + " /data/" + dir);
					if (!r.success()) {
						UpdateStatus("Hit an error, undoing lagfix!");
						err = r.stderr;
						// TODO
						return "Could not mount " + dir + ": " + err;
					}
				}
			}

			UpdateStatus("Creating symlink for app-private into app");
			r = vt.busybox("ln -s /data/app/app-private /data/app-private");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				// TODO
				return "Could not create symlink for /app-private: " + err;
			}

			UpdateStatus("Setting up boot support");
			try {
				Utils.SetupBootSupport("oclfv2_3.sh", vt);
			} catch (Exception ex) {
				UpdateStatus("Error while setting up boot support! Undoing lag fix.");
				// TODO
				return ex.getLocalizedMessage();
			}
			UpdateStatus("Removing temporary backup files off RFS. It is possible to see apps FC at this point.");
			for (String dir : dataDirectoriesToMoveLater) {
				r = vt.busybox("rm -rf /data/" + dir + ".bak");
			}

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
		VirtualTerminal vt = new VirtualTerminal();

		final long bytecounttotal = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		final long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();

		long dataDirSize = 0;

		for (String dir : dataDirectoriesToMoveNow) {
			long dirSize = Utils.getDirectorySize("/data/" + dir, vt);
			dataDirSize += dirSize;
		}

		final long minsize = 1024L * 1024L * 100L + dataDirSize;
		final long maxsize = bytecounttotal - 1024L * 1024L * 200L - (bytecounttotal - dataDirSize - bytecountfree);

		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		LagFixSeekOption lagFixSeekOption = new LagFixSeekOption("ext2size", "EXT Size",
				"Size of EXT2 partition. It is recommended that you leave this bar on the maximum value for this lag fix.", 10000);
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
