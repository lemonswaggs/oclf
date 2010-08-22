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

public class BusyboxLagFix extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Install Busybox";
	}

	@Override
	public String GetShortDescription() {
		return "Copy the included busybox to your device.";
	}

	@Override
	public String GetLongDescription() {
		return "This will copy the included busybox to your device for use by this app. It will not replace the busybox you already have!";
		//return "This will install busybox v1.17.1 on your device. You can use your own busybox, but the lagfixes are known to work with the included busybox version.";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {		
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.busyboxWaitFor("");
		if (r.success() && r.stdout.contains("BusyBox v1.17.1"))
			return "You already have the included BusyBox v1.17.1 installed.";
		
		r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";
		
		StatFs statfs = new StatFs("/data/");
		long bytecountfree = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		if (bytecountfree < 1024*1024)
			return "You need to have "+Utils.FormatByte(1024*1024)+" available in /data to install this!";
		
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		cmd.su.runWaitFor("mkdir /data/oclf");

		UpdateStatus("Saving busybox binary to disk...");
		
		Utils.SaveIncludedFileIntoFilesFolder(R.raw.busybox, "busybox", ApplicationContext);

		String FromPath = ApplicationContext.getFilesDir()+"/busybox";
		CommandResult r = cmd.su.runWaitFor("dd if="+FromPath+" of=/data/oclf/busybox");
		if (!r.success()) {
			return "Could not copy busybox to /data/oclf! - "+r.stderr;
		}
		
		UpdateStatus("Setting up permissions...");
		
		r = cmd.su.runWaitFor("chmod 755 /data/oclf/busybox");
		if (!r.success()) {
			cmd.su.runWaitFor("rm /data/oclf/busybox");
			return "Could not set up permissions! - "+r.stderr;
		}
		
		return SUCCESS;
	}

	@Override
	public void GetOptions(OptionListener listener) {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		listener.LagFixOptionListCompleted(lagFixOptions);
	}

}
