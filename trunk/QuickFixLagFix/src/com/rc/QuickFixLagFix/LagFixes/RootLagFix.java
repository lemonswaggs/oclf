package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.os.Build;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.OptionListener;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class RootLagFix extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Root Device 2.1";
	}

	@Override
	public String GetShortDescription() {
		return "This will root your device.";
	}

	@Override
	public String GetLongDescription() {
		return "Run this to root your device. This only works for android 2.1. It includes busybox (version 1.17.1) and Superuser.apk (version 2.3.1)";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (r.success())
			return "Your device is already rooted.";
		
		final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		if (sdkVersion != 7)
			return "This only works on Android 2.1";
		
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		CommandResult r = Utils.CopyIncludedFiletoPath(R.raw.root, "root.zip", "/sdcard/update.zip", ApplicationContext);
		if (!r.success()) {
			return "Error copying over root file: "+r.stderr;
		}
		
		UpdateStatus("You will now need to reboot your phone, and use the correct key sequence to enter the recover console. Then using the volume control, select 'apply sdcard:update.zip' in the recovery console.");
		
		return SUCCESS;
	}

	@Override
	public void GetOptions(OptionListener listener) {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		listener.LagFixOptionListCompleted(lagFixOptions);
	}

}
