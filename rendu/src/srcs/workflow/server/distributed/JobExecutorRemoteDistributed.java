package srcs.workflow.server.distributed;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

import srcs.workflow.executor.JobExecutor;
import srcs.workflow.job.Job;

/**
 * La classe cliente sera une implantation de JobExecutor nommée JobExecutorRemoteDistributed.
 *
 */
public class JobExecutorRemoteDistributed extends JobExecutor {

	public JobExecutorRemoteDistributed(Job job) {
		super(job);
	}

	/**
	 * L’appel à cette méthode exécute le job et renvoie une map qui associe pour chaque tâche son résultat.
	 * Cette méthode délègue l'exécution au Master.
	 * L'exécution est synchrone, comme pour tout JobExecutor.
	 * @return liste des associations entre id d'une tâche et résultat de cette tâche.
	 * @throws Exception  si le job a levé une exception (autre que RemoteException, bien entendu)
	 */
	@Override
	public Map<String, Object> execute() throws Exception {
		
		// Accès au registery
		final Registry registry = LocateRegistry.getRegistry();
		
		// Je me connecte au Master, il va répartir les tâches
		JobTrackerMasterInterface master =  (JobTrackerMasterInterface) registry.lookup("Alexander the Great");
		
		// L'exécution depuis le master
		// le job ne pouvant pas être exporté, il est serializé (donc une copie en est envoyé au Master)
		return master.executeJob(job);
	}

}
