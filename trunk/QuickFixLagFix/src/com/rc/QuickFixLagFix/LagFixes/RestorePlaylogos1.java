package com.rc.QuickFixLagFix.LagFixes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Build;
import android.os.StatFs;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
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

		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools from the menu to use this lag fix.";

		// try {
		// if
		// (Utils.GetMD5Hash("/system/bin/playlogos1").equalsIgnoreCase(MD5Hashes.playlogos1))
		// return
		// "You already have the correct playlogos1. playlogos1 matches original signature.";
		// } catch (FileNotFoundException ex) {
		// }

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {

		boolean LagfixInstalled = false;
		StatFs statfs = new StatFs("/data/");
		long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		statfs.restat("/data/data/");
		long datadata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		if (data_total != datadata_total)
			LagfixInstalled = true;

		File ext2data = new File("/dbdata/ext2data");
		if (ext2data.exists() && ext2data.isDirectory()) {
			statfs.restat("/dbdata/ext2data/");
			long ext2data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			if (ext2data_total == data_total)
				LagfixInstalled = true;
		}

		final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		if (sdkVersion != 7 && sdkVersion != 8)
			return "This only works on Eclair or Froyo";

		if (!LagfixInstalled) {
			UpdateStatus("Removing any old playlogos1 file");
			vt.busybox("rm /system/bin/playlogos1");
			if (sdkVersion == 7) {
				UpdateStatus("Copying over included eclair playlogos1 file");
				Utils.CopyIncludedFiletoPath(R.raw.playlogosnow, "playlogosnow", "/system/bin/playlogos1", ApplicationContext, vt);
			} else {
				UpdateStatus("Copying over included froyo playlogos1 file");
				Utils.CopyIncludedFiletoPath(R.raw.playlogosnowfroyo, "playlogosnow", "/system/bin/playlogos1", ApplicationContext, vt);
			}
			UpdateStatus("Setting permissions...");
			vt.busybox("chmod 755 /system/bin/playlogos1");
		} else {
			UpdateStatus("You already appear to have a lagfix installed. Updating playlogosnow instead of playlogos1");
			vt.busybox("rm /system/bin/playlogosnow");
			if (sdkVersion == 7) {
				UpdateStatus("Copying over included eclair playlogos1 file");
				Utils.CopyIncludedFiletoPath(R.raw.playlogosnow, "playlogosnow", "/system/bin/playlogosnow", ApplicationContext, vt);
			} else {
				UpdateStatus("Copying over included froyo playlogos1 file");
				Utils.CopyIncludedFiletoPath(R.raw.playlogosnowfroyo, "playlogosnow", "/system/bin/playlogosnow", ApplicationContext, vt);
			}
			UpdateStatus("Setting permissions...");
			vt.busybox("chmod 755 /system/bin/playlogosnow");
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
