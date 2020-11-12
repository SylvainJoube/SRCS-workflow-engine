package srcs.workflow.executor;

import java.lang.reflect.Method;

import srcs.workflow.job.Job;

/**
 * J'ai fait une classe abstraite JobExecutorParallelAbstract pour avoir du code en commun
 * entre l'exo 4 (parallèle local) et l'exo 6 (parallèle distribué avec maître)
 */
public class JobExecutorParallel extends JobExecutorParallelAbstract {
	
	public JobExecutorParallel(Job job) {
		super(job);
	}
	
	// Ici, l'exécution des tâches est sur la machine locale
	// (dans un thread séparé géré par JobExecutorParallelAbstract.execute())
	@Override
	public Object executeMethod(Method method, Object[] args) throws Exception {
		return method.invoke(job, args);
	}
	
}
