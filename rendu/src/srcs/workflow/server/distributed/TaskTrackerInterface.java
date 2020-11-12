package srcs.workflow.server.distributed;

import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import srcs.workflow.job.Job;

public interface TaskTrackerInterface extends Remote {
	
	/**
	 * 
	 * @param job
	 * @param params
	 * @param methodName
	 * @param methodParamTypes
	 * @return  la valeur de retour de cette tâche. //un identifiant unique pour cette tâche
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public Object executeTask(Job job, Object[] params, String methodName, Class<?>[] methodParamTypes)
			throws RemoteException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;
	
	public String getName() throws RemoteException;
}
