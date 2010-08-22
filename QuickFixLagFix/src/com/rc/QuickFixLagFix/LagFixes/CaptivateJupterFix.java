package com.rc.QuickFixLagFix.LagFixes;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;

import android.content.Context;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.MD5Hashes;
import com.rc.QuickFixLagFix.lib.OptionListener;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class CaptivateJupterFix extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Captivate Jupiter Fix";
	}

	@Override
	public String GetShortDescription() {
		return "A fix for jupter.xml for some Captivates";
	}

	@Override
	public String GetLongDescription() {
		return "On certain firmware for the Captivate, the jupter.xml file has logging enabled. This will disable that logging.";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		r = cmd.su.busyboxWaitFor("");
		if (!r.success() || !r.stdout.contains("BusyBox v1.17.1"))
			return "Packaged BusyBox v1.17.1 is required for this fix. Install it from the menu.";

		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools to use this lag fix.";
		
		try {
			if (!Utils.GetMD5Hash("/system/etc/jupiter.xml").equalsIgnoreCase(MD5Hashes.bad_jupiter))
				return "You do not have a bad version of jupiter.xml. (jupiter.xml does not match hash)";
		} catch (FileNotFoundException ex) {
			return "You do not have a /system/etc/jupiter.xml file!";
		}

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		UpdateStatus("Copying over included jupiter.xml with fix to /system/etc/jupiter.xml");
		Utils.CopyIncludedFiletoPath(R.raw.jupiter, "jupiter.xml", "/system/etc/jupiter.xml", ApplicationContext, vt);

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
