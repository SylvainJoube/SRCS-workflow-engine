package srcs.workflow.job;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Désigne le résultat d’une tâche précédente :
 * c’est ainsi que l’on définit les dépendances entre tâches.
 * Cette annotation possède un attribut obligatoire de type string
 * qui référence l’identifiant de la tâche précédente censée délivrer le résultat.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface LinkFrom {
	
	/** Référence l’identifiant de la tâche précédente censée délivrer le résultat. */
	public String value();
}
