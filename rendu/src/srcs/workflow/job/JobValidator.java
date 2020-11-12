package srcs.workflow.job;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import srcs.workflow.graph.Graph;
import srcs.workflow.graph.GraphImpl;

public class JobValidator {
	
	protected final Job job;
	protected Graph<String> taskGraph;
	
	/**
	 * Map des tâches (valeurs renseignées dans @Task("value") + la méthode associée)
	 * J'ai trouvé ça beaucoup plus simle de faire une Map plutôt que deux List, pour directement avoir
	 * le paramètre de l'annotation @Task associé à la mathode courante.
	 */
	protected Map<String, Method> taskMethods = new HashMap<>();
	
	/** 
	 * Constructeur qui prend un Job en paramètre et qui jette une ValidationException (classe
	 * à définir) si le job n’est pas conforme. La construction d’un JobValidator ne peut donc aboutir
	 * si le job passé en paramètre n’est pas conforme. */
	public JobValidator(Job job) throws ValidationException {
		this.job = job;
		taskGraph = new GraphImpl<String>();
		
		// vérification de la conformité du job, throw si problème :
		checkJobValidity();
	}
	
	/** 
	 * Renvoie le graphe de tâches correspondant
	 * au job. Dans le graphe les tâches sont référencées par leur identifiant. */
	public Graph<String> getTaskGraph() {
		return taskGraph;
	}
	
	/**
	 * Pour un identifiant de tâche donné, renvoie
	 * sa méthode dans la classe d’implantation du job. */
	public Method getMethod(String id) throws IllegalArgumentException {
		Method m = taskMethods.get(id);
		if (m == null) {
			throw new IllegalArgumentException("La tâche " + id + " n'existe pas.");
		}
		return m;
	}
	
	/** renvoie le job associé et validé. */
	public Job getJob() {
		return job;
	}
	
