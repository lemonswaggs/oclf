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

	final static String[] dataDirectories = new String[]{"data", "system", "dalvik-cache", "app", "app-private"};
	
	@Override
	public String GetDisplayName() {
		return "OneClickLagFix V2.2+";
	}

	@Override
	public String GetShortDescription() {
		return "The 1.0 lagfix using bind mounts.";
	}

	@Override
	public String GetLongDescription() {
		return "This lagfix is similar to the 1.0 lagfix, in that it uses a loopback EXT2 mount on top of RFS to store the RFS data inside. The difference between V2 and V1 is that this lagfix will use a bind mount, while V1 uses symlinks. The bind mount allows for accurate space information in Android, and has slightly increased speed. \n\nThe difference between 2.1 and 2.0 is a technique used to ensure that the device does not boot until the EXT2 checks are done.\n\nV2.2 fixes a vibration issue with certain firmwares, as well as the timezone issue.\n\nThe slider controls how big the EXT2 loopback partition will be. The bigger it is, the more space you will have available after the lagfix is installed. The problem with putting it all the way to the right is that when uninstalling, you may have to delete apps to get enough free space to copy them back to RFS. So the default is around 800-1200mb which generally works well for most people.\n\n";
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
			// Check for playlogo file for Tab
			try {
				rfile = new RandomAccessFile("/system/bin/playlogo", "r");
				if (rfile.getChannel().size() < 5000)
					return "You already appear to have another lag fix installed! (/system/bin/playlogo less than 5KB)";
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
			} catch (FileNotFoundException ex2) {
				return "You do not have a /system/bin/playlogos1 / /system/bin/playlogo file!"; 
			}
		} finally {
			Utils.CloseFile(rfile);
		}

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Enabling Flight Mode and killing all running apps.");
		try {
			Utils.EnableFlightMode(ApplicationContext);
		} catch (Exception ex) {
			UpdateStatus("Could not enable flight mode!");
		}
		try {
			Utils.KillAllRunningApps(ApplicationContext);

			UpdateStatus("Initializing boot support");
			Utils.InitializeBootSupport(R.raw.oclfv22plus, "oclfv2plus.sh", ApplicationContext, vt);

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
			r = vt.busybox("mknod /dev/loop5 b 7 5");
			UpdateStatus("Linking loopback to the file store");
			r = vt.busybox("losetup /dev/loop5 /dbdata/rfsdata/ext2/linux.ex2");
			if (!r.success() && !r.stderr.trim().equalsIgnoreCase("losetup: /dev/loop5")) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not link loopback device /dev/loop5 to /dbdata/rfsdata/ext2/linux.ex2! " + err;
			}
			UpdateStatus("Creating the EXT2 filesystem");
			r = vt.busybox("mkfs.ext2 -b 4096 /dev/loop5");
			vt.busybox("rm -rf /dbdata/ext2data");
			vt.busybox("mkdir /dbdata/ext2data");
			UpdateStatus("Mounting Device");
			r = vt.runCommand("mount -t ext2 -o noatime,nodiratime,errors=continue /dev/loop5 /dbdata/ext2data");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("losetup -d /dev/loop5");
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not mount loopback device /dev/loop5! " + err;
			}

			UpdateStatus("Override /data/linux.ex2 with a null mount, so it doesn't get copied across");
			r = vt.busybox("mount -o bind /dbdata/null /data/ext2");
			if (!r.success()) {
				UpdateStatus("Hit an error, undoing lagfix!");
				err = r.stderr;
				r = vt.busybox("losetup -d /dev/loop5");
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
				r = vt.busybox("losetup -d /dev/loop5");
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
				r = vt.busybox("losetup -d /dev/loop5");
				r = vt.busybox("rm -rf /dbdata/rfsdata/ext2");
				return "Could not copy over /data to EXT2: " + err;
			}

			UpdateStatus("Setting up boot support");
			try {
				if (new File("/system/bin/playlogos1").exists())
					Utils.SetupBootSupport("oclfv2plus.sh", vt, "playlogos1");
				else
					Utils.SetupBootSupport("oclfv2plus.sh", vt, "playlogo");
			} catch (Exception ex) {
				return ex.getLocalizedMessage();
			}
			
			UpdateStatus("Creating stump files to prevent the phone from booting until EXT2 checks are complete");
			for (String dir : dataDirectories) {
				vt.busybox("mv /dbdata/rfsdata/"+dir+" /dbdata/rfsdata/"+dir+".old");
				vt.runCommand("echo \"stump\" > /dbdata/rfsdata/"+dir);
			}

			UpdateStatus("System will reboot in 5 seconds, to ensure everything works properly.");
			vt.FNF("sync");
			Thread.sleep(5000);
			vt.FNF("sync");
			vt.FNF("reboot");
		} finally {
			try {
				Utils.DisableFlightMode(ApplicationContext);
			} catch (Exception ex) {
				UpdateStatus("Could not disable flight mode!");
			}	
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
