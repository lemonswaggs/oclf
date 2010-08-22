package com.rc.QuickFixLagFix.LagFixOptions;

import android.app.Activity;
import android.view.View;

public abstract class LagFixOption {
	
	String CodeName;
	String DisplayName;
	String Description;
	
	public LagFixOption(String codeName, String displayName, String description) {
		CodeName = codeName;
		DisplayName = displayName;
		Description = description;
	}

	public String GetCodeName() {
		return CodeName;
	}

	public String GetDisplayName() {
		return DisplayName;
	}

	public String GetDescription() {
		return Description;
	}
	
	public abstract View GetView(Activity ActivityContext);
	public abstract String GetSelectedValue() throws IllegalStateException;
	
}
