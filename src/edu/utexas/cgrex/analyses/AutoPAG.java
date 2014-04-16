package edu.utexas.cgrex.analyses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.SootField;
import soot.Type;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.VarNode;

public class AutoPAG extends PAG {

	protected PAG father;

	// HashMap<VarNode, VarNode>
	protected Map<Object, Object> match = new HashMap<Object, Object>();

	// HashMap<VarNode, VarNode>
	protected Map<Object, Object> matchInv = new HashMap<Object, Object>();

	protected Map<Object, Object> flow = new HashMap<Object, Object>();

	protected Map<Object, Object> flowInv = new HashMap<Object, Object>();

	public AutoPAG(PAG pag) {
		super(pag.getOpts());
		father = pag;
	}

	public void build() {
		createMatch(); // create match and matchInv
		createFlow(); // create flow and flowInv by match and simple
	}

	public boolean query(List<Local> vars, Type type) {

		return false;
	}

	public boolean doAddMatchEdge(VarNode from, VarNode to) {
		return addToMap(match, from, to) | addToMap(matchInv, to, from);
	}

	public boolean addFlowEdge(VarNode from, VarNode to) {
		return addToMap(flow, from, to) | addToMap(flowInv, to, from);
	}

	public Iterator<Object> matchSourcesIterator() {
		return match.keySet().iterator();
	}

	public Iterator<Object> matchInvSourcesIterator() {
		return match.keySet().iterator();
	}

	public Iterator<Object> flowSourcesIterator() {
		return flow.keySet().iterator();
	}

	public Iterator<Object> flowInvSourcesIterator() {
		return flowInv.keySet().iterator();
	}

	public Set<Object> matchSources() {
		return match.keySet();
	}

	public Set<Object> matchInvSources() {
		return match.keySet();
	}

	public Set<Object> flowSources() {
		return flow.keySet();
	}

	public Set<Object> flowInvSources() {
		return flowInv.keySet();
	}

	public Node[] matchLookup(VarNode key) {
		return lookup(match, key);
	}

	public Node[] matchInvLookup(VarNode key) {
		return lookup(matchInv, key);
	}

	public Node[] flowLookup(VarNode key) {
		return lookup(flow, key);
	}

	public Node[] flowInvLookup(VarNode key) {
		return lookup(flowInv, key);
	}

	public void dumpFlow() {
		// maintain a set of all VarNodes
		Set<Object> allVars = new HashSet<Object>();
		for (Object obj : flow.keySet())
			allVars.add(obj);
		for (Object obj : flowInv.keySet())
			allVars.add(obj);
		Iterator<Object> it = father.allocSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());
		it = father.allocInvSourcesIterator();
		while (it.hasNext())
			allVars.add(it.next());

		StringBuilder b = new StringBuilder("digraph AutoPAG {\n");
		b.append("  rankdir = LR;\n");
		// create nodes in the dumped graph
		for (Object obj : allVars) {
			if (obj instanceof VarNode) {
				VarNode var = (VarNode) obj;
				b.append("  ").append("\"VarNode" + var.getNumber() + "\"");
				b.append(" [shape=circle,label=\"");
				b.append(var.getNumber());
				b.append("\"];\n");
			} else if (obj instanceof AllocNode) {
				AllocNode var = (AllocNode) obj;
				b.append("  ").append("\"AllocNode" + var.getNumber() + "\"");
				b.append(" [shape=triangle,label=\"");
				b.append(var.getNumber());
				b.append("\"];\n");
			}
		}

