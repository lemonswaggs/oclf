package com.rc.QuickFixLagFix.Activities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.LagFixWorker;
import com.rc.QuickFixLagFix.lib.OptionListener;

public class OptionActivity extends Activity implements OptionListener {

	LagFix lagFix;
	List<LagFixOption> optionList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sub);
		
		int lfIndex = getIntent().getIntExtra("LagFixIndex", 0);
		lagFix = LagFixWorker.getBackgroundWorker().getLagFix(lfIndex);
		
		new Thread(){
			public void run() {
				try {
					lagFix.GetOptions(OptionActivity.this);
				} catch (Exception e) {
					e.printStackTrace();
				} catch (Error e) {
					e.printStackTrace();
				}
			};
		}.start();
		
		Button GoButton = (Button) findViewById(R.id.gobutton);
		GoButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable != null) {
					AlertDialog.Builder builder = new AlertDialog.Builder(OptionActivity.this);
					builder.setMessage("There is already a lagfix running.").setCancelable(true);
					AlertDialog alert = builder.create();
					alert.setOwnerActivity(OptionActivity.this);
					alert.show();
					return;
				}
				
				Map<String, String> optionsMap = new HashMap<String, String>();
				for (LagFixOption option : optionList) {
					optionsMap.put(option.GetCodeName(), option.GetSelectedValue());
				}
				
				LagFixWorker.getBackgroundWorker().RunLagFix(lagFix, optionsMap, getApplicationContext());
				
				Intent intent = new Intent(OptionActivity.this, ProgressViewActivity.class);
				intent.putExtra("LagFixIndex", LagFixWorker.getBackgroundWorker().getIndexOfLagFix(lagFix));
				startActivity(intent);
				
				OptionActivity.this.finish();
			}
		});
		GoButton.setVisibility(View.GONE);
		
		TextView DisplayName = (TextView) findViewById(R.id.displayname);
		TextView LongDescription = (TextView) findViewById(R.id.longdescription);
		
		DisplayName.setText(lagFix.GetDisplayName());
		LongDescription.setText(lagFix.GetLongDescription());
	}

	@Override
	public void LagFixOptionListCompleted(final List<LagFixOption> optionList) {
		this.optionList = optionList;
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				LinearLayout mainLayout = (LinearLayout) findViewById(R.id.mainlayout);
				
				for (LagFixOption option : optionList) {
					LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					View optionrow = inflater.inflate(R.layout.optionrow, null);
					
					FrameLayout fr = (FrameLayout) optionrow.findViewById(R.id.frame);
					View view = option.GetView(OptionActivity.this);
					fr.addView(view, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					
					TextView DisplayName = (TextView) optionrow.findViewById(R.id.displayname);
					TextView Description = (TextView) optionrow.findViewById(R.id.description);
					
					DisplayName.setText(option.GetDisplayName());
					Description.setText(option.GetDescription());
					
					mainLayout.addView(optionrow);
				}
				
				Button GoButton = (Button) findViewById(R.id.gobutton);
				GoButton.setVisibility(View.VISIBLE);
				ProgressBar loading = (ProgressBar) findViewById(R.id.loading);
				loading.setVisibility(View.GONE);
			}
		});
	}
	
}
