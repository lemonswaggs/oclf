package com.rc.QuickFixLagFix.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.rc.QuickFixLagFix.LagFixes.ChangeSchedulerLagFix;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class BootTimeActivity extends BroadcastReceiver {

	void SetScheduler(String SchedType) throws Exception {
		VirtualTerminal vt = new VirtualTerminal();
		for (String blockdev : ChangeSchedulerLagFix.BlockDevices) {
			Log.i("OCLF", "echo " + SchedType + " > /sys/block/"+blockdev+"/queue/scheduler");
			vt.runCommand("echo " + SchedType + " > /sys/block/"+blockdev+"/queue/scheduler");
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
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

}
