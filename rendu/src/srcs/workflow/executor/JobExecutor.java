package srcs.workflow.executor;

import java.util.Map;

import srcs.workflow.job.Job;

/**
 * Une classe abstraite JobExecutor possède un attribut de type Job (renseigné lors
 * de l’appel au constructeur) et offre la méthode abstraite execute().
 * 
 */
public abstract class JobExecutor {
	
	protected final Job job;
	
	/** (exo 5) permet d'appeler une méthode lorsqu'une tâche est terminée. */
	protected JobExecutorNotification notificationMethod;
	
	public JobExecutor(Job job) {
		this.job = job;
	}
	
	/**
	 * (exo 5) permet d'appeler une méthode lorsqu'une tâche est terminée.
	 * @param notif  l'objet duquel appeler la méthode taskFinished(String taskID)
	 */
	public void setNotificationMethod(JobExecutorNotification notif) {
		notificationMethod = notif;
	}
	
	/** 
	 * L’appel à cette méthode exécute le job et renvoie une map qui associe pour chaque
	 * tâche son résultat.
	 * @return liste des associations entre id d'une tâche et résultat de cette tâche.
	 * @throws Exception   si le job a levé une exception
	 */
	public abstract Map<String,Object> execute() throws Exception;
	
}
