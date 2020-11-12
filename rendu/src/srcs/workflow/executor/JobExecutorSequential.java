package srcs.workflow.executor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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

/**
	La première implantation de JobExexutor sera la classe JobExecutorSequential qui permet
	d’exécuter un job séquentiellement sur la machine locale. Pour exécution séquentielle nous enten-
	dons une exécution monothreadée qui exécute les tâches séquentiellement en respectant l’ordre des
	dépendances.
 */
public class JobExecutorSequential extends JobExecutor {
	
	
	/**
	 * Exécute un job séquentiellement sur la machine locale.
	 * @param job
	 */
	public JobExecutorSequential(Job job) {
		super(job);
	}
	

	@Override
	public Map<String, Object> execute() throws Exception {
		
		/**
		 * Une méthode est exécutable lorsque tous les noeuds qui pointent vers elle sont terminés.
		 * La liste des méthodes terminées est accessible via le résultat.
		 */
		
		// Création de la Map retournée
		Map<String, Object> results = new HashMap<>();
		
		// Création du JobValidator, pour avoir le graphe de tâches et l'association ID tâche <-> méthode
		JobValidator validator = new JobValidator(job);
		
		// Graphe des tâches
		Graph<String> graph = validator.getTaskGraph();
		
		// Liste des tâches en attente d'exécution
		LinkedList<String> awaitingTasksID = new LinkedList<>();
		
		// Ajout des tâches qui attendent d'être exécutées
		for (String taskID : graph) {
			awaitingTasksID.add(taskID);
		}
		
		// Exécution l'une après l'autre des tâches qui peuvent s'exécuter.
		while ( ! awaitingTasksID.isEmpty()) {
			
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
				
				// Si la tâche n'est pas encore exécutable, je passe à la suivante
				if ( ! ready) continue;
				
				// La tâche est donc exécutable, je l'exécute
				Method method = validator.getMethod(taskID);
				
				// Liste des arguments de la méthode
				List<Object> args = new ArrayList<>();
				
				// J'ajoute les arguments : pour chaque paramètre, j'en regarde l'annotation
				for (Parameter param : method.getParameters()) {
					Annotation[] pannots = param.getAnnotations();
					for (Annotation a : pannots) {
						// Dépendance à une tâche
						if (a.annotationType().equals(LinkFrom.class)) {
							String ref = ((LinkFrom) a).value();
							Object taskObj = results.get(ref);
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
				
				// Appel de la méthode
				Object res = method.invoke(job, args.toArray());
				
				// Ajout du résultat
				results.put(taskID, res);
				
				// Suppression de la méthode de la liste des méthodes restant à exécuter
				awaitingTasksID.remove(taskID);
				
				// Break pour ne pas perturber l'itérateur sur awaitingTasksID
				break;
			}
		}
		return results;
	}
}








