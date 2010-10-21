package com.rc.QuickFixLagFix.lib;

import com.rc.QuickFixLagFix.lib.LagFix.LogRow;


public interface StatusListener {

	public void NofifyChanged();
	public void UpdateStatusForLagFix(LagFix lagfix, LogRow Status);
	public void LagFixFinishedWithMessage(LagFix lagfix, String Message, final boolean Success);
	public void Update(int progress);
	
}
