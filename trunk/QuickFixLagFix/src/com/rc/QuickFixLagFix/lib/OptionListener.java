package com.rc.QuickFixLagFix.lib;

import java.util.List;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;

public interface OptionListener {
	void LagFixOptionListCompleted(List<LagFixOption> optionList);
	void LagFixOptionListFailed(Exception e);
}
