package com.rc.QuickFixLagFix.LagFixes;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.StatFs;
import android.util.Log;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class RemoveEXT2ToolsLagFix extends LagFix {

	final static String[] binaries = new String[]{"busybox", "e2fsck", "mke2fs", "resize2fs", "tune2fs"};
	final static int[] binaries_id = new int[]{R.raw.busybox, R.raw.e2fsck, R.raw.mke2fs, R.raw.resize2fs, R.raw.tune2fs};

	final static String[] libraries = new String[]{"libext2_blkid.so", "libext2_com_err.so", "libext2_e2p.so", "libext2_profile.so", "libext2_uuid.so", "libext2fs.so"};
	final static int[] libraries_id = new int[]{R.raw.libext2_blkid, R.raw.libext2_com_err, R.raw.libext2_e2p, R.raw.libext2_profile, R.raw.libext2_uuid, R.raw.libext2fs};

	static boolean IsInstalled() {
		try {
			for (String binary : binaries) {
				FileInputStream fis = new FileInputStream("/data/oclf/" + binary);
				fis.close();
			}
			for (String library : libraries) {
				FileInputStream fis = new FileInputStream("/system/lib/" + library);
				fis.close();
			}
			return true;
		} catch (Exception ex) {
			Log.i("OCLF", ex.getLocalizedMessage());
			return false;
		}
	}

	@Override
	public String GetDisplayName() {
		return "Remove EXT2 Tools";
	}

	@Override
	public String GetShortDescription() {
		return "Remove the included EXT2 Tools from your device.";
	}

	@Override
	public String GetLongDescription() {
		return "This will remove the included EXT2 Tools from device. The tools and the folder they are in will be removed.";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		if (!IsInstalled())
			return "EXT2Tools is not installed.";

		StatFs statfs = new StatFs("/data/");
		long data_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		statfs.restat("/data/data/");
		long datadata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		if (data_total != datadata_total)
			return "You appear to have a lag fix installed! Removing EXT2 tools could cause your device to no longer boot. (free space on /data does not match /data/data)";
		File ext2data = new File("/dbdata/ext2data");
		if (ext2data.exists() && ext2data.isDirectory()) {
			statfs.restat("/dbdata/ext2data/");
			long dbdata_total = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
			if (data_total == dbdata_total)
				return "You appear to have a lag fix installed! Removing EXT2 tools could cause your device to no longer boot. (free space on /dbdata/ext2data matches /data)";
		}

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Removing libraries...");
		for (int i = 0; i < libraries.length; i++) {
			String library = libraries[i];

			VTCommandResult r = vt.busybox("rm /system/lib/" + library);
			if (!r.success())
				return "Could not remove library " + library + ": " + r.stderr;
		}

		UpdateStatus("Removing folder /data/oclf and contents");
		VTCommandResult cr = vt.busybox("rm -rf /data/oclf");
		if (!cr.success())
			return "Could not remove folder /data/oclf: " + cr.stderr;

		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		return lagFixOptions;
	}

}
