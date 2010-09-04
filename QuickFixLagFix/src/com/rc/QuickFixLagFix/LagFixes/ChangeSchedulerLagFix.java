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
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class ChangeSchedulerLagFix extends LagFix {

	public final static String[] BlockDevices = new String[]{"mmcblk0", "mmcblk1", "stl3", "stl9", "stl10", "stl11"};

	@Override
	public String GetDisplayName() {
		return "Change Scheduler";
	}

	@Override
	public String GetShortDescription() {
		return "Changes the disk scheduler.";
	}

	@Override
	public String GetLongDescription() {
		return "This will let you easily change which scheduler your phone is using. The default option is cfq. There are other options available: noop, anticipatory, deadline.\n\nCFQ [cfq] (Completely Fair Queuing) is an I/O scheduler for the Linux kernel and default under many Linux distributions.\n\nNoop scheduler (noop) is the simplest I/O scheduler for the Linux kernel based upon FIFO queue concept.\n\nAnticipatory scheduler (anticipatory) is an algorithm for scheduling hard disk input/output as well as old scheduler which is replaced by CFQ\n\nDeadline scheduler (deadline) - it attempt to guarantee a start service time for a request.\n\nThe deadline scheduler seems to work very well on the Galaxy S because reads are given a higher priority than writes.. Give it a try!\n\nThe scheduler will need to be set again if your reboot your phone!";
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
		
		String SetOnBoot = options.get("SetOnBoot");
		SharedPreferences settings = ApplicationContext.getSharedPreferences(QuickFixLagFix.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();		
		if (SetOnBoot.equals(LagFixCheckOption.NOT_CHECKED)) {
			editor.remove("SchedType");
		} else {
			editor.putString("SchedType", SchedType);
		}
		editor.commit();
		
		for (String blockdev : BlockDevices) {
			VTCommandResult r = vt.runCommand("cat /sys/block/"+blockdev+"/queue/scheduler");
			if (!r.success()) {
				UpdateStatus("Could not access /sys/block/"+blockdev+"/queue/scheduler': " + r.stderr);
				UpdateStatus("Skipping to next block device...");
				continue;
			}

			StringBuilder buf = new StringBuilder();
			boolean inside = false;
			for (int i = 0; i < r.stdout.length(); i++) {
				if (r.stdout.charAt(i) == ']')
					break;
				if (inside)
					buf.append(r.stdout.charAt(i));
				if (r.stdout.charAt(i) == '[')
					inside = true;
			}
			UpdateStatus("Currently selected scheduler for "+blockdev+": " + buf.toString());

			UpdateStatus("Setting scheduler for "+blockdev+" to " + SchedType + "...");
			r = vt.runCommand("echo " + SchedType + " > /sys/block/"+blockdev+"/queue/scheduler");
			if (!r.success())
				return "Could not run change scheduler: " + r.stderr;

			r = vt.runCommand("cat /sys/block/"+blockdev+"/queue/scheduler");
			if (!r.success())
				return "Could not run 'cat /sys/block/"+blockdev+"/queue/scheduler': " + r.stderr;
			buf = new StringBuilder();
			inside = false;
			for (int i = 0; i < r.stdout.length(); i++) {
				if (r.stdout.charAt(i) == ']')
					break;
				if (inside)
					buf.append(r.stdout.charAt(i));
				if (r.stdout.charAt(i) == '[')
					inside = true;
			}
			UpdateStatus("Currently selected scheduler for "+blockdev+": " + buf.toString());
		}

		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		lagFixOptions.add(new LagFixChoiceOption("SchedType", "Scheduler", "The scheduler to use.", new String[]{"cfq", "deadline", "anticipatory", "noop"}));
		lagFixOptions.add(new LagFixCheckOption("SetOnBoot","Set On Boot", "If checked, the selected scheduler will be set on boot up.", false));

		return lagFixOptions;
	}

}
