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
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class UndoOneClickLagFixV1PLUS extends LagFix {

	final static String[] dataDirectories = new String[]{"data", "system", "dalvik-cache", "app", "app-private"};

	@Override
	public String GetDisplayName() {
		return "Undo OneClickLagFix V1+";
	}

	@Override
	public String GetShortDescription() {
		return "Will remove OneClickLagFix V1 PLUS.";
	}

	@Override
	public String GetLongDescription() {
		return "This will remove OneClickLagFix V1 PLUS. This should not affect your apps or data.";
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
		long bytecountfree_rfs = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		statfs = new StatFs("/data/data/");
		long bytecountfree_ext2 = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		long bytecountused_ext2 = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree_ext2;

		if (bytecountused_ext2 > bytecountfree_rfs)
			return "You have only " + Utils.FormatByte(bytecountfree_rfs) + " free on RFS, but " + Utils.FormatByte(bytecountused_ext2)
					+ " used on EXT2. You must free up some space.";

		if (Utils.GetBatteryLevel(ApplicationContext) < 40) {
			return "You need at least 40% battery charge to use this fix.";
		}

		RandomAccessFile rfile = null;
		try {
			rfile = new RandomAccessFile("/system/bin/playlogos1", "r");
			if (rfile.getChannel().size() > 5000)
				return "You do not appear to have a lagfix installed. (/system/bin/playlogos1 greater than 5KB)";
			statfs.restat("/data/");
			long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			statfs.restat("/data/data/");
			long datadata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			if (data_total == datadata_total)
				return "You do not appear to have a V1 lagfix installed! (free space on /data matches /data/data)";
		} catch (FileNotFoundException ex) {
			try {
				rfile = new RandomAccessFile("/system/bin/playlogo", "r");
				if (rfile.getChannel().size() > 5000)
					return "You do not appear to have a lagfix installed. (/system/bin/playlogo greater than 5KB)";
				statfs.restat("/data/");
				long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
				statfs.restat("/data/data/");
				long datadata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
				if (data_total == datadata_total)
					return "You do not appear to have a V1 lagfix installed! (free space on /data matches /data/data)";
			} catch (FileNotFoundException ex2) {
				return "You do not have a /system/bin/playlogos1 file or a /system/bin/playlogo file!";
			}
		} finally {
			Utils.CloseFile(rfile);
		}

		try {
			if (Utils.GetMD5Hash("/system/bin/playlogos1").equalsIgnoreCase(MD5Hashes.playlogos1_eclair))
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
			if (!Hash.equalsIgnoreCase(MD5Hashes.oclfv1_plus))
				return "You do not have OCLF V1 PLUS installed. userinit.sh does not match signature.";
		} catch (FileNotFoundException ex) {
			return "You do not have OCLF V1 PLUS installed. userinit.sh not found.";
		}

		for (String dir : dataDirectories) {
			r = cmd.su.busyboxWaitFor("test -L /data/" + dir);
			if (!r.success()) {
				return "You do not have OCLF V1 installed. /data/" + dir + " is not a symlink";
			}
		}

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Enabling Flight Mode and killing all running apps.");
		Utils.EnableFlightMode(ApplicationContext);
		try {
			Utils.KillAllRunningApps(ApplicationContext);

			UpdateStatus("Removing old backups");
			for (String dir : dataDirectories) {
				vt.busybox("rm -rf /data/" + dir + ".bak");
			}

			vt.busybox("rm -rf /data/bak");
			vt.busybox("mkdir -p /data/bak");
			for (String dir : dataDirectories) {
				UpdateStatus("Copying " + dir + " back to RFS from EXT2");
				vt.busybox("cp -rp /data/ext2data/" + dir + " /data/bak/" + dir);
				// VTCommandResult r =
				// vt.busybox("cp -rp /data/ext2data/"+dir+" /data/bak/"+dir );
				// if (!r.success()) {
				// vt.busybox("rm -rf /data/bak");
				// return
				// "Could not copy your data back! This is probably because something changed in your data while the undo was running. Try it again, and see if the problem persists. Error: "+r.stderr;
				// }
			}

			UpdateStatus("Removing Symlinks, possible FCs at this point.");
			for (String dir : dataDirectories) {
				VTCommandResult r = vt.busybox("rm /data/" + dir);
				if (!r.success()) {
					return "Could not remove symlinks! This is bad, and something has gone very wrong.";
				}
			}

			for (String dir : dataDirectories) {
				VTCommandResult r = vt.busybox("mv /data/bak/" + dir + " /data/" + dir);
				if (!r.success()) {
					return "Could not rename your data! This is bad, and something has gone very wrong.";
				}
			}

			UpdateStatus("Removing boot support");
			if (new File("/system/bin/playlogos1").exists())
				Utils.RemoveBootSupport(vt, "playlogos1");
			else
				Utils.RemoveBootSupport(vt, "playlogo");

			UpdateStatus("Removing old datafile");
			vt.busybox("rm /data/linux.ex2");

			UpdateStatus("System will reboot in 10 seconds, to ensure everything works properly.");
			vt.FNF("sync");
			Thread.sleep(10000);
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
