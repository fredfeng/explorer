package edu.utexas.cgrex.test;

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
//		String targetLoc = "benchmarks/CFLexample/bin";


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
		
		int total = 0;
		int hit = 0;
		int eq = 0;

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
		                if( (ie instanceof VirtualInvokeExpr) 
		                		|| (ie instanceof InterfaceInvokeExpr)){
		                		//ie.get
		                	total++;
		                	Local var = (Local)ie.getUseBoxes().get(0).getValue();
		                	System.out.println("begin points-to set query:" + var);
		                	List list1 = new ArrayList();
		                	list1.add(var);
		                	Set s1 = me.insensitiveQueryTest(list1);
		                	Set s2 = me.sensitiveQueryTest(list1);
		                	//s1 should be always the super set of s2.
		                	System.out.println("result::::" + s1 + s2 + s1.containsAll(s2));
		                	if(s1.containsAll(s2)) hit++;
		                	if(s1.containsAll(s2) && s2.containsAll(s1)) eq++;

		                }
					}
				}
				
			}
		}
		
		System.out.println("FINAL result of precision: " + "soundness: " + hit + "/" + total + " precision: " + eq + "/" + total);
		
	}

}
