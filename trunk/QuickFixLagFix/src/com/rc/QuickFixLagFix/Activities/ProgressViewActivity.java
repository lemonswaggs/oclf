package com.rc.QuickFixLagFix.Activities;

import java.util.ArrayList;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.LagFix.LogRow;
import com.rc.QuickFixLagFix.lib.LagFixWorker;
import com.rc.QuickFixLagFix.lib.StatusListener;

public class ProgressViewActivity extends ListActivity implements StatusListener {

	static final int DIALOG_REQUEST_SENDLOG = 0;
	static final int DIALOG_REQUEST_SENDLOG_DETAILS = 1;
	
	LagFix lagFix;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.progress);
		
		int lfIndex = getIntent().getIntExtra("LagFixIndex", 0);
		lagFix = LagFixWorker.getBackgroundWorker().getLagFix(lfIndex);
		
		setListAdapter(new LogListAdapter(this, lagFix.getStatusLog()));
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
	}
	
	@Override
	public void UpdateStatusForLagFix(final LagFix lagfix, final String Status) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				TextView currentStatus = (TextView) findViewById(R.id.status);
				TextView lastResult = (TextView) findViewById(R.id.lastresult);

				currentStatus.setText(Status);
				currentStatus.setBackgroundColor(Color.rgb(65, 65, 0));

				lastResult.setVisibility(View.GONE);
				currentStatus.setVisibility(View.VISIBLE);
				
				LogListAdapter adapter = (LogListAdapter) getListAdapter();
				adapter.logList.clear();
				adapter.logList.addAll(lagfix.getStatusLog());
				((LogListAdapter) getListAdapter()).notifyDataSetChanged();
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
				
				if (!Success) {
					showDialog(DIALOG_REQUEST_SENDLOG);
				}
			}
		});
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
	    final Dialog dialog;
	    switch(id) {
	    case DIALOG_REQUEST_SENDLOG:
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage("This lagfix failed! Do you want to email the log to the developer so that it can be checked?")
	    	       .setCancelable(true)
	    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                showDialog(DIALOG_REQUEST_SENDLOG_DETAILS);
	    	           }
	    	       })
	    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	           }
	    	       });
	    	dialog = builder.create();
	        return dialog;
	    case DIALOG_REQUEST_SENDLOG_DETAILS:
	    	dialog = new Dialog(this);
	    	dialog.setCancelable(true);
	    	dialog.setOwnerActivity(this);

	    	dialog.setContentView(R.layout.email_info_dialog);
	    	dialog.setTitle("User Information");

	    	final EditText contact = (EditText) dialog.findViewById(R.id.contact);
	    	final EditText firmware = (EditText) dialog.findViewById(R.id.firmware);
	    	Button sendbutton = (Button) dialog.findViewById(R.id.sendbutton);
	    	
	    	sendbutton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					StringBuilder buf = new StringBuilder();
					buf.append("Contact: "+contact.getText().toString()+"\n");
					buf.append("Firmware: "+firmware.getText().toString()+"\n");
					buf.append("\n");
					for (LogRow row : lagFix.getStatusLog()) {
						buf.append(row.LogTime.toString()+": "+row.LogMessage+"\n");
					}
					
					Intent sendIntent = new Intent(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, buf.toString());
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Error Log for "+lagFix.GetDisplayName());
					sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{lagFix.GetFeedbackLogEmailAddress()});
					sendIntent.setType("message/rfc822");
					startActivity(Intent.createChooser(sendIntent, "Title:"));
					
					dialog.cancel();
				}
			});
	        return dialog;
	    }
	    return null;
	}
	
	@Override
	public void Update(final int progress) {
	}

}

class LogListAdapter extends BaseAdapter {

	List<LogRow> logList = new ArrayList<LagFix.LogRow>();
	Context context;

	public LogListAdapter(Context context, List<LogRow> logList) {
		this.logList.addAll(logList);
		this.context = context;
	}

	@Override
	public int getCount() {
		return logList.size();
	}

	@Override
	public Object getItem(int position) {
		return logList.get(position);
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
			convertView = inflater.inflate(R.layout.logrow, null);
		}

		TextView LogTime = (TextView) convertView.findViewById(R.id.logtime);
		TextView LogMessage = (TextView) convertView.findViewById(R.id.logmessage);

		LogTime.setText(logList.get(position).LogTime.toLocaleString());
		LogMessage.setText(logList.get(position).LogMessage);

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
		return logList.isEmpty();
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