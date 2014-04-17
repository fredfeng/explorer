package edu.utexas.cgrex.analyses;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.utexas.cgrex.utils.SootUtils;
import soot.G;
import soot.Local;
import soot.RefType;
import soot.SootField;
import soot.Type;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.ArrayElement;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.GlobalVarNode;
import soot.jimple.spark.pag.LocalVarNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.VarNode;

public class AutoPAG {

	protected PAG father; // just a shallow copy of the pag

	// HashMap<VarNode, VarNode>
	protected Map<Object, Object> match = new HashMap<Object, Object>();

	// HashMap<VarNode, VarNode>
	protected Map<Object, Object> matchInv = new HashMap<Object, Object>();

	// combine match and simple map together
	protected Map<Object, Object> flow = new HashMap<Object, Object>();

	// combine matchInv and simpleInv together
	protected Map<Object, Object> flowInv = new HashMap<Object, Object>();

	protected final static Node[] EMPTY_NODE_ARRAY = new Node[0];

	public AutoPAG(PAG pag) {
		father = pag;
	}

	public void build() {
		createMatch(); // create match and matchInv
		createFlow(); // create flow and flowInv by match and simple
	}

	// given a list of variables to query the type
	// return true if at least one variable points to type by pt analysis
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

		// allocate Ids of the variables
		for (Object obj : allVars) {
			assert (obj instanceof VarNode || obj instanceof FieldRefNode || obj instanceof AllocNode);
			if (obj instanceof VarNode) {
				VarNode var = (VarNode) obj;
				b.append("  ").append("\"VarNode" + var.getNumber() + "\"");
				b.append(" [shape=circle,label=\"");
				b.append(var.getNumber());
				b.append("\"];\n");
			} else if (obj instanceof FieldRefNode) {
				FieldRefNode var = (FieldRefNode) obj;
				b.append("  ")
						.append("\"FieldRefNode" + var.getNumber() + "\"");
				b.append(" [shape=square,label=\"");
				if (var.getField() instanceof SootField)
					b.append(var.getBase().getNumber() + "." + var.getNumber()
							+ "(" + ((SootField) var.getField()).getName()
							+ ")");
				else
					b.append(var.getBase().getNumber() + "." + var.getNumber()
							+ "(ArrayElement)");
				b.append("\"];\n");
			} else if (obj instanceof AllocNode) {
				AllocNode var = (AllocNode) obj;
				b.append("  ").append("\"AllocNode" + var.getNumber() + "\"");
				b.append(" [shape=triangle,label=\"");
				b.append(var.getNumber());
				b.append("\"];\n");
			}
		}

		StringBuilder c = new StringBuilder("");

		// draw the graph
		for (Object ms : match.keySet()) {
			VarNode matchSrc = (VarNode) ms;

			Set<Object> matchTgts = (Set<Object>) match.get(matchSrc);
			for (Object obj : matchTgts) {
				VarNode matchTgt = (VarNode) obj;
				b.append("  ")
						.append("\"VarNode" + matchSrc.getNumber() + "\"");
				b.append(" -> ")
						.append("\"VarNode" + matchTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("match");
				b.append("\"]\n");

				c.append("VarNode: " + matchSrc.getNumber() + "\n"
						+ " get Variable " + matchSrc.getVariable() + "\n"
						+ " with type " + matchSrc.getType() + "\n"
						+ " matchTo VarNode: " + matchTgt.getNumber() + "\n"
						+ " get Variable " + matchTgt.getVariable() + "\n"
						+ " with type " + matchTgt.getType() + "\n\n");
			}
		}

		it = father.simpleSourcesIterator();
		while (it.hasNext()) {
			VarNode assgnSrc = (VarNode) it.next();
			Node[] assgnTgts = father.simpleLookup(assgnSrc);
			for (int i = 0; i < assgnTgts.length; i++) {
				VarNode assgnTgt = (VarNode) assgnTgts[i];
				b.append("  ")
						.append("\"VarNode" + assgnSrc.getNumber() + "\"");
				b.append(" -> ")
						.append("\"VarNode" + assgnTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("assign");
				b.append("\"]\n");

				c.append("VarNode: " + assgnSrc.getNumber() + "\n"
						+ " get Variable " + assgnSrc.getVariable() + "\n"
						+ " with type " + assgnSrc.getType() + "\n"
						+ " assgnTo VarNode: " + assgnTgt.getNumber() + "\n"
						+ " get Variable " + assgnTgt.getVariable() + "\n"
						+ " with type " + assgnTgt.getType() + "\n\n");
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
						.append("\"VarNode" + loadTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("load");
				b.append("\"]\n");

				c.append("FieldRefNode: ");
				if (loadSrc.getField() instanceof SootField)
					c.append(loadSrc.getBase().getNumber() + "."
							+ loadSrc.getNumber() + "("
							+ ((SootField) loadSrc.getField()).getName()
							+ ")\n");
				else
					c.append(loadSrc.getBase().getNumber() + "."
							+ loadSrc.getNumber() + "(ArrayElement)\n\n");
				c.append(" loadTo VarNode: " + loadTgt.getNumber() + "\n"
						+ " get Variable " + loadTgt.getVariable() + "\n"
						+ " with type " + loadTgt.getType() + "\n\n");
			}
		}

		it = father.storeSourcesIterator();
		while (it.hasNext()) {
			VarNode storeSrc = (VarNode) it.next();
			Node[] storeTgts = father.storeLookup(storeSrc);
			for (int i = 0; i < storeTgts.length; i++) {
				FieldRefNode storeTgt = (FieldRefNode) storeTgts[i];
				b.append("  ")
						.append("\"VarNode" + storeSrc.getNumber() + "\"");
				b.append(" -> ")
						.append("\"FieldRefNode" + storeTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("store");
				b.append("\"]\n");

				c.append("VarNode: " + storeSrc.getNumber() + "\n"
						+ " get Variable " + storeSrc.getVariable() + "\n"
						+ " with type " + storeSrc.getType() + "\n");

				c.append(" storeTo FieldRefNode: ");
				if (storeTgt.getField() instanceof SootField)
					c.append(storeTgt.getBase().getNumber() + "."
							+ storeTgt.getNumber() + "("
							+ ((SootField) storeTgt.getField()).getName()
							+ ")\n\n");
				else
					c.append(storeTgt.getBase().getNumber() + "."
							+ storeTgt.getNumber() + "(ArrayElement)\n\n");

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
						.append("\"VarNode" + allocTgt.getNumber() + "\"")
						.append(" [label=\"");
				b.append("new");
				b.append("\"]\n");

				c.append("AllocNode: " + allocSrc.getNumber() + "\n"
						+ " with type " + allocSrc.getType() + "\n"
						+ " allocTo VarNode: " + allocTgt.getNumber() + "\n"
						+ " get Variable " + allocTgt.getVariable() + "\n"
						+ " with type " + allocTgt.getType() + "\n\n");
			}
		}

		b.append("}\n");

		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"src/autopag.dot"));
			BufferedWriter cufw = new BufferedWriter(new FileWriter(
					"sootOutput/autopagDot"));
			cufw.write(c.toString());
			bufw.write(b.toString());
			cufw.close();
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		// System.out.println(b.toString());
	}

	/** protected methods */

	@SuppressWarnings("unchecked")
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

	protected void fillSrc(Map<SootField, Set<VarNode>> storeSrc,
			Set<VarNode> ptAllSrc) {
		for (Iterator<Object> it = father.storeInvSourcesIterator(); it
				.hasNext();) {
			FieldRefNode store = (FieldRefNode) it.next();
			SparkField field = store.getField();

			assert (field instanceof SootField || field instanceof ArrayElement);

			if (field instanceof SootField) {
				Node[] varList = father.storeInvLookup(store);
				for (int i = 0; i < varList.length; i++)
					addToMap(storeSrc, (SootField) field,
							(VarNode) varList[i]);
			} else if (field instanceof ArrayElement) {
				Node[] varList = father.storeInvLookup(store);
				for (int i = 0; i < varList.length; i++)
					ptAllSrc.add((VarNode) varList[i]);
			}
		}
	}

	protected void fillTgt(Map<SootField, Set<VarNode>> loadTgt,
			Set<VarNode> ptAllTgt) {
		for (Iterator<Object> it = father.loadSourcesIterator(); it.hasNext();) {
			FieldRefNode load = (FieldRefNode) it.next();
			SparkField field = load.getField();

			assert(field instanceof SootField);

			if (field instanceof SootField) {
				Node[] varList = father.loadLookup(load);
				for (int i = 0; i < varList.length; i++)
					addToMap(loadTgt, (SootField) field,
							(VarNode) varList[i]);
			} else if (field instanceof ArrayElement) {
				Node[] varList = father.loadLookup(load);
				for (int i = 0; i < varList.length; i++)
					ptAllTgt.add((VarNode) varList[i]);
			}
		}
	}

	protected boolean isCompatible(SootField sf1, SootField sf2) {
		if (sf1.getName() != sf2.getName())
			return false;
		assert(sf1.getType() instanceof RefType);
		//Set<SootClass> subTypeOfSf1 = SootUtils.subTypesOf()
		return true;
	}

	protected void createMatch() {
		// for SootField, add match according to getName + getType
		// Do not use getSignature, signature is Class + Type + Name
		Map<SootField, Set<VarNode>> storeSrc = new HashMap<SootField, Set<VarNode>>(); // from
		Map<SootField, Set<VarNode>> loadTgt = new HashMap<SootField, Set<VarNode>>(); // to
		// for ArrayElement, add match to anyone in target set
		Set<VarNode> ptAllSrc = new HashSet<VarNode>();
		Set<VarNode> ptAllTgt = new HashSet<VarNode>();
		// fill storeSrc and ptAllSrc
		fillSrc(storeSrc, ptAllSrc);
		// fill loadTgt and ptAllTgt
		fillTgt(loadTgt, ptAllTgt);

		// add match edges according to storeSrc and loadTgt maps
		for (SootField s : storeSrc.keySet()) {
			Set<VarNode> srcList = storeSrc.get(s);
			for (SootField t : loadTgt.keySet()) {
				if (isCompatible(s, t)) {
					Set<VarNode> tgtList = loadTgt.get(t);
					for (VarNode src : srcList) {
						for (VarNode tgt : tgtList) {
							doAddMatchEdge(src, tgt);
						}
					}
				}
			}
		}
		// add match edges according to ptAllSrc and ptAllTgt
		for (VarNode src : ptAllSrc) {
			for (VarNode tgt : ptAllTgt) {
				doAddMatchEdge(src, tgt);
			}
		}
	}

	/** protected methods */
	protected boolean addToMap(Map<Object, Object> m, Node key, Node value) {
		Object valueList = m.get(key);

		if (valueList == null) {
			m.put(key, valueList = new HashSet(4));
		} else if (!(valueList instanceof Set)) {
			Node[] ar = (Node[]) valueList;
			HashSet<Node> vl = new HashSet<Node>(ar.length + 4);
			m.put(key, vl);
			for (Node element : ar)
				vl.add(element);
			return vl.add(value);
		}
		return ((Set<Node>) valueList).add(value);
	}

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

	protected boolean addToMap(Map<SootField, Set<VarNode>> m, SootField key,
			VarNode value) {
		Set<VarNode> valueList = m.get(key);

		if (valueList == null) {
			m.put(key, valueList = new HashSet(4));
		}

		return (valueList).add(value);
	}
}
