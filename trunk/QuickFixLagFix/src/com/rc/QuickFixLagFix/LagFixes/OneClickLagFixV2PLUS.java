package com.rc.QuickFixLagFix.LagFixes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.StatFs;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixSeekOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixSeekOption.SeekOptionUpdate;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class OneClickLagFixV2PLUS extends LagFix {

	@Override
	public String GetDisplayName() {
		return "OneClickLagFix V2+";
	}

	@Override
	public String GetShortDescription() {
		return "The 1.0 lagfix using bind mounts.";
	}

	@Override
	public String GetLongDescription() {
		return "This lagfix is similar to the 1.0 lagfix, in that it uses a loopback EXT2 mount on top of RFS to store the RFS data inside. The difference between V2 and V1 is that this lagfix will use a bind mount, while V1 uses symlinks. The bind mount allows for accurate space information in Android, and has slightly increased speed. \n\nALPHA QUALITY\n\n";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools from the menu to use this lag fix.";

		StatFs statfs = new StatFs("/data/");
		long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		long bytecountused = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree;

		final long minsize = bytecountused + 1024L * 1024L * 100;
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
			File ext2data = new File("/dbdata/ext2data");
			if (ext2data.exists() && ext2data.isDirectory()) {
				statfs.restat("/dbdata/ext2data/");
				long ext2data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
				if (ext2data_total == data_total)
					return "You already appear to have another lag fix installed! (free space on /data matches /dbdata/ext2data)";
			}
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
			Utils.InitializeBootSupport(R.raw.oclfv2plus, "oclfv2plus.sh", ApplicationContext, vt);

			UpdateStatus("Calculating sizes...");

			StatFs statfs = new StatFs("/data/");
			long progress = Long.parseLong(options.get("ext2size"));
			long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
			long bytecountused = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree;

			final long minsize = bytecountused + 1024L * 1024L * 100;
			final long maxsize = bytecountfree - 1024L * 1024L * 200;
			long curval = minsize + progress * (maxsize - minsize) / 10000;

			UpdateStatus("Mounting RFS /data onto /dbdata/rfsdata");
			vt.runCommand("sync");
			vt.runCommand("umount /dbdata/rfsdata");
			vt.busybox("rm -rf /dbdata/rfsdata");
			vt.busybox("mkdir /dbdata/rfsdata");
			vt.busybox("rm -rf /dbdata/null");
			vt.busybox("mkdir /dbdata/null");
			VTCommandResult r = vt.runCommand("mount -t rfs -o nosuid,nodev,check=no,noatime,nodiratime /dev/block/mmcblk0p2 /dbdata/rfsdata");
			if (!r.success())
				return "Could not mount /dev/block/mmcblk0p2 onto /dbdata/rfsdata: " + r.stderr;

			UpdateStatus("Creating " + Utils.FormatByte(curval) + " file to store data inside. This could take a long time...");
			curval /= 1024L;
			curval /= 1024L;

			vt.busybox("rm -rf /dbdata/rfsdata/ext2");
			vt.busybox("mkdir /dbdata/rfsdata/ext2");
			String err;
			UpdateStatus("Running command: " + "dd if=/dev/zero of=/dbdata/rfsdata/ext2/linux.ex2 bs=1M count=0 seek=" + curval);
			r = vt.busybox("dd if=/dev/zero of=/dbdata/rfsdata/ext2/linux.ex2 bs=1M count=0 seek=" + curval);
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not create /dbdata/rfsdata/ext2/linux.ex2 - " + err;
			}

			UpdateStatus("Creating loopback device");
			r = vt.busybox("mknod /dev/loop0 b 7 0");
			UpdateStatus("Linking loopback to the file store");
			r = vt.busybox("losetup /dev/loop0 /dbdata/rfsdata/ext2/linux.ex2");
			if (!r.success() && !r.stderr.trim().equalsIgnoreCase("losetup: /dev/loop0")) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not link loopback device /dev/loop0 to /dbdata/rfsdata/ext2/linux.ex2! " + err;
			}
			UpdateStatus("Creating the EXT2 filesystem");
			r = vt.busybox("mkfs.ext2 -b 4096 /dev/loop0");
			vt.busybox("rm -rf /dbdata/ext2data");
			vt.busybox("mkdir /dbdata/ext2data");
			UpdateStatus("Mounting Device");
			r = vt.runCommand("mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop0 /dbdata/ext2data");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("losetup -d /dev/loop0");
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not mount loopback device /dev/loop0! " + err;
			}

			UpdateStatus("Override /data/linux.ex2 with a null mount, so it doesn't get copied across");
			r = vt.busybox("mount -o bind /dbdata/null /data/ext2");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("losetup -d /dev/loop0");
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not mount null mount! " + err;
			}
			UpdateStatus("Copying over /data to EXT2");
			vt.runCommand("sync");
			r = vt.busybox("cp -rp /data /dbdata/ext2data/");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("umount /dbdata/ext2data");
				r = vt.busybox("umount /dbdata/rfsdata");
				r = vt.busybox("losetup -d /dev/loop0");
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not copy over /data to EXT2: " + err;
			}

			UpdateStatus("Bind mounting /dbdata/ext2data/data onto /data");
			r = vt.runCommand("mount -o bind /dbdata/ext2data/data /data");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("umount /dbdata/ext2data");
				r = vt.busybox("umount /dbdata/rfsdata");
				r = vt.busybox("losetup -d /dev/loop0");
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not copy over /data to EXT2: " + err;
			}

			UpdateStatus("Setting up boot support");
			try {
				Utils.SetupBootSupport("oclfv2plus.sh", vt);
			} catch (Exception ex) {
				return ex.getLocalizedMessage();
			}

			UpdateStatus("System will reboot in 5 seconds, to ensure everything works properly.");
			vt.FNF("sync");
			Thread.sleep(5000);
			vt.FNF("sync");
			vt.FNF("reboot");
		} finally {
			Utils.DisableFlightMode(ApplicationContext);
		}

		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		StatFs statfs = new StatFs("/data/");
		long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		long bytecountused = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree;

		final long minsize = bytecountused + 1024L * 1024L * 100;
		final long maxsize = bytecountfree - 1024L * 1024L * 200;

		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		LagFixSeekOption lagFixSeekOption = new LagFixSeekOption("ext2size", "EXT Size", "Size of EXT2 partition", 4500);
		lagFixSeekOption.setUpdatehandler(new SeekOptionUpdate(lagFixSeekOption) {

			@Override
			public String DisplayValue(int progress) {
				long curval = minsize + progress * (maxsize - minsize) / 10000;
				return Utils.FormatByte(curval);
			}
		});
		lagFixOptions.add(lagFixSeekOption);

		return lagFixOptions;
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

}