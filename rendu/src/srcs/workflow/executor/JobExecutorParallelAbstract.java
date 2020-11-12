package srcs.workflow.executor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import srcs.workflow.graph.Graph;
import srcs.workflow.job.Context;
import srcs.workflow.job.Job;
import srcs.workflow.job.JobValidator;
import srcs.workflow.job.LinkFrom;
import srcs.workflow.job.ValidationException;

/**
 * Exécute un job d'une manière parallèle.
 * La seule méthode à redéfinir est executeMethod,
 * elle est responsable de l'exécution d'une tâche du job,
 * toutes les dépendances ont été vérifiées et les arguments sont
 * prêts.
 * 
 * Cette classe est la classe parente de JobExecutorParallel (exo 4)
 * qui s'occupe de l'exécution multi-threadée en local,
 * et aussi de JobExecutorParallelDistributed (exo 6)
 * qui s'occupe de l'exécution sur plusieurs machines distantes.
 */
public abstract class JobExecutorParallelAbstract extends JobExecutor {
	
	/**
	 * Exécution (synchrone) d'une tâche sur un tracker libre. Cette méthode est appelée dans un thread
	 * géré dans la méthode execute().
	 * 
	 * <p>Méthode exécutée lorsqu'une tâche est prête.
	 * Pour l'exo4, c'est simplement method.invoke.
	 * Pour l'exo6, trouve un tracker libre et execute la méthode dessus (via RMI).
	 * @param method
	 * @param args
	 * @return
	 * @throws Exception
	 */
	abstract protected Object executeMethod(Method method, Object[] args) throws Exception;
	
	public JobExecutorParallelAbstract(Job job) {
		super(job);
	}
	
	// Résultats retournés par la fonction execute()
	protected Map<String, Object> results;
	
	// JobValidator créé à partir du job fourni
	protected JobValidator validator;
	
	// Graph construit par le JobValidator
	protected Graph<String> graph;
	
	// Liste de tâches en attente d'être exécutées (initialement : toutes)
	protected List<String> awaitingTasksID;
	
	/* Lock pour protéger les variables results et awaitingTasksID des accès concurrents */
	protected final Object lock = new Object();

	// Exceptions éventuellement jetées lors de l'exécution dans un thread
	protected ArrayList<Exception> exceptions = new ArrayList<>();
	
	/**
	 * Initialisation de l'exécution
	 * @throws ValidationException
	 */
	protected void initExecute() throws ValidationException {
		
		// Création de la Map retournée
		results = new HashMap<>();
		
		// Création du JobValidator, pour avoir le graphe de tâches et l'association ID tâche <-> méthode
		validator = new JobValidator(job);
		
		// Graphe des tâches
		graph = validator.getTaskGraph();
		
		// Liste des tâches en attente d'exécution
		awaitingTasksID = new LinkedList<>();
		
		// Ajout des tâches qui attendent d'être exécutées
		for (String taskID : graph) {
			awaitingTasksID.add(taskID);
		}
	}
	
	/**
	 * Récupérer un ID de tâche exécutable, c'est à dire une tâche dont toutes les
	 * dépendances sont satisfaites. Retourne null s'il n'y a plus aucune tâche
	 * à exécuter, ou s'il y a eu une exception dans un des threads. </br>
	 * S'il reste des tâches à exécuter mais qu'aucune n'est actuellement exécutable,
	 * bloque et attend qu'une tache soit exécutable.
	 * @return  l'ID d'une tâche exécutable, ou null s'il n'y a plus de tâche à exécuter
	 *          ou qu'une exception a été jetée dans un des threads.
	 * @throws InterruptedException
	 */
	protected String getReadyTaskID() throws InterruptedException {
		
		synchronized(lock) {
			
			// J'essaie de trouver une tâche prête, je m'endors s'il en reste au moins
			// une en attente mais non exécutable.
			while ( ! awaitingTasksID.isEmpty()) {
				
				// Exception levée dans un des threads, système devenu instable
				// arrêt et renvoi d'une exception via execute().
				if ( ! exceptions.isEmpty()) return null;
				
				// Pour toutes les tâches restantes...
				for (String taskID : awaitingTasksID) {
					
					// ...je regarde si toutes les tâches qui pointent vers cette tâche sont terminées
					boolean ready = true;
					List<String> neededTasks = graph.getNeighborsIn(taskID);
					for (String ntask : neededTasks) {
						if ( ! results.containsKey(ntask)) {
							// La tâche n'est pas terminée, le résultat n'est pas encore disponible
							ready = false;
							break;
						}
					}
					
					// Si la tâche est exécutable, je la renvoie.
					if (ready) {
						// La tâche n'est plus en attente
						// Suppression de la méthode de la liste des méthodes restant à exécuter
						awaitingTasksID.remove(taskID);
						return taskID;
					}
				}
				
				// Il reste au moins une tâche à exécuter, j'attends que la situation se débloque
				lock.wait();
			}
		}
		
		// null signifie qu'il n'y a plus de tâche en attente d'exécution.
		return null;
	}
	
