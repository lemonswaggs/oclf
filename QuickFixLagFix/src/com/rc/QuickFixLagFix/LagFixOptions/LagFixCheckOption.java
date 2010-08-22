package com.rc.QuickFixLagFix.LagFixOptions;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;

public class LagFixCheckOption extends LagFixOption {

	public final static String CHECKED = "YES";
	public final static String NOT_CHECKED = "NO";
	
	WeakReference<CheckBox> checkboxWR = new WeakReference<CheckBox>(null);
	final boolean CheckedOnStart;

	public LagFixCheckOption(String codeName, String displayName, String description, boolean CheckedOnStart) {
		super(codeName, displayName, description);
		this.CheckedOnStart = CheckedOnStart;
	}

	@Override
	public View GetView(Activity ActivityContex) {
		CheckBox checkbox = checkboxWR.get();
		if (checkbox != null) {
			if (checkbox.getContext().equals(ActivityContex))
				return checkbox;
		}

		checkbox = new CheckBox(ActivityContex);
		checkbox.setChecked(CheckedOnStart);
		checkboxWR = new WeakReference<CheckBox>(checkbox);

		return checkbox;
	}

	@Override
	public String GetSelectedValue() throws IllegalStateException {
		CheckBox checkbox = checkboxWR.get();
		if (checkbox == null) {
			throw new IllegalStateException();
		}
		
		return checkbox.isChecked() ? CHECKED : NOT_CHECKED;
	}


}
