package com.rc.QuickFixLagFix.LagFixes;

import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.os.StatFs;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.OptionListener;
import com.rc.QuickFixLagFix.lib.Utils;
import com.rc.QuickFixLagFix.lib.VirtualTerminal;

public class CheckFreeSpaceLagFix extends LagFix {

	@Override
	public String GetDisplayName() {
		return "Check Free Space";
	}

	@Override
	public String GetShortDescription() {
		return "Free Space in /data/data";
	}

	@Override
	public String GetLongDescription() {
		return "This will grab how much free space you have available in /data/data";
	}

	@Override
	public String GetFeedbackLogEmailAddress() {
		return "oneclicklagfix@gmail.com";
	}

	@Override
	public String IsEnabled(Context ApplicationContext) throws Exception, Error {		
		return ENABLED;
	}

	@Override
	public String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error {
		StatFs statfs = new StatFs("/data/data/");
		long bytecountfree_ext2 = (long) statfs.getAvailableBlocks() * (long) statfs.getBlockSize();
		long bytecounttotal_ext2 = (long) statfs.getBlockCount() * (long) statfs.getBlockSize();
		
		UpdateStatus("You have "+Utils.FormatByte(bytecountfree_ext2)+" out of "+Utils.FormatByte(bytecounttotal_ext2)+" available.");
		
		return SUCCESS;
	}

	@Override
	public void GetOptions(OptionListener listener) {

		ArrayList<LagFixOption> lagFixOptions = new ArrayList<LagFixOption>();
		
		listener.LagFixOptionListCompleted(lagFixOptions);
	}

}
