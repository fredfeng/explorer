package edu.utexas.cgrex.test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
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
import soot.Unit;
import soot.Value;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.spark.pag.PAG;
import soot.util.Chain;
import edu.utexas.cgrex.analyses.AutoPAG;

public class TestPrecision extends SceneTransformer {

	public static void main(String[] args) {
		String targetLoc = "benchmarks/sablecc-3.7/classes";
		// String targetLoc = "benchmarks/CFLexample/bin";

		try {
			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.test", new TestPrecision()));

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

	@Override
	protected void internalTransform(String phaseName,
			Map<String, String> options) {
		// TODO Auto-generated method stub
		System.out.println("*****************TEST PT PRECISION*************");
		// read default pag from soot.
		PAG pag = (PAG) Scene.v().getPointsToAnalysis();
		// build my AutoPAG
		AutoPAG me = new AutoPAG(pag);
		me.build();

		// test1(me);
		test2(me);
	}

	protected void test2(AutoPAG me) {
		List<Double> a = new ArrayList<Double>();
		StringBuilder b = new StringBuilder("");
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

				Chain<Unit> units = body.getUnits();
				Iterator<Unit> uit = units.snapshotIterator();
				while (uit.hasNext()) {
					Stmt stmt = (Stmt) uit.next();
					// invocation statements
					if (stmt.containsInvokeExpr()) {
						InvokeExpr ie = stmt.getInvokeExpr();
						if ((ie instanceof VirtualInvokeExpr)
								|| (ie instanceof InterfaceInvokeExpr)) {
							// ie.get
							Local var = (Local) ie.getUseBoxes().get(0)
									.getValue();
							List list1 = new ArrayList();
							list1.add(var);
							Set s1 = me.insensitiveQueryTest(list1);
							Set s2 = me.sensitiveQueryTest(list1);
							double size1 = s1.size();
							double size2 = s2.size();
							double ratio = size2 / size1;
							// s1 should be always the super set of s2.
							if (size1 != size2) {
								System.out
										.println("------------------------------------------------");
								b.append("------------------------------------------------\n");
								System.out.println("insensitive_result::::"
										+ size1);
								b.append("insensitive_result::::" + s1 + "\n");
								b.append("insensitive result size::::" + size1
										+ "\n");
								System.out.println("sensitive_result::::"
										+ size2);
								b.append("sensitive_result::::" + s2 + "\n");
								b.append("sensitive result size::::" + size2
										+ "\n");
								System.out.println("ratio::::" + size2 / size1);
								b.append("ratio::::" + ratio + "\n");
								a.add(ratio);
							}
						}
					}
				}
			}
		}
		Collections.sort(a);
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/precision_details"));
			bufw.write(b.toString());
			bufw.close();
			b = new StringBuilder("");
			bufw = new BufferedWriter(new FileWriter(
					"sootOutput/precision_ratio"));
			double[] x = { 0, 1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
			for (int i = 1; i < 12; i++) {
				int count = 0;
				for (int j = 0; j < a.size(); j++) {
					if (a.get(j) >= x[i - 1] / 100 && a.get(j) < x[i] / 100) {
						count++;
					}
				}
				b.append("[" + x[i - 1] / 100 + "," + x[i] / 100 + "):  "
						+ count + "\n");
			}
			bufw.write(b.toString());
			bufw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	protected void test1(AutoPAG me) {
		int total = 0;
		int hit = 0;
		int eq = 0;

		StringBuilder b = new StringBuilder("");
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

				Chain<Unit> units = body.getUnits();
				Iterator<Unit> uit = units.snapshotIterator();
				while (uit.hasNext()) {
					Stmt stmt = (Stmt) uit.next();
					// invocation statements
					if (stmt.containsInvokeExpr()) {
						InvokeExpr ie = stmt.getInvokeExpr();
						if ((ie instanceof VirtualInvokeExpr)
								|| (ie instanceof InterfaceInvokeExpr)) {
							// ie.get
							total++;
							Local var = (Local) ie.getUseBoxes().get(0)
									.getValue();
							List list1 = new ArrayList();
							list1.add(var);
							Set s1 = me.insensitiveQueryTest(list1);
							Set s2 = me.sensitiveQueryTest(list1);
							// s1 should be always the super set of s2.
							if (!s1.containsAll(s2)) {
								System.out
										.println("----------------------------------------------------------------------------------------------------");
								b.append("---------------------------------------------------------------------------------------------------\n");
								System.out.println("begin points-to set query:"
										+ var);
								b.append("begin points-to set query: " + var
										+ "\n");
								System.out.println("insensitive_result::::"
										+ s1);
								b.append("insensitive_result::::" + s1 + "\n");
								System.out.println("sensitive_result::::" + s2);
								b.append("sensitive_result::::" + s2 + "\n");
							}
							if (s1.containsAll(s2))
								hit++;
							if (s1.containsAll(s2) && s2.containsAll(s1))
								eq++;

						}
					}
				}
			}
		}
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/precision"));
			bufw.write(b.toString());
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		System.out.println("FINAL result of precision: " + "soundness: " + hit
				+ "/" + total + " precision: " + eq + "/" + total);
	}

}
