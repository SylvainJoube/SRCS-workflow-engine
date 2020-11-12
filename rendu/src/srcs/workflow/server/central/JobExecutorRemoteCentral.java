package srcs.workflow.server.central;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import srcs.workflow.executor.JobExecutor;
import srcs.workflow.executor.JobExecutorNotification;
import srcs.workflow.job.Job;

/**
 * Code client du serveur.
 *
 */
public class JobExecutorRemoteCentral extends JobExecutor implements JobExecutorNotification {

	public JobExecutorRemoteCentral(Job job) {
		super(job);
	}
	
	// Permet de compter le nombre total de tâches terminées
	protected AtomicInteger finishedTaskCount = new AtomicInteger(0);
	
	
	@Override
	public Map<String, Object> execute() throws RemoteException, Exception {
		
		// Je récupère le registry
		Registry registry = LocateRegistry.getRegistry("localhost");
		
		// Je récupère le tracker exporté dans ce registry
		JobTrackerCentralInterface tracker = (JobTrackerCentralInterface) registry.lookup("JobTrackerCentralSingleton");
		
		/* J'exporte cet objet JobExecutorRemoteCentral
		 * pour pouvoir appeler taskFinished() à chaque tâche terminée.
		 * (il serait serializé sinon) */
		UnicastRemoteObject.exportObject(this, 0);
		
		// Exécution du job à distance
		Map<String,Object> result = tracker.executeJob(job, this);
		
		// Je n'ai plus besoin d'avoir mon objet exporté
		UnicastRemoteObject.unexportObject(this, true);
		
		return result;
	}
	
	@Override
	public void taskFinished(String taskID) throws RemoteException {
		System.out.println("" + finishedTaskCount.addAndGet(1));
	}
}

