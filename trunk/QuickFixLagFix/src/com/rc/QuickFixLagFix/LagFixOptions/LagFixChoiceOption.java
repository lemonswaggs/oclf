package com.rc.QuickFixLagFix.LagFixOptions;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class LagFixChoiceOption extends LagFixOption {

	public final static String NO_SELECTION = "[NO SELECTION]";
	
	String[] Choices;
	WeakReference<RadioGroup> choiceGroupWR = new WeakReference<RadioGroup>(null);

	public LagFixChoiceOption(String codeName, String displayName, String description, String[] Choices) {
		super(codeName, displayName, description);
		this.Choices = Choices;
	}

	@Override
	public View GetView(Activity ActivityContex) {
		RadioGroup choiceGroup = choiceGroupWR.get();
		if (choiceGroup != null) {
			if (choiceGroup.getContext().equals(ActivityContex))
				return choiceGroup;
		}

		choiceGroup = new RadioGroup(ActivityContex);
		choiceGroupWR = new WeakReference<RadioGroup>(choiceGroup);

		for (int i=0; i<Choices.length; i++) {
			RadioButton newRadioButton = new RadioButton(ActivityContex);
			newRadioButton.setText(Choices[i]);
			newRadioButton.setId(i);
			LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
			choiceGroup.addView(newRadioButton, 0, layoutParams);
		}

		return choiceGroup;
	}

	@Override
	public String GetSelectedValue() throws IllegalStateException {
		RadioGroup choiceGroup = choiceGroupWR.get();
		if (choiceGroup == null) {
			throw new IllegalStateException();
		}
		
		int checkedID = choiceGroup.getCheckedRadioButtonId();
		
		if (checkedID == -1) return NO_SELECTION;
		
		return Choices[checkedID];
	}


}
