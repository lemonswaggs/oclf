package com.rc.QuickFixLagFix.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;

public abstract class LagFix {
	
	public final static String INITIALIZING = "LagFix still initializing";
	public final static String SUCCESS = "S";
	public final static String ENABLED = "E";
	String Status = "UNKNOWN";
	List<LogRow> statusLog = new ArrayList<LogRow>();
	public WeakReference<StatusListener> statusListenerWR;
	
	public boolean EnabledStatus = false;
	public String DisabledReason = INITIALIZING;

	public abstract String GetDisplayName();
	public abstract String GetShortDescription();
	public abstract String GetLongDescription();
	public abstract String GetFeedbackLogEmailAddress();
	
	public String GetStatus() {
		return Status;
	}
	
	public List<LogRow> getStatusLog() {
		return statusLog;
	};
	
	public final void UpdateStatus(String Status) {
		Log.i("OCLF", Status);
		this.Status = Status;
		statusLog.add(new LogRow(Status));
		StatusListener statusListener = statusListenerWR.get();
		if (statusListener != null) {
			statusListener.UpdateStatusForLagFix(this, Status);
		}
	}
	
	public final void SetStatusUpdateListener(StatusListener listener) {
		statusListenerWR = new WeakReference<StatusListener>(listener);
	}
	
	public abstract String IsEnabled(Context ApplicationContext) throws Exception, Error;
	public abstract String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error;
	public abstract void GetOptions(OptionListener listener) throws Exception, Error;
	
	public static String GetTextFromStream(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] bytebuf = new byte[1024];
		int read;
		while ((read = is.read(bytebuf)) >= 0) {
			baos.write(bytebuf, 0, read);
		}
		
		return new String(baos.toByteArray());
	}
	
	public final boolean getEnabledStatus() {
		return EnabledStatus;
	}
	public final String getDisabledReason() {
		return DisabledReason;
	}
	public final void doEnabledTest(Context ApplicationContext) {
		try {
			String EnabledResult = IsEnabled(ApplicationContext);
			EnabledStatus = LagFix.ENABLED.equals(EnabledResult);
			DisabledReason = EnabledResult;
		} catch (Exception ex) {
			EnabledStatus = false;
			DisabledReason = ex.getLocalizedMessage();
		} catch (Error ex) {
			EnabledStatus = false;
			DisabledReason = ex.getLocalizedMessage();			
		}
	}
	
	public class LogRow {
		public String LogMessage;
		public Date LogTime;
		
		public LogRow(String LogMessage) {
			this.LogMessage = LogMessage;
			LogTime = new Date();
		}
	}
}
