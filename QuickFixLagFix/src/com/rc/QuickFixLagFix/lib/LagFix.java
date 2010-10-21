package com.rc.QuickFixLagFix.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.rc.QuickFixLagFix.LagFixOptions.LagFixOption;

import android.content.Context;
import android.util.Log;

public abstract class LagFix {
	
	public final static String INITIALIZING = "LagFix still initializing";
	public final static String SUCCESS = "S";
	public final static String ENABLED = "E";
	LogRow Status = new LogRow("UNKNOWN");
	List<LogRow> statusLog = new ArrayList<LogRow>();
	public WeakReference<StatusListener> statusListenerWR;
	
	public final static int LAGFIX_ENABLED = 2;
	public final static int LAGFIX_DISABLED = 1;
	public final static int LAGFIX_UNKNOWN = 0;
	
	public int EnabledStatus = LAGFIX_UNKNOWN;
	public String DisabledReason = INITIALIZING;

	public abstract String GetDisplayName();
	public abstract String GetShortDescription();
	public abstract String GetLongDescription();
	public abstract String GetFeedbackLogEmailAddress();
	public boolean CanForce() {
		return false;
	}
	
	public LogRow GetStatus() {
		return Status;
	}
	
	public List<LogRow> getStatusLog() {
		return statusLog;
	};
	
	public final LogRow UpdateStatus(String Status) {
		LogRow lr = new LogRow(Status);
		Log.i("OCLF", Status);
		this.Status = lr;
		statusLog.add(lr);
		StatusListener statusListener = statusListenerWR == null ? null : statusListenerWR.get();
		if (statusListener != null) {
			statusListener.UpdateStatusForLagFix(this, lr);
		}
		return lr;
	}
	
	public final void SetStatusUpdateListener(StatusListener listener) {
		statusListenerWR = new WeakReference<StatusListener>(listener);
	}
	
	public abstract String IsEnabled(Context ApplicationContext) throws Exception, Error;
	public abstract String Run(Map<String, String> options, Context ApplicationContext, VirtualTerminal vt) throws Exception, Error;
	protected abstract List<LagFixOption> GetOptions() throws Exception, Error;
	
	public void RunGetOptions(OptionListener listener) {
		try {
			List<LagFixOption> lagFixOptions = GetOptions();
			listener.LagFixOptionListCompleted(lagFixOptions);
		} catch (Exception ex) {
			listener.LagFixOptionListFailed(ex);						
		}		
	}
	
	public static String GetTextFromStream(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] bytebuf = new byte[1024];
		int read;
		while ((read = is.read(bytebuf)) >= 0) {
			baos.write(bytebuf, 0, read);
		}
		
		return new String(baos.toByteArray());
	}
	
	public final int getEnabledStatus() {
		return EnabledStatus;
	}
	public final String getDisabledReason() {
		return DisabledReason;
	}
	public final void doEnabledTest(Context ApplicationContext) {
		try {
			String EnabledResult = IsEnabled(ApplicationContext);
			EnabledStatus = LagFix.ENABLED.equals(EnabledResult) ? LAGFIX_ENABLED : LAGFIX_DISABLED;
			DisabledReason = EnabledResult;
		} catch (Exception ex) {
			EnabledStatus = LAGFIX_DISABLED;
			DisabledReason = ex.getLocalizedMessage();
		} catch (Error ex) {
			EnabledStatus = LAGFIX_DISABLED;
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
