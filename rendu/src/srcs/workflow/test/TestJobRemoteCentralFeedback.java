package srcs.workflow.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Map;

import org.junit.Test;

import srcs.workflow.executor.JobExecutor;
import srcs.workflow.server.central.JobExecutorRemoteCentral;

public class TestJobRemoteCentralFeedback  extends TestJobRemoteCentral {
	
	
	@Test
	public void test() throws Exception {
		
		
		for(JobTest jobtest : JobTests.jobtests()) {
			JobForTest job = jobtest.getJob();
			job.reset();
			
			PrintStream old = System.out;
			File file = new File(System.getProperty("java.io.tmpdir")+"/feedback"+job.getName()+""+System.currentTimeMillis());
			System.setOut(new PrintStream(file));
			
			JobExecutor je = new JobExecutorRemoteCentral(job);
			Map<String,Object> res = je.execute();
			jobtest.check(res);
			
				
			Integer my_pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
			assertEquals(job.getMappingTaskThread().size(),job.getMappingTaskThread().values().stream().distinct().count());
			
			
			assertEquals(1,job.getMappingTaskPid().values().stream().distinct().count());
			assertEquals(getPidOfProcess(processjobtracker), job.getMappingTaskPid().get("A").intValue());
			assertNotEquals(my_pid, job.getMappingTaskPid().get("A"));
			
			
			System.setOut(old);
			try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
				String line;
				int xavant=-1;
				int xcourant=-1;
				while(( line = br.readLine()) !=null) {
					xcourant=Integer.parseInt(line);	
					assertTrue(xcourant>=xavant);
					xavant=xcourant;
				}
				assertEquals(7,xcourant);
			}
			file.delete();
		}
	}
	
	
}
