package edu.utexas.cgrex.analyses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import soot.SootField;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.VarNode;
import soot.options.SparkOptions;

public class AutoPAG extends PAG {

	protected PAG father;

	// HashMap<VarNode, VarNode>
	protected Map<Object, Object> match = new HashMap<Object, Object>();

	// HashMap<VarNode, VarNode>
	protected Map<Object, Object> matchInv = new HashMap<Object, Object>();

	public AutoPAG(PAG pag) {
		super(pag.getOpts());
		father = pag;
	}

	public void build() {
		// time complexity = O(mn), need improvement
		Iterator<Object> storeIt = father.storeSourcesIterator();
		// storeMap: store_src(VarNode) ----> store_targets(FieldRefNode)
		// x.f = a
		while (storeIt.hasNext()) {
			// get the representative of the union
			VarNode from = ((VarNode) storeIt.next()); // storeSrc, a
			Node[] storeTargets = father.storeLookup(from);
			for (int i = 0; i < storeTargets.length; i++) {
				FieldRefNode storeTarget = (FieldRefNode) storeTargets[i]; // x.f
				VarNode storeTargetBase = storeTarget.getBase(); // x
				SparkField storeTargetField = storeTarget.getField(); // f
				String storeTargetFieldSig = ((SootField) storeTargetField)
						.getSignature(); // f's signature
				// three inheritances of SparkField:
				// ArrayElement, Parm, and SootField
				if (storeTargetField instanceof SootField) {
					// b = y.f
					Iterator<Object> loadIt = father.loadSourcesIterator();
					while (loadIt.hasNext()) {
						FieldRefNode loadSrc = (FieldRefNode) loadIt.next(); // y.f
						VarNode loadSrcBase = loadSrc.getBase(); // y
						SparkField loadSrcField = loadSrc.getField(); // f
						if (loadSrcField instanceof SootField) {
							// see whether x.f and y.f has the same offset of
							// the field
							String loadSrcFieldSig = ((SootField) loadSrcField)
									.getSignature(); // f's signature
							if (loadSrcFieldSig.equals(storeTargetFieldSig)) {
								Node[] to = father.loadLookup(loadSrc); // loadTargets,
																		// b
								for (int j = 0; j < to.length; j++) {
									doAddMatchEdge(from, (VarNode) to[j]);
								}
							}
						}
					}
				}
			}
		}
	}

	public void dump() {
		// keep records of all variables in the match edges (both ends)
		// and deal with matchEdges
		Set<Object> allVars = new HashSet<Object>();
		for (Object obj : match.keySet())
			allVars.add(obj);
		for (Object obj : matchInv.keySet())
			allVars.add(obj);
		// simple (assign)
		Iterator<Object> it = father.simpleSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());
		it = father.simpleInvSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());
		// load (put field)
		it = father.loadSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());
		it = father.loadInvSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());
		// store (get field)
		it = father.storeSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());
		it = father.storeInvSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());
		// allocate (new)
		it = father.allocSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());
		it = father.allocInvSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());

		StringBuilder b = new StringBuilder("digraph AutoPAG {\n");
		b.append("  rankdir = LR;\n");

		for (Object obj : allVars) {
			if (obj instanceof VarNode) {
				VarNode var = (VarNode) obj;
				b.append("  ").append("\"VarNode" + var.getNumber() + "\"");
				b.append(" [shape=circle,label=\"");
				b.append(var.getNumber());
				b.append("\"];\n");
			} else if (obj instanceof FieldRefNode) {
				FieldRefNode var = (FieldRefNode) obj;
				b.append("  ").append("\"FieldRefNode" + var.getNumber() + "\"");
				b.append(" [shape=square,label=\"");
				b.append(var.getBase().getNumber() + "." + var.getNumber()
						+ "(" + ((SootField) var.getField()).getName() + ")");
				b.append("\"];\n");
			} else if (obj instanceof AllocNode) {
				AllocNode var = (AllocNode) obj;
				b.append("  ").append("\"AllocNode" + var.getNumber() + "\"");
				b.append(" [shape=triangle,label=\"");
				b.append(var.getNumber());
				b.append("\"];\n");
			}
		}
		
		for (Object ms : match.keySet()) {
			VarNode matchSrc = (VarNode) ms;

			Set<Object> matchTgts = (Set<Object>) match.get(matchSrc);
			for (Object obj : matchTgts) {
				VarNode matchTgt = (VarNode) obj;
				b.append("  ").append("\"VarNode" + matchSrc.getNumber() + "\"");
				b.append(" -> ").append("\"VarNode" + matchTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("match");
				b.append("\"]\n");
			}
		}
		
		it = father.simpleSourcesIterator();
		while (it.hasNext()) {
			VarNode assgnSrc = (VarNode) it.next();
			Node[] assgnTgts = father.simpleLookup(assgnSrc);
			for (int i = 0; i < assgnTgts.length; i++) {
				VarNode assgnTgt = (VarNode) assgnTgts[i];
				b.append("  ").append("\"VarNode" + assgnSrc.getNumber() + "\"");
				b.append(" -> ").append("\"VarNode" + assgnTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("assign");
				b.append("\"]\n");
			}
		}
		
		it = father.loadSourcesIterator();
		while (it.hasNext()) {
			FieldRefNode loadSrc = (FieldRefNode) it.next();
			Node[] loadTgts = father.loadLookup(loadSrc);
			for (int i = 0; i < loadTgts.length; i++) {
				VarNode loadTgt = (VarNode) loadTgts[i];
				b.append("  ").append("\"FieldRefNode" + loadSrc.getNumber() + "\"");
				b.append(" -> ").append("\"VarNode" + loadTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("load");
				b.append("\"]\n");
			}
		}
		
		it = father.storeSourcesIterator();
		while (it.hasNext()) {
			VarNode storeSrc = (VarNode) it.next();
			Node[] storeTgts = father.storeLookup(storeSrc);
			for (int i = 0; i < storeTgts.length; i++) {
				FieldRefNode storeTgt = (FieldRefNode) storeTgts[i];
				b.append("  ").append("\"VarNode" + storeSrc.getNumber() + "\"");
				b.append(" -> ").append("\"FieldRefNode" + storeTgt.getNumber() + "\"").append(" [label=\"");
				b.append("store");
				b.append("\"]\n");
			}
		}
		
		it = father.allocSourcesIterator();
		while (it.hasNext()) {
			AllocNode allocSrc = (AllocNode) it.next();
			Node[] allocTgts = father.allocLookup(allocSrc);
			for (int i = 0; i < allocTgts.length; i++) {
				VarNode allocTgt = (VarNode) allocTgts[i];
				b.append("  ").append("\"AllocNode" + allocSrc.getNumber() + "\"");
				b.append(" -> ").append("\"VarNode" + allocTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("new");
				b.append("\"]\n");
			}
		}
		
		b.append("}\n");
		System.out.println(b.toString());

	}

	public boolean doAddMatchEdge(VarNode from, VarNode to) {
		return addToMap(match, from, to) | addToMap(matchInv, to, from);
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

}
