package srcs.workflow.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 *  LES VALEURS DES NOEUDS SONT SUPPOSEES UNIQUES.
 *  @author Sylvin JOUBE - M1 SAR
 */
public class GraphImpl<T> implements Graph<T> {
	
	/** Liste des noeuds. Chaquenoeud continent sa valeur associée ainsi qu'une
	 *  liste des noeuds vers lesquels il pointe, et une liste des noeuds qui pointent vers lui.
	 *  J'ai préféré optimiser la complexité en calcul et prendre plus de mémoire que l'inverse. */
	protected List<GraphNode<T>> nodes = new LinkedList<>();
	
	@Override
	public Iterator<T> iterator() {
		return new GraphIterator<T>(nodes);
	}
	
	@Override
	public void addNode(T n) throws IllegalArgumentException {
		GraphNode<T> exists = getSysNode(n);
		if (exists != null) {
			throw new IllegalArgumentException("Le noeud existe déjà.");
		}
		
		GraphNode<T> newNode = new GraphNode<>(n);
		nodes.add(newNode);
	}

	@Override
	public void addEdge(T from, T to) throws IllegalArgumentException {
		GraphNode<T> sysTo, sysFrom;

		sysFrom = getSysNode(from);
		sysTo = getSysNode(to);
		
		// Si au moins un des noeuds n'existe pas, c'est une erreur.
		if ( (sysFrom == null) || (sysTo == null) ) {
			throw new IllegalArgumentException("Au moins un des noeuds passés en paramètre n'existe pas dans le graphe.");
		}
		
		// Je vérifie que le lien n'existe pas
		if (sysFrom.outValueExists(to)) {
			throw new IllegalArgumentException("Le lien existe déjà.");
		}
		
		// Ajout du lien entre les deux noeuds
		sysFrom.addSysOut(sysTo);
	}

	@Override
	public boolean existEdge(T from, T to) {
		GraphNode<T> sysFrom;

		sysFrom = getSysNode(from);
		
		// Si au moins un des noeuds n'existe pas, c'est une erreur.
		if ( (sysFrom == null) ) return false;
		
		return sysFrom.outValueExists(to);
	}

	@Override
	public boolean existNode(T n) {
		return (getSysNode(n) != null);
	}

	@Override
	public boolean isEmpty() {
		return nodes.isEmpty();
	}

	@Override
	public int size() {
		return nodes.size();
	}

	@Override
	public List<T> getNeighborsOut(T from) throws IllegalArgumentException {
		
		GraphNode<T> sysFrom = getSysNode(from);
		
		if (sysFrom == null) throw new IllegalArgumentException("Le noeud n'existe pas dans le graphe.");
		
		return sysFrom.getOutValues();
	}

	@Override
	public List<T> getNeighborsIn(T to) throws IllegalArgumentException {
		GraphNode<T> sysTo = getSysNode(to);
		
		if (sysTo == null) throw new IllegalArgumentException("Le noeud n'existe pas dans le graphe.");
		
		return sysTo.getInValues();
	}

	@Override
	public Set<T> accessible(T from) throws IllegalArgumentException {
		Set<T> result = new HashSet<>();
		
		GraphNode<T> sysFrom = getSysNode(from);
		
		if (sysFrom == null) throw new IllegalArgumentException("Le noeud n'existe pas dans le graphe.");
		
		accessibleRecur(from, result);
		
		return result;
	}
	
	protected void accessibleRecur(T from, Set<T> result) {
		
		// Tous les noeuds pointés sont supposés exister.
		GraphNode<T> sysFrom = getSysNode(from);
		
		for (T value : sysFrom.getOutValues()) {
			// Ajout récursif des noeuds pointés
			if (! result.contains(value)) {
				result.add(value);
				accessibleRecur(value, result);
			}
		}
		
	}

	@Override
	public boolean isDAG() {
		
		for (GraphNode<T> node : nodes) {
			// S'il est possible d'accéder depuis node.value à node.value,
			// c'est que le graphe a au moins un cycle.
			if (accessible(node.value).contains(node.value)) {
				return false;
			}
		}
		
		return true;
	}
	
