package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class TestUndoWrapper extends LagFix {

	@Override
	public String GetDisplayName() {
		return "TestUndoWrapper";
	}

	@Override
	public String GetShortDescription() {
		return "TestUndoWrapper";
	}

	@Override
	public String GetLongDescription() {
		return "TestUndoWrapper";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "TestUndoWrapper";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		vt.runCommand("mount -o remount,rw rootfs /");
		vt.runCommand("mount -o remount,rw /dev/block/stl9 /system");
		vt.busybox("mkdir /dbdata/logs");
		vt.busybox("mkdir /dbdata/logs/gpsd");
		vt.busybox("chmod 777 /system/bin");
		vt.busybox("chmod 777 /system/bin/gpsd");
		
		for (String command : TestWrapper.ReplaceCommands) {
			vt.busybox("mv /system/bin/"+command+".replace /system/bin/"+command);
		}
		
		vt.busybox("mv /system/bin/playlogos1.replace /system/bin/playlogos1");
		
		return SUCCESS;
	}

	@Override
	protected List<LagFixOption> GetOptions() throws Exception, Error {
		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();

		return lagFixOptions;
	}

}
