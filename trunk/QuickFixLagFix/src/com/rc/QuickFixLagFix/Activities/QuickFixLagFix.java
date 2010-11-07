package com.rc.QuickFixLagFix.Activities;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.LagFixWorker;
import com.rc.QuickFixLagFix.lib.StatusListener;
import com.rc.QuickFixLagFix.lib.LagFix.LogRow;

public class QuickFixLagFix extends TabActivity implements StatusListener {

	final static String VERSION = "2.2.8";
	static final int DIALOG_FORCE_CLOSE = 0;
	static final int DIALOG_HELP = 1;
	public static final String PREFS_NAME = "OCLFPrefs";
	public static final String PREFS_ADS = "AdsEnabled";
	public static final String FIRST_LOAD = "FirstLoad";

	FixListAdapter enabledAdapter;
	FixListAdapter disabledAdapter;
	FixListAdapter allAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		enabledAdapter = new FixListAdapter(this, LagFixWorker.getBackgroundWorker().getEnabledList(LagFix.LAGFIX_ENABLED));
		disabledAdapter = new FixListAdapter(this, LagFixWorker.getBackgroundWorker().getEnabledList(LagFix.LAGFIX_DISABLED));
		allAdapter = new FixListAdapter(this, LagFixWorker.getBackgroundWorker().getLagFixList());

		requestWindowFeature(Window.FEATURE_PROGRESS);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean AdsEnabled = settings.getBoolean(PREFS_ADS, true);
		if (AdsEnabled) {
			setContentView(R.layout.mainwadd);
		} else {
			setContentView(R.layout.main);
		}
		
		boolean FirstLoad = settings.getBoolean(FIRST_LOAD, true);
		if (FirstLoad) {
			showDialog(DIALOG_HELP);
		}

		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab

		TabContentFactory factory = new MyTabFactory();

		spec = tabHost.newTabSpec("All").setIndicator("All");
		spec.setContent(factory);
		tabHost.addTab(spec);

		spec = tabHost.newTabSpec("Enabled").setIndicator("Available");
		spec.setContent(factory);
		tabHost.addTab(spec);

