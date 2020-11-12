package srcs.workflow.server.central;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

import srcs.workflow.executor.JobExecutor;
import srcs.workflow.executor.JobExecutorNotification;
import srcs.workflow.executor.JobExecutorParallel;
import srcs.workflow.job.Job;

import java.rmi.AlreadyBoundException;

/**
 * Déploie le service d'exécution de job.
 *
 */
public class JobTrackerCentral implements JobTrackerCentralInterface {
	
	public JobTrackerCentral() throws RemoteException {
		
	}
	
	/**
	 * Déploie le service d'exécution de job.
	 * @param args  n'attend pas d'argument
	 * @throws RemoteException 
	 * @throws InterruptedException 
	 * @throws AlreadyBoundException 
	 */
	public static void main(String[] args) throws InterruptedException, RemoteException, AlreadyBoundException {
		
		// Création du Registry. Il sera automatiquement détruit lorsque la VM s'arrêtera.
		LocateRegistry.createRegistry(1099);
		Thread.sleep(100);
		
		// Accès au registery
		final Registry registry = LocateRegistry.getRegistry();
		
		// Service d'exécution du job
		JobTrackerCentral tracker = new JobTrackerCentral();
		
		// Export de l'objet (sinon, il est serialisé)
		UnicastRemoteObject.exportObject(tracker, 0);
		
		// Je nomme mon objet exporté pour pouvoir le récupérer côté client
		registry.bind("JobTrackerCentralSingleton", tracker);
		
		/* Lors de la terminaison de la VM, le thread ayant créé le registry sera stoppé,
		 * fermant effectivement le registry. */
	}

	@Override
	public Map<String, Object> executeJob(Job job, JobExecutorNotification notificationMethod) throws RemoteException, Exception {
		// Lors de la réception d'un job, l'exécuter via JobExecutorParallel.
		JobExecutor j = new JobExecutorParallel(job);
		j.setNotificationMethod(notificationMethod);
		return j.execute();
	}
	
}
