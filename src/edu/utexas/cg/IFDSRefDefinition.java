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
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
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

public class IFDSRefDefinition extends DefaultJimpleIFDSTabulationProblem<Pair<Value, Set<DefinitionStmt>>,InterproceduralCFG<Unit, SootMethod>> {
	public IFDSRefDefinition(InterproceduralCFG<Unit, SootMethod> icfg) {
		super(icfg);
	}
	
	@Override
	public FlowFunctions<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod> createFlowFunctionsFactory() {
		return new FlowFunctions<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod>() {

			@Override
			public FlowFunction<Pair<Value, Set<DefinitionStmt>>> getNormalFlowFunction(final Unit curr, Unit succ) {
				if (curr instanceof DefinitionStmt) {
					final DefinitionStmt assignment = (DefinitionStmt) curr;
					//only care about the def stmt with contains heap alloc.
					//for now, let's ignore reflection.

					return new FlowFunction<Pair<Value, Set<DefinitionStmt>>>() {
						@Override
						public Set<Pair<Value, Set<DefinitionStmt>>> computeTargets(Pair<Value, Set<DefinitionStmt>> source) {
							if (source != zeroValue()) {
								for(DefinitionStmt dfs : source.getO2()) {

									if (dfs.getRightOp() instanceof AnyNewExpr) {
										return Collections.singleton(source);
									}
								}
								return Collections.emptySet();

							} else {
								LinkedHashSet<Pair<Value, Set<DefinitionStmt>>> res = new LinkedHashSet<Pair<Value, Set<DefinitionStmt>>>();
								if(assignment.getRightOp() instanceof AnyNewExpr)
									res.add(new Pair<Value, Set<DefinitionStmt>>(assignment.getLeftOp(),
												Collections.<DefinitionStmt> singleton(assignment)));

								return res;
							}
							
						}
					};
				}

				return Identity.v();
			}

			@Override
			public FlowFunction<Pair<Value, Set<DefinitionStmt>>> getCallFlowFunction(Unit callStmt,
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

				return new FlowFunction<Pair<Value, Set<DefinitionStmt>>>() {

					@Override
					public Set<Pair<Value, Set<DefinitionStmt>>> computeTargets(Pair<Value, Set<DefinitionStmt>> source) {

						//if (!destinationMethod.getName().equals("<clinit>"))

							/*if(localArguments.contains(source.getO1())) {
								int paramIndex = args.indexOf(source.getO1());
								Pair<Value, Set<DefinitionStmt>> pair = new Pair<Value, Set<DefinitionStmt>>(
										new EquivalentValue(Jimple.v().newParameterRef(destinationMethod.getParameterType(paramIndex), paramIndex)),
										source.getO2());
								return Collections.singleton(pair);
							}*/
							
						    if (destinationMethod.getName().equals("<clinit>"))
							    return Collections.emptySet();

							if(localArguments.size() == 0) return Collections.emptySet();

							for(DefinitionStmt dfs : source.getO2()) {

								if (dfs.getRightOp() instanceof AnyNewExpr) {

									//int paramIndex = args.indexOf(source.getO1());
									//FIXME: use 0 argument to propagate alloc.
									int paramIndex = 0;
									
									Pair<Value, Set<DefinitionStmt>> pair = new Pair<Value, Set<DefinitionStmt>>(
											new EquivalentValue(Jimple.v().newParameterRef(destinationMethod.getParameterType(paramIndex), paramIndex)),
											source.getO2());

									return Collections.singleton(pair);
								}
							}

						return Collections.emptySet();
						
					}
				};
			}

			@Override
			public FlowFunction<Pair<Value, Set<DefinitionStmt>>> getReturnFlowFunction(final Unit callSite,
					SootMethod calleeMethod, final Unit exitStmt, Unit returnSite) {
				if (!(callSite instanceof DefinitionStmt))
					return KillAll.v();


				if (exitStmt instanceof ReturnVoidStmt)
					return KillAll.v();

				return new FlowFunction<Pair<Value, Set<DefinitionStmt>>>() {

					@Override
					public Set<Pair<Value, Set<DefinitionStmt>>> computeTargets(Pair<Value, Set<DefinitionStmt>> source) {
						if(exitStmt instanceof ReturnStmt) {
							ReturnStmt returnStmt = (ReturnStmt) exitStmt;
							for(DefinitionStmt dfs : source.getO2()) {

								if (dfs.getRightOp() instanceof AnyNewExpr) {
									DefinitionStmt DefinitionStmt = (DefinitionStmt) callSite;
									Pair<Value, Set<DefinitionStmt>> pair = new Pair<Value, Set<DefinitionStmt>>(
										DefinitionStmt.getLeftOp(), source.getO2());

									return Collections.singleton(pair);
								}
							}
						}
						return Collections.emptySet();
						
					}
				};
			}

			@Override
			public FlowFunction<Pair<Value, Set<DefinitionStmt>>> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
				if (!(callSite instanceof DefinitionStmt))
					return Identity.v();
				
				final DefinitionStmt DefinitionStmt = (DefinitionStmt) callSite;
				return new FlowFunction<Pair<Value, Set<DefinitionStmt>>>() {

					@Override
					public Set<Pair<Value, Set<DefinitionStmt>>> computeTargets(Pair<Value, Set<DefinitionStmt>> source) {
						for(DefinitionStmt dfs : source.getO2()) {
							if (dfs.getRightOp() instanceof AnyNewExpr) {

								return Collections.singleton(source);
							}
						}

						return Collections.emptySet();
					}
				};
			}
		};
	}

	public Map<Unit, Set<Pair<Value, Set<DefinitionStmt>>>> initialSeeds() {
		return DefaultSeeds.make(Collections.singleton(Scene.v().getMainMethod().getActiveBody().getUnits().getFirst()), zeroValue());
	}


	public Pair<Value, Set<DefinitionStmt>> createZeroValue() {
		return new Pair<Value, Set<DefinitionStmt>>(new JimpleLocal("<<zero>>", NullType.v()), Collections.<DefinitionStmt> emptySet());
	}

}
