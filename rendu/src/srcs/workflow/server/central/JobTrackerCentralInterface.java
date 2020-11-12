package srcs.workflow.server.central;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import srcs.workflow.executor.JobExecutorNotification;
import srcs.workflow.job.Job;

/**
 * Permet l'exportation et la récupération d'un JobTrackerCentral via RMI.
 *
 */
public interface JobTrackerCentralInterface extends Remote {
	
	/**
	 * Exécute un job.
	 * @param job  job à exécuter. Doit être Serializable s'il est exécuté à distance via RMI.
	 * @param notificationMethod  objet dont la méthode taskFinished sera appelée à chaque fin de tâche du job.
	 * @return  un Map associant l'ID de la tâche avec le résultat retourné.
	 * @throws Exception
	 * @throws RemoteException
	 */
	public Map<String, Object> executeJob(Job job, JobExecutorNotification notificationMethod) throws Exception, RemoteException;
	
}
