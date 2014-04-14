package edu.utexas.cgrex.analyses;

import java.util.*;

import soot.G;
import soot.SootField;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.VarNode;

public class MatchEdges {
	
	protected Map<Object, Object> match = new HashMap<Object, Object>();
	
	protected Map<Object, Object> matchInv = new HashMap<Object, Object>();

	/** constructor, add edges to maps */
	public MatchEdges(PAG pag) {
		Iterator<Object> store_it = pag.storeSourcesIterator();
		// storeMap: store_src(VarNode) ----> store_targets(FieldRefNode)
		while (store_it.hasNext()) {
			// get the representative of the union
			VarNode from = ((VarNode) store_it.next()); // storeSrc, a
			Node[] storeTargets = pag.storeLookup(from);
			for (int i = 0; i < storeTargets.length; i++) {
				FieldRefNode storeTarget = (FieldRefNode) storeTargets[i]; // x.f
				VarNode storeTargetBase = storeTarget.getBase(); // x
				SparkField storeTargetField = storeTarget.getField(); // f
				String storeTargetFieldSig = ((SootField) storeTargetField)
						.getSignature(); // f's signature
				// three inheritances of SparkField:
				// ArrayElement, Parm, and SootField
				if (storeTargetField instanceof SootField) {
					Iterator<Object> load_it = pag.loadSourcesIterator();
					while (load_it.hasNext()) {
						FieldRefNode loadSrc = (FieldRefNode) load_it.next(); // y.f
						VarNode loadSrcBase = loadSrc.getBase(); // y
						SparkField loadSrcField = loadSrc.getField(); // f
						if (loadSrcField instanceof SootField) {
							// see whether x.f and y.f has the same offset of
							// the field
							String loadSrcFieldSig = ((SootField) loadSrcField)
									.getSignature(); // f's signature
							if (loadSrcFieldSig.equals(storeTargetFieldSig)) {
								Node[] to = pag.loadLookup(loadSrc); // loadTargets
								for (int j = 0; j < to.length; j++) {
									addMatchEdge(from, (VarNode) to[j]);
								}
							}
						}
					}
				}
			}
		}
	}

	/** add a match edge between from and to into PAG */
	public boolean addMatchEdge(VarNode from, VarNode to) {
		return addToMap(match, from, to) | addToMap(matchInv, to, from);
	}

	/** add the specified edge into PAG */
	protected boolean addToMap(Map<Object, Object> m, Node key, Node value) {
		Object valueList = m.get(key);

		if (valueList == null) {
			m.put(key, valueList = new HashSet(4));
		} else if (!(valueList instanceof Set)) {
			// I still do not know what this means
			Node[] ar = (Node[]) valueList;
			HashSet<Node> vl = new HashSet<Node>(ar.length + 4);
			m.put(key, vl);
			for (Node elem : ar) {
				vl.add(elem);
			}
			return vl.add(value);
		}

		// because it's Set, we can just add without checking
		return ((Set<Node>) valueList).add(value);
	}

	public Iterator<Object> matchSourcesIterator() {
		return match.keySet().iterator();
	}

	public Iterator<Object> matchInvSourcesIterator() {
		return match.keySet().iterator();
	}

	public Set<Object> matchSources() {
		return match.keySet();
	}

	public Set<Object> matchInvSources() {
		return match.keySet();
	}

	public Node[] matchLookup(VarNode key) {
		return lookup(match, key);
	}

	public Node[] matchInvLookup(VarNode key) {
		return lookup(matchInv, key);
	}

	protected final static Node[] EMPTY_NODE_ARRAY = new Node[0];

	protected Node[] lookup(Map<Object, Object> m, Object key) {
		Object valueList = m.get(key);
		if (valueList == null) {
			return EMPTY_NODE_ARRAY;
		}
		if (valueList instanceof Set) {
			try {
				m.put(key,
						valueList = ((Set) valueList).toArray(EMPTY_NODE_ARRAY));
			} catch (Exception e) {
				for (Iterator it = ((Set) valueList).iterator(); it.hasNext();) {
					G.v().out.println(" " + it.next());
				}
				throw new RuntimeException(" " + valueList + e);
			}
		}
		Node[] ret = (Node[]) valueList;
		return ret;
	}
}
