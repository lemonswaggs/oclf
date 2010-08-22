package com.rc.QuickFixLagFix.Activities;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.LagFixWorker;
import com.rc.QuickFixLagFix.lib.StatusListener;

public class QuickFixLagFix extends ListActivity implements StatusListener {

	final static String VERSION = "1.3";
	static final int DIALOG_FORCE_CLOSE = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_PROGRESS);
		
		setContentView(R.layout.main);

		setListAdapter(new FixListAdapter(this, LagFixWorker.getBackgroundWorker().getLagFixList()));
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
			currentStatus.setText(LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable.CurrentLagFix.GetStatus());
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
	public void UpdateStatusForLagFix(LagFix lagfix, final String Status) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				TextView currentStatus = (TextView) findViewById(R.id.status);
				TextView lastResult = (TextView) findViewById(R.id.lastresult);

				currentStatus.setText(Status);
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
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.about :
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("One Click Lag Fix Version " + VERSION+"\nMade by RyanZA").setCancelable(true);
				AlertDialog alert = builder.create();
				alert.setOwnerActivity(this);
				alert.show();
				return true;
			case R.id.exit :
				showDialog(DIALOG_FORCE_CLOSE);
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
				builder.setMessage("Are you sure you wish to force close?").setCancelable(true)
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
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
		}
		return null;
	}

	@Override
	public void Update(final int progress) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				FixListAdapter adapter = (FixListAdapter) getListAdapter();
				adapter.notifyDataSetChanged();
				getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress);
			}
		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		LagFix lf = (LagFix) l.getItemAtPosition(position);
		if (lf.getEnabledStatus()) {
			if (LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable != null && LagFixWorker.getBackgroundWorker().CurrentLagFixRunnable.CurrentLagFix == lf) {
				Intent intent = new Intent(this, ProgressViewActivity.class);
				intent.putExtra("LagFixIndex", LagFixWorker.getBackgroundWorker().getIndexOfLagFix(lf));
				startActivity(intent);
				return;
			}
			Intent intent = new Intent(this, OptionActivity.class);
			intent.putExtra("LagFixIndex", LagFixWorker.getBackgroundWorker().getIndexOfLagFix(lf));
			startActivity(intent);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(lf.GetDisplayName() + " disabled");
			builder.setMessage(lf.getDisabledReason()).setCancelable(true);
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(this);
			alert.show();
		}
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
		if (fixList.get(position).getEnabledStatus()) {
			Enabled.setText("Enabled");
			convertView.setBackgroundColor(Color.rgb(0, 65, 0));
		} else {
			Enabled.setText("Disabled");
			convertView.setBackgroundColor(Color.rgb(65, 0, 0));
		}

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

}