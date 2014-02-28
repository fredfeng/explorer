package edu.utexas.cg;

import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.Identity;
import heros.flowfunc.KillAll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.EquivalentValue;
import soot.Local;
import soot.NullType;
import soot.RefType;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.NewExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.toolkits.scalar.Pair;

/**
 * TO-DO list, the original skeleton comes from Soot.
 * 1. Every heap allocation will flow to the call site;
 * 2. Improve it by adding field and return type constraints.
 * @author yufeng
 *
 */

public class IFDSRefDefinition extends DefaultJimpleIFDSTabulationProblem<Set<DefinitionStmt>,InterproceduralCFG<Unit, SootMethod>> {
	public IFDSRefDefinition(InterproceduralCFG<Unit, SootMethod> icfg) {
		super(icfg);
	}
	
	@Override
	public FlowFunctions<Unit, Set<DefinitionStmt>, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Set<DefinitionStmt>, SootMethod>() {

			@Override
			public FlowFunction<Set<DefinitionStmt>> getNormalFlowFunction(final Unit curr, Unit succ) {
				if (curr instanceof DefinitionStmt) {
					final DefinitionStmt assignment = (DefinitionStmt) curr;
					//only care about the def stmt with contains heap alloc.
					//for now, let's ignore reflection.

					return new FlowFunction<Set<DefinitionStmt>>() {
						@Override
						public Set<Set<DefinitionStmt>> computeTargets(Set<DefinitionStmt> source) {
							if (source != zeroValue()) {
								for(DefinitionStmt dfs : source) {

									if (dfs.getRightOp() instanceof NewExpr) {
                                        if (!((RefType)dfs.getRightOp().getType()).getClassName().startsWith("java."))
										    return Collections.singleton(source);
									}
								}
								return Collections.singleton(source);

							} else {
								LinkedHashSet<Set<DefinitionStmt>> res = new LinkedHashSet<Set<DefinitionStmt>>();
								if(assignment.getRightOp() instanceof NewExpr)
									res.add(
												Collections.<DefinitionStmt> singleton(assignment));

								return res;
							}
							
						}
					};
				}

				return Identity.v();
			}

			@Override
			public FlowFunction<Set<DefinitionStmt>> getCallFlowFunction(Unit callStmt,
					final SootMethod destinationMethod) {
				Stmt stmt = (Stmt) callStmt;
				InvokeExpr invokeExpr = stmt.getInvokeExpr();
				final List<Value> args = invokeExpr.getArgs();

				final List<Local> localArguments = new ArrayList<Local>(args.size());
				for (Value value : args) {
					if (value instanceof Local)
						localArguments.add((Local) value);
					else
						localArguments.add(null);
				}

				return new FlowFunction<Set<DefinitionStmt>>() {

					@Override
					public Set<Set<DefinitionStmt>> computeTargets(Set<DefinitionStmt> source) {

                        if (destinationMethod.getName().equals("<clinit>"))
                            return Collections.emptySet();

                        if(localArguments.size() == 0) return Collections.emptySet();

                        assert(source.size() < 2);
                        for(DefinitionStmt dfs : source) {

                            if (dfs.getRightOp() instanceof NewExpr) {

                                if (((RefType)dfs.getRightOp().getType()).getClassName().startsWith("java."))
                                    return Collections.emptySet();
                            }
                        }

                        //System.out.println("solving ....." + source);

						return Collections.singleton(source);
                        //return Collections.emptySet();
					}
				};
			}

			@Override
			public FlowFunction<Set<DefinitionStmt>> getReturnFlowFunction(final Unit callSite,
					SootMethod calleeMethod, final Unit exitStmt, Unit returnSite) {

				if (exitStmt instanceof ReturnVoidStmt)
					return Identity.v();

				return new FlowFunction<Set<DefinitionStmt>>() {

					@Override
					public Set<Set<DefinitionStmt>> computeTargets(Set<DefinitionStmt> source) {
						return Collections.singleton(source);
					}
				};
			}

			@Override
			public FlowFunction<Set<DefinitionStmt>> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
				if (!(callSite instanceof DefinitionStmt))
					return Identity.v();
				
				final DefinitionStmt DefinitionStmt = (DefinitionStmt) callSite;
				return new FlowFunction<Set<DefinitionStmt>>() {

					@Override
					public Set<Set<DefinitionStmt>> computeTargets(Set<DefinitionStmt> source) {
						for(DefinitionStmt dfs : source) {
							if (dfs.getRightOp() instanceof NewExpr) {

								return Collections.singleton(source);
							}
						}

						return Collections.singleton(source);
                        //return Collections.emptySet();
					}
				};
			}
		};
	}

	public Map<Unit, Set<Set<DefinitionStmt>>> initialSeeds() {
		return DefaultSeeds.make(Collections.singleton(Scene.v().getMainMethod().getActiveBody().getUnits().getFirst()), zeroValue());
	}


	public Set<DefinitionStmt> createZeroValue() {
		return new LinkedHashSet<DefinitionStmt>(Collections.<DefinitionStmt> emptySet());
	}

}
