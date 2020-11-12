package srcs.workflow.job;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Désigne une donnée définie dans le contexte du job.
 * Cette annotation possède un attribut obligatoire de type string qui
 * référence une valeur dans le contexte du job.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Context {
	
	/** Référence une valeur dans le contexte du job. */
	public String value();
}
