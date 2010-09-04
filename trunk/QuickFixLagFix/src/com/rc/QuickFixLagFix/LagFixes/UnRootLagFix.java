package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Build;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class UnRootLagFix extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Un-Root Device 2.1";
	}

	@Override
	public String GetShortDescription() {
		return "This will un-root your device.";
	}

	@Override
	public String GetLongDescription() {
		return "Run this to un-root your device. This only works for android 2.1.";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Your device is not rooted.";
		
		final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		if (sdkVersion != 7)
			return "This only works on Android 2.1";
		
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Removing root...");
		vt.runCommand("rm /system/bin/su");
		vt.runCommand("rm /system/xbin/su");
		vt.runCommand("rm /system/xbin/busybox");
		vt.runCommand("rm /system/app/Superuser.apk");
		vt.FNF("sync");
		vt.FNF("reboot");
		
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
	
	@Override
	public boolean CanForce() {
		return true;
	}

}
