package com.rc.QuickFixLagFix.LagFixes;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Build;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.Activities.KernelListActivity;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class InstallKernelLagFix extends LagFix {

	JSONObject json;
	
	public InstallKernelLagFix(JSONObject json) {
		this.json = json;
	}
	
	@Override
	public String GetDisplayName() {
		return "InstallKernelLagFix";
	}

	@Override
	public String GetShortDescription() {
		return "InstallKernelLagFix";
	}

	@Override
	public String GetLongDescription() {
		return "InstallKernelLagFix";
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
		UpdateStatus("Setting up permissions...");
		VTCommandResult r = vt.runCommand("chmod 755 " + busybox);
		if (!r.success())
			return "Could not set permissions for busybox: " + r.stderr;
		r = vt.runCommand("chmod 755 " + redbend_ua);
		if (!r.success())
			return "Could not set permissions for redbend_ua: " + r.stderr;
		vt.busybox = busybox;
		
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

		r = vt.busybox("rm /sdcard/zImage");
		UpdateStatus("Downloading the required kernel file. This will take some time.");
		JSONArray DownloadURLS = json.getJSONArray(KernelListActivity.DOWNLOADURLS);
		int retrycount = 0;
		boolean download_success = false;
		while (retrycount < 2 && !download_success) {
			for (int i=0;i<DownloadURLS.length();i++) {
				String downloadurl = DownloadURLS.getString(i);
				UpdateStatus("Downloading kernel from mirror: "+downloadurl);
				try {
					Utils.SaveWebFileToPath(downloadurl, "/sdcard/zImage", ApplicationContext, this);
					String MD5 = Utils.GetMD5Hash("/sdcard/zImage");
					if (MD5.equalsIgnoreCase(json.optString(KernelListActivity.MD5))) {
						download_success = true;
						break;
					} else {
						UpdateStatus("MD5 Check Failed: "+MD5+" does not match "+json.optString(KernelListActivity.MD5));
					}					
				} catch (Exception ex) {
					UpdateStatus("Could not download from "+downloadurl+": "+ex.getLocalizedMessage());
				}
			}			
			retrycount++;
		}
		if (!download_success)
			return "Could not retrieve the kernel! Please try again later.";		
		

		UpdateStatus("Backing up existing kernel to /sdcard/zImage.backup - This could take some time.");
		r = vt.runCommand("rm /sdcard/zImage_backup");
		r = vt.runCommand("rm /sdcard/zImage_backup_manifest");
		r = vt.runCommand("offset=`"+ busybox + " dd if=/dev/block/bml7 bs=1 skip=44 count=4 2>/dev/null| "+busybox + " hexdump -e '1/4 \"%d\"' -e '\"\\n\"'`");
		r = vt.runCommand(busybox + " dd if=/dev/block/bml7 bs=1 count=$offset of=/sdcard/zImage_backup");
		if (!r.success())
			return "Error backing up existing kernel: " + r.stderr;
		
		UpdateStatus("Verifying backed up kernel...");
		r = vt.runCommand(busybox + " test `"+busybox+" hexdump -s 0x24 -n 4 -C /sdcard/zImage_backup | "+busybox+" grep \"18 28 6f 01\" | "+busybox+" wc -l` -eq 1");
		if (!r.success())
			return "Verification failed - zImage magic not found: 18 28 6f 01";
		
		UpdateStatus("Writing backed up kernel manifest to /sdcard/zImage_backup_manifest");
		FileOutputStream fos = new FileOutputStream("/sdcard/zImage_backup_manifest");
		JSONObject manifest = new JSONObject();
		manifest.put(KernelListActivity.ANDROID_VERSION, Build.VERSION.SDK.trim());
		manifest.put(KernelListActivity.DEVICE, Build.MODEL);
		manifest.put(KernelListActivity.MD5, Utils.GetMD5Hash("/sdcard/zImage_backup"));
		fos.write(manifest.toString().getBytes());
		fos.close();
		
		UpdateStatus("Verifying downloaded kernel...");
		r = vt.runCommand(busybox+" test `"+busybox+" hexdump -s 0x24 -n 4 -C /sdcard/zImage | "+busybox+" grep \"18 28 6f 01\" | "+busybox+" wc -l` -eq 1");
		if (!r.success())
			return "Verification failed - zImage magic not found: 18 28 6f 01";
//		r = vt.runCommand("/data/oclf/busybox test `/data/oclf/busybox hexdump -s 0x28 -n 4 -C /sdcard/zImage_backup | /data/oclf/busybox grep \"00 00 00 00\" | /data/oclf/busybox wc -l` -eq 1");
//		if (!r.success())
//			return "Verification failed - zImage does not start at 00 00 00 00";
//		r = vt.runCommand("/data/oclf/busybox test `/data/oclf/busybox hexdump -s 0x2c -n 4 -e '1/4 \"%d\"' -e '\"\\n\"' /sdcard/zImage_backup` -gt 5000000");
//		if (!r.success())
//			return "Verification failed - zImage is not the correct size";
		
		UpdateStatus("Now flashing the downloaded kernel...");
		r = vt.runCommand(redbend_ua + " restore /sdcard/zImage /dev/block/bml7");
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
