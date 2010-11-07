package com.rc.QuickFixLagFix.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.rc.QuickFixLagFix.LagFixes.ChangeSchedulerLagFix;
import com.rc.QuickFixLagFix.LagFixes.MinFreeLagFix;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;

public class BootTimeActivity extends BroadcastReceiver {

	void SetTweaks(String SchedType) throws Exception {
		VirtualTerminal vt = new VirtualTerminal();
		ChangeSchedulerLagFix.DoTweaks(vt, SchedType, null);
	}
	
	void SetScheduler(String SchedType) throws Exception {
		ShellCommand sc = new ShellCommand();
		sc.busybox = "busybox";
		for (String blockdev : ChangeSchedulerLagFix.BlockDevices) {
			Log.i("OCLF", "echo " + SchedType + " > /sys/block/"+blockdev+"/queue/scheduler");
			CommandResult r = sc.su.busyboxWaitFor("echo " + SchedType + " > /sys/block/"+blockdev+"/queue/scheduler");
			Log.i("OCLF", "success: "+r.success()+" "+r.stderr+" "+r.stdout);
		}
	}
	
	void SetMinFree(String preset) throws Exception {
		ShellCommand sc = new ShellCommand();
		sc.busybox = "busybox";
		for (int i=0;i<MinFreeLagFix.Presets.length;i++) {
			String presetname = MinFreeLagFix.Presets[i];
			if (presetname.equals(preset)) {
				Log.i("OCLF", "echo \""+MinFreeLagFix.PresetValues[i]+"\" > /sys/module/lowmemorykiller/parameters/minfree");
				CommandResult r = sc.su.busyboxWaitFor("echo \""+MinFreeLagFix.PresetValues[i]+"\" > /sys/module/lowmemorykiller/parameters/minfree");
				Log.i("OCLF", "success: "+r.success()+" "+r.stderr+" "+r.stdout);
			}
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			SharedPreferences settings = context.getSharedPreferences(QuickFixLagFix.PREFS_NAME, 0);
			String SchedType = settings.getString("SchedType", null);
			if (SchedType != null) {
				try {
					SetScheduler(SchedType);
					boolean Tweaks = settings.getBoolean("Tweaks", false);
					if (Tweaks) {
						SetTweaks(SchedType);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			String AutokillerPreset = settings.getString("AutokillerPreset", null);
			if (AutokillerPreset != null) {
				try {
					SetMinFree(AutokillerPreset);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

}
