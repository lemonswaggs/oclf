package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixChoiceOption;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class ManageMediaScanningLagFix extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Manage Media Scanning";
	}

	@Override
	public String GetShortDescription() {
		return "Enable/disable the media scanner";
	}

	@Override
	public String GetLongDescription() {
		return "This option will enable or disable the media scanner. With the media scanner disabled, no new media such as mp3s will be detected by the device until it is re-enabled.";
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
		
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		String SchedType = options.get("SchedType");
		if (SchedType.equals(LagFixChoiceOption.NO_SELECTION))
			return "No scheduler selected.";
		
		VTCommandResult r = vt.runCommand("cat /sys/block/mmcblk0/queue/scheduler");
		if (!r.success())
			return "Could not run 'cat /sys/block/mmcblk0/queue/scheduler': "+r.stderr;
		StringBuilder buf = new StringBuilder();
		boolean inside = false;
		for (int i=0;i<r.stdout.length();i++) {
			if (r.stdout.charAt(i) == ']')
				break;
			if (inside)
				buf.append(r.stdout.charAt(i));
			if (r.stdout.charAt(i) == '[')
				inside = true;			
		}
		UpdateStatus("Currently selected scheduler: "+buf.toString());
		
		UpdateStatus("Setting scheduler to "+SchedType+"...");
		r = vt.runCommand("echo "+SchedType+" > /sys/block/mmcblk0/queue/scheduler");
		if (!r.success())
			return "Could not run change scheduler: "+r.stderr;
		
		r = vt.runCommand("cat /sys/block/mmcblk0/queue/scheduler");
		if (!r.success())
			return "Could not run 'cat /sys/block/mmcblk0/queue/scheduler': "+r.stderr;
		buf = new StringBuilder();
		inside = false;
		for (int i=0;i<r.stdout.length();i++) {
			if (r.stdout.charAt(i) == ']')
				break;
			if (inside)
				buf.append(r.stdout.charAt(i));
			if (r.stdout.charAt(i) == '[')
				inside = true;			
		}
		UpdateStatus("Currently selected scheduler: "+buf.toString());
		
		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();
		
		lagFixOptions.add(new LagFixChoiceOption("SchedType","Scheduler","The scheduler to use.", new String[]{"cfq", "deadline", "anticipatory", "noop"}));
		
		return lagFixOptions;
	}

}
