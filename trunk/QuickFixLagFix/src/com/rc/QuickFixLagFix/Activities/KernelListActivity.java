package com.rc.QuickFixLagFix.Activities;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import com.rc.QuickFixLagFix.R;
import com.rc.QuickFixLagFix.LagFixes.InstallBackupKernelLagFix;
import com.rc.QuickFixLagFix.LagFixes.InstallKernelLagFix;
import com.rc.QuickFixLagFix.lib.LagFix;
import com.rc.QuickFixLagFix.lib.LagFixWorker;
import com.rc.QuickFixLagFix.lib.ShellCommand;
import com.rc.QuickFixLagFix.lib.ShellCommand.CommandResult;
import com.rc.QuickFixLagFix.lib.Utils;

public class KernelListActivity extends TabActivity {

	public final static String NAME = "Name";
	public final static String DESCRIPTION = "Description";
	public final static String ANDROID_VERSION = "Android_Version";
	public final static String WEBURL = "WebURL";
	public final static String DOWNLOADURLS = "DownloadURLs";
	public final static String KERNELVERSION = "KernelVersion";
	public final static String DEVICE = "Device";
	public final static String MD5 = "MD5";

	static final int DIALOG_HELP = 1;
	static final int DIALOG_LOADING_MANIFEST = 2;
	static final int DIALOG_ERROR = 3;
	static final int DIALOG_KERNEL_INFO = 4;
	public static final String PREFS_NAME = "OCLFPrefs";
	public static final String PREFS_ADS = "AdsEnabled";
	public static final String FIRST_LOAD_KERNEL = "FirstLoadKernel";

	KernelListAdapter thisDeviceAdapter;
	KernelListAdapter allDeviceAdapter;
	String error = null;
	String Device = Build.MODEL;
	final String sdkVersion = Build.VERSION.SDK.trim();

	public static LagFix currentLagFix;
	public JSONObject currentJSON;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ShellCommand cmd = new ShellCommand();
		CommandResult r = cmd.su.runWaitFor("id");
		if (!r.success()) {
			error = "Your device must be rooted to install custom kernels.";
			showDialog(DIALOG_ERROR);
		}

		thisDeviceAdapter = new KernelListAdapter(this);
		allDeviceAdapter = new KernelListAdapter(this);

		requestWindowFeature(Window.FEATURE_PROGRESS);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		boolean AdsEnabled = settings.getBoolean(PREFS_ADS, true);
		if (AdsEnabled) {
			setContentView(R.layout.mainwadd);
		} else {
			setContentView(R.layout.main);
		}

		showDialog(DIALOG_LOADING_MANIFEST);
		LoadManifestThread.start();

		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab

		TabContentFactory factory = new MyTabFactory();

		spec = tabHost.newTabSpec("This Device").setIndicator("This Device");
		spec.setContent(factory);
		tabHost.addTab(spec);

		spec = tabHost.newTabSpec("All Devices").setIndicator("All Devices");
		spec.setContent(factory);
		tabHost.addTab(spec);

		spec = tabHost.newTabSpec("Backed-up Kernel").setIndicator("Backed-up Kernel");
		spec.setContent(factory);
		tabHost.addTab(spec);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
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

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// // Handle item selection
	// switch (item.getItemId()) {
	// case R.id.about :
	// AlertDialog.Builder builder = new AlertDialog.Builder(this);
	// builder.setMessage("One Click Lag Fix Version " + VERSION +
	// "\nMade by RyanZA").setCancelable(true);
	// AlertDialog alert = builder.create();
	// alert.setOwnerActivity(this);
	// alert.show();
	// return true;
	// case R.id.exit :
	// showDialog(DIALOG_FORCE_CLOSE);
	// return true;
	// case R.id.disableads :
	// SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	// boolean AdsEnabled = settings.getBoolean(PREFS_ADS, true);
	// SharedPreferences.Editor editor = settings.edit();
	// editor.putBoolean(PREFS_ADS, !AdsEnabled);
	// editor.commit();
	//
	// Intent i = new Intent(getApplicationContext(), this.getClass());
	// startActivity(i);
	// finish();
	// return true;
	// case R.id.help :
	// showDialog(DIALOG_HELP);
	// default :
	// return super.onOptionsItemSelected(item);
	// }
	// }

