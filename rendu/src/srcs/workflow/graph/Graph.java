package srcs.workflow.graph;

import java.util.List;
import java.util.Set;

public interface Graph<T> extends Iterable<T> {
	
	
	/** Ajoute un nouveau noeud dans le graphe,
	 *  throw si le noeud existe déjà dans le graphe.
	 *  @param n
	 *  @throws IllegalArgumentException  si le noeud existe déjà dans le graphe.
	 */
	void addNode(T n) throws IllegalArgumentException;
	
	
	/** Ajoute un nouveau lien unidirectionnel partant du nœud from
	 *  et arrivant au noeud to. Une IllegalArgumentException est jetée
	 *  si ni from, ni to n’existent ou bien si le lien existe déjà. 
	 *  @param from
	 *  @param to
	 *  @throws IllegalArgumentException  si ni from, ni to n’existent ou bien si le lien existe déjà.
	 */
	void addEdge(T from, T to) throws IllegalArgumentException;
	
	
	/** Teste l’existence d’un arc entre from et to dans le graphe
	 *  (arc orienté de from à to)
	 *  @param from
	 *  @param to
	 *  @return
	 */
	boolean existEdge(T from, T to);
	
	
	/** Teste l’existence d’un noeud dans le graphe.
	 *  @param n
	 *  @return
	 */
	boolean existNode(T n);
	
	
	/** Teste si le graphe ne contient aucun noeud.
	 *  @return
	 */
	boolean isEmpty();
	
	
	/** Renvoie le nombre de nœuds présents dans le graphe
	 *  @return
	 */
	int size();
	
	
	/** Renvoie la liste des nœuds voisins du nœud from
	 *  via les arcs sortants. Une IllegalArgumentException
	 *  est jetée si from n’existe pas dans le graphe.
	 *  @param from
	 *  @return
	 *  @throws IllegalArgumentException  si from n’existe pas dans le graphe.
	 */
	List<T> getNeighborsOut(T from) throws IllegalArgumentException;
	
	
	/** Renvoie la liste des nœuds voisins du nœud to via les arcs entrants.
	 *  Une IllegalArgumentException est jetée si to n’existe pas dans le graphe.
	 *  @param to
	 *  @return
	 *  @throws IllegalArgumentException  si to n’existe pas dans le graphe.
	 */
	List<T> getNeighborsIn(T to) throws IllegalArgumentException;
	
	
	/** Renvoie la liste des nœuds accessibles à partir de from.
	 *  Une IllegalArgumentException est jetée si from n’existe pas dans le graphe.
	 *  @param from
	 *  @return
	 *  @throws IllegalArgumentException  si from n’existe pas dans le graphe.
	 */
	Set<T> accessible(T from) throws IllegalArgumentException;
	
	
	/** Teste si le graphe est acyclique. On notera que ceci est vrai si aucun des
	 *  noeuds ne peut être accessible à partir de lui-même.
	 *  @return  true si le graphe est acyclique, false sinon.
	 */
	boolean isDAG();
	
}