	/** 
	 * Vérifie que le Job job est bien conforme.
	 * Conditions :
		- il existe au moins une méthode annotée Task
		- les méthodes annotées Task doivent être des méthodes d’instance
		- les méthodes annotées Task ne doivent pas renvoyer void
		- deux méthodes annotées Task ne doivent pas avoir le même identifiant de tâche
		- tout paramètre d’une méthode annotée Task doit être soit annoté par Context ou par LinkFrom
		- toute annotation LinkFrom doit référencer une tâche existante (vérifié à la fin, quand on a toutes les tâches)
		- toute annotation Context doit référencer un objet existant dans le contexte du job
		- il doit y avoir une compatibilité de type entre un paramètre annoté LinkFrom et le retour de
		la méthode correspondante.
		- il doit y avoir une compatibilité de type entre un paramètre annoté Context et l’objet corres-
		pondant dans le contexte
		- le graphe de tâches doit être acyclique
	 */
	protected void checkJobValidity() throws ValidationException {
		Method[] methods = job.getClass().getDeclaredMethods();
		
		// Ajout de toutes les méthodes annotées @Task à taskMethods.
		for (Method method : methods) {
			Annotation[] annotations = method.getAnnotations();
			if (annotations.length == 0) continue;
			
			for (Annotation annotation : annotations) {
				if (annotation.annotationType().equals(Task.class)) {
					// Deux méthodes annotées Task ne doivent pas avoir le même identifiant de tâche
					Task task = (Task) annotation;
					if (taskMethods.containsKey(task.value())) {
						throw new ValidationException("Les méthodes annotées via @Task doivent avoir un identifiant unique.");
					}
					taskMethods.put(task.value(), method);
					taskGraph.addNode(task.value());
					// Au passage, une méthode ne peut être annotée @Task qu'une seule fois.
				}
			}
		}

		if (taskMethods.isEmpty()) throw new ValidationException("La classe n'a aucune méthode annotée @Task.");
		
		// Pour chaque couple (nom annotation @Task), (méthode associée)
		for (Map.Entry<String, Method> methodEntry : taskMethods.entrySet()) {
			Method method = methodEntry.getValue();
			String taskName = methodEntry.getKey(); // /!\ taskName n'est en général pas égal au nom de la méthode !
			
			// Les méthodes annotées Task doivent être des méthodes d’instance
			if ((method.getModifiers() & Modifier.ABSTRACT) != 0) {
				throw new ValidationException("Les méthodes annotées via @Task ne peuvent pas être abstraites."
							+ " (Elles doivent être des méthodes concrètes d'instance)");
			}
			if ((method.getModifiers() & Modifier.STATIC) != 0) {
				throw new ValidationException("Les méthodes annotées via @Task ne peuvent pas être statiques."
						+ " (Elles doivent être des méthodes concrètes d'instance)");
			}
			
			// Les méthodes annotées Task ne doivent pas renvoyer void
			if (method.getReturnType().equals(void.class)) {
				throw new ValidationException("Les méthodes annotées via @Task ne peuvent renvoyer void."
						+ " (Elles doivent renvoyer un résultat réel.)");
			}
			
			// Tout paramètre d’une méthode annotée Task doit être soit annoté par Context ou par LinkFrom
			for (Parameter param : method.getParameters()) {
				Annotation[] pannots = param.getAnnotations();
				
				boolean valid = false;
				
				for (Annotation a : pannots) {
					if (a.annotationType().equals(Context.class)
					||  a.annotationType().equals(LinkFrom.class)) {
						// Si déjà "valid", c'est que le paramètre a été annoté @Context ET @LinkFrom, c'est impossible.
						if (valid) throw new ValidationException("Tout paramètre d'une méthode annotée via @Task"
								+ "doit être annoté @Context OU (exclusif) @LinkFrom.");
						valid = true;
					}
					
					if (a.annotationType().equals(LinkFrom.class)) {
						LinkFrom from = (LinkFrom) a;
						
						Method previousTaskMethod = taskMethods.get(from.value());
						
						// Toute annotation LinkFrom doit référencer une tâche existante
						if (previousTaskMethod == null) { //( ! taskMethods.containsKey(lf.value())) {
							throw new ValidationException("La tâche " + taskName + " fait référence la tâche "
									+ from.value() + " qui n'a pas été déclarée dans cette classe.");
						}
						
						taskGraph.addEdge(from.value(), taskName);
						
						/* Il doit y avoir une compatibilité de type entre un paramètre annoté LinkFrom et le retour de
						 * la méthode correspondante. 
						 * i.e la valeur de retour de la méthode doit être un sous-type de la valeur attendue. */
						Class<?> expected = param.getType();
						Class<?> given = previousTaskMethod.getReturnType();
						
						// Attention, Byte ne peut pas être casté en Integer par exemple, mais Byte ou Integer -> Number sans souci.
						if ( ! expected.isAssignableFrom(given)) {
							throw new ValidationException("Assignation impossible.");
						}
					}
					
					if (a.annotationType().equals(Context.class)) {
						
						String ref = ((Context) a).value();
						Object objLink = job.getContext().get(ref);
						
						// Toute annotation Context doit référencer un objet existant dans le contexte du job
						if (objLink == null) {
							throw new ValidationException("Le paramètre " + param.getName() + " de la méthode " + method.getName()
									+ " fait référence à l'objet '" + ref + "' qui n'existe pas dans le counexte du job.");
						}
						
						/* Il doit y avoir une compatibilité de type entre un paramètre annoté Context et l’objet
						 * correspondant dans le contexte
						 * i.e. la valeur dans le contexte du job doit être castable en cette valeur.*/
						Class<?> expected = param.getType();
						
						if (! expected.isInstance(objLink)) {
							throw new ValidationException("Assignation impossible via l'objet du contexte.");
						}
					}
				}
				
				if ( ! valid) {
					throw new ValidationException("Un paramètre d'une méthode annotée via @Task"
							+ "doit être obligatoirement annoté via @Context ou @LinkFrom.");
				}
			}
		} // fin "pour chaque méthode"
		
		if ( ! taskGraph.isDAG()) {
			throw new ValidationException("Le graphe contient au moins un cycle.");
		}
	}
	
}


