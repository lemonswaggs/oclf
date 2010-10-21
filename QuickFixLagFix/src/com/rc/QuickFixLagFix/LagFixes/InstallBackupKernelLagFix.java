package com.rc.QuickFixLagFix.LagFixes;

import java.io.File;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class InstallBackupKernelLagFix extends LagFix {

	public InstallBackupKernelLagFix() {
	}

	@Override
	public String GetDisplayName() {
		return "InstallBackupKernelLagFix";
	}

	@Override
	public String GetShortDescription() {
		return "InstallBackupKernelLagFix";
	}

	@Override
	public String GetLongDescription() {
		return "InstallBackupKernelLagFix";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Copying required binaries to device...");
		Utils.SaveIncludedFileIntoFilesFolder(R.raw.busybox, "busybox", ApplicationContext);
		Utils.SaveIncludedFileIntoFilesFolder(R.raw.redbendua, "redbend_ua", ApplicationContext);
		String busybox = ApplicationContext.getFilesDir() + "/busybox";
		String redbend_ua = ApplicationContext.getFilesDir() + "/redbend_ua";
		vt.busybox = busybox;
		UpdateStatus("Setting up permissions...");
		VTCommandResult r = vt.runCommand("chmod 755 " + busybox);
		if (!r.success())
			return "Could not set permissions for busybox: " + r.stderr;
		r = vt.runCommand("chmod 755 " + redbend_ua);
		if (!r.success())
			return "Could not set permissions for redbend_ua: " + r.stderr;

		UpdateStatus("Checking for an EXT4 partition...");
		r = vt.runCommand("mount");
		if (r.stdout.contains(" ext4 ")) {
			UpdateStatus("EXT 4 partition detected (voodoo kernel)");
			UpdateStatus("Checking for /sdcard/Voodoo/disable-lagfix");
			File disablelagfix = new File("/sdcard/Voodoo/disable-lagfix");
			if (disablelagfix.exists()) {
				return "/sdcard/Voodoo/disable-lagfix already exists!";
			}
			UpdateStatus("Creating /sdcard/Voodoo/disable-lagfix");
			r = vt.runCommand("echo \"disable\" > /sdcard/Voodoo/disable-lagfix");
			if (!r.success()) {
				return "Error creating disable-lagfix file: " + r.stderr;
			}
			UpdateStatus("Rebooting in 10 seconds so that the kernel can remove the EXT 4 partition.");
			UpdateStatus("Re-run this tool after the reboot to continue installing your kernel.");
			Thread.sleep(10000);
			vt.FNF("sync");
			vt.FNF("reboot");
			return SUCCESS;
		}
		UpdateStatus("Removing /sdcard/Voodoo if it exists...");
		r = vt.busybox("rm -rf /sdcard/Voodoo");

		UpdateStatus("No EXT4 partition detected, carrying on with kernel restore");
		UpdateStatus("Verifying backed up kernel...");
		r = vt.runCommand(busybox+" test `"+busybox+" hexdump -s 0x24 -n 4 -C /sdcard/zImage_backup | "+busybox+" grep \"18 28 6f 01\" | "+busybox+" wc -l` -eq 1");
		if (!r.success())
			return "Verification failed - zImage magic not found: 18 28 6f 01";

		UpdateStatus("Now flashing the backed up kernel...");
		r = vt.runCommand(redbend_ua + " restore /sdcard/zImage_backup /dev/block/bml7");
		if (!r.success())
			return "Error flashing kernel: " + r.stderr;

		return SUCCESS;
	}

	@Override
	public boolean CanForce() {
		return false;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		return null;
	}

}