	/**
	 * Renvoie les arguments de la méthode associée à une tâche.
	 * La tâche associée à la méthode passée en paramètre doit exister.
	 * Cette fonction va chercher les arguments dans les objets
	 * du contexte et dans les dépendances aux autres tâches.
	 * @param method  méthode associée
	 * @return
	 */
	protected Object[] prepareTaskArgs(Method method) {
		// Liste des arguments de la méthode
		List<Object> args = new ArrayList<>();
		
		// J'ajoute les arguments : pour chaque paramètre, j'en regarde l'annotation
		for (Parameter param : method.getParameters()) {
			Annotation[] pannots = param.getAnnotations();
			for (Annotation a : pannots) {
				
				// Dépendance à une tâche
				if (a.annotationType().equals(LinkFrom.class)) {
					String ref = ((LinkFrom) a).value();
					// Récupération (thread-safe) de la valeur de retour d'une autre tâche
					Object taskObj;
					synchronized(lock) {
						taskObj = results.get(ref);
					}
					args.add(taskObj);
				}
				
				// Dépendance à un objet du contexte
				if (a.annotationType().equals(Context.class)) {
					String ref = ((Context) a).value();
					Object contextObj = job.getContext().get(ref);
					args.add(contextObj);
				}
			}
		}
		return args.toArray();
	}
	
	// Voir le compte rendu pour de plus amples explications (exercice 4)
	@Override
	public Map<String, Object> execute() throws Exception {
		
		// Initialisation de l'exécution
		initExecute();
		
		String taskID;
		
		// Threads qui vont s'occuper de l'exécution des tâches :
		// Pour l'exo 4, c'est eux qui vont exécuter les tâches,
		// pour l'exo 6 c'est un tracker qui va exécuter la tâche (en
		// synchrone, d'où la nécessité d'avoir un thread aussi)
		List<Thread> threads = new ArrayList<>();
		
		// Tant qu'il y a une tâche à exécuter, je l'exécute dans un thread séparé
		while ((taskID = getReadyTaskID()) != null) {
			// Une tâche prête le reste à jamais, taskID ne peut pas ne plus être prête ici.
			
			// tID sert à passer au thread l'ID de la tâche à exécuter
			final String tID = taskID;
			
			Thread th = new Thread( () -> {
				
				// validator est thread-safe car immuable (i.e. lecture seule).
				Method method = validator.getMethod(tID);
				
				Object args[] = prepareTaskArgs(method);
				
				Object res;
				try {
					// Appel de la méthode
					res = executeMethod(method, args);
					//res = method.invoke(job, args); exo 4
				} catch (Exception e) {
					synchronized (lock) {
						exceptions.add(e);
					}
					return;
				}
				synchronized(lock) {
					// Ajout du résultat, la tâche avait déjà été enlevée de la liste
					// des tâches en attente.
					results.put(tID, res);
					// (exo 5) S'il faut notifier via une méthode, j'appelle la méthode.
					if (notificationMethod != null) {
						try {
							notificationMethod.taskFinished(tID);
						} catch (RemoteException e) { }
					}
					lock.notifyAll(); // (notify() aurait suffit)
				}
			});
			
			// Démarrage de l'exécution de la tâche
			th.start();
			
			// Ajout du thread à attendre
			threads.add(th);
		}
		
		// Attente de la fin de l'exécution de toutes les tâches
		for (Thread th : threads) {
			th.join();
		}
		
		// S'il y a eu une exception dans un thread, je la renvoie ici.
		if (! exceptions.isEmpty()) {
			throw exceptions.get(0);
		}
		
		return results;
	}
	
}
