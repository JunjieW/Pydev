package org.python.pydev.core.uiutils;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * This class overrides the ProgressMonitorDialog to limit the
 * number of task name changes in the GUI.
 * 
 * @author rickard
 */
public class AsynchronousProgressMonitorDialog extends ProgressMonitorDialog {
	
	private static int UPDATE_INTERVAL_MS = 300;
	private volatile Runnable updateStatus;
	private volatile String lastTaskName = null;
	
	/**
	 * Lock for assigning to updateStatus.
	 */
	private Object updateStatusLock = new Object();
	
	private IProgressMonitor progressMonitor = null;
	
	public AsynchronousProgressMonitorDialog(Shell parent) {
		super(parent);
	}
	
	private void scheduleTaskNameChange() {
		synchronized(updateStatusLock){
			if(updateStatus == null) {
				updateStatus = new Runnable() {
					public void run() {
						try {
							IProgressMonitor monitor = AsynchronousProgressMonitorDialog.super.getProgressMonitor();
							String s = lastTaskName;
							if(s != null) {
								monitor.setTaskName(s);
							}
						} finally {
							updateStatus = null;
						}
					}
				};
				Display disp = getContents().getDisplay();
				disp.timerExec(UPDATE_INTERVAL_MS, updateStatus);
			}
		}
	}
	
	
	@Override
	public IProgressMonitor getProgressMonitor() {
		if(progressMonitor == null) {
			final IProgressMonitor m = super.getProgressMonitor();
			progressMonitor = new IProgressMonitor() {				
				public void worked(int work) {
					m.worked(work);
				}
				
				public void subTask(String name) {
					m.subTask(name);
				}
				
				public void setTaskName(String name) {
					if(updateStatus == null) {
						scheduleTaskNameChange();
					}
					lastTaskName = name;
				}
				
				public void setCanceled(boolean value) {
					m.setCanceled(value);
				}
				
				public boolean isCanceled() {
					return m.isCanceled();
				}
				
				public void internalWorked(double work) {
					m.internalWorked(work);
				}
				
				public void done() {
					m.done();
				}
				
				public void beginTask(String name, int totalWork) {
					m.beginTask(name, totalWork);
				}
			};
		}
		return progressMonitor;
	}
	
	/**
	 * Test code below
	 */
	public static void main(String[] arg)
	{
		Shell shl = new Shell();
		ProgressMonitorDialog dlg = new AsynchronousProgressMonitorDialog(shl);
		
		long l = System.currentTimeMillis();
		try {
			dlg.run(true, true, new IRunnableWithProgress() {
				
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("Testing", 100000);
					for(long i=0; i<100000 && !monitor.isCanceled() ; i++) {
						//monitor.worked(1);
						monitor.setTaskName("Task " + i);
					}
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Took " + ((System.currentTimeMillis()-l)));
	}
}
