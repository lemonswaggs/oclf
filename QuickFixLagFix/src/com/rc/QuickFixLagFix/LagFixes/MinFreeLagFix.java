package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

import com.rc.QuickFixLagFix.Activities.QuickFixLagFix;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixCheckOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixChoiceOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixTextOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class MinFreeLagFix extends LagFix {
	
	final static public String[] Presets = new String[]{"Moderate", "Optimum", "Strict", "Aggressive"};
	final static public String[] PresetValues = new String[]{
		"1536,2048,4096,7680,8960,10240",
		"1536,2048,4096,10240,12800,15360",
		"1536,2048,4096,15360,17920,20480",
		"1536,2048,4096,21000,23000,25000",
	};
	
	@Override
	public String GetDisplayName() {
		return "Alter minfree";
	}

	@Override
	public String GetShortDescription() {
		return "Changes Android's taskkiller settings";
	}

	@Override
	public String GetLongDescription() {
		return "This option allows you to change Android's default task-killers settings. The defaults work quite poorly on the SGS, and so this will allow you to change them. This does the equivalent of other apps on the market such as 'Autokiller'.\n\nI personally use 'strict' setting, and find it to work well.\n\nPresets are the same as Autokiller:\nModerate: 30,35,40\nOptimum: 40,50,60\nStrict: 60,70,80\nAggressive:82,90,98\n(Hidden app, Content provider, Empty app)\n\n\n";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success())
			return "Root is required to run this fix.";

		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		boolean autoset = LagFixCheckOption.CHECKED.equals(options.get("autoset"));
		String preset = options.get("preset");
		
		if (preset.equals(LagFixChoiceOption.NO_SELECTION))
			return "No preset selected.";
		
		SharedPreferences settings = ApplicationContext.getSharedPreferences(QuickFixLagFix.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();		
		if (!autoset) {
			editor.remove("AutokillerPreset");
		} else {
			editor.putString("AutokillerPreset", preset);
		}
		editor.commit();
		
		for (int i=0;i<Presets.length;i++) {
			String presetname = Presets[i];
			if (presetname.equals(preset)) {
				UpdateStatus("Running command: "+"echo \""+PresetValues[i]+"\" > /sys/module/lowmemorykiller/parameters/minfree");
				VTCommandResult r = vt.runCommand("echo \""+PresetValues[i]+"\" > /sys/module/lowmemorykiller/parameters/minfree");
				if (!r.success()) {
					return "Error! "+r.stderr;
				}
			}
		}

		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		VirtualTerminal vt = new VirtualTerminal();
		VTCommandResult r = vt.runCommand("cat /sys/module/lowmemorykiller/parameters/minfree");
		if (r.success()) {
			String[] output = r.stdout.split("\n");
			String preset = "This setting does not match one of the presets.";
			for (int i=0;i<Presets.length;i++) {
				if (output[0].equals(PresetValues[i])) {
					preset = "This setting matches preset "+Presets[i]+".";
				}
			}
			lagFixOptions.add(new LagFixTextOption("descr", "Current Settings", preset, output[0]));
		} else {
			lagFixOptions.add(new LagFixTextOption("descr", r.stderr, r.stderr, r.stderr));
		}
		
		lagFixOptions.add(new LagFixChoiceOption("preset", "Autokiller Preset", "Strict is recommended", Presets));
		lagFixOptions.add(new LagFixCheckOption("autoset", "Set On Boot", "If checked, this option will be enabled on every reboot. If unchecked, on your next reboot the setting will be reset to default.", true));
		
		return lagFixOptions;
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

}
