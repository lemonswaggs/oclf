package com.rc.QuickFixLagFix.lib;


public interface StatusListener {

	public void UpdateStatusForLagFix(LagFix lagfix, String Status);
	public void LagFixFinishedWithMessage(LagFix lagfix, String Message, final boolean Success);
	public void Update(int progress);
	
}