	// ----- Partie système -----
	
	/** Trouver le noeud (GraphNode) associé à la valeur value.
	 *  Si aucun noeud n'est trouvé, null est renvoyé.
	 *  @param value
	 *  @return
	 */
	protected GraphNode<T> getSysNode(T value) {
		for (GraphNode<T> node : nodes) {
			if (node.getValue().equals(value)) {
				return node;
			}
		}
		return null;
	}
}


class GraphIterator<T> implements Iterator<T> {
	
	protected final Iterator<GraphNode<T>> sysIterator;
	
	public GraphIterator(List<GraphNode<T>> nodes) {
		sysIterator = nodes.iterator();
	}
	
	@Override
	public boolean hasNext() {
		return sysIterator.hasNext();
	}

	@Override
	public T next() {
		GraphNode<T> node = sysIterator.next();
		if (node == null) return null;
		return node.getValue();
	}
	
}

/**
 *  LES VALEURS DES NOEUDS SONT SUPPOSEES UNIQUES.
 */
class GraphNode<T> {
	
	/** Objet associé */
	protected final T value;
	
	/** Liste des noeuds vers lesquels pointe ce noeud (arcs sortants) */
	protected List<GraphNode<T>> pointsOut;
	
	/** Liste des noeurs pointant vers ce noeud (arcs entrants) */
	protected List<GraphNode<T>> pointsIn;
	
	public GraphNode(T value) {
		this.value = value;
		pointsOut = new ArrayList<>();
		pointsIn = new ArrayList<>();
	}
	
	public T getValue() {
		return value;
	}
	
	/** Ajout d'un noeud vers lequel je pointe.
	 *  Aucune vérification n'est faite, ici.
	 *  
	 *  Attention, appeler une seule fois soit addSysOut soit addSysIn.
	 *  Appeler les deux reviendrait à ajouter le lien deux fois.
	 */
	public void addSysOut(GraphNode<T> out) {
		pointsOut.add(out);
		
		// Sur l'autre noeud, j'ajoute que je pointe vers lui
		out.pointsIn.add(this);
	}
	
	/** Ajout d'un noeud qui pointe vers moi.
	 *  Aucune vérification n'est faite, ici.
	 */
	public void addSysIn(GraphNode<T> in) {
		pointsIn.add(in);

		// Sur l'autre noeud, j'ajoute que qu'il pointe vers moi
		in.pointsOut.add(this);
	}
	
	/*public List<GraphNode<T>> getList() {
		return pointsOut;
	}*/
	
	
	/** Regarde je pointe vers un noeud ayant la valeur indiquée.
	 *  @param to
	 *  @return
	 */
	public boolean outValueExists(T to) {
		for (GraphNode<T> node : pointsOut) {
			if (node.getValue().equals(to)) {
				return true;
			}
		}
		return false;
	}

	/** Regarde je suis pointé par un noeud ayant la valeur indiquée.
	 *  @param in
	 *  @return
	 */
	public boolean inValueExists(T in) {
		for (GraphNode<T> node : pointsIn) {
			if (node.getValue().equals(in)) {
				return true;
			}
		}
		return false;
	}
	
	

	/** Retourne la liste des valeurs pointées par ce noeud
	 * @return
	 */
	public List<T> getOutValues() {
		ArrayList<T> result = new ArrayList<>();
		result.ensureCapacity(pointsOut.size());
		for (GraphNode<T> node : pointsOut) {
			result.add(node.getValue());
		}
		return result;
	}

	/** Retourne la liste des valeurs qui pointent vers ce noeud
	 * @return
	 */
	public List<T> getInValues() {
		ArrayList<T> result = new ArrayList<>();
		result.ensureCapacity(pointsIn.size());
		for (GraphNode<T> node : pointsIn) {
			result.add(node.getValue());
		}
		return result;
	}
	
}
















