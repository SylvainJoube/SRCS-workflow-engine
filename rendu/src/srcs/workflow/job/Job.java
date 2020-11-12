package srcs.workflow.job;

import java.io.Serializable;
import java.util.Map;

/**
 * Un Job est serializable, mais n'implémente pas Remote
 * pour ne pas ajouter une contrainte supplémentaire
 * lors de l'implémenttaion d'un job.
 */
public abstract class Job implements Serializable {
	private static final long serialVersionUID = -3680914636179022780L;
	final protected String name;
	final protected Map<String, Object> context;
	
	/** 
	 * Création du job : nécessite un nom et un contexte d'exécution.
	 * @param name  nom du job
	 * @param context  contexte d'exécution
	 */
	public Job(String name, Map<String, Object> context) {
		this.context = context;
		this.name = name;
	}
	
	/**
	 * Récupérer le nom du Job.
	 * @return le nom du job
	 */
	public String getName() {
		return name;
	}
	
	/** Un contexte  qui permet d’associer un nom de paramètre du job à une valeur.
	 *  Cet attribut permet de paramétrer le job,
	 *  utile notamment pour paramétrer les tâches racines du graphe. */
	public Map<String, Object> getContext() {
		return context;
	}
}
