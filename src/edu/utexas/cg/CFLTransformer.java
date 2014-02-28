package edu.utexas.cg;

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

public class CFLTransformer extends SceneTransformer {

protected void internalTransform(String phaseName,
@SuppressWarnings("rawtypes") Map options) {

    int totalAllocs = 0;
    int totalivks = 0;
    int totaloptZero = 0;
    int totalopt = 0;
    int totalclz = 0;
    int totalType1 = 0;
    int totalType2 = 0;
    int totalType3_1 = 0;
    int totalType3_2 = 0;
    long nanoBeforeCFG = System.nanoTime();
    InterproceduralCFG<Unit, SootMethod> icfg = new JimpleBasedInterproceduralCFG();
    JimpleBasedInterproceduralCFG myig = (JimpleBasedInterproceduralCFG) icfg;
    System.out.println("ICFG created in " + (System.nanoTime() - nanoBeforeCFG) / 1E9 + " seconds.");

    IFDSTabulationProblem<Unit, Set<RefType>, 
    SootMethod, InterproceduralCFG<Unit, SootMethod>> problem = new IFDSRefDefinition(icfg);
    IFDSSolver<Unit, Set<RefType>, SootMethod, 
    InterproceduralCFG<Unit, SootMethod>> solver = 
    new IFDSSolver<Unit, Set<RefType>, 
        SootMethod, InterproceduralCFG<Unit, SootMethod>>(problem);

    long beforeSolver = System.nanoTime();
    System.out.println("Running solver...");
    solver.solve();
    System.out.println("Solver done in " + ((System.nanoTime() - beforeSolver) / 1E9) + " seconds.");
    System.out.println("Go through ICFG and output result at each program point:");

    Set<Stmt> allocSet = new HashSet<Stmt>(); 
    Set<Stmt> virtIvkSet = new HashSet<Stmt>(); 

    for (String path : (Collection<String>) Options.v().process_dir()) {
        for (String cl : SourceLocator.v().getClassesUnder(path)) {
            SootClass clazz = Scene.v().getSootClass(cl);
            totalclz++;
            System.out.println("Analyzing...." + clazz);
            for(SootMethod m : clazz.getMethods()) {
                if(!m.isConcrete()) continue;
                Body body = m.retrieveActiveBody();
                //running intra-proc reaching defs.
                ReachingDefsAnalysis.runReachingDef(body);
                UnitGraph g = new ExceptionalUnitGraph(body);
                Chain<Unit> units = body.getUnits();
                Iterator<Unit> uit = units.snapshotIterator();
                while (uit.hasNext()) {
                    Stmt stmt = (Stmt) uit.next();
                    if(stmt instanceof AssignStmt) {
                        AssignStmt as = (AssignStmt)stmt;
                        if(as.getRightOp() instanceof AnyNewExpr) totalAllocs++;
                    }
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr ie = stmt.getInvokeExpr();
                        if((ie instanceof VirtualInvokeExpr) || (ie instanceof InterfaceInvokeExpr)) {
                            virtIvkSet.add(stmt);
                            totalivks++;
                            //System.out.println("Callees***: " + stmt);
                            //System.out.println("Result ***** " + solver.resultsAt(stmt));

                            if(stmt.getInvokeExpr().getUseBoxes().get(0).getValue().getType() instanceof RefType){
                                RefType callsiteType = (RefType)stmt.getInvokeExpr().getUseBoxes().get(0).getValue().getType();
                                HashSet<String> hs = new HashSet();
                                Set<SootClass> subClazz = SootUtils.subTypesOf(callsiteType.getSootClass());
                                for (Set<RefType> key : solver.resultsAt(stmt).keySet()) {
                                    for(RefType dfs : key){
                                        //need to filter out incompatible alloc here.
                                            RefType allocType = dfs;
                                            if(subClazz.contains(allocType.getSootClass())) {
                                                //alloc becomes useful when it overrides the method.`
                                                if(allocType.getSootClass().declaresMethodByName(ie.getMethod().getName()))
                                                    //hs.add(dfs.getRightOp().getType().toString());
                                                    hs.add(dfs.toString());
                                            }
                                            //System.out.println( callsiteType + " subtypes::::" + SootUtils.subTypesOf(callsiteType.getSootClass()));
                                    }

                                }
                                if(subClazz.size() == 1) {
                                    //not interesting.
                                    totalType1++;
                                    assert(subClazz.contains(callsiteType.getSootClass()));
                                } else {
                                    if(hs.size() == 0) {
                                        totaloptZero++;
                                    } else if(hs.size() == 1){
                                        //benefit from isil's assumption.
                                        totalType2++;
                                    } else {
                                        //totalType3++;
                                        if(callsiteType.getClassName().startsWith("java.")) totalType3_1++;
                                        else totalType3_2++;

                                        System.out.println("Callees: " + stmt);
                                        System.out.print(callsiteType);
                                        System.out.println(" Alloc:" + hs);
                                        System.out.println(" ReachingDefs:" + stmt.getTags());
                                        System.out.println("\n");

                                    }
                                }
                                /*if(hs.size() > 1 || (!hs.contains(callsiteType.toString()) && hs.size()>0)) {
                                    totalopt++;
                                    System.out.println("Callees: " + stmt);
                                    System.out.print(callsiteType);
                                    System.out.println(" Alloc:" + hs);
                                    System.out.println(" ReachingDefs:" + stmt.getTags());
                                    System.out.println("\n");
                                }*/
                            }
                        }
                    }
                }
            }

        }
    }

    System.out.println("Number of Classes: " + totalclz);
    System.out.println("Number of Allocation sites: " + totalAllocs);
    System.out.println("Number of Virtual invocations: " + totalivks);
    System.out.println("Number of Type 1: " + totalType1);
    System.out.println("Number of Type 2: " + totalType2);
    System.out.println("Number of Type 3_1(client overrides java library): " + totalType3_1);
    System.out.println("Number of Type 3_2(pure app): " + totalType3_2);
    System.out.println("Number of OPT Zero invocations: " + totaloptZero);
    System.out.println("Number of OPT Virtual invocations: " + totalopt);
    System.out.println("All allocs that are compatible: " + 2);
    System.out.println("All allocs that are compatible: " + 2);
    System.out.println("All allocs that flow to callsite: " + 10);
    System.out.println("======================================");

}
}
