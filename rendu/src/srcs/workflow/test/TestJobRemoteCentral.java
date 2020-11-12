package srcs.workflow.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.lang.management.ManagementFactory;
import java.util.Map;

import org.junit.Test;

import srcs.workflow.executor.JobExecutor;
import srcs.workflow.server.central.JobExecutorRemoteCentral;
import srcs.workflow.server.central.JobTrackerCentral;

public class TestJobRemoteCentral extends SystemDeployer{

	public TestJobRemoteCentral() {
		this.name_class_jobtracker = JobTrackerCentral.class.getCanonicalName();
	}
	
	
	@Test
	public void test() throws Exception {
		
		for(JobTest jobtest : JobTests.jobtests()) {
			JobForTest job = jobtest.getJob();
			job.reset();
			JobExecutor je = new JobExecutorRemoteCentral(job);
			Map<String,Object> res = je.execute();
			jobtest.check(res);
			
				
			Integer my_pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
			assertEquals(job.getMappingTaskThread().size(),job.getMappingTaskThread().values().stream().distinct().count());
			
			
			assertEquals(1,job.getMappingTaskPid().values().stream().distinct().count());
			assertEquals(getPidOfProcess(processjobtracker), job.getMappingTaskPid().get("A").intValue());
			assertNotEquals(my_pid, job.getMappingTaskPid().get("A"));
		}
	}

	
	
}
