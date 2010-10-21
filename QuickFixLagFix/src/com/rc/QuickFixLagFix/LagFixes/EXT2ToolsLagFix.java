package com.rc.QuickFixLagFix.LagFixes;

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
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class EXT2ToolsLagFix extends LagFix {

	final static String[] binaries = new String[] {"busybox", "e2fsck", "mke2fs", "resize2fs", "tune2fs"};
	final static int[] binaries_id = new int[] {R.raw.busybox, R.raw.e2fsck, R.raw.mke2fs, R.raw.resize2fs, R.raw.tune2fs};
	
	final static String[] libraries = new String[] {"libext2_blkid.so", "libext2_com_err.so", "libext2_e2p.so", "libext2_profile.so", "libext2_uuid.so", "libext2fs.so"};
	final static int[] libraries_id = new int[] {R.raw.libext2_blkid, R.raw.libext2_com_err, R.raw.libext2_e2p, R.raw.libext2_profile, R.raw.libext2_uuid, R.raw.libext2fs};
	
	static boolean IsInstalled() {
		try {
			for (String binary : binaries) {
				FileInputStream fis = new FileInputStream("/data/oclf/"+binary);
				fis.close();
			}
			for (String library : libraries) {
				FileInputStream fis = new FileInputStream("/system/lib/"+library);
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
		return "Install EXT2 Tools";
	}

	@Override
	public String GetShortDescription() {
		return "Copy the included EXT2 Tools to your device.";
	}

	@Override
	public String GetLongDescription() {
		return "This will copy the included EXT2 Tools to your device, for use by some of the lag fixes. You will only need to run this if the lag fix you are using requires these tools.";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {		
//		ShellCommand cmd = new ShellCommand();
//		CommandResult r = cmd.su.busyboxWaitFor("");
//		if (!r.success() || !r.stdout.contains("BusyBox v1.17.1"))
//			return "Packaged BusyBox v1.17.1 is required for this fix. Install it from the menu.";
		
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";
		
		if (IsInstalled())
			return "EXT2Tools are already installed.";
		
		StatFs statfs = new StatFs("/data/");
		long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		if (bytecountfree < 1024*1024)
			return "You need to have "+Utils.FormatByte(1024*1024*5)+" available in /data to install this!";
		
		statfs = new StatFs("/system/lib/");
		bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		if (bytecountfree < 1024*1024)
			return "You need to have "+Utils.FormatByte(1024*1024)+" available in /system/lib to install this!";
		
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		VTCommandResult cr = vt.runCommand("mount");
		if (cr.stdout.contains("/dev/block/stl9 /system rfs ro")) {
			UpdateStatus("Your /system appears to be read only, remounts as read-write");
			cr = vt.runCommand("mount -o remount,rw /dev/block/stl9 /system");
			if (!cr.success()) {
				return "Could not mount /system as read-write: "+cr.stderr;
			}
		}		
		UpdateStatus("Creating folder /data/oclf");
		vt.runCommand("mkdir /data/oclf");
		if (!cr.success())
			if (!cr.stderr.contains("File exists"))
				return "Could not create folder /data/oclf: "+cr.stderr;

		UpdateStatus("Saving binaries to disk...");
		
		for (int i=0;i<binaries.length;i++) {
			String binary = binaries[i];
			int binary_id = binaries_id[i];
			VTCommandResult r = Utils.CopyIncludedFiletoPath(binary_id, binary, "/data/oclf/"+binary, ApplicationContext, vt);
			if (!r.success())
				return "Could not copy binary "+binary+": "+r.stderr;
			r = vt.runCommand("chmod 755 /data/oclf/"+binary);
			if (!r.success())
				return "Could not set permissions for "+binary+": "+r.stderr;
		}
		
		vt.busybox("mount -o remount,rw /system");
		
		UpdateStatus("Saving libraries to disk...");

		for (int i=0;i<libraries.length;i++) {
			String library = libraries[i];
			int library_id = libraries_id[i];
			VTCommandResult r = Utils.CopyIncludedFiletoPath(library_id, library, "/system/lib/"+library, ApplicationContext, vt);
			if (!r.success())
				return "Could not copy library "+library+": "+r.stderr;
			r = vt.runCommand("chmod 644 /system/lib/"+library);
			if (!r.success())
				return "Could not set permissions for "+library+": "+r.stderr;
		}
		
		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		return lagFixOptions;
	}
	
	@Override
	public boolean CanForce() {
		return true;
	}

}
