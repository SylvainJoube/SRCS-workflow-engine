package srcs.workflow.server.distributed;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import srcs.workflow.job.Job;

public interface JobTrackerMasterInterface extends Remote {
	
	/**
	 * Cette méthode est appelée par JobExecutorRemoteDistributed,
	 * c'est donc une exécution demandée par le proxy d'une machine cliente
	 * au JobTrackerMaster du Maître. </br>
	 * Cette exécution est donc à partir d'un thread propre et est bloquante.
	 * Au passage, le thread ressemble un peu à ça : Thread[RMI TCP Connection(n°connexion)-127.0.0.1,5,RMI Runtime]
	 * @param job
	 * @return
	 * @throws RemoteException
	 * @throws Exception
	 */
	public Map<String, Object> executeJob(Job job) throws RemoteException, Exception;
	
	/** Récupérer un nom de tracker unique dans un objet JobTrackerMaster. */
	public String getUniqueTrackerName() throws RemoteException;

	/** Récupérer un nombre aléatoire simulant la capacité d'un tracker. */
	public int getRandomTrackerCapacity() throws RemoteException;
	
	/** Ajout d'un nouveau tracker (=esclave) à la JVM maître (JobTrackerMaster). */
	public void registerTracker(String name) throws RemoteException, NotBoundException;
	
}
