package edu.utexas;

import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.CompilationDeathException;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.Unit;
import soot.Type;
import soot.RefType;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;
import edu.utexas.RegularPT.RegularPTTransformer;

public class Harness {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
        String targetLoc = args[0];

		try {

			StringBuilder options = new StringBuilder();			
			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.regularPT", new RegularPTTransformer()));

			soot.Main.v().run(new String[] {
					"-W",
					"-process-dir", targetLoc,
					"-src-prec", "java",
					"-allow-phantom-refs",
					"-no-bodies-for-excluded",
					"-exclude", "java",
					"-exclude", "javax",
					"-output-format", "none",
					"-p", "jb", "use-original-names:true",
					//"-p", "cg.cha", "on",
					"-p", "cg.spark", "on",
					"-debug"} );

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}

	}

}