		spec = tabHost.newTabSpec("Disabled").setIndicator("Unavailable");
		spec.setContent(factory);
		tabHost.addTab(spec);
	}

	@Override
	protected void onResume() {
		super.onResume();
		LagFixWorker.getBackgroundWorker().SetStatusListener(this);

		TextView currentStatus = (TextView) findViewById(R.id.status);
		TextView lastResult = (TextView) findViewById(R.id.lastresult);
		lastResult.setVisibility(View.GONE);
		currentStatus.setVisibility(View.GONE);

		if (LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable != null) {
			currentStatus.setText(LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable.CurrentLagFix.GetStatus().LogMessage);
			currentStatus.setVisibility(View.VISIBLE);
			currentStatus.setBackgroundColor(Color.rgb(65, 65, 0));
		} else if (LagFixWorker.getBackgroundWorker().LastLagFixResult != null) {
			lastResult.setText(LagFixWorker.getBackgroundWorker().LastLagFixResult);
			lastResult.setVisibility(View.VISIBLE);
			if (LagFixWorker.getBackgroundWorker().wasLastLagFixSuccess) {
				lastResult.setBackgroundColor(Color.rgb(0, 65, 0));
			} else {
				lastResult.setBackgroundColor(Color.rgb(65, 0, 0));
			}
		}

		if (LagFixWorker.getBackgroundWorker().CurrentLagFixChecker == null) {
			LagFixWorker.getBackgroundWorker().doLagFixChecks(getApplicationContext());
			if (!LagFixWorker.getBackgroundWorker().LagFixCheckRunning) {
				getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void UpdateStatusForLagFix(LagFix lagfix, final LogRow Status) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				TextView currentStatus = (TextView) findViewById(R.id.status);
				TextView lastResult = (TextView) findViewById(R.id.lastresult);

				currentStatus.setText(Status.LogMessage);
				currentStatus.setBackgroundColor(Color.rgb(65, 65, 0));

				lastResult.setVisibility(View.GONE);
				currentStatus.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public void LagFixFinishedWithMessage(LagFix lagfix, final String Message, final boolean Success) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				TextView currentStatus = (TextView) findViewById(R.id.status);
				TextView lastResult = (TextView) findViewById(R.id.lastresult);

				lastResult.setText(Message);
				if (Success) {
					lastResult.setBackgroundColor(Color.rgb(0, 65, 0));
				} else {
					lastResult.setBackgroundColor(Color.rgb(65, 0, 0));
				}

				currentStatus.setVisibility(View.GONE);
				lastResult.setVisibility(View.VISIBLE);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean AdsEnabled = settings.getBoolean(PREFS_ADS, true);
		if (!AdsEnabled) {
			menu.getItem(2).setTitle("Enable Ads");
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.about :
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("One Click Lag Fix Version " + VERSION + "\nMade by RyanZA").setCancelable(true);
				AlertDialog alert = builder.create();
				alert.setOwnerActivity(this);
				alert.show();
				return true;
			case R.id.exit :
				showDialog(DIALOG_FORCE_CLOSE);
				return true;
			case R.id.disableads :
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				boolean AdsEnabled = settings.getBoolean(PREFS_ADS, true);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(PREFS_ADS, !AdsEnabled);
				editor.commit();

				Intent i = new Intent(getApplicationContext(), this.getClass());
				startActivity(i);
				finish();
				return true;
			case R.id.help :
				showDialog(DIALOG_HELP);
				return true;
			case R.id.kernels :
				i = new Intent(getApplicationContext(), KernelListActivity.class);
				startActivity(i);
				return true;
			default :
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final Dialog dialog;
		switch (id) {
			case DIALOG_FORCE_CLOSE :
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Are you sure you wish to force close?").setCancelable(true).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						System.exit(0);
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				dialog = builder.create();
				return dialog;
			case DIALOG_HELP :
				dialog = new Dialog(this);
				dialog.setContentView(R.layout.helpdialog);
				dialog.setTitle("Information");
				Button dismiss = (Button) dialog.findViewById(R.id.dismiss);
				dismiss.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View arg0) {
						dismissDialog(DIALOG_HELP);
					}
				});
				dialog.setOnDismissListener(new OnDismissListener() {
					
					@Override
					public void onDismiss(DialogInterface dialog) {
						SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean(FIRST_LOAD, false);
						editor.commit();
					}
				});
				return dialog;
		}
		return null;
	}

	@Override
	public void Update(final int progress) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				enabledAdapter.UpdateLagFixList(LagFixWorker.getBackgroundWorker().getEnabledList(LagFix.LAGFIX_ENABLED));
				disabledAdapter.UpdateLagFixList(LagFixWorker.getBackgroundWorker().getEnabledList(LagFix.LAGFIX_DISABLED));
				allAdapter.notifyDataSetChanged();
				getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress);
			}
		});
	}

	OnItemClickListener clickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final LagFix lf = (LagFix) view.getTag();
			if (lf.getEnabledStatus() == LagFix.LAGFIX_ENABLED) {
				if (LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable != null && LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable.CurrentLagFix == lf) {
					Intent intent = new Intent(QuickFixLagFix.this, ProgressViewActivity.class);
					intent.putExtra("LagFixIndex", LagFixWorker.getBackgroundWorker().getIndexOfLagFix(lf));
					startActivity(intent);
					return;
				}
				Intent intent = new Intent(QuickFixLagFix.this, OptionActivity.class);
				intent.putExtra("LagFixIndex", LagFixWorker.getBackgroundWorker().getIndexOfLagFix(lf));
				startActivity(intent);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(QuickFixLagFix.this);
				builder.setTitle(lf.GetDisplayName() + " disabled");
				builder.setMessage(lf.getDisabledReason()).setCancelable(true);
				if (lf.CanForce()) {
					builder.setPositiveButton("Force Anyway", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable != null && LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable.CurrentLagFix == lf) {
								Intent intent = new Intent(QuickFixLagFix.this, ProgressViewActivity.class);
								intent.putExtra("LagFixIndex", LagFixWorker.getBackgroundWorker().getIndexOfLagFix(lf));
								startActivity(intent);
								return;
							}
							Intent intent = new Intent(QuickFixLagFix.this, OptionActivity.class);
							intent.putExtra("LagFixIndex", LagFixWorker.getBackgroundWorker().getIndexOfLagFix(lf));
							startActivity(intent);
						}
					});
					builder.setNegativeButton("Cancel", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
				}
				AlertDialog alert = builder.create();
				alert.setOwnerActivity(QuickFixLagFix.this);
				alert.show();
			}
		}
	};

	class MyTabFactory implements TabContentFactory {

		@Override
		public View createTabContent(String tag) {
			if (tag.equals("Enabled")) {
				ListView lv = new ListView(QuickFixLagFix.this);
				lv.setAdapter(enabledAdapter);
				lv.setOnItemClickListener(clickListener);
				return lv;
			}
			if (tag.equals("Disabled")) {
				ListView lv = new ListView(QuickFixLagFix.this);
				lv.setAdapter(disabledAdapter);
				lv.setOnItemClickListener(clickListener);
				return lv;
			}
			if (tag.equals("All")) {
				ListView lv = new ListView(QuickFixLagFix.this);
				lv.setAdapter(allAdapter);
				lv.setOnItemClickListener(clickListener);
				return lv;
			}
			return null;
		}

	}

	@Override
	public void NofifyChanged() {
	}
}

class FixListAdapter extends BaseAdapter {

	List<LagFix> fixList;
	Context context;

	public FixListAdapter(Context context, List<LagFix> fixList) {
		this.fixList = fixList;
		this.context = context;
	}

	@Override
	public int getCount() {
		return fixList.size();
	}

	@Override
	public Object getItem(int position) {
		return fixList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.row, null);
		}

		TextView DisplayName = (TextView) convertView.findViewById(R.id.displayname);
		TextView ShortDescription = (TextView) convertView.findViewById(R.id.shortdescription);
		TextView Enabled = (TextView) convertView.findViewById(R.id.enabled);
		DisplayName.setText(fixList.get(position).GetDisplayName());
		ShortDescription.setText(fixList.get(position).GetShortDescription());
		if (fixList.get(position).getEnabledStatus() == LagFix.LAGFIX_ENABLED) {
			Enabled.setText("Available");
			convertView.setBackgroundResource(R.drawable.greenbar);
			DisplayName.setTextColor(0xFF00FF00);
		} else if (fixList.get(position).getEnabledStatus() == LagFix.LAGFIX_DISABLED) {
			Enabled.setText("Unavailable");
			convertView.setBackgroundResource(R.drawable.redbar);
			DisplayName.setTextColor(0xFFFF6666);
		} else {
			Enabled.setText("Initializing");
			convertView.setBackgroundResource(R.drawable.blackbar);
			DisplayName.setTextColor(0xFFFFFFFF);
		}
		convertView.setTag(fixList.get(position));

		return convertView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return fixList.isEmpty();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	public void UpdateLagFixList(List<LagFix> list) {
		fixList = list;
		notifyDataSetChanged();
	}

}