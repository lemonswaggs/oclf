package com.rc.QuickFixLagFix.LagFixes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.StatFs;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.MD5Hashes;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class UndoOneClickLagFixV2PLUS extends LagFix {

	final static String[] dataDirectories = new String[]{"data", "system", "dalvik-cache", "app", "app-private"};

	@Override
	public String GetDisplayName() {
		return "Undo OneClickLagFix V2+";
	}

	@Override
	public String GetShortDescription() {
		return "Will remove OneClickLagFix V2 PLUS.";
	}

	@Override
	public String GetLongDescription() {
		return "This will remove OneClickLagFix V2 PLUS. This should not affect your apps or data.";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools from the menu to use this lag fix.";

		if (Utils.GetBatteryLevel(ApplicationContext) < 40) {
			return "You need at least 40% battery charge to use this fix.";
		}

		RandomAccessFile rfile = null;
		try {
			rfile = new RandomAccessFile("/system/bin/playlogos1", "r");
			if (rfile.getChannel().size() > 5000)
				return "You do not appear to have a lagfix installed. (/system/bin/playlogos1 greater than 5KB)";
			StatFs statfs = new StatFs("/data/");
			long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			File ext2data = new File("/dbdata/ext2data");
			if (!ext2data.exists() || !ext2data.isDirectory()) {
				return "You do not appear to have a V2 lagfix installed! (/dbdata/ext2data does not exist)";
			}
			statfs.restat("/dbdata/ext2data/");
			long ext2data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			if (data_total != ext2data_total)
				return "You do not appear to have a V2 lagfix installed! (free space on /data does not match /dbdata/ext2data)";
		} catch (FileNotFoundException ex) {
			return "You do not have a /system/bin/playlogos1 file!";
		} finally {
			Utils.CloseFile(rfile);
		}

		try {
			if (Utils.GetMD5Hash("/system/bin/playlogos1").equalsIgnoreCase(MD5Hashes.playlogos1_eclair))
				return "You do not have a lagfix installed. playlogos1 matches original signature.";
			if (Utils.GetMD5Hash("/system/bin/playlogos1").equalsIgnoreCase(MD5Hashes.playlogos1_froyo))
				return "You do not have a lagfix installed. playlogos1 matches original signature.";
		} catch (FileNotFoundException ex) {
			return "You do not have a /system/bin/playlogos1 file!";
		}

		try {
			rfile = new RandomAccessFile("/system/bin/playlogosnow", "r");
			if (rfile.getChannel().size() < 5000)
				return "You do not have a backup of the playlogos1 file! This lagfix cannot be undone. (/system/bin/playlogosnow less than 5kb)";
		} catch (FileNotFoundException ex) {
			return "You do not have a backup of the playlogos1 file! This lagfix cannot be undone. (/system/bin/playlogosnow could not be found)";
		} finally {
			Utils.CloseFile(rfile);
		}

		try {
			String Hash = Utils.GetMD5Hash("/system/bin/userinit.sh");
			if (!Hash.equalsIgnoreCase(MD5Hashes.oclfv2_plus) && !Hash.equalsIgnoreCase(MD5Hashes.oclfv21_plus) && !Hash.equalsIgnoreCase(MD5Hashes.oclfv22_plus))
				return "You do not have OCLF V2 PLUS installed. userinit.sh does not match signature.";
		} catch (FileNotFoundException ex) {
			return "You do not have OCLF V2 PLUS installed. userinit.sh not found.";
		}

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Enabling Flight Mode and killing all running apps.");
		Utils.EnableFlightMode(ApplicationContext);
		try {
			Utils.KillAllRunningApps(ApplicationContext);

			UpdateStatus("Removing old backups. This could take some time.");
			for (String dir : dataDirectories) {
				vt.busybox("rm -rf /dbdata/rfsdata/" + dir);
				vt.busybox("rm -rf /dbdata/rfsdata/" + dir + ".old");
			}

			UpdateStatus("Checking to see if enough space is available on RFS...");
			StatFs statfs = new StatFs("/dbdata/rfsdata/");
			long bytecountfree_rfs = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
			statfs = new StatFs("/data/");
			long bytecountfree_ext2 = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
			long bytecountused_ext2 = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree_ext2;

			if (bytecountused_ext2 > bytecountfree_rfs)
				return "You have only " + Utils.FormatByte(bytecountfree_rfs) + " free on RFS, but " + Utils.FormatByte(bytecountused_ext2)
						+ " used on EXT2. You must free up some space.";

			UpdateStatus("Copying data back from EXT2 to RFS. This could take a long time...");
			vt.runCommand("sync");
			vt.busybox("cp -rp /data/* /dbdata/rfsdata/");
			// VTCommandResult r =
			// vt.busybox("cp -rp /data/* /dbdata/rfsdata/");
			// if (!r.success())
			// return
			// "Could not copy your data back to RFS. Please try this again after stopping or removing all background apps."+r.stderr;
			// This check is removed, since we'r going to get errors anyway
			// 'same file' for /data/wifi, etc. Need a workaround maybe?

			UpdateStatus("Removing boot support");
			Utils.RemoveBootSupport(vt);

			UpdateStatus("Removing old datafile. If the system crashes at this point, just restart your device.");
			vt.busybox("rm -rf /dbdata/rfsdata/ext2");

			UpdateStatus("System will reboot, to ensure everything works properly.");
			vt.FNF("sync");
			vt.FNF("reboot");
		} finally {
			Utils.DisableFlightMode(ApplicationContext);
		}
		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		return lagFixOptions;
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

}
