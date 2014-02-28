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

public class IFDSRefDefinition extends DefaultJimpleIFDSTabulationProblem<Set<RefType>,InterproceduralCFG<Unit, SootMethod>> {
	public IFDSRefDefinition(InterproceduralCFG<Unit, SootMethod> icfg) {
		super(icfg);
	}
	
	@Override
	public FlowFunctions<Unit, Set<RefType>, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Set<RefType>, SootMethod>() {

			@Override
			public FlowFunction<Set<RefType>> getNormalFlowFunction(final Unit curr, Unit succ) {
				if (curr instanceof DefinitionStmt) {
					final DefinitionStmt assignment = (DefinitionStmt) curr;
					//only care about the def stmt with contains heap alloc.
					//for now, let's ignore reflection.

					return new FlowFunction<Set<RefType>>() {
						@Override
						public Set<Set<RefType>> computeTargets(Set<RefType> source) {
							if (source != zeroValue()) {
								for(RefType dfs : source) {
                                    //FIXME: We don't need java lib flows along the path.
                                    if (dfs.getClassName().startsWith("java."))
                                        return Collections.emptySet();
								}
								return Collections.singleton(source);

							} else {
								LinkedHashSet<Set<RefType>> res = new LinkedHashSet<Set<RefType>>();
								if(assignment.getRightOp() instanceof NewExpr)
									res.add(
											Collections.<RefType> singleton((RefType)assignment.getRightOp().getType()));

								return res;
							}
							
						}
					};
				}

				return Identity.v();
			}

			@Override
			public FlowFunction<Set<RefType>> getCallFlowFunction(Unit callStmt,
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

				return new FlowFunction<Set<RefType>>() {

					@Override
					public Set<Set<RefType>> computeTargets(Set<RefType> source) {

                        //if (destinationMethod.getName().equals("<clinit>"))
                         //   return Collections.emptySet();

                        //if(localArguments.size() == 0) return Collections.emptySet();

                        assert(source.size() < 2);
                        for(RefType dfs : source) {
                            //FIXME.ignore java libs
                            if (dfs.getClassName().startsWith("java."))
                                return Collections.emptySet();
                        }


						return Collections.singleton(source);
                        //return Collections.emptySet();
					}
				};
			}

			@Override
			public FlowFunction<Set<RefType>> getReturnFlowFunction(final Unit callSite,
					SootMethod calleeMethod, final Unit exitStmt, Unit returnSite) {

				if (exitStmt instanceof ReturnVoidStmt)
					return Identity.v();

				return new FlowFunction<Set<RefType>>() {

					@Override
					public Set<Set<RefType>> computeTargets(Set<RefType> source) {
						return Collections.singleton(source);
					}
				};
			}

			@Override
			public FlowFunction<Set<RefType>> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
				if (!(callSite instanceof DefinitionStmt))
					return Identity.v();
				
				final DefinitionStmt DefinitionStmt = (DefinitionStmt) callSite;
				return new FlowFunction<Set<RefType>>() {

					@Override
					public Set<Set<RefType>> computeTargets(Set<RefType> source) {
						for(RefType dfs : source) {
							return Collections.singleton(source);
						}

						return Collections.singleton(source);
					}
				};
			}
		};
	}

	public Map<Unit, Set<Set<RefType>>> initialSeeds() {
		return DefaultSeeds.make(Collections.singleton(Scene.v().getMainMethod().getActiveBody().getUnits().getFirst()), zeroValue());
	}


	public Set<RefType> createZeroValue() {
		return new LinkedHashSet<RefType>(Collections.<RefType> emptySet());
	}

}
