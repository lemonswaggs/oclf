package com.rc.QuickFixLagFix.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.PowerManager;

import com.rc.QuickFixLagFix.LagFixes.CaptivateJupterFix;
import com.rc.QuickFixLagFix.LagFixes.ChangeSchedulerLagFix;
import com.rc.QuickFixLagFix.LagFixes.CheckFreeSpaceLagFix;
import com.rc.QuickFixLagFix.LagFixes.EXT2ToolsLagFix;
import com.rc.QuickFixLagFix.LagFixes.InstallFontLagFix;
import com.rc.QuickFixLagFix.LagFixes.OneClickLagFixV1PLUS;
import com.rc.QuickFixLagFix.LagFixes.RemoveEXT2ToolsLagFix;
import com.rc.QuickFixLagFix.LagFixes.RestorePlaylogos1;
import com.rc.QuickFixLagFix.LagFixes.RootLagFix;
import com.rc.QuickFixLagFix.LagFixes.UnRootLagFix;
import com.rc.QuickFixLagFix.LagFixes.UndoOneClickLagFixV1PLUS;

public class LagFixWorker {

	private static LagFixWorker WorkerInstance = new LagFixWorker();
	
	static {
		WorkerInstance.RegisterLagFix(new RootLagFix());
		//WorkerInstance.RegisterLagFix(new UndoOneClickLagFixV1());
		WorkerInstance.RegisterLagFix(new EXT2ToolsLagFix());
		//WorkerInstance.RegisterLagFix(new OneClickLagFixV1_3());
		WorkerInstance.RegisterLagFix(new CheckFreeSpaceLagFix());
		WorkerInstance.RegisterLagFix(new OneClickLagFixV1PLUS());
		WorkerInstance.RegisterLagFix(new UndoOneClickLagFixV1PLUS());
		WorkerInstance.RegisterLagFix(new UnRootLagFix());
		WorkerInstance.RegisterLagFix(new CaptivateJupterFix());
		WorkerInstance.RegisterLagFix(new RestorePlaylogos1());
		WorkerInstance.RegisterLagFix(new RemoveEXT2ToolsLagFix());
		WorkerInstance.RegisterLagFix(new ChangeSchedulerLagFix());
		WorkerInstance.RegisterLagFix(new InstallFontLagFix());
		//WorkerInstance.RegisterLagFix(new TestLagFix());
	}

	public static LagFixWorker getBackgroundWorker() {
		return WorkerInstance;
	}

	public String LastLagFixResult = null;
	public boolean wasLastLagFixSuccess = false;
	List<LagFix> LagFixList = new ArrayList<LagFix>();
	public LagFixRunnable CurrentLagFixRunnable;
	public LagFixChecker CurrentLagFixChecker;
	private long LastLagFixCheckTime = 0;
	public boolean LagFixCheckRunning = false;

	public List<LagFix> getLagFixList() {
		return LagFixList;
	}
	
	public List<LagFix> getEnabledList(int EnabledStatus) {
		List<LagFix> list = new ArrayList<LagFix>();
		for (LagFix fix : LagFixList) {
			if (fix.getEnabledStatus() == EnabledStatus) {
				list.add(fix);
			}				
		}
		return list;
	}

	public void RegisterLagFix(LagFix lf) {
		LagFixList.add(lf);
	}

	public synchronized void RunLagFix(LagFix lagFix, Map<String, String> optionsMap, Context ApplicationContext) throws IllegalStateException {
		if (CurrentLagFixRunnable != null)
			throw new IllegalStateException(CurrentLagFixRunnable.CurrentLagFix.GetDisplayName() + " is already running!");
		CurrentLagFixRunnable = new LagFixRunnable(lagFix, optionsMap, ApplicationContext);
		CurrentLagFixRunnable.start();		
	}
	
	public synchronized void doLagFixChecks(Context ApplicationContext) throws IllegalStateException {
		if (System.currentTimeMillis()-LastLagFixCheckTime<120000)
			return;
		if (CurrentLagFixChecker != null)
			return;
		CurrentLagFixChecker = new LagFixChecker(WorkerInstance, ApplicationContext);
		CurrentLagFixChecker.start();
		LastLagFixCheckTime = System.currentTimeMillis();
		LagFixCheckRunning = true;
	}

