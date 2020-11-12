package srcs.workflow.server.distributed;

import java.lang.reflect.Method;

import srcs.workflow.executor.JobExecutorParallelAbstract;
import srcs.workflow.job.Job;

/**
 * Exécution d'un job d'une manière distribuée sur plusieurs machines.</br>
 * 
 * JobExecutorParallelDistributed s'utilise comme tout JobExecutor,
 * sa méthode execute() est synchrone (=bloqaunte).</br></br>
 * 
 * En plus, lors de la création, il faut lui spécifier l'objet master (singleton).
 * Il y a une instance de JobExecutorParallelDistributed par Job en cours d'exécution sur Master.
 * Toutes ces instances sont sur la JVM de l'objet Master, jamais sur les trackers.</br></br>
 * 
 * JobExecutorParallelDistributed reprend le principe de JobExecutorParallel en modifiant uniquement
 * l'appel à la méthode réalisant la tâche. Lors de l'exécution d'une tâche, l'instance demande
 * au Master d'exécuter cette tâche sur un des trackers.</br></br>
 * 
 * JobExecutorParallelDistributed.execute() est géré par le Master dans un thread généré par RMI,
 * c'est pour ça qu'il est possible d'avoir plusieurs jobs exécutés simultanément sur Master.</br></br>
 * 
 * Améliorations possibles :</br>
 * - Le job est serialisé à chaque réalisation d'une tâche</br>
 * - Il n'y a pas de priorité dans le traîtement des tâches, ainsi, même si un job ancien a une plus
 *   grande probabilité d'avoir ses tâches exécutées (du fait de l'ordonnanceur de la JVM), rien ne 
 *   garantit parfaitement l'équité.
 * 
 */
public class JobExecutorParallelDistributed extends JobExecutorParallelAbstract {
	
	/** Référence vers l'objet Master. */
	protected final JobTrackerMaster master;
	
	/**
	 * Toute instance de JobExecutorParallelDistributed doit se trouver dans la même JVM que
	 * le Master.
	 * @param job  Le job à exécuter
	 * @param master  Une référence vers le singleton Master
	 */
	public JobExecutorParallelDistributed(Job job, JobTrackerMaster master) {
		super(job);
		this.master = master;
	}
	

	@Override
	protected Object executeMethod(Method method, Object[] args) throws Exception {
		return master.executeTaskOnFreeTracker(job, args, method.getName(), method.getParameterTypes());
		// remplace le method.invoke(job, args); de l'exo 4
	}
	
	
}
