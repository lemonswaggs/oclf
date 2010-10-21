package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixSeekOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixSeekOption.SeekOptionUpdate;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class WifiTimeoutLagFix extends LagFix {
	
	final static long minms = 3000;
	final static long midms = 1000*60;
	final static long maxms = 1000*60*60;
	
	@Override
	public String GetDisplayName() {
		return "Wifi Timeout";
	}

	@Override
	public String GetShortDescription() {
		return "Changes Wifi timeout on screen off";
	}

	@Override
	public String GetLongDescription() {
		return "This option will change how long it takes wifi to turn off when you turn off your screen. \n\nIn Options->Wifi->Menu button->Advanced, make sure that sleep policy is set to 'When screen turns off'. \n\nDefault setting appears to be 15 minutes, but depends on your firmware.";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";
		
		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools from the menu to use this lag fix.";

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		long curval;
		long progress = Long.parseLong(options.get("timer"));
		if (progress > 5000) {
			progress -= 5000;
			curval = midms + progress * (maxms - midms) / 5000;
		} else {
			curval = minms + progress * (midms - minms) / 5000;
		}
		
		vt.busybox("rm /sdcard/settings.db");
		UpdateStatus("Making a copy of settings.db...");
		VTCommandResult r = vt.busybox("cp /dbdata/databases/com.android.providers.settings/settings.db /sdcard/settings.db");
		if (!r.success()) {
			return "Could not run 'cp /dbdata/databases/com.android.providers.settings/settings.db /sdcard/settings.db' : "+r.stderr;
		}
		SQLiteDatabase settings = SQLiteDatabase.openDatabase("/sdcard/settings.db", null, 0);
		ContentValues values = new ContentValues();
		values.put("name", "wifi_idle_ms");
		values.put("value", curval);
		UpdateStatus("Updating 'secure' table with wifi_idle_ms = "+curval);
		if (settings.replace("secure", null, values) == -1)
			UpdateStatus("Error inserting value into 'secure' table in settings.db");
		UpdateStatus("Updating 'gservices' table with wifi_idle_ms = "+curval);
		if (settings.replace("gservices", null, values) == -1)
			UpdateStatus("Error inserting value into 'gservices' table in settings.db");
		settings.close();
		
		UpdateStatus("Copying settings.db bak and changing ownership...");
		r = vt.busybox("cp /sdcard/settings.db /dbdata/databases/com.android.providers.settings/settings.db.bak");
		if (!r.success()) {
			return "Could not run 'cp /sdcard/settings.db /dbdata/databases/com.android.providers.settings/settings.db.bak' : "+r.stderr;
		}
		r = vt.runCommand("chown system.system /dbdata/databases/com.android.providers.settings/settings.db.bak");
		if (!r.success()) {
			UpdateStatus("Could not run 'chown system.system /dbdata/databases/com.android.providers.settings/settings.db.bak' : "+r.stderr);
			UpdateStatus("Trying 1000.1000 instead...");
			r = vt.runCommand("chown 1000.1000 /dbdata/databases/com.android.providers.settings/settings.db.bak");
			if (!r.success()) {
				return "Could not run 'chown 1000.1000 /dbdata/databases/com.android.providers.settings/settings.db.bak' : "+r.stderr;
			}
		}		
		UpdateStatus("Replacing existing settings db with new one...");
		r = vt.runCommand("mv /dbdata/databases/com.android.providers.settings/settings.db.bak /dbdata/databases/com.android.providers.settings/settings.db");
		if (!r.success()) {
			return "Could not run 'mv /dbdata/databases/com.android.providers.settings/settings.db.bak /dbdata/databases/com.android.providers.settings/settings.db' : "+r.stderr;
		}
		
		UpdateStatus("Rebooting to apply settings...");
		vt.runCommand("sync");
		vt.FNF("reboot");

		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		LagFixSeekOption seekoption = new LagFixSeekOption("timer", "Wifi off time", "", 1000);
		seekoption.setUpdatehandler(new SeekOptionUpdate(seekoption) {
			
			@Override
			public String DisplayValue(int progress) {
				long curval;
				if (progress > 5000) {
					progress -= 5000;
					curval = midms + progress * (maxms - midms) / 5000;
				} else {
					curval = minms + progress * (midms - minms) / 5000;
				}
				
				return FormatTime(curval);
			}
		});
		lagFixOptions.add(seekoption);
		
		return lagFixOptions;
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}
	
	String FormatTime(long ms) {
		if (ms < 1000)
			return ""+ms+" ms";
		ms /= 1000;
		if (ms < 60)
			return ""+ms+" sec";
		ms /= 60;
		return ""+ms+" mins";
	}

}
