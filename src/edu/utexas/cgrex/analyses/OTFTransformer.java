package edu.utexas.cgrex.analyses;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import soot.G;
import soot.Scene;
import soot.SceneTransformer;
import soot.SourceLocator;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.solver.PropWorklist;
import soot.options.SparkOptions;

/**
 * Entry point of the whole analysis.
 * 
 * @author yufeng
 * 
 */
public class OTFTransformer extends SceneTransformer {

	public boolean debug = true;

	public OTFTransformer() {
	}

	protected void internalTransform(String phaseName, Map options) {
		Date startPrj = new Date();

		HashMap<String, String> opt = new HashMap<String, String>(options);
		opt.put("enabled", "true");
		opt.put("verbose", "true");
		opt.put("field-based", "false");
		opt.put("on-fly-cg", "true");
		opt.put("set-impl", "double");
		opt.put("double-set-old", "hybrid");
		opt.put("double-set-new", "hybrid");

		SparkOptions opts = new SparkOptions(opt);
		final String output_dir = SourceLocator.v().getOutputDir();

		// Build pointer assignment graph
		ContextInsensitiveBuilder b = new ContextInsensitiveBuilder();

		// if(debug) doGC();
		Date startBuild = new Date();
		final PAG pag = b.setup(opts);
		b.build();
		Date endBuild = new Date();
		reportTime("Pointer Assignment Graph", startBuild, endBuild);
		// doGC();

		// Build type masks
		Date startTM = new Date();
		pag.getTypeManager().makeTypeMask();
		Date endTM = new Date();
		reportTime("Type masks", startTM, endTM);
		// doGC();

		if (debug) {
			G.v().out.println("VarNodes: " + pag.getVarNodeNumberer().size());
			G.v().out.println("FieldRefNodes: "
					+ pag.getFieldRefNodeNumberer().size());
			G.v().out.println("AllocNodes: "
					+ pag.getAllocNodeNumberer().size());
		}

		// doGC();

		// Propagate. This will run our actual points-to analysis.
		Date startProp = new Date();

		// new PropWorklist(pag).propagate();
		new OndemandWorklist(pag).propagate();

		Date endProp = new Date();
		reportTime("Propagation", startProp, endProp);

		pag.dump();
		// doGC();

		if (debug) {
			G.v().out.println("[Spark] Number of reachable methods: "
					+ Scene.v().getReachableMethods().size());
		}

		Date ss = new Date();
		AutoPAG me = new AutoPAG(pag);
		me.build();
		Date ee = new Date();
		reportTime("adding match edges: ", ss, ee);

		me.dump();

		Date endPrj = new Date();
		reportTime("TOTAL project", startPrj, endPrj);

		// me.dump();

	}

	protected static void reportTime(String desc, Date start, Date end) {
		long time = end.getTime() - start.getTime();
		G.v().out.println("[Spark] " + desc + " in " + time / 1000 + "."
				+ (time / 100) % 10 + " seconds.");
	}

	protected static void doGC() {
		// Do 5 times because the garbage collector doesn't seem to always
		// collect
		// everything on the first try.
		System.gc();
		System.gc();
		System.gc();
		System.gc();
		System.gc();
	}
}
