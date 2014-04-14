package edu.utexas.cgrex.analyses;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.SootField;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.VarNode;
import soot.options.SparkOptions;

public class AutoPAG extends PAG {

	protected PAG father;

	protected Map<Object, Object> match = new HashMap<Object, Object>();

	protected Map<Object, Object> matchInv = new HashMap<Object, Object>();

	public AutoPAG(PAG pag) {
		super(pag.getOpts());
		father = pag;
	}

	public void build() {
		Iterator<Object> storeIt = father.storeSourcesIterator();
		// storeMap: store_src(VarNode) ----> store_targets(FieldRefNode)
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
								Node[] to = father.loadLookup(loadSrc); // loadTargets
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
