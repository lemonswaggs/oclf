package com.rc.QuickFixLagFix.LagFixOptions;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class LagFixSeekOption extends LagFixOption {

	public final static String CHECKED = "YES";
	public final static String NOT_CHECKED = "NO";
	
	WeakReference<LinearLayout> llWR = new WeakReference<LinearLayout>(null);
	WeakReference<SeekBar> seekbarWR = new WeakReference<SeekBar>(null);
	WeakReference<TextView> textWR = new WeakReference<TextView>(null);
	
	SeekOptionUpdate updatehandler;
	final int startVal;

	public LagFixSeekOption(String codeName, String displayName, String description, int startVal) {
		super(codeName, displayName, description);
		this.startVal = startVal;
	}
	
	public void setUpdatehandler(SeekOptionUpdate updatehandler) {
		this.updatehandler = updatehandler;
	}

	@Override
	public View GetView(Activity ActivityContex) {
		LinearLayout ll = llWR.get();
		if (ll != null) {
			if (ll.getContext().equals(ActivityContex))
				return ll;
		}

		ll = new LinearLayout(ActivityContex);
		ll.setOrientation(LinearLayout.VERTICAL);
		
		SeekBar seekbar = new SeekBar(ActivityContex);
		TextView text = new TextView(ActivityContex);
		
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(180, RadioGroup.LayoutParams.WRAP_CONTENT);
		ll.addView(seekbar, layoutParams);
		ll.addView(text, layoutParams);
		
		llWR = new WeakReference<LinearLayout>(ll);
		seekbarWR = new WeakReference<SeekBar>(seekbar);
		textWR = new WeakReference<TextView>(text);
		
		seekbar.setOnSeekBarChangeListener(updatehandler);
		seekbar.setMax(10000);
		seekbar.setProgress(startVal);

		return ll;
	}

	@Override
	public String GetSelectedValue() throws IllegalStateException {
		SeekBar seekbar = seekbarWR.get();
		if (seekbar == null) {
			throw new IllegalStateException();
		}
		
		return Integer.toString(seekbar.getProgress());
	}

	public abstract static class SeekOptionUpdate implements OnSeekBarChangeListener {

		public abstract String DisplayValue(int progress);
		final LagFixSeekOption main;
		public SeekOptionUpdate(LagFixSeekOption main) {
			this.main = main;
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			String stringToDisplay = DisplayValue(progress);
			TextView text = main.textWR.get();
			if (text != null) {
				text.setText(stringToDisplay);
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
		
	}

}
