package srcs.workflow.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	TestGraph.class,
	TestJobValidator.class,
	TestJobLocalSequential.class,
	TestJobLocalParallel.class,
	TestJobRemoteCentral.class,
	TestJobRemoteCentralFeedback.class,
	TestJobRemoteDistributed.class,
	TestJobRemoteDistributedRafale.class,
	TestJobRemoteDistributedRafaleWithCrashes.class
	})
public class AllTests {

}
