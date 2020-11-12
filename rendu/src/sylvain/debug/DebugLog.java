package sylvain.debug;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DebugLog {
	protected final static boolean SHOW_LOG = true;
	protected final static boolean SHOW_ERRORS = true;
	
	protected static int baseLogLevel = 2;
	
	protected static AtomicLong startTimeMs = new AtomicLong(0);
	
	protected static AtomicInteger traceExeStep = new AtomicInteger(0);
	
	// log simple
	public static void log(String message) {
		log(message, 1);
	}
	public static void info(String message) {
		info(message, 1);
	}
	

	public static void log(String message, int level) {
		if (SHOW_LOG) {
			fullLog(message, false, level);
		}
	}
	public static void info(String message, int level) {
		if (SHOW_LOG) {
			fullLog(message, false, level);
		}
	}

	// erreur
	public static void error(String message, int level) {
		if (SHOW_ERRORS) {
			fullLog(message, true, level);
		}
	}
	public static void error(String message) {
		error(message, 1);
	}

	public static void traceExec() {
		info("" + traceExeStep.getAndAdd(1) + " trace", 1);
	}
	
	public static void resetExec() {
		traceExeStep.set(0);
	}
	
	
	
	// log générique
	private static void fullLog(String message, boolean isError, int level) {
		
		StackTraceElement[] slist = new Exception().getStackTrace();			
		StackTraceElement e = slist[baseLogLevel + level]; // level 2 est bon
		
		if (startTimeMs.get() == 0) {
			startTimeMs.getAndSet(System.currentTimeMillis());
		}
		
		long time = System.currentTimeMillis() - startTimeMs.get();
		
		
		String prefix = null;
		if (e.isNativeMethod()) {
			prefix = "[native]";
		} else {
			String fileName = e.getFileName();
			//String classFullPrefix = null;
			
			if (fileName != null) {
				String className = fileName.substring(0, fileName.lastIndexOf(".")); //.substring(f.getAbsolutePath().lastIndexOf("\\")+1)
				
				String methodName = e.getMethodName();
				
				if (methodName != null) {
					prefix = "[" + className + "." + methodName + "]";
				} else {
					prefix = "[" + className + ".?]";
				}
			}
		}
		
		if (prefix == null) {
			prefix = "";
		} else {
			prefix = "("+time+") " + prefix + "  ";
		}
		
		
		// Code adapté à mes besoins
		String fileLink = 
				" "
				+ (e.isNativeMethod() ? "(Native Method)" :
					(e.getFileName() != null && e.getLineNumber() >= 0 ?
							"(" + e.getFileName() + ":" + e.getLineNumber() + ")" :
								(e.getFileName() != null ?  "("+e.getFileName()+")" : "(Unknown Source)")));
		
		String spacesBeforeFileLink = "     ";
		
		if (isError) {
			System.err.println("ERREUR " + prefix + message + spacesBeforeFileLink + fileLink);
		} else {
			System.out.println(prefix + message + spacesBeforeFileLink + fileLink);
		}
		
	}
	
	
}