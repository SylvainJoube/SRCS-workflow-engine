# Projet SRCS - Sylvain JOUBE - M1 SAR - juin 2020

**Tous les test passent, j'ai tout implémenté.**  Le code que j'ai produit est bien commenté. Ce compte rendu permet d'avoir une vision globale de ce que j'ai fait.



## Exercice 2

En utilisant une chaîne de caractères personnalisée plutôt que le nom de la méthode, on peut définir un nom plus adapté à la tâche réalisée. En plus, lors d'un refactoring automatique, seul le nom de la méthode sera changé dans le code et pas ses références sous forme de chaîne de caractères. Ainsi, ça me semble juste beaucoup plus rigoureux de procéder ainsi.



## Exercice 3

Description de mon implémentation de la méthode `execute`.

### Résumé des grandes étapes

1) Je crée un `JobValidator` à partir du `job` passé via la constructeur de `JobExecutorSequential`, j'en récupère le graphe et je dresse la liste des tâches qui sont à exécuter (`awaitingTasksID`).

2) Tant qu'il reste des tâches à exécuter, j'en exécute une qui est exécutable, c'est à dire dont toutes les dépendances sont satisfaites (toutes les tâches dont elle dépend doivent être terminées, touts les nœuds voisins entrants sont terminés). J'ajoute son résultat et l'ID unique de la tâche à la `Map` qui sera retournée.

### Outils utilisés

Mon implémentation repose sur les fonctions précédemment développées. Le `JobValidator` me permet d'obtenir les ID et dépendances des tâches, et de faire le lien entre un ID de tâche et la méthode correspondante à exécuter.  



## Exercice 4

Pour essayer de rendre plus aisée la lecture du code de la méthode `execute()`, j'ai divisé le code en plusieurs méthodes. En une phrase, les tâches sont exécutées dès que possible. Aussi, j'ai fait hériter `JobExecutorParallel` d'une classe abstraite `JobExecutorParallelAbstract` pour pouvoir réutiliser (d'une manière claire) le code pour l'exercice 6.

### Fonctions nécessaires à la compréhension de l'algorithme

#### initExecute()

Comme pour l'exercice 3, je crée la `Map` des résultats `result`, un `JobValidator` depuis le `Job` passé en paramètre, et la liste des tâches en attente d'exécution (`awaitingTasksID`). Je récupère le `Graph` construit par le `JobValidator`. Je crée aussi un objet `lock` pour protéger des accès concurrents les sections critiques : les accès à `awaitingTasksID` ainsi qu'à `result`.

#### getReadyTaskID()

La méthode `getReadyTaskID` me sert à récupérer l'identifiant d'une tâche prête à être exécutée. Si aucune tâche n'est exécutable et qu'il reste des tâches en attente d'exécution, je m'endors sur l'objet `lock`. Un thread ayant fini l'exécution d'une tâche réveille tous les threads endormis sur le `lock`. S'il n'y a plus de tâche en attente d'exécution, la fonction renvoie `null`. Elle revoie également `null` si une exception a été levée dans un des threads car le système est alors devenu instable et ne peut plus continuer (l'exception sera remontée au Job qui la lèvera à son tour).

#### prepareTaskArgs()

La méthode `prepareTaskArgs()` renvoie les arguments d'une méthode passée en paramètre, récupérant (d'une manière thread-safe) les résultats des dépendances (autres tâches et contexte).

### Algorithme en lui-même

1) Initialisation via `initExecute`

2) Dès qu'une tâche est prête à être exécutée, je l'exécute dans un nouveau thread. L'identifiant de la tâche choisie est récupéré via `getReadyTaskID` qui gère la concurrence et vérifie les dépendances.

3) Lorsqu'une tâche a fini de s'exécuter (dans un thread), je mets à jour la `Map` des valeurs de retour des tâches et je réveille le thread responsable des tâches toujours en attente d'exécution.



## Exercice 5

### Question 5.1.

J'ai décidé d'utiliser java RMI. (au passage, ce projet m'a permis de bien re-comprendre comment RMI fonctionne, j'ai passé quand-même quelques heures juste sur cette question !).  

