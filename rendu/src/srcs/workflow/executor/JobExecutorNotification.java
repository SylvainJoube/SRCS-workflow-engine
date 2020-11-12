package srcs.workflow.executor;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Permet d'appeler une méthode d'un objet exporté via RMI
 * lorsqu'une tâche est terminée.
 */
public interface JobExecutorNotification extends Remote {
	
	/**
	 * Méthode appelée lors de la terminaison d'une tâche.
	 * @param taskID
	 * @throws RemoteException
	 */
	public void taskFinished(String taskID) throws RemoteException;
}
