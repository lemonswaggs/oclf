package com.rc.QuickFixLagFix.LagFixes;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixChoiceOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class InstallFontLagFix extends LagFix {

	final static String[] font_files = new String[] {"DroidSans.ttf", "DroidSans-Bold.ttf"};

	static boolean HasBackup() {
		try {
			for (String font : font_files) {
				FileInputStream fis = new FileInputStream("/system/fonts/"+font+".bak");
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
		return "Install Font";
	}

	@Override
	public String GetShortDescription() {
		return "Utility to install fonts.";
	}

	@Override
	public String GetLongDescription() {
		return "On JF firmwares, after installing the lagfix you will be unable to use custom fonts. This will allow you to install custom fonts in this case.\n\nEXPERIMENTAL\nThis fix has not been well tested yet.";
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
		
		if (!EXT2ToolsLagFix.IsInstalled())
			return "You must install EXT2Tools from the menu to use this lag fix.";
		
		File fontdir = new File("/data/data/com.android.settings/app_fonts");
		if (!fontdir.exists()) 
			return "You do not have any available fonts to install. Try select one from the android system menu first.";
		
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		String FontDir = options.get("FontDir");
		if (FontDir.equals(LagFixChoiceOption.NO_SELECTION))
			return "You must select a font to install.";
		FontDir = FontDir.trim();
		if (FontDir.equals("Default")) {
			UpdateStatus("Restoring default font");
			for (String font : font_files) {
				VTCommandResult r = vt.busybox("mv /system/fonts/"+font+".bak /system/fonts/"+font);
				if (!r.success())
					return "Error running command: mv /system/fonts/"+font+".bak /system/fonts/"+font+": "+r.stderr;
			}
			UpdateStatus("System will reboot in 10 seconds to enable new font...");
			vt.FNF("sync");
			Thread.sleep(10000);
			vt.FNF("reboot");
			return SUCCESS;
		}
		
		if (!HasBackup()) {
			UpdateStatus("No backup detected, backing up current font files");
			for (String font : font_files) {
				VTCommandResult r = vt.busybox("mv /system/fonts/"+font+" /system/fonts/"+font+".bak");
				if (!r.success()) {
					String err = r.stderr;
					for (String font2 : font_files) {
						vt.busybox("mv /system/fonts/"+font2+".bak /system/fonts/"+font2);
						return "Error while backing up old font: "+err;
					}
				}
			}
		}
		
		UpdateStatus("Installing font "+FontDir);
		for (String font : font_files) {
			VTCommandResult r = vt.busybox("cp /data/data/com.android.settings/app_fonts/"+FontDir+"/"+font+" /system/fonts/"+font);
			if (!r.success()) {
				String err = r.stderr;
				for (String font2 : font_files) {
					vt.busybox("mv /system/fonts/"+font2+".bak /system/fonts/"+font2);
					return "Error while copying over new font, font backup restored: "+err;
				}
			}
		}
	
		UpdateStatus("System will reboot in 10 seconds to enable new font...");
		vt.FNF("sync");
		Thread.sleep(10000);
		vt.FNF("reboot");
		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		List<String> Options = new ArrayList<String>();
		if (HasBackup())
			Options.add("Default");
		
		File fontdir = new File("/data/data/com.android.settings/app_fonts");
		File[] fontfiledirs =  fontdir.listFiles();
		for (File dir : fontfiledirs) {
			if (dir.isDirectory()) {
				Options.add(dir.getName());
			}
		}
		
		String[] finaloptions = Options.toArray(new String[0]);
		
		lagFixOptions.add(new LagFixChoiceOption("FontDir", "Font", "Select the font that you wish to install:", finaloptions));
		
		return lagFixOptions;
	}
	
	@Override
	public boolean CanForce() {
		return false;
	}

}