	@Override
	protected Dialog onCreateDialog(int id) {
		final Dialog dialog;
		switch (id) {
			case DIALOG_LOADING_MANIFEST :
				ProgressDialog progressDialog = new ProgressDialog(this);
				progressDialog.setMessage("Retrieving kernel manifest...");
				return progressDialog;
			case DIALOG_HELP :
				dialog = new Dialog(this);
				dialog.setContentView(R.layout.helpkerneldialog);
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
						editor.putBoolean(FIRST_LOAD_KERNEL, false);
						editor.commit();
					}
				});
				return dialog;
			case DIALOG_ERROR :
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(error).setCancelable(false).setTitle("Error").setPositiveButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						KernelListActivity.this.finish();
					}
				});
				dialog = builder.create();
				return dialog;
			case DIALOG_KERNEL_INFO :
				dialog = new Dialog(KernelListActivity.this);
				dialog.setContentView(R.layout.kernelinfo);
				dialog.setTitle(currentJSON.optString(NAME));
				dialog.setCancelable(true);

				TextView tv;
				tv = (TextView) dialog.findViewById(R.id.name);
				tv.setText(currentJSON.optString(NAME));
				tv = (TextView) dialog.findViewById(R.id.description);
				tv.setText(currentJSON.optString(DESCRIPTION));
				tv = (TextView) dialog.findViewById(R.id.kernelversion);
				tv.setText(currentJSON.optString(KERNELVERSION));

				Button weburl = (Button) dialog.findViewById(R.id.weburl);
				weburl.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentJSON.optString(WEBURL)));
						startActivity(myIntent);
					}
				});

				Button installkernel = (Button) dialog.findViewById(R.id.installkernel);
				installkernel.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						currentLagFix = new InstallKernelLagFix(currentJSON);
						LagFixWorker.getBackgroundWorker().RunLagFix(currentLagFix, null, getApplicationContext());
						Intent intent = new Intent(KernelListActivity.this, ProgressViewActivity.class);
						intent.putExtra("IsKernel", true);
						startActivity(intent);
					}
				});

				return dialog;
		}
		return null;
	}

	Thread LoadManifestThread = new Thread() {
		public void run() {
			try {
				InputStream is = Utils.getHttpStream("http://sgskernels.appspot.com/kernel-manifest");
				final JSONArray arr = new JSONArray(Utils.readInputStreamAsString(is));
				final JSONArray newArr = new JSONArray();
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = arr.getJSONObject(i);
					obj.put("ENABLED", false);
					if (obj.optString(ANDROID_VERSION).equals(sdkVersion) && obj.optString(DEVICE).equalsIgnoreCase(Device)) {
						obj.put("ENABLED", true);
						newArr.put(obj);
					}
					// if (obj.optString(DEVICE).equalsIgnoreCase(Device)) {
					// obj.put("ENABLED", true);
					// newArr.put(obj);
					// }
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						allDeviceAdapter.UpdateKernelList(arr);
						thisDeviceAdapter.UpdateKernelList(newArr);
						dismissDialog(DIALOG_LOADING_MANIFEST);
						showDialog(DIALOG_HELP);
					}
				});
			} catch (Exception ex) {
				ex.printStackTrace();
				error = "Could not retrieve kernel manifest file. Please try again later.";
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						showDialog(DIALOG_ERROR);
					}
				});	
			}
		};
	};

	OnItemClickListener clickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final JSONObject obj = (JSONObject) view.getTag();
			if (obj.optBoolean("ENABLED")) {
				currentJSON = obj;
				showDialog(DIALOG_KERNEL_INFO);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(KernelListActivity.this);
				builder.setMessage(
						"This kernel is not available as it requires a " + obj.optString(DEVICE) + " and Android Version " + obj.optString(ANDROID_VERSION) + ", while you have a "
								+ Device + " running Android version " + sdkVersion).setCancelable(true).setPositiveButton("Force Anyway", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						currentJSON = obj;
						showDialog(DIALOG_KERNEL_INFO);
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.setOwnerActivity(KernelListActivity.this);
				alert.show();
			}
		}
	};

	class MyTabFactory implements TabContentFactory {

		@Override
		public View createTabContent(String tag) {
			if (tag.equals("This Device")) {
				ListView lv = new ListView(KernelListActivity.this);
				lv.setAdapter(thisDeviceAdapter);
				lv.setOnItemClickListener(clickListener);
				return lv;
			}
			if (tag.equals("All Devices")) {
				ListView lv = new ListView(KernelListActivity.this);
				lv.setAdapter(allDeviceAdapter);
				lv.setOnItemClickListener(clickListener);
				return lv;
			}
			if (tag.equals("Backed-up Kernel")) {
				try {
					FileInputStream fis = new FileInputStream("/sdcard/zImage_backup_manifest");
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] buffer = new byte[4096];
					int read;
					while ((read = fis.read(buffer)) >= 0) {
						baos.write(buffer, 0, read);
					}
					fis.close();
					JSONObject obj = new JSONObject(new String(baos.toByteArray()));
					LinearLayout layout = new LinearLayout(KernelListActivity.this);
					layout.setOrientation(LinearLayout.VERTICAL);
					TextView tv = new TextView(KernelListActivity.this);
					tv.setText("Device: " + obj.optString(DEVICE));
					layout.addView(tv);
					tv = new TextView(KernelListActivity.this);
					tv.setText("Android Version: " + obj.optString(ANDROID_VERSION));
					layout.addView(tv);
					tv = new TextView(KernelListActivity.this);
					tv.setText("MD5: " + obj.optString(MD5));
					layout.addView(tv);

					Button button = new Button(KernelListActivity.this);
					button.setText("Restore backed up kernel");
					button.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							currentLagFix = new InstallBackupKernelLagFix();
							LagFixWorker.getBackgroundWorker().RunLagFix(currentLagFix, null, getApplicationContext());
							Intent intent = new Intent(KernelListActivity.this, ProgressViewActivity.class);
							startActivity(intent);
						}
					});
					layout.addView(button);

					return layout;
				} catch (Exception ex) {
					TextView tv = new TextView(KernelListActivity.this);
					tv.setText("You do not have a backed up kernel. Your kernel will be backed up when you flash a different one.");
					return tv;
				}
			}
			return null;
		}
	}
}

class KernelListAdapter extends BaseAdapter {

	JSONArray kernelList;
	Context context;

	public KernelListAdapter(Context context) {
		this.context = context;
	}

	@Override
	public int getCount() {
		return kernelList == null ? 0 : kernelList.length();
	}

	@Override
	public Object getItem(int position) {
		return kernelList.opt(position);
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

		JSONObject obj = kernelList.optJSONObject(position);

		DisplayName.setText(obj.optString(KernelListActivity.NAME));
		ShortDescription.setText(obj.optString(KernelListActivity.DESCRIPTION));
		if (obj.optBoolean("ENABLED")) {
			Enabled.setText("Available");
			convertView.setBackgroundResource(R.drawable.greenbar);
			DisplayName.setTextColor(0xFF00FF00);
		} else {
			Enabled.setText("Unavailable");
			convertView.setBackgroundResource(R.drawable.redbar);
			DisplayName.setTextColor(0xFFFF6666);
		}
		convertView.setTag(obj);

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
		return kernelList == null || kernelList.length() == 0;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	public void UpdateKernelList(JSONArray list) {
		kernelList = list;
		notifyDataSetChanged();
	}

}
