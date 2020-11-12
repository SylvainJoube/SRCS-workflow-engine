package srcs.workflow.server.distributed;


/**
 * Référence à un TaskTracker depuis le singleton de la classe JobTrackerMaster.
 * Les instances de cette classes sont sur la JVM Master. </br>
 * Gère la nombre de tâches qu'il est possible d'avoir sur chaque TaskTracker,
 * optimise les appels distants pour ne pas avoir à demander à chaque fois au
 * tracker s'il lui reste de la place.</br></br>
 * 
 * IMPORTANT : il est supposé qu'un tracker n'a qu'un seul Master. Sans cela, la gestion
 * centralisée de la capacité d'un traker serait impossible.
 */
public class TaskTrackerRefOnMaster {
	
	/** Nom du tracker (unique car attribué par Master) */
	protected final String trackerName;
	
	/** Nombre de tâches maximales exécutées par ce tracker */
	protected final int maxTaskNumber;
	
	/** Nombre de tâches actuellement en cours d'exécution */
	protected volatile int runningTaskNumber = 0;
	
	protected TaskTrackerInterface proxyTaskTracker;
	
	/** Lock pour préserver la cohérence de runningTaskNumber,
	 *  et assurer l'atomicité lors de l'appel à canAcceptTask() */
	protected Object taskNumberLock = new Object();
	
	public TaskTrackerRefOnMaster(String name, int capacity, TaskTrackerInterface rmiProxy) {
		trackerName = name;
		maxTaskNumber = capacity;
		proxyTaskTracker = rmiProxy;
	}
	
	/**
	 * Si le tracker n'est pas plein et peut donc accepter une tâche :
	 * incrémente le nombre de tâches en cours (i.e. s'assure que
	 * la place sera toujours libre quand on va lui soumettre réellement la tâche)
	 * et retourne vrai. Retourne faux si le tracker est plein.
	 * @return  true si le tracker peut accepter une nouvelle tâche (et incrémente son
	 *          compteur de tâches en cours). </br>
	 *          false si le tracker est plein.
	 */
	public boolean acquireTaskSlot() {
		synchronized (taskNumberLock) {
			//DebugLog.info(slaveName + " actuel/max = " + runningTaskNumber + " / " + maxTaskNumber);
			if (runningTaskNumber < maxTaskNumber) {
				runningTaskNumber++;
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Libération d'un slot pour exécuter une tâche.
	 */
	public void releaseTaskSlot() {
		synchronized (taskNumberLock) {
			runningTaskNumber--;
		}
	}
	
	/**
	 * Calcule le taux d'occupation de ce tracker.
	 * @return le taux d'occupation de ce tracker : 1 signifie qu'il est complet, 0 qu'il est vide.
	 */
	public float getCapacityRatio() {
		if (maxTaskNumber <= 0) return 1; // pas de place (les divisions par 0 c'est pas cool)
		synchronized (taskNumberLock) {
			return ((float)runningTaskNumber) / ((float)maxTaskNumber);
		}
	}
	
	/**
	 * Récupérér la référence distance à l'objet TaskTracker distant.
	 * @return la référence distance à l'objet TaskTracker distant.
	 */
	public TaskTrackerInterface getProxy() {
		return proxyTaskTracker;
	}
	
	/**
	 * @return le nom du tracker.
	 */
	public String getName() {
		return trackerName;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null) return false;
		if (o instanceof TaskTrackerRefOnMaster) {
			return (((TaskTrackerRefOnMaster) o).getName().equals(getName()));
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		// Le nom de chaque tracker est unique
		return getName().hashCode();
	}
	
}
