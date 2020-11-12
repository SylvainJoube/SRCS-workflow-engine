package srcs.workflow.job;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** 
 * Les paramètres d’une méthode annotée par Task sont les paramètres de la tâche.
 * Un paramètre d’une tâche peut être :
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Task {
	
	/** Identifiant de la tâche au sein du job. */
	public String value();
}
