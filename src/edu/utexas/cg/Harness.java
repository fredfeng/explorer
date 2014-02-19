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

public class Harness {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("My harness for reaching defs.");

        String targetLoc = args[0];

		try {

			StringBuilder options = new StringBuilder();			

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.ifds", new SceneTransformer() {
						protected void internalTransform(String phaseName,
								@SuppressWarnings("rawtypes") Map options) {

                            int totalAllocs = 0;
                            int totalivks = 0;
                            int totalclz = 0;
							long nanoBeforeCFG = System.nanoTime();
							InterproceduralCFG<Unit, SootMethod> icfg = new JimpleBasedInterproceduralCFG();
							JimpleBasedInterproceduralCFG myig = (JimpleBasedInterproceduralCFG) icfg;
							System.out.println("ICFG created in " + (System.nanoTime() - nanoBeforeCFG) / 1E9 + " seconds.");


							IFDSTabulationProblem<Unit, Pair<Value, Set<DefinitionStmt>>, 
							         SootMethod, InterproceduralCFG<Unit, SootMethod>> problem = new IFDSRefDefinition(icfg);
							IFDSSolver<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod, 
									InterproceduralCFG<Unit, SootMethod>> solver = 
										new IFDSSolver<Unit, Pair<Value, Set<DefinitionStmt>>, 
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
										Body body = m.retrieveActiveBody();
										
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

													//System.out.println(" result: " + solver.resultsAt(stmt) + "\n");
                                                    //System.out.println(" result: " + ie.getMethod().getDeclaringClass().getType());
                                                    if(ie.getMethod().getDeclaringClass().getType() instanceof RefType){
													    System.out.println("Callees: " + stmt);
                                                        RefType callsiteType = (RefType)ie.getMethod().getDeclaringClass().getType();

                                                        for (Pair<Value,Set<DefinitionStmt>> key : solver.resultsAt(stmt).keySet()) {
                                                            for(DefinitionStmt dfs : key.getO2()){
                                                                //need to filter out incompatible alloc here.
                                                                if(dfs.getRightOp().getType() instanceof RefType) {
                                                                    RefType allocType = (RefType)dfs.getRightOp().getType();
                                                                    if(SootUtils.subTypesOf(callsiteType.getSootClass()).contains(allocType.getSootClass()))
                                                                        System.out.print(dfs.getRightOp().getType() + " ");
                                                                }
                                                            }
                                        
                                                        }
                                                        System.out.println("\n");
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
                            System.out.println("All allocs that are compatible: " + 2);
                            System.out.println("All allocs that are compatible: " + 2);
                            System.out.println("All allocs that flow to callsite: " + 10);
                            System.out.println("======================================");
						}
					}));

			soot.Main.v().run(new String[] {
					"-W",
					"-process-dir", targetLoc,
					"-src-prec", "java",
//					"-pp",
					//"-cp", sootcp,
					"-allow-phantom-refs",
					"-no-bodies-for-excluded",
					"-exclude", "java",
					"-exclude", "javax",
					"-output-format", "none",
					"-p", "jb", "use-original-names:true",
					"-p", "cg.cha", "on",
					//"-p", "cg.spark", "on",
					//"-v", 
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
