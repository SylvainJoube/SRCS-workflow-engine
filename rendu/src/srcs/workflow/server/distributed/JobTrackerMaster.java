package srcs.workflow.server.distributed;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import srcs.workflow.job.Job;
import sylvain.debug.DebugLog;

/**
	JVM maître dont le rôle sera de recevoir les jobs des clients
	et d’attribuer les tâches sur un ensemble de JVM esclaves qui les exécuteront.
	La JVM maître n’exécute aucune tâche et a juste un rôle de coordinateur
	qui assure une répartition équitable des tâches entre les esclaves. 
	On suppose que le Master ne peut pas tomber en panne.
	
	<p>Je suppose que le Job a une taille raisonnable et peut être serialisé plusieurs fois,
	une fois pour le passer au Master, puis une fois pour le passer à chaque esclave qui va
	en exécuter une tâche.
	
	Comme précédemment, on suppose que les méthodes du job n'ont effectivement
	que besoin du contexte d'exécution et des résultats de tâches précédentes,
	et qu'elles ne modifient jamais de vraiables déclarées
	dans la classe étendant Job elle-même.
	
	Voir le compte rendu (exercice 6) pour de plus amples informations.
	
 */
public class JobTrackerMaster implements JobTrackerMasterInterface {
	
	/**
	 * Liste des trackers (proxy RMI) enregistrés sur le Maître.
	 * Ce sont des proxy à qui on peut déléguer les tâches.
	 * La gestion de cette liste doit toujours être thread-safe. */
	protected List<TaskTrackerRefOnMaster> trackers = new ArrayList<>();
	
	/**
	 * Deux stratégies sont possibles lors de la recherche d'un tracker libre.</br>
	 * - False = (non demandé) Je prends le premier tracker qui a un slot libre : c'est optimisé en recherche, mais
	 *   certains trackers seront plus sollicités que d'autres.</br>
	 * - True = Je prends le tracker qui a le ratio entre nombre de tâches en cours
	 *   d'exécution et capacité maximale le plus faible. C'est à dire que je prends
	 *   l'esclave le moins occupé, en proportion. */
	protected final boolean forceTrackerEquity = true;
	
	/**
	 * trackersLock pour protéger les accès à la liste des trackers.
	 * J'aurais pu utiliser directement l'objet trackers, mais j'ai trouvé
	 * plus clair de faire un objet à part. */
	protected final Object trackersLock = new Object();
	
	/** Pour protéger l'accès à la liste des jobs en cours d'exécution. */
	//protected final Object jobsLock = new Object();
	
	/** Liste des jobs en cours d'exécution. */
	//protected List<JobExecutorParallelDistributed> runningJobs = new ArrayList<>();
	
	/** Utile pour reproduire la suite des évènements d'une manière déterministe. */
	protected Random rand = new Random(21_42_84);
	
	/** Pour garantir l'unicité des ID, c'est Master qui donne leurs identifiants aux trackers. */
	protected volatile int nextTrackerUniqueID = 1;
	
	/**
	 * Démarrage de la JVM Master
	 * @param args  n'attend aucun argument
	 * @throws InterruptedException
	 * @throws RemoteException
	 * @throws AlreadyBoundException
	 */
	public static void main(String[] args) throws InterruptedException, RemoteException, AlreadyBoundException {
		DebugLog.info("Service master démarré.");

		// Création du Registry. Il sera automatiquement détruit lorsque la VM s'arrêtera.
		LocateRegistry.createRegistry(1099);
		Thread.sleep(100);
		
		// Accès au registery
		final Registry registry = LocateRegistry.getRegistry();
		
		// Création du service de gestion des jobs et des trackers
		JobTrackerMaster master = new JobTrackerMaster();
		
		// Export de l'objet (sinon, il est serialisé)
		UnicastRemoteObject.exportObject(master, 0);
		
		// Je nomme mon objet exporté pour pouvoir le récupérer côté client
		registry.bind("Alexander the Great", master);
		
		/* Lors de la terminaison de la VM, le thread ayant créé le registry sera stoppé,
		 * fermant effectivement le registry. */
	}
	
	
	public JobTrackerMaster() throws RemoteException {
		
	}
	
