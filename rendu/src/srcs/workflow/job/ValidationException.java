package srcs.workflow.job;

/**
 * ValidationException : si le job n’est pas conforme.
 */
public class ValidationException extends Exception {
	private static final long serialVersionUID = -3521285058008180968L;
	
	public ValidationException() {
		
	}
	
	public ValidationException(String message) {
        super(message);
    }

}
