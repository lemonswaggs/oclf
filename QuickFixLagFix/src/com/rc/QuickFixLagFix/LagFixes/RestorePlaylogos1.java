package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.os.StatFs;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.OptionListener;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class RestorePlaylogos1 extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Restore Boot Animation";
	}

	@Override
	public String GetShortDescription() {
		return "This will restore your playlogos1 file.";
	}

	@Override
	public String GetLongDescription() {
		return "This includes a copy of a stock boot animation file (/system/bin/playlogos1) taken from an I9000 device. This is the flashing S boot logo.";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		r = cmd.su.busyboxWaitFor("");
		if (!r.success() || !r.stdout.contains("BusyBox v1.17.1"))
			return "Included BusyBox v1.17.1 is required for this fix.";

		StatFs statfs = new StatFs("/data/");
		long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		statfs.restat("/data/data/");
		long datadata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		if (data_total != datadata_total)
			return "You appear to have a lagfix installed! Replacing the bootlogo in this case could cause your device to become un-bootable. (free space on /data matches /data/data)";

//		try {
//			if (Utils.GetMD5Hash("/system/bin/playlogos1").equalsIgnoreCase(MD5Hashes.playlogos1))
//				return "You already have the correct playlogos1. playlogos1 matches original signature.";
//		} catch (FileNotFoundException ex) {
//		}

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Removing any old playlogos1 file");
		vt.busybox("rm /system/bin/playlogos1");
		UpdateStatus("Copying over included playlogos1 file");
		Utils.CopyIncludedFiletoPath(R.raw.playlogosnow, "playlogosnow", "/system/bin/playlogos1", ApplicationContext, vt);
		UpdateStatus("Setting permissions...");
		vt.busybox("chmod 755 /system/bin/playlogos1");
		return SUCCESS;
	}

	@Override
	public void GetOptions(OptionListener listener) {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		listener.LagFixOptionListCompleted(lagFixOptions);
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

}