Ma classe `JobTrackerCentral` déploie le service d'exécution de job. Dans son main, elle crée un `Registry`, instancie un objet `JobTrackerCentral`, l'exporte et l'enregistre sous le nom `JobTrackerCentralSingleton` pour le rendre accessible via un nom. Cette instance contient une méthode `executeJob(..)` permettant d'exécuter un job. Cette classe étend également une interface nommée `JobTrackerCentralInterface` pour que le cast lors du `registry.lookup` sur le code client soit possible (c'est en effet un objet proxy qui est retourné, étendant cette interface).

La classe `JobExecutorRemoteCentral` est le code client du serveur. Elle offre une méthode d'instance `execute()` permettant d'exécuter un job sur le serveur central. La méthode se connecte au `Registry` créé précédemment, et récupère le `tracker` en passant par l'interface `JobTrackerCentralInterface` pour que ça fonctionne avec RMI. Cette interface étend `Remote`, nous verrons ça plus en détail à la question 5.4. Le job est soumis au `tracker` et le résultat est retourné.

### Question 5.2.

RMI est simple d'utilisation et permet une gestion efficace des appels. Le code à produire pour cet exercice n'est pas noyé par les appels distants et les communications réseaux. L'utilisation des objets est transparente, et utiliser un objet au niveau du serveur est presque identique à utiliser un objet local. De plus, personnellement j'avais envie de mieux comprendre ce protocole que nous avions déjà vu au TME-7.

### Question 5.3.

Dans la méthode `execute` de `JobExecutorRemoteCentral`, l'instance de `Job` n'est pas exportée car `Job` n'implémente pas `Remote` pour ne pas ajouter une contrainte supplémentaire aux objets en héritant. Comme le job est passé à l'objet distant, il doit être `serializable`, donc tous les objets de son contexte doivent également être `serializable`.

### Question 5.4.

Comme évoqué à la fin de ma réponse à la question 5.1, `JobExecutorRemoteCentral` implémente l'interface `JobExecutorNotification` qui elle-même étend `Remote`. Elle offre une méthode `taskFinished` qui doit être appelée dans `JobExecutorParallel` à chaque fois qu'une tâche est terminée. Dans la méthode `execute` de `JobExecutorRemoteCentral`, j'exporte l'objet actuel (`this`) pour que sa méthode `taskFinished` puisse être appelée lors de l'exécution du job par le serveur. Mon tracker prend en paramètre cet objet, et la méthode `execute` de `JobExecutorParallel` se charge ensuite d'appeler `taskFinished`. J'ai donc légèrement modifié la classe `JobExecutorParallel` pour l'adapter. Une fois l'exécution du job terminée sur le tracker, j'annule l'exportation de l'objet courant, puis je retourne le résultat de l'exécution distante du job. (de la même manière que le contexte du job doit être serializable, les valeurs de retour doivent l'être aussi)



## Exercice 6

### Question 6.1.

J'ai une fois de plus choisi d'utiliser RMI parce que nous l'avions vu en cours et pour sa facilité d'utilisation. Le singleton `JobTrackerMaster` crée le registre RMI, s'exporte et s'associe un nom dans le registre. Lorsqu'un nouveau tracker (=esclave) `TaskTracker` est créé (une instance par JVM), il se connecte au registre RMI, récupère un proxy vers l'instance `JobTrackerMaster`, récupère depuis ce Master un nom (pour assurer l'unicité des noms, c'est Master qui nomme), s'exporte dans le registre et s'enregistre au près de Master via la fonction `registerTracker` (fonction du Master).

Les seuls autres appels distants sont du Master vers les esclaves pour leur demander d'exécuter une tâche. Il n'y a pas de communication entre esclaves.

### Question 6.2.

La gestion des `TaskTracker` est centralisée sur le Master, et il ne peut pas y avoir plusieurs Masters. C'est le Master qui gère la capacité des esclaves et qui leur affecte des tâches. Le Master a une liste d'objets locaux esclaves : ces objets stockent le nom, le nombre maximal et actuel de tâches exécutées ainsi qu'un proxy RMI vers l'objet esclave distant. Lors de l'affectation d'une tâche à un esclave, le Master regarde quel est l'objet esclave qui a l'occupation la plus faible (ratio entre nombre de tâches en cours et capacité maximale) et prend cet esclave. Si tous les esclaves ont un taux d'occupation de 1, le thread s'endort et attend qu'un slot soit libéré sur un esclave. (voir question 6.5. pour le parallélisme de l'exécution des jobs).  

Au début, j'avais stocké leur capacité sur les esclaves mais ça nécessitait un appel distant synchrone (sur l'objet esclave de la bonne JVM) à chaque fois que je voulais obtenir le ratio d'occupation de l'esclave, ce qui n'était pas du tout optimisé ! Et comme il n'y a qu'un seul Master, pas de souci pour gérer la capacité en local sur le Master.

### Question 6.3.

Lors de l'affectation d'une tâche à un esclave par le Master, je prends un `lock` (bloc `synchronized`) et je parcours la liste des esclaves comme décrit à la question précédente. Du fait du parallélisme (des tâches et des jobs), des appels concurrents peuvent être faits à `JobTrackerMaster.executeTaskOnFreeTracker(..)` mais je suis assuré qu'un seul thread à la fois peut rechercher un tracker libre. Si aucun traker libre n'est trouvé, j'endors ce thread et je serai réveillé lorsqu'une tâche se terminera. Si j'ai trouvé un esclave libre, je réserve dessus un slot via `TaskTracker.acquireTaskSlot()`. Je peux alors sortir du bloc synchronized en toute sécurité, un emplacement pour ma tâche m'est réservé. Là, j'exécute (synchrone) la tâche sur l'esclave, puis je libère le slot et réveille les threads (sur Master) en attente d'un slot libr epour exécuter une tâche. (voir question 6.6. pour la gestion d'une panne lors d'une demande d'exécution).

