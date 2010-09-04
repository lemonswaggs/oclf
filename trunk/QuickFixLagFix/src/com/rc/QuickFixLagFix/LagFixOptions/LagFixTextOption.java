package com.rc.QuickFixLagFix.LagFixOptions;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

public class LagFixTextOption extends LagFixOption {

	String text;
	
	public LagFixTextOption(String codeName, String displayName, String description, String text) {
		super(codeName, displayName, description);
		this.text = text;
	}

	@Override
	public View GetView(Activity ActivityContex) {
		TextView tv = new TextView(ActivityContex);
		tv.setText(text);

		return tv;
	}

	@Override
	public String GetSelectedValue() throws IllegalStateException {
		return text;
	}


}
