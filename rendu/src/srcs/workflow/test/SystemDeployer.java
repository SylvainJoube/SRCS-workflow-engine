package srcs.workflow.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;

import sylvain.debug.DebugLog;

public class SystemDeployer {

	Process processjobtracker;
	Process[] processesTaskTracker = new Process[0];
	
	String name_class_jobtracker = "";
	
	String name_class_tasktracker = "";
	int nb_tasktracker = 2;
	int max_slot=2;
	
	final String path_project = System.getProperty("user.dir"); 
	final String path_bin = path_project+"/bin";
	
	static int vmCount = 0;
	
	
	@Before
	public void startAll() throws IOException, InterruptedException {
				
		processjobtracker = startJVM(path_bin, 
				name_class_jobtracker,
				new String[0],
				System.getProperty("java.io.tmpdir")+"/"+name_class_jobtracker, name_class_jobtracker);
		
		Thread.sleep(500);
		
		if(!name_class_tasktracker.equals("")) {
			processesTaskTracker=new Process[nb_tasktracker];
			for(int i =0 ; i< nb_tasktracker ; i++) {
				processesTaskTracker[i] = startJVM(path_bin,
						name_class_tasktracker,
						new String[] { ""+i, max_slot+""},
						System.getProperty("java.io.tmpdir")+"/"+name_class_tasktracker+"_"+i, name_class_jobtracker+"_"+i);
			}
			
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		      @Override
		      public void run() {
		    	 stopAll();
		      }
		    });
		Thread.sleep(500);
		
	}
	
	@After
	public void stopAll() {
		processjobtracker.destroyForcibly();
		for(int i = 0 ;i < processesTaskTracker.length; i++) {
			processesTaskTracker[i].destroyForcibly();
		}
	}
	
	public static Process  startJVM(String path_bin, String main,String[] args, String prefix, String classUniqueName) throws IOException {
		//DebugLog.info("name_class_jobtracker = " + main);
		String tokens[] = new String[2+args.length];
		tokens[0]="java";
		tokens[1]=main;
		for(int i=0 ; i< args.length;i++) {
			tokens[i+2]=args[i];
		}
		
		ProcessBuilder pbuilder = new ProcessBuilder(tokens);
		pbuilder.environment().put("CLASSPATH", path_bin);
		
		String fpath = "/home/sylbe/Desktop/SRCS/SRCS/PROJET FINAL/log/" + classUniqueName + "";

		pbuilder.redirectError(new File(fpath+"_err.txt"));
		pbuilder.redirectOutput(new File(fpath+"_inf.txt"));
		//pbuilder.redirectError(new File(prefix+".stderr"));
		//pbuilder.redirectOutput(new File(prefix+".stdout"));
		return pbuilder.start();
	}
	
	
	public static synchronized long getPidOfProcess(Process p) {
	    long pid = -1;

	    try {
	      if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
	        Field f = p.getClass().getDeclaredField("pid");
	        f.setAccessible(true);
	        pid = f.getLong(p);
	        f.setAccessible(false);
	      }
	    } catch (Exception e) {
	      pid = -1;
	    }
	    return pid;
	  }
	
}