### Question 6.4.

Dans l'implémentation que j'ai choisie, chaque Job soumis au Master est géré via un objet `JobExecutorParallelDistributed`. A chaque fois qu'une tâche est terminée, le Master stocke le résultat, regarde quelles sont les autres tâches qui sont exécutables et débloque leur exécution si possible (l'exécution sur un esclave étant synchrone, un nouveau thread responsable de la tâche est créé sur le Master). L'inconvénient de cette méthode est que c'est le Master qui orchestre le job, ne demandant aux esclaves que d'exécuter les tâches. Ainsi, le Master a plus de travail à réaliser et peut devenir un goulot d'étranglement. De plus, un thread par tâche prête est créé ce qui finit par faire beaucoup de threads si de nombreux jobs comprenant de nombreuses tâches sont soumis au Master (au passage, j'aurais aussi pu faire un système de notification avec timeout, depuis les esclaves vers le Master). Les avantages sont que : C'est simple à implémenter et ça repose sur l'implémentation du `JobExecutorParallel` de l'exercice 4. Les esclaves ne font qu'exécuter les tâches et n'ont pas besoin de se connaître mutuellement ou d'échanger de messages. Les pannes sont aussi plus simples à gérer (supposant que le Master est infaillible). En résumé, ce qu'on gagne en nombre de messages transmis et complexité du code des esclaves, on le perd en puissance de calcul sur Master.

### Question 6.5.

L'objet `JobExecutorRemoteDistributed` est la classe cliente d'une JVM qui fait un appel au singleton Master sur une autre JVM via RMI. L'appel est synchrone, et sur la JVM du Master, RMI crée un thread pour gérer cet appel (ressemblant à `Thread[RMI TCP Connection({n°connexion})-{IP_source},5,RMI Runtime]`). Il y a donc autant de threads RMI que de jobs soumis au Master via RMI. Pour gérer plusieurs jobs, il suffit donc de gérer la concurrence. Dans mon implémentation, chaque exécution de job sur Master (méthode `executeJob` du Master) crée un `JobExecutorParallelDistributed` qui gère le job du début à la fin. Il peut y en avoir plusieurs sur Master sans qu'ils ne se gênent. La quasi totalité du code de cette classe est identique à `JobExecutorParallel` et n'en diffère que par la méthode permettant d'exécuter une tâche : localement ou sur un esclave. J'ai donc créé une super-classe abstraite à ces deux `JobExecutor` nommée `JobExecutorParallelAbstract` car dans les deux cas l'exécution est parallèle et requiert les mêmes mécanismes de synchronisation et de gestion d'erreur.  

Il est quand-même à noter que l'équité dans l'exécution des jobs n'est pas forcément respectée : un ancien job n'est pas plus prioritaire qu'un nouveau job (sauf si l'ordonnanceur de la JVM favorise le réveil des anciens threads RMI plutôt que les plus jeunes lors de la recherche d'esclaves pouvant exécuter des tâches).

### Question 6.6.

Là encore, RMI m'a beaucoup facilité la vie. Si l'exécution d'une tâche renvoie une exception autre que `RemoteException` sur un des esclaves, je la renvoie au Master qui la renvoie lui-même à l'objet qui lui a soumis un job au Master, car le job a été mal codé et a renvoyé une exception lors de son exécution. Mais s'il s'agit d'une `RemoteException` levée par l'appel à la méthode sur l'esclave depuis Master, c'est à dire si l'appel à `executeTask` sur le proxy RMI associé à l'esclave sur lequel j'ai choisi d'exécuter une tâche lève l'exception `RemoteException`, c'est que la tâche n'a pas pu se terminer (ou commencer, d'ailleurs) et que le proxy est déconnecté, c'est à dire que l'objet sur l'autre JVM n'est plus joignable. Donc cela veut dire que la tâche n'a pas été exécutée, que je dois supprimer cet esclave de la liste des esclaves enregistrés sur Master (s'il existe toujours, accès concurrents obligent) et rechercher un nouvel esclave sur lequel exécuter ma tâche, en recommençant le processus de sélection d'un esclave comme si la tâche venait d'être prête.

Comme évoqué à la question 6.4, si je n'avais pas utilisé RMI j'aurais du faire un système de notification avec timeout : les esclaves auraient du notifier le Master du résultat de leur exécution d'une tâche, et si aucun résultat n'avait été reçu pendant trop longtemps ou que l'esclave avait été détecté comme étant déconnecté, il aurait fallu réaffecter les tâches perdues. En bref, RMI m'a facilité la vie.  


## Fin du rapport - Sylvain JOUBE - étidiant 3105491 - M1 SAR - UE : SRCS - juin 2020