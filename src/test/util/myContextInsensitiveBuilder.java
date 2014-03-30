package test.util;

import java.util.Iterator;

import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.internal.SparkNativeHelper;
import soot.jimple.spark.pag.MethodPAG;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.solver.OnFlyCallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.pointer.util.NativeMethodDriver;
import soot.options.SparkOptions;
import soot.util.queue.QueueReader;

//inherit the ContextInsensitiveBuilder to setup and build myPAG
public class myContextInsensitiveBuilder extends ContextInsensitiveBuilder {
	private myPAG mypag;
	private CallGraphBuilder cgb;
	private OnFlyCallGraph ofcg;
	private ReachableMethods reachables;
	int classes = 0;
	int totalMethods = 0;
	int analyzedMethods = 0;
	int stmts = 0;

	@Override
	public myPAG setup(SparkOptions opts) {
		mypag = opts.geom_pta() ? new myGeomPointsTo(opts) : new myPAG(opts);
		if (opts.simulate_natives()) {
			mypag.nativeMethodDriver = new NativeMethodDriver(
					new SparkNativeHelper(mypag));
		}
		if (opts.on_fly_cg() && !opts.vta()) {
			ofcg = new OnFlyCallGraph(mypag);
			mypag.setOnFlyCallGraph(ofcg);
		} else {
			cgb = new CallGraphBuilder(DumbPointerAnalysis.v());
		}
		return mypag;
	}

	@Override
	public void build() {
		QueueReader callEdges;
		if (ofcg != null) {
			callEdges = ofcg.callGraph().listener();
			ofcg.build();
			reachables = ofcg.reachableMethods();
			reachables.update();
		} else {
			callEdges = cgb.getCallGraph().listener();
			cgb.build();
			reachables = cgb.reachables();
		}
		for (Iterator cIt = Scene.v().getClasses().iterator(); cIt.hasNext();) {
			final SootClass c = (SootClass) cIt.next();
			handleClass(c);
		}
		while (callEdges.hasNext()) {
			Edge e = (Edge) callEdges.next();
			MethodPAG.v(mypag, e.tgt()).addToPAG(null);
			mypag.addCallTarget(e);
		}
		if (mypag.getOpts().verbose()) {
			G.v().out.println("Total methods: " + totalMethods);
			G.v().out
					.println("Initially reachable methods: " + analyzedMethods);
			G.v().out.println("Classes with at least one reachable method: "
					+ classes);
		}
	}

	@Override
	protected void handleClass(SootClass c) {
		boolean incedClasses = false;
		Iterator methodsIt = c.methodIterator();
		while (methodsIt.hasNext()) {
			SootMethod m = (SootMethod) methodsIt.next();
			if (!m.isConcrete() && !m.isNative())
				continue;
			totalMethods++;
			if (reachables.contains(m)) {
				MethodPAG mpag = MethodPAG.v(mypag, m);
				mpag.build();
				mpag.addToPAG(null);
				analyzedMethods++;
				if (!incedClasses) {
					incedClasses = true;
					classes++;
				}
			}
		}
	}
}
