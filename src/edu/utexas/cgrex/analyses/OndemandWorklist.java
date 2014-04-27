package edu.utexas.cgrex.analyses;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import soot.G;
import soot.MethodOrMethodContext;
import soot.jimple.spark.pag.AllocDotField;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.spark.solver.OnFlyCallGraph;
import soot.jimple.spark.solver.Propagator;
import soot.jimple.spark.solver.TopoSorter;
import soot.util.IdentityHashSet;
import soot.util.queue.QueueReader;

/**
 * Propagating points-to set using xinyu's points-to analysis
 * 
 * @author yufeng
 * 
 */

public final class OndemandWorklist extends Propagator {

	public boolean debug = true;
	protected final Set<VarNode> varNodeWorkList = new TreeSet<VarNode>();

	protected final AutoPAG me;

	public OndemandWorklist(PAG pag) {
		this.pag = pag;
		me = new AutoPAG(pag);
		me.build();
	}

	public int count = 0;

	/** Actually does the propagation. */
	public final void propagate() {
		ofcg = pag.getOnFlyCallGraph();
		new TopoSorter(pag, false).sort();
		for (Object object : pag.allocSources()) {
			// initially add the AllocNode to the VarNodes that are directly
			// connected to the AllocNodes
			handleAllocNode((AllocNode) object);
		}

		pag.dump();

		do {
			if (debug) {
				G.v().out.println("Worklist has " + varNodeWorkList.size()
						+ " nodes.");
			}
			System.out.println("***********" + varNodeWorkList.size());
			while (!varNodeWorkList.isEmpty()) {
				VarNode src = varNodeWorkList.iterator().next();
				varNodeWorkList.remove(src);
				handleVarNode(src);
			}

			if (debug) {
				G.v().out.println("Now handling field references");
			}
			for (Object object : pag.storeSources()) {
				final VarNode src = (VarNode) object;
				Node[] targets = pag.storeLookup(src);
				for (Node element0 : targets) {
					final FieldRefNode target = (FieldRefNode) element0;
					target.getBase().makeP2Set().forall(new P2SetVisitor() {
						public final void visit(Node n) {
							AllocDotField nDotF = pag.makeAllocDotField(
									(AllocNode) n, target.getField());

							// my implementation
							Set<AllocNode> pt = me.insensitivePTAnalysis(src);
							for (AllocNode alc : pt) {
								nDotF.makeP2Set().add(alc);
							}

							// soot implementation
							// nDotF.makeP2Set().addAll(src.getP2Set(), null);
						}
					});
				}
			}
			HashSet<Object[]> edgesToPropagate = new HashSet<Object[]>();
			for (Object object : pag.loadSources()) {
				handleFieldRefNode((FieldRefNode) object, edgesToPropagate);
			}
			IdentityHashSet<PointsToSetInternal> nodesToFlush = new IdentityHashSet<PointsToSetInternal>();
			for (Object[] pair : edgesToPropagate) {
				PointsToSetInternal nDotF = (PointsToSetInternal) pair[0];
				PointsToSetInternal newP2Set = nDotF.getNewSet();
				VarNode loadTarget = (VarNode) pair[1];
				if (loadTarget.makeP2Set().addAll(newP2Set, null)) {
					varNodeWorkList.add(loadTarget);
				}
				nodesToFlush.add(nDotF);
			}
			for (PointsToSetInternal nDotF : nodesToFlush) {
				nDotF.flushNew();
			}
		} while (!varNodeWorkList.isEmpty());

		System.out.println("***********count: " + count);
	}

	/* End of public methods. */
	/* End of package methods. */

	/**
	 * Propagates new points-to information of node src to all its successors.
	 */
	// given src, propagate src to all VarNodes by pag.allocLookup and add the
	// VarNodes to the worklist because they might result in some more varNodes'
	// pt set to be updated
	protected final boolean handleAllocNode(AllocNode src) {
		boolean ret = false;
		Node[] targets = pag.allocLookup(src);

		for (Node element : targets) {

			// my implementation
			Set<AllocNode> pt = me.insensitivePTAnalysis((VarNode) element);
			boolean succ = false;
			for (AllocNode alc : pt) {
				succ = succ | element.makeP2Set().add(alc);
			}
			if (succ) {
				varNodeWorkList.add((VarNode) element);
				ret = true;
			}

			// following is soot implementation
			// if (element.makeP2Set().add(src)) {
			// varNodeWorkList.add((VarNode) element);
			// ret = true;
			// }

		}
		return ret;
	}

	/**
	 * Propagates new points-to information of node src to all its successors.
	 */
	protected final boolean handleVarNode(final VarNode src) {
		boolean ret = false;
		boolean flush = true;

		if (src.getReplacement() != src)
			throw new RuntimeException("Got bad node " + src + " with rep "
					+ src.getReplacement());

		final PointsToSetInternal newP2Set = src.getP2Set().getNewSet();
		if (newP2Set.isEmpty())
			return false;

		if (ofcg != null) {
			QueueReader addedEdges = pag.edgeReader();
			ofcg.updatedNode(src);
			ofcg.build();

			// update the autopag and then rebuild
			me.update(pag);
			me.build();

			while (addedEdges.hasNext()) {
				Node addedSrc = (Node) addedEdges.next();
				Node addedTgt = (Node) addedEdges.next();
				ret = true;
				if (addedSrc instanceof VarNode) {
					if (addedTgt instanceof VarNode) {
						VarNode edgeSrc = (VarNode) addedSrc.getReplacement();
						VarNode edgeTgt = (VarNode) addedTgt.getReplacement();

						// my implementation
						Set<AllocNode> pt = me
								.insensitivePTAnalysis((VarNode) edgeSrc);
						boolean succ = false;
						for (AllocNode alc : pt) {
							succ = succ | edgeTgt.makeP2Set().add(alc);
						}
						if (succ) {
							varNodeWorkList.add((VarNode) edgeTgt);
							if (edgeTgt == src)
								flush = false;
						}

						// following is soot implementation
						// if (edgeTgt.makeP2Set()
						// .addAll(edgeSrc.getP2Set(), null)) {
						// varNodeWorkList.add(edgeTgt);
						// if (edgeTgt == src)
						// flush = false;
						// }
					}
				} else if (addedSrc instanceof AllocNode) {
					AllocNode edgeSrc = (AllocNode) addedSrc;
					VarNode edgeTgt = (VarNode) addedTgt.getReplacement();

					// my implementation
					Set<AllocNode> pt = me
							.insensitivePTAnalysis((VarNode) edgeTgt);
					boolean succ = false;
					for (AllocNode alc : pt) {
						succ = succ | edgeTgt.makeP2Set().add(alc);
					}
					if (succ) {
						varNodeWorkList.add((VarNode) edgeTgt);
						if (edgeTgt == src)
							flush = false;
					}

					// soot implementation
					// if (edgeTgt.makeP2Set().add(edgeSrc)) {
					// varNodeWorkList.add(edgeTgt);
					// if (edgeTgt == src)
					// flush = false;
					// }
				}
			}
		}

		Node[] simpleTargets = pag.simpleLookup(src);
		for (Node element : simpleTargets) {

			// my implementation
			Set<AllocNode> pt = me.insensitivePTAnalysis((VarNode) element);
			boolean succ = false;
			for (AllocNode alc : pt) {
				succ = succ | element.makeP2Set().add(alc);
			}
			if (succ) {
				varNodeWorkList.add((VarNode) element);
				if (element == src)
					flush = false;
				ret = true;
			}

			// soot implementation
			// if (element.makeP2Set().addAll(newP2Set, null)) {
			// varNodeWorkList.add((VarNode) element);
			// if (element == src)
			// flush = false;
			// ret = true;
			// }
		}

		Node[] storeTargets = pag.storeLookup(src);
		for (Node element : storeTargets) {
			final FieldRefNode fr = (FieldRefNode) element;
			final SparkField f = fr.getField();
			ret = fr.getBase().getP2Set().forall(new P2SetVisitor() {
				public final void visit(Node n) {
					AllocDotField nDotF = pag.makeAllocDotField((AllocNode) n,
							f);

					// my implementation
					Set<AllocNode> pt = me.insensitivePTAnalysis(nDotF);
					for (AllocNode alc : pt) {
						nDotF.makeP2Set().add(alc);
					}

					// soot implementation
					// if (nDotF.makeP2Set().addAll(newP2Set, null)) {
					// returnValue = true;
					// }
				}
			})
					| ret;
		}

		final HashSet<Node[]> storesToPropagate = new HashSet<Node[]>();
		final HashSet<Node[]> loadsToPropagate = new HashSet<Node[]>();
		for (final FieldRefNode fr : src.getAllFieldRefs()) {
			final SparkField field = fr.getField();
			final Node[] storeSources = pag.storeInvLookup(fr);
			if (storeSources.length > 0) {
				newP2Set.forall(new P2SetVisitor() {
					public final void visit(Node n) {
						AllocDotField nDotF = pag.makeAllocDotField(
								(AllocNode) n, field);
						for (Node element : storeSources) {
							Node[] pair = { element, nDotF.getReplacement(), fr };
							storesToPropagate.add(pair);
						}
					}
				});
			}

			final Node[] loadTargets = pag.loadLookup(fr);
			if (loadTargets.length > 0) {
				newP2Set.forall(new P2SetVisitor() {
					public final void visit(Node n) {
						AllocDotField nDotF = pag.makeAllocDotField(
								(AllocNode) n, field);
						if (nDotF != null) {
							for (Node element : loadTargets) {
								Node[] pair = { nDotF.getReplacement(),
										element, fr };
								loadsToPropagate.add(pair);
							}
						}
					}
				});
			}
		}
		if (flush)
			src.getP2Set().flushNew();
		for (Node[] p : storesToPropagate) {
			VarNode storeSource = (VarNode) p[0];
			AllocDotField nDotF = (AllocDotField) p[1];
			FieldRefNode fr = (FieldRefNode) p[2];

			// my implementation
			// Set<AllocNode> pt = me.insensitivePTAnalysis(storeSource);
			// boolean succ = false;
			// for (AllocNode alc : pt) {
			// succ = succ | nDotF.makeP2Set().add(alc);
			// }
			// if (succ) {
			// ret = true;
			// }
			boolean succ = false;
			Set<AllocNode> pt = me.insensitivePTAnalysis(nDotF);
			for (AllocNode alc : pt) {
				succ = succ | nDotF.makeP2Set().add(alc);
			}
			if (succ)
				ret = true;

			// soot implementation
			// if (nDotF.makeP2Set().addAll(storeSource.getP2Set(), null)) {
			// ret = true;
			// }
		}
		for (Node[] p : loadsToPropagate) {
			AllocDotField nDotF = (AllocDotField) p[0];
			VarNode loadTarget = (VarNode) p[1];
			FieldRefNode fr = (FieldRefNode) p[2];

			// soot implementation
			// I did not find a proper way to use my pointer analysis
			// and actually I think this should use the soot implementation
			// because even if using my pointer analysis, I can do it in this
			// way which is more concise
			if (loadTarget.makeP2Set().addAll(nDotF.getP2Set(), null)) {
				varNodeWorkList.add(loadTarget);
				ret = true;
			}
		}

		return ret;
	}

	/**
	 * Propagates new points-to information of node src to all its successors.
	 */
	protected final void handleFieldRefNode(FieldRefNode src,
			final HashSet<Object[]> edgesToPropagate) {
		count++;
		final Node[] loadTargets = pag.loadLookup(src);
		if (loadTargets.length == 0)
			return;
		final SparkField field = src.getField();

		src.getBase().getP2Set().forall(new P2SetVisitor() {

			public final void visit(Node n) {
				AllocDotField nDotF = pag.makeAllocDotField((AllocNode) n,
						field);
				if (nDotF != null) {
					PointsToSetInternal p2Set = nDotF.getP2Set();
					if (!p2Set.getNewSet().isEmpty()) {
						for (Node element : loadTargets) {
							Object[] pair = { p2Set, element };
							edgesToPropagate.add(pair);
						}
					}
				}
			}
		});
	}

	protected PAG pag;
	protected OnFlyCallGraph ofcg;
}