	public void SetStatusListener(StatusListener statusListener) {
		for (LagFix lf : LagFixList) {
			lf.SetStatusUpdateListener(statusListener);
		}
	}

	public class LagFixRunnable extends Thread {

		public LagFix CurrentLagFix;
		Map<String, String> optionsMap;
		Context ApplicationContext;

		public LagFixRunnable(LagFix CurrentLagFix, Map<String, String> optionsMap, Context ApplicationContext) {
			this.CurrentLagFix = CurrentLagFix;
			this.optionsMap = optionsMap;
			this.ApplicationContext = ApplicationContext;
		}

		@Override
		public void run() {
		    PowerManager pm = (PowerManager) ApplicationContext.getSystemService(Context.POWER_SERVICE);
		    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "OCLF");
		    wl.acquire();

			String result = null;
			try {
				CurrentLagFix.getStatusLog().clear();
				if (!CurrentLagFix.CanForce()) {
					result = CurrentLagFix.IsEnabled(ApplicationContext);
					if (!result.equals(LagFix.ENABLED))
						throw new Exception("Lagfix failed test: "+result);
				}
				VirtualTerminal vt = null;
				try {
					vt = new VirtualTerminal();
				} catch (Exception ex) {}
				try {
					result = CurrentLagFix.Run(optionsMap, ApplicationContext, vt);
				} catch (BrokenPipeException bpe) {
					// Broken pipe exception means we should try it again.
					vt = null;
					Thread.sleep(2500);
					try {
						vt = new VirtualTerminal();
					} catch (Exception ex2) {}
					result = CurrentLagFix.Run(optionsMap, ApplicationContext, vt);
				}
			} catch (Exception ex) {
				result = ex.getLocalizedMessage();
			} catch (Error ex) {
				result = ex.getLocalizedMessage();
			} finally {
				LastLagFixCheckTime = 0;
				boolean success;
				if (LagFix.SUCCESS.equals(result)) {
					result = CurrentLagFix.GetDisplayName() + " finished successfully.";
					success = true;
				} else {
					result = CurrentLagFix.GetDisplayName() + " failed with error: "+result;
					success = false;
				}
				StatusListener statusListener = CurrentLagFix.statusListenerWR.get();
				CurrentLagFix.UpdateStatus(result);
				if (statusListener != null) {
					statusListener.LagFixFinishedWithMessage(CurrentLagFix, result, success);					
				}
				CurrentLagFixRunnable = null;
				LastLagFixResult = result;
				wasLastLagFixSuccess = success;
				wl.release();
			}
		}
	}

	public LagFix getLagFix(int lfIndex) {
		return LagFixList.get(lfIndex);
	}

	public int getIndexOfLagFix(LagFix lf) {
		return LagFixList.indexOf(lf);
	}

}

class LagFixChecker extends Thread {

	LagFixWorker workerInstance;
	Context ApplicationContext;

	public LagFixChecker(LagFixWorker workerInstance, Context ApplicationContext) {
		this.workerInstance = workerInstance;
		this.ApplicationContext = ApplicationContext;
	}

	@Override
	public void run() {
		for (LagFix lagFix : workerInstance.LagFixList) {
			lagFix.EnabledStatus = LagFix.LAGFIX_UNKNOWN;
			lagFix.DisabledReason = LagFix.INITIALIZING;
		}
		for (int i=0;i<workerInstance.LagFixList.size();i++) {
			LagFix lagFix = workerInstance.LagFixList.get(i);
			StatusListener listener = lagFix.statusListenerWR.get();
			if (listener != null)
				listener.Update(i*10000/workerInstance.LagFixList.size());			
			lagFix.doEnabledTest(ApplicationContext);			
			if (listener != null)
				listener.Update((i+1)*10000/workerInstance.LagFixList.size());
		}		

		workerInstance.CurrentLagFixChecker = null;
		workerInstance.LagFixCheckRunning = false;
	}

}
