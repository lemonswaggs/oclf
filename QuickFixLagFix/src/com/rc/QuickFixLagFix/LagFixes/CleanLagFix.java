package com.rc.QuickFixLagFix.LagFixes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.StatFs;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class CleanLagFix extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Repair/Clean Lagfixes";
	}

	@Override
	public String GetShortDescription() {
		return "This will attempt to repair any lagfix problems.";
	}

	@Override
	public String GetLongDescription() {
		return "This will attempt to repair the following types of problems:\n\n1. Symlinks lost to dalvik-cache or other folders.\n\n2. No lagfix installed, but a linux.ex2 file left behind from previous failed attempts. Support for V2+ linux.ex2 files as well now.";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools from the menu to use this lag fix.";

		boolean HasLagfixInstalled = false;
		StatFs statfs = new StatFs("/data/");
		long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		statfs.restat("/data/data/");
		long datadata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		if (data_total != datadata_total)
			HasLagfixInstalled = true;

		boolean HasEXT2FileType1 = new File("/data/linux.ex2").exists();
		boolean HasEXT2FileType2 = new File("/data/ex2").exists();

		if (!HasLagfixInstalled && (HasEXT2FileType1 || HasEXT2FileType2))
			return ENABLED;

		boolean HasMissingSymlinks = false;
		if (HasLagfixInstalled) {
			for (String dir : OneClickLagFixV1PLUS.dataDirectories) {
				r = cmd.su.busyboxWaitFor("test -L /data/" + dir);
				if (!r.success()) {
					HasMissingSymlinks = true;
				}
			}
		}

		if (HasMissingSymlinks)
			return ENABLED;

		return "No problems detected.";
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		boolean HasLagfixInstalled = false;
		StatFs statfs = new StatFs("/data/");
		long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		statfs.restat("/data/data/");
		long datadata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		if (data_total != datadata_total)
			HasLagfixInstalled = true;

		if (!HasLagfixInstalled) {
			boolean HasEXT2FileType1 = new File("/data/linux.ex2").exists();
			boolean HasEXT2FileType2 = new File("/data/ex2").exists();
			if (HasEXT2FileType1) {
				UpdateStatus("Removing unused /data/linux.ex2 file.");
				VTCommandResult r = vt.busybox("rm /data/linux.ex2");
				if (!r.success()) {
					return "Could not remove old /data/linux.ex2 file: " + r.stderr;
				}
			}
			if (HasEXT2FileType2) {
				UpdateStatus("Removing unused /data/ex2 folder.");
				VTCommandResult r = vt.busybox("rm -rf /data/ex2");
				if (!r.success()) {
					return "Could not remove old /data/ex2 folder: " + r.stderr;
				}
			}
		} else {
			for (String dir : OneClickLagFixV1PLUS.dataDirectories) {
				VTCommandResult r = vt.busybox("test -L /data/" + dir);
				if (!r.success()) {
					UpdateStatus(dir + " is missing its symlink");
					UpdateStatus("Removing old " + dir + " on EXT2");
					r = vt.busybox("rm -rf /data/ext2data/" + dir);
					if (!r.success()) {
						return "Could not remove " + dir + ": " + r.stderr;
					}
					UpdateStatus("Copying " + dir + " to EXT2");
					r = vt.busybox("cp -rp /data/" + dir + " /data/ext2data/" + dir);
					if (!r.success()) {
						return "Could not copy " + dir + ": " + r.stderr;
					}
					UpdateStatus("Renaming old " + dir + " folder to " + dir + ".bak");
					r = vt.busybox("mv /data/" + dir + " /data/" + dir + ".bak");
					if (!r.success()) {
						return "Could not rename " + dir + ": " + r.stderr;
					}
					UpdateStatus("Linking " + dir + " on EXT2 to /data/" + dir);
					r = vt.busybox("ln -s /data/ext2data/" + dir + " /data/" + dir);
					if (!r.success()) {
						String err = "Could not link " + dir + ": " + r.stderr;
						UpdateStatus(err);
						UpdateStatus("Renaming " + dir + ".bak folder back to " + dir);
						r = vt.busybox("mv /data/" + dir + ".bak /data/" + dir);
						return err;
					}
				}
			}
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