		for (Object fl : flowInv.keySet()) {
			VarNode flowInvSrc = (VarNode) fl;

			Set<Object> flowInvTgts = (Set<Object>) flowInv.get(flowInvSrc);
			for (Object obj : flowInvTgts) {
				VarNode flowInvTgt = (VarNode) obj;
				b.append("  ").append(
						"\"VarNode" + flowInvSrc.getNumber() + "\"");
				b.append(" -> ")
						.append("\"VarNode" + flowInvTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("flowTo");
				b.append("\"]\n");
				System.out.println("VarNode: " + flowInvSrc.getNumber()
						+ " with type " + flowInvSrc.getType()
						+ " flowTo VarNode: " + flowInvTgt.getNumber()
						+ " with type " + flowInvTgt.getType());
			}
		}

		it = father.allocInvSourcesIterator();
		while (it.hasNext()) {
			VarNode allocInvSrc = (VarNode) it.next();
			Node[] allocTgts = father.allocInvLookup(allocInvSrc);
			for (int i = 0; i < allocTgts.length; i++) {
				AllocNode allocInvTgt = (AllocNode) allocTgts[i];
				b.append("  ").append(
						"\"VarNode" + allocInvSrc.getNumber() + "\"");
				b.append(" -> ")
						.append("\"AllocNode" + allocInvTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("new");
				b.append("\"]\n");
				System.out.println("VarNode: " + allocInvSrc.getNumber()
						+ " with type " + allocInvSrc.getType()
						+ " flowTo AllocNode: " + allocInvTgt.getNumber()
						+ " with type " + allocInvTgt.getType());
			}
		}

		b.append("}\n");
		System.out.println(b.toString());
	}

	public void dump() {
		// keep records of all variables in the match edges (both ends)
		Set<Object> allVars = new HashSet<Object>();
		// deal with matchEdges
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
				b.append("  ").append("\"VarNode" + var.getVariable().hashCode() + "\"");
				b.append(" [shape=circle,label=\"");
				b.append(var.getNumber());
				b.append("\"];\n");
			} else if (obj instanceof FieldRefNode) {
				FieldRefNode var = (FieldRefNode) obj;
				b.append("  ")
						.append("\"FieldRefNode" + var.getNumber() + "\"");
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
		
		//
		for (Object ms : match.keySet()) {
			VarNode matchSrc = (VarNode) ms;

			Set<Object> matchTgts = (Set<Object>) match.get(matchSrc);
			for (Object obj : matchTgts) {
				VarNode matchTgt = (VarNode) obj;
				b.append("  ")
						.append("\"VarNode" + matchSrc.getVariable().hashCode() + "\"");
				b.append(" -> ")
						.append("\"VarNode" + matchTgt.getVariable().hashCode() + "\"")
						.append(" [label=\"");
				b.append("match");
				b.append("\"]\n");
				
				System.out.println("VarNode: " + matchSrc.getNumber()
						+ " get Variable " + matchSrc.getVariable()
						+ " with type " + matchSrc.getType()
						+ " matchTo VarNode: " + matchTgt.getNumber()
						+ " get Variable " + matchTgt.getVariable()
						+ " with type " + matchTgt.getType());
			}
		}

		it = father.simpleSourcesIterator();
		while (it.hasNext()) {
			VarNode assgnSrc = (VarNode) it.next();
			Node[] assgnTgts = father.simpleLookup(assgnSrc);
			for (int i = 0; i < assgnTgts.length; i++) {
				VarNode assgnTgt = (VarNode) assgnTgts[i];
				b.append("  ")
						.append("\"VarNode" + assgnSrc.getVariable().hashCode() + "\"");
				b.append(" -> ")
						.append("\"VarNode" + assgnTgt.getVariable().hashCode() + "\"")
						.append(" [label=\"");
				b.append("assign");
				b.append("\"]\n");
				
				System.out.println("VarNode: " + assgnSrc.getNumber()
						+ " get Variable " + assgnSrc.getVariable()
						+ " with type " + assgnSrc.getType()
						+ " assgnTo VarNode: " + assgnTgt.getNumber()
						+ " get Variable " + assgnTgt.getVariable()
						+ " with type " + assgnTgt.getType());
			}
		}

		it = father.loadSourcesIterator();
		while (it.hasNext()) {
			FieldRefNode loadSrc = (FieldRefNode) it.next();
			Node[] loadTgts = father.loadLookup(loadSrc);
			for (int i = 0; i < loadTgts.length; i++) {
				VarNode loadTgt = (VarNode) loadTgts[i];
				b.append("  ").append(
						"\"FieldRefNode" + loadSrc.getNumber() + "\"");
				b.append(" -> ")
						.append("\"VarNode" + loadTgt.getVariable().hashCode() + "\"")
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
				b.append("  ")
						.append("\"VarNode" + storeSrc.getVariable().hashCode() + "\"");
				b.append(" -> ")
						.append("\"FieldRefNode" + storeTgt.getNumber() + "\"")
						.append(" [label=\"");
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
				b.append("  ").append(
						"\"AllocNode" + allocSrc.getNumber() + "\"");
				b.append(" -> ")
						.append("\"VarNode" + allocTgt.getVariable().hashCode() + "\"")
						.append(" [label=\"");
				b.append("new");
				b.append("\"]\n");
				
				System.out.println("AllocNode: " + allocSrc.getNumber()
						+ " get Variable " + " no available "
						+ " with type " + allocSrc.getType()
						+ " allocTo VarNode: " + allocTgt.getNumber()
						+ " get Variable " + allocTgt.getVariable()
						+ " with type " + allocTgt.getType());
			}
		}

		b.append("}\n");
		System.out.println(b.toString());
	}

	/** protected methods */

	protected void createFlow() {
		// first fill flow by simple
		Iterator<Object> it = father.simpleSourcesIterator();
		while (it.hasNext()) {
			VarNode simpleSrc = (VarNode) it.next();
			Node[] simpleTgts = father.simpleLookup(simpleSrc);
			for (int i = 0; i < simpleTgts.length; i++) {
				addFlowEdge(simpleSrc, (VarNode) simpleTgts[i]);
			}
		}
		// then fill flow by match
		for (Object obj : match.keySet()) {
			VarNode matchSrc = (VarNode) obj;
			Set<Object> matchTgts = (Set<Object>) match.get(obj);
			for (Object matchTgt : matchTgts)
				addFlowEdge(matchSrc, (VarNode) matchTgt);
		}
	}

	protected void createMatch() {
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

}