	/**
	 * Exéction d'une tâche sur un tracker libre. Cette exécution est bloquante,
	 * l'exécution d'un Job étant dans un thread propre géré par l'appel RMI à la méthode
	 * executeJob de la classe JobTrackerMaster. Le thread a d'ailleurs une description
	 * qui ressemble à ça : Thread[RMI TCP Connection(n°connexion)-127.0.0.1,5,RMI Runtime] </br></br>
	 * 
	 * Malheureusement, et c'est pas super optimisé comme je le fais là,
	 * le job est à nouveau serializé à chaque fois que j'envoie une tâche.
	 * Je pourrai améliorer ça mais ça me prendrait un peu plus de temps,
	 * temps qui me manque en ce moment :'(
	 * @param job
	 * @param params
	 * @param methodName
	 * @param methodParamTypes   //assignTaskToFreeTracker
	 * @throws InterruptedException 
	 */
	public Object executeTaskOnFreeTracker(Job job, Object[] params, String methodName, Class<?>[] methodParamTypes) throws Exception {
		
		//DebugLog.info("Recherche d'un tracker libre...");
		
		boolean taskComplete = false;
		
		while ( ! taskComplete) {
			
			TaskTrackerRefOnMaster foundTracker = null;
			
			// J'essaie de trouver un tracker qui peut accepter une tâche.
			
			// Ce JobTrackerMaster est un singleton, comme j'ai pris le lock trackersLock,
			// je suis assuré qu'aucune autre tâche ne peut être soumise jusqu'à ce que
			// je sorte du block synchronized.
			// La seule chose qui peut arriver, c'est qu'un tracker finisse un tâche, jamais le contraire.
			// Je réserve un slot via tracker.acquireTaskSlot(), qui permet de conserver
			// la place (pour une future tâche) même en dehors du bloc synchronized.
			synchronized (trackersLock) {
				
				if (trackers.size() == 0) throw new Exception("Aucun tracker enregistré, tâche non exécutée.");
				
				if (forceTrackerEquity) {
					// Cas de la recherche équitable :
					// recherche du tracker le moins occupé (en proportion)
					// Je recherche le ratio le plus petit, non égal à 1
					float minRatio = 1;
					for (TaskTrackerRefOnMaster tracker : trackers) {
						float ratio = tracker.getCapacityRatio();
						if (ratio == 1) continue;
						if (ratio < minRatio) {
							minRatio = ratio;
							foundTracker = tracker;
						}
					}
					if (foundTracker != null ) {
						// Si je ne peux pas réserver une place sur le tracker, je recommence la recherche.
						if ( ! foundTracker.acquireTaskSlot()) {
							foundTracker = null;
						}
					}
				} else {
					// Cas de la recherche non équitable : FirstFit, je prends le premier qui peut gérer ma tâche
					for (TaskTrackerRefOnMaster tracker : trackers) {
						if (tracker.acquireTaskSlot()) {
							foundTracker = tracker;
							break;
						}
					}
				}
				
				// Si aucun tracker trouvé, j'attends qu'un slot se libère pour l'exécution
				if (foundTracker == null) {
					DebugLog.info("------- Aucun racker trouvé, je m'endors... size = " + trackers.size());
					trackersLock.wait();
				}
			}
			
			if (foundTracker == null) continue; // je recommence la recherche de tracker libre
			
			Object result = null;
			boolean mustFreeSlot = false;
			try {
				DebugLog.info("Exécution d'une tâche sur le tracker " + foundTracker.getName() + "  (ratio " + foundTracker.getCapacityRatio()+")");
				result = foundTracker.getProxy().executeTask(job, params, methodName, methodParamTypes);
				taskComplete = true;
				mustFreeSlot = true;
				return result;
			} catch (RemoteException re) {
				// Tracker probablement déconnecté
				// (Ne libère pas de slot sur le tracker vu que le tracker n'est plus là !)
				synchronized (trackersLock) {
					// Supprime le tracker de la liste s'il n'a pas déjà été supprimé
					trackers.remove(foundTracker);
					DebugLog.info("Tracker {"+foundTracker.getName()+"} probablement down. Nombre de trackers restant : " + (trackers.size()));
					foundTracker = null;
					continue;
				}
			} catch (Exception e) {
				mustFreeSlot = true;
				// Erreur dans l'exécution de la fonction, système instable.
				// => erreur critique lors de l'exécution du job.
				throw e; // Renvoi de l'exception au JobHandler
			} finally {
				if (mustFreeSlot) {
					// Libération du slot occupé par la tâche
					foundTracker.releaseTaskSlot();
					// Réveil des threads en attente d'un tracker libre
					synchronized (trackersLock) {
						trackersLock.notifyAll();
					}
				}
			}
		}
		throw new Exception("Exception qui ne devrait jamais arriver.");
	}

	
	@Override
	public Map<String, Object> executeJob(Job job) throws RemoteException, Exception {
		// Soumet des tâches aux trackers en fonction de leur disponibilité
		
		//DebugLog.info("Exécution du job sur le master...");
		
		// Création d'un objet handler qui gèrera le job
		JobExecutorParallelDistributed handler;
		handler = new JobExecutorParallelDistributed(job, this);
		
		// Exécution bloquante : l'appel executeJob est dans un thread géré par RMI.
		// Plusieurs exécutions de executeJob sont ainsi possibles, grâce à RMI.
		Map<String, Object> result = handler.execute();
		
		return result;
	}

	@Override
	public String getUniqueTrackerName() throws RemoteException {
		int result;
		synchronized (trackersLock) {
			result = nextTrackerUniqueID++;
		}
		switch (result) {
		case 0: return "Mr. Nobody";
		case 1: return "X ae A-12"; // huhuhu
		case 2: return "John Doe";
		case 3: return "Jean-Paul Dupont";
		case 4: return "Jane Doe";
		case 5: return "Johann Sebastian Bach";
		case 6: return "Ludwig van Beethoven";
		default: return "poor anonymous tracker " + result;
		}
	}

	@Override
	public int getRandomTrackerCapacity() throws RemoteException {
		synchronized (trackersLock) {
			return rand.nextInt(2) + 1;
		}
	}

	@Override
	public void registerTracker(String name) throws RemoteException, NotBoundException {
		// Accès au registery
		final Registry registry = LocateRegistry.getRegistry();
		
		// Référence (proxy RMI) vers le tracker
		TaskTrackerInterface trackerFromRegistry =  (TaskTrackerInterface) registry.lookup(name);
		
		// Création d'un objet local à Master pour pouvoir gérer plus efficacement le tracker
		// (et sa capacité par exemple)
		TaskTrackerRefOnMaster ref = new TaskTrackerRefOnMaster(name, getRandomTrackerCapacity(), trackerFromRegistry);
		
		DebugLog.info("Enregistrement du tracker {" + name + "}");
		
		// Ajout du tracker à la liste des trackers enregistrés sur Master
		synchronized (trackersLock) {
			trackers.add(ref);
		}
	}
	
}