package com.rc.QuickFixLagFix.LagFixes;

import java.io.FileNotFoundException;
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

public class UndoOneClickLagFixV1 extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Undo OneClickLagFix V1";
	}

	@Override
	public String GetShortDescription() {
		return "Will remove OneClickLagFix V1";
	}

	@Override
	public String GetLongDescription() {
		return "This will remove OneClickLagFix V1. It can also remove the stand alone version.";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		r = cmd.su.busyboxWaitFor("");
		if (!r.success() || !r.stdout.contains("BusyBox v1.17.1"))
			return "BusyBox v1.17.1 is required for this fix.";

		StatFs statfs = new StatFs("/data/");
		long bytecountfree_rfs = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		statfs = new StatFs("/data/data/");
		long bytecountfree_ext2 = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		long bytecountused_ext2 = (long) statfs.getBlockCount() * (long) statfs.getBlockSize() - bytecountfree_ext2;

		if (bytecountused_ext2 > bytecountfree_rfs)
			return "You have only "+Utils.FormatByte(bytecountfree_rfs)+" free on RFS, but "+Utils.FormatByte(bytecountused_ext2)+" used on EXT2. You must free up some space.";

		if (Utils.GetBatteryLevel(ApplicationContext) < 40) {
			return "You need at least 40% battery charge to use this fix.";
		}
		
		try {
			if (Utils.GetMD5Hash("/system/bin/playlogos1").equalsIgnoreCase(MD5Hashes.playlogos1))
				return "You do not have a lagfix installed. playlogos1 matches original signature.";
		} catch (FileNotFoundException ex) {
			return "You do not have a /system/bin/playlogos1 file!";
		}
		
		try {
			String Hash = Utils.GetMD5Hash("/system/bin/userinit.sh");
			if (!Hash.equalsIgnoreCase(MD5Hashes.oclfv1) && !Hash.equalsIgnoreCase(MD5Hashes.oclfv1_external)) 
				return "You do not have OCLF V1 installed. userinit.sh does not match signature.";
		} catch (FileNotFoundException ex) {
			return "You do not have OCLF V1 installed. userinit.sh not found.";
		}
		
		String[] dataDirectories = new String[]{"data", "system", "dalvik-cache"};
		for (String dir : dataDirectories) {
			r = cmd.su.busyboxWaitFor("test -L /data/" + dir);
			if (!r.success()) {
				return "You do not have OCLF V1 installed. /data/"+dir+" is not a symlink";
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
			
			String[] dataDirectories = new String[]{"data", "system", "dalvik-cache"};
			UpdateStatus("Removing Symlinks");
			for (String dir : dataDirectories) {
				VTCommandResult r = vt.busybox("rm /data/" + dir);
				if (!r.success()) {
					return "Could not remove symlinks! This is bad, and something has gone very wrong.";
				}				
			}
			
			UpdateStatus("Removing old backups");
			for (String dir : dataDirectories) {
				vt.busybox("rm -rf /data/" + dir + ".bak");
			}
			
			for (String dir : dataDirectories) {
				UpdateStatus("Copying "+dir+" back to RFS from EXT2");
				VTCommandResult r = vt.busybox("cp -rp /data/ext2data/"+dir+" /data/"+dir );
				if (!r.success()) {
					return "Could not copy your data back! This is bad, and something has gone very wrong.";
				}	
			}
			
			UpdateStatus("Removing datafile");
			vt.busybox("rm /data/linux.ex2");
			
			UpdateStatus("Removing boot support");
			Utils.RemoveBootSupport(vt);

			UpdateStatus("System will reboot in 10 seconds, to ensure everything works properly.");
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
