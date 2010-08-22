package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.OptionListener;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;
import com.rc.QuickFixLagFix.lib.VirtualTerminal.VTCommandResult;

public class TestLagFix extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Test";
	}

	@Override
	public String GetShortDescription() {
		return "Test";
	}

	@Override
	public String GetLongDescription() {
		return "Test";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		VTCommandResult r = vt.busybox("ls -l /data");
		UpdateStatus(r.stdout);
		
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
