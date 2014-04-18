package edu.utexas.cgrex.test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.CompilationDeathException;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Value;
import soot.jimple.spark.pag.PAG;
import edu.utexas.cgrex.analyses.AutoPAG;

public class TestPTAnalysis extends SceneTransformer {

	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		System.out.println("*****************TEST PT ANALYSIS*************");
		// read default pag from soot.
		PAG pag = (PAG) Scene.v().getPointsToAnalysis();
		// build my AutoPAG
		AutoPAG me = new AutoPAG(pag);
		me.build();
		System.out.println("This is in TestPTAnalysis Class.");

		String b = testSensitive(me);
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/sensitivePTset"));
			bufw.write(b);
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		b = testInsensitive(me);
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/insensitivePTset"));
			bufw.write(b);
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		me.dumpFlow();
		me.dump();
	}

	protected String testInsensitive(AutoPAG me) {
		StringBuilder b = new StringBuilder(
				"This is in insensitive pointer analysis");
		// do query via insensitive pt analysis
		for (Iterator cIt = Scene.v().getClasses().iterator(); cIt.hasNext();) {
			final SootClass c = (SootClass) cIt.next();

			// collect variables in each method
			for (Iterator mIt = c.methodIterator(); mIt.hasNext();) {
				List<Value> toQuery = new ArrayList<Value>();
				SootMethod m = (SootMethod) mIt.next();
				if (!m.isConcrete())
					continue;
				if (!m.hasActiveBody())
					continue;

				Body body = m.getActiveBody();
				for (Local l : body.getLocals()) {
					assert (l instanceof Local);
					toQuery.add(l);
				}

				Set<Type> ptTypeSet = me.insensitiveQueryTest(toQuery);
				b.append("In method: " + m.toString() + "\n");
				b.append("Local variable list: ");
				for (Value v : toQuery) {
					if (me.getFather().findLocalVarNode(v) != null)
						b.append(me.getFather().findLocalVarNode(v).getNumber()
								+ " ");
				}
				b.append("\n");
				b.append("PointToSet: " + ptTypeSet.toString() + "\n\n");
			}
		}
		return b.toString();
	}

	protected String testSensitive(AutoPAG me) {
		StringBuilder b = new StringBuilder(
				"This is in sensitive pointer analysis");
		// do query via sensitive pt analysis
		for (Iterator cIt = Scene.v().getClasses().iterator(); cIt.hasNext();) {
			final SootClass c = (SootClass) cIt.next();

			// collect variables in each method
			for (Iterator mIt = c.methodIterator(); mIt.hasNext();) {
				List<Value> toQuery = new ArrayList<Value>();
				SootMethod m = (SootMethod) mIt.next();
				if (!m.isConcrete())
					continue;
				if (!m.hasActiveBody())
					continue;

				Body body = m.getActiveBody();
				for (Local l : body.getLocals()) {
					assert (l instanceof Local);
					toQuery.add(l);
				}

				Set<Type> ptTypeSet = me.sensitiveQueryTest(toQuery);
				b.append("In method: " + m.toString() + "\n");
				b.append("Local variable list: ");
				for (Value v : toQuery) {
					if (me.getFather().findLocalVarNode(v) != null)
						b.append(me.getFather().findLocalVarNode(v).getNumber()
								+ " ");
				}
				b.append("\n");
				b.append("PointToSet: " + ptTypeSet.toString() + "\n\n");
			}
		}
		return b.toString();
	}

	public static void main(String[] args) {
		String targetLoc = // "benchmarks/CFLexamples/bin";
		// "benchmarks/sablecc-3.7/classes";
		"benchmarks/test/bin";
		try {
			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.test", new TestPTAnalysis()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
							"-no-bodies-for-excluded", "-exclude", "java",
							"-exclude", "javax", "-output-format", "none",
							"-p", "jb", "use-original-names:true",
							// "-p", "cg.cha", "on",
							"-p", "cg.spark", "on", "-debug" });

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}
	}

}
