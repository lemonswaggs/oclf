package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class Repartition extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Repartition";
	}

	@Override
	public String GetShortDescription() {
		return "Repartition";
	}

	@Override
	public String GetLongDescription() {
		return "Repartition";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "Repartition";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		VTCommandResult r;
//		vt.runCommand("umount /mnt/sdcard/external_sd/.android_secure");
//		vt.runCommand("umount /mnt/secure/asec");
//		vt.busybox("fuser -km /mnt/sdcard/external_sd");
//		vt.runCommand("umount /mnt/sdcard/external_sd");
//		vt.busybox("busybox fuser -km /mnt/sdcard");
//		r = vt.runCommand("umount /mnt/sdcard");
//		if (!r.success())
//			return "Error! Could not unmount /mnt/sdcard : "+r.stderr;
		
		r = vt.runCommand("/dbdata/parted-static /dev/block/mmcblk0 print");
		UpdateStatus(r.stdout);

		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		return lagFixOptions;
	}

}
