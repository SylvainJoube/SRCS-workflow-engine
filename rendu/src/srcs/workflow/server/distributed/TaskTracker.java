package srcs.workflow.server.distributed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import srcs.workflow.job.Job;
import sylvain.debug.DebugLog;


/**
	JVM esclave qui exécute des tâches.
	Une JVM esclave prend deux arguments :
	- un nom de type String qui fera office d’identifiant (on suppose que deux esclaves ne peuvent
	pas avoir le même nom)
	- un nombre maximal de tâches qu’elle peut exécuter à un instant donné. Il faudra s’assurer
	que le nombre de tâches en cours d’exécution sur l’esclave ne dépasse pas cette borne. Il
	faut également considérer que la valeur de ce paramètre est propre à un esclave et peut être
	différente d’un esclave à l’autre.
	
	Un tracker (=esclave) sera exporté via RMI sur la machine maître,
	et sera ainsi monitoré.
 *
 */
public class TaskTracker implements TaskTrackerInterface {
	
	/** 
	 * Pour simuler qu'une tâche prend plus de temps, et du coup tester
	 * la répartition de la charge.
	 * Mettre à 0 pour ne pas faire de sleep à chaque tâche. */
	public static int sleepOnEveryTaskMs = 0;
	
	/** Nom (unique) du tracker */
	protected final String slaveName;
	
	/**
	 * La capacité du tracker est géré d'une manière centralisée par Master.
	 * @param slaveName
	 * @throws RemoteException
	 */
	public TaskTracker(String slaveName/*, int maxTaskNumber*/) throws RemoteException {
		this.slaveName = slaveName;
	}
	
	public static void main(String[] args) throws RemoteException, AlreadyBoundException, NotBoundException {
		
		// Connexion au Registry (il est bien créé, car dans les tests il y a un petit sleep de 500ms bien sympa :)
		Registry registry = LocateRegistry.getRegistry("localhost");
		
		// Je récupère le tracker exporté dans ce registry
		JobTrackerMasterInterface master = (JobTrackerMasterInterface) registry.lookup("Alexander the Great");
		
		//int capacity = master.getRandomTrackerCapacity(); géré par Master
		String uniqueName = master.getUniqueTrackerName();
		
		// Tracker (singleton) associé à cette JVM
		TaskTracker tracker = new TaskTracker(uniqueName);
		
		// Export du tracker pour pouvoir y accéder du Master
		UnicastRemoteObject.exportObject(tracker, 0);
		
		// Association d'un nom avec ce tracker
		registry.bind(uniqueName, tracker);
		
		// Enregistrement de ce tracker sur Master
		master.registerTracker(uniqueName);
	}
	
	@Override
	public Object executeTask(Job job, Object[] params, String methodName, Class<?>[] methodParamTypes)
			throws RemoteException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		// Il est nécessaire d'avoir réservé une exécution via canAcceptTask() avant d'appeler cette fonction.
		long t = System.currentTimeMillis();
		
		// Je trouve la méthode sur la VM actuelle à partir de son nom et
		// du type de ses paramètres
		Method method = job.getClass().getMethod(methodName, methodParamTypes);
		
		// L'exécution est donc bien sur ce tracker : le job est serializé (malheuseusement à chaque tâche,
		// du coup c'est pas super niveau activité réseau)
		Object result;
		
		// Là, je suis sur le tracker.
		// Une exception est levée si la méthode rencontre un problème en elle-même (i.e. pas RemoteException)
		// ce n'est pas le cas dans les tests, mais si c'était le cas, ça serait une erreur de programmation
		// et le job tout entier devrait être arrêté.
		result = method.invoke(job, params);
		
		if (sleepOnEveryTaskMs != 0) {
			try {
				Thread.sleep(sleepOnEveryTaskMs);
			} catch (InterruptedException e) { }
		}
		
		DebugLog.info("Temps pris pour l'exécution d'une tâche " + methodName + " : " + (System.currentTimeMillis() - t));
		
		return result;
	}
	
	public String getName() throws RemoteException {
		return slaveName;
	}
	
}
