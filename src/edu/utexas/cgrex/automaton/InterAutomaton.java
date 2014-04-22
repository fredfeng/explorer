package edu.utexas.cgrex.automaton;

import java.util.*;

public class InterAutomaton extends Automaton {

	protected InterAutoOpts options; // control optimization options

	protected Automaton masterAutomaton;

	protected Automaton slaveAutomaton;

	public InterAutomaton(InterAutoOpts options, Automaton masterAutomaton,
			Automaton slaveAutomaton) {
		this.options = options;
		this.masterAutomaton = masterAutomaton;
		this.slaveAutomaton = slaveAutomaton;

		// for (AutoState s : slaveAutomaton.getStates()) {
		// System.out.println("slave" + s);
		// System.out.println(s.getOutgoingStatesInvKeySet());
		// System.out.println(s.getOutgoingStatesKeySet());
		// System.out.println(s.getIncomingStatesInvKeySet());
		// }
		//
		// for (AutoState s : masterAutomaton.getStates()) {
		// System.out.println("master" + s);
		// System.out.println(s.getOutgoingStatesInvKeySet());
		// System.out.println(s.getOutgoingStatesKeySet());
		// System.out.println(s.getIncomingStatesInvKeySet());
		// }
	}

	// if this machine contains a statepair consisting of master and slave
	// return that statepair
	// otherwise, return null
	public InterAutoState containMasterandSlaveState(AutoState master,
			AutoState slave) {
		for (AutoState stat : states) {
			InterAutoState s = (InterAutoState) stat;
			if (s.hasMasterandSlaveState(master, slave)) {
				return s;
			}
		}
		return null;
	}

	public void build() {
		Set<AutoState> masterInitStates = masterAutomaton.getInitStates();
		Set<AutoState> slaveInitStates = slaveAutomaton.getInitStates();
		for (AutoState masterInitSt : masterInitStates) {
			for (AutoState slaveInitSt : slaveInitStates) {
				InterAutoState interInitSt = new InterAutoState(
						masterInitSt.getId() + "@" + slaveInitSt.getId(),
						masterInitSt, slaveInitSt, true, false);
				states.add(interInitSt);
				initStates.add(interInitSt);
				interInitSt.setInitState();
				if (options.annotated()) {
					Map<AutoState, Set<AutoEdge>> regExprOpts = ((RegAutomaton) masterAutomaton)
							.find();
					Map<AutoState, Map<AutoState, Boolean>> annots = ((CGAutomaton) slaveAutomaton)
							.annotate(regExprOpts);
					intersectAnnot(masterInitSt, slaveInitSt, interInitSt,
							annots);
				} else {
					intersect(masterInitSt, slaveInitSt, interInitSt);
				}
			}
		}
	}

	// this is the currently in-use method which does not do any optimizations
	protected void intersect(AutoState masterSt, AutoState slaveSt,
			InterAutoState interSt) {
		for (AutoState masterNxtSt : masterSt.getOutgoingStatesKeySet()) {
			// master automaton first makes a move
			RegAutoState masterNextState = (RegAutoState) masterNxtSt;
			for (AutoEdge masterNextEdge : masterSt
					.outgoingStatesLookup(masterNextState)) {
				// slave automaton then makes a move accordingly

				// if the edge of the master automaton is a (.*) edge
				if (masterNextEdge.isDotEdge()) {
					/*
					 * boolean toContinue = true; if
					 * (annots.containsKey(masterSt)) { Map<AutoState, Boolean>
					 * annot = annots.get(masterSt); if
					 * (annot.containsKey(slaveSt)) { toContinue =
					 * annot.get(slaveSt); if (!toContinue) continue; // jump to
					 * the next master } }
					 */

					// then the slave automaton can move to any of the next
					// states (because of the .* edge)
					for (AutoState slaveNxtSt : slaveSt
							.getOutgoingStatesKeySet()) {
						// slave automaton makes a move
						CGAutoState slaveNextState = (CGAutoState) slaveNxtSt;

						// I guess the following two lines of code is not useful
						// at all
						// for (AutoEdge slaveNextEdge : slaveSt
						// .outgoingStatesLookup(slaveNextState)) {

						// check whether the newly generated interAutoState
						// is existed
						InterAutoState newInterSt = containMasterandSlaveState(
								masterNextState, slaveNextState);
						// if so, just add an edge connecting them
						if (newInterSt != null) {
							// create a new interAutoEdge with id to be the
							// id of two states connected by this edge
							// new edge : state1 --> state2
							// id of new edge : (state1.id + $ + state2.id)
							InterAutoEdge newInterEdge = new InterAutoEdge(
									interSt.getId() + "$" + newInterSt.getId());
							// update the maps of two end points
							interSt.addOutgoingStates(newInterSt, newInterEdge);
							newInterSt.addIncomingStates(interSt, newInterEdge);
						} else {
							// if not, create a new interAutoState and
							// create a new interAutoEdge
							newInterSt = new InterAutoState(
									masterNextState.getId() + "@"
											+ slaveNextState.getId(),
									masterNextState, slaveNextState);
							InterAutoEdge newInterEdge = new InterAutoEdge(
									interSt.getId() + "$" + newInterSt.getId());
							if (newInterSt.buildAsFinal()) {
								newInterSt.setFinalState();
								addFinalState(newInterSt);
							}
							// update the maps of two end points
							interSt.addOutgoingStates(newInterSt, newInterEdge);
							newInterSt.addIncomingStates(interSt, newInterEdge);
							states.add(newInterSt);
							intersect(masterNextState, slaveNextState,
									newInterSt);
						}
					}
					// }

				} else {
					// if the edge of master automaton is not a (.*) edge
					// then the slave automaton cannot move arbitrarily
					// it should move accordingly by only going along the right
					// edge
					Set<AutoState> slaveNextStates = slaveSt
							.outgoingStatesInvLookup(masterNextEdge);
					if (slaveNextStates == null)
						continue; // not have that edge of master automaton
					// if the edge is found
					for (AutoState slaveNxtSt : slaveNextStates) {
						CGAutoState slaveNextState = (CGAutoState) slaveNxtSt;
						InterAutoState newInterSt = containMasterandSlaveState(
								masterNextState, slaveNextState);
						if (newInterSt != null) {
							// the new state is existed
							// we just add a new interAutoEdge between them
							InterAutoEdge newInterEdge = new InterAutoEdge(
									interSt.getId() + "$" + newInterSt.getId());
							interSt.addOutgoingStates(newInterSt, newInterEdge);
							newInterSt.addIncomingStates(interSt, newInterEdge);
						} else {
							// if not existed, we should create a
							// newInterAutoState and a corresponding new
							// interAuotEdge
							newInterSt = new InterAutoState(
									masterNextState.getId() + "@"
											+ slaveNextState.getId(),
									masterNextState, slaveNextState);
							InterAutoEdge newInterEdge = new InterAutoEdge(
									interSt.getId() + "$" + newInterSt.getId());
							if (newInterSt.buildAsFinal()) {
								newInterSt.setFinalState();
								addFinalState(newInterSt);
							}
							interSt.addOutgoingStates(newInterSt, newInterEdge);
							newInterSt.addIncomingStates(interSt, newInterEdge);
							states.add(newInterSt);
							intersect(masterNextState, slaveNextState,
									newInterSt);
						}
					}
				}
			}
		}
	}

	// this is the currently in-use method for intersection automaton
	// generation which annotates the states on the fly
	protected void intersectAnnot(AutoState masterSt, AutoState slaveSt,
			InterAutoState interSt,
			Map<AutoState, Map<AutoState, Boolean>> annots) {
		for (AutoState masterNxtSt : masterSt.getOutgoingStatesKeySet()) {
			RegAutoState masterNextState = (RegAutoState) masterNxtSt;
			for (AutoEdge masterNextEdge : masterSt
					.outgoingStatesLookup(masterNextState)) {
				if (masterNextEdge.isDotEdge()) {
					/*
					 * boolean toContinue = true; if
					 * (annots.containsKey(masterSt)) { Map<AutoState, Boolean>
					 * annot = annots.get(masterSt); if
					 * (annot.containsKey(slaveSt)) { toContinue =
					 * annot.get(slaveSt); if (!toContinue) continue; // jump to
					 * the next master } }
					 */
					for (AutoState slaveNxtSt : slaveSt
							.getOutgoingStatesKeySet()) {
						CGAutoState slaveNextState = (CGAutoState) slaveNxtSt;

						boolean toCreate = true;
						if (annots.containsKey(masterNextState)) {
							Map<AutoState, Boolean> annot = annots
									.get(masterNextState);
							if (annot.containsKey(slaveNextState)) {
								toCreate = annot.get(slaveNextState);
								if (!toCreate)
									continue; // jump to the next master
							}
						}

						// I guess the following two lines of code are not
						// useful at all
						// for (AutoEdge slaveNextEdge : slaveSt
						// .outgoingStatesLookup(slaveNextState)) {

						InterAutoState newInterSt = containMasterandSlaveState(
								masterNextState, slaveNextState);
						if (newInterSt != null) {
							InterAutoEdge newInterEdge = new InterAutoEdge(
									interSt.getId() + "$" + newInterSt.getId());
							interSt.addOutgoingStates(newInterSt, newInterEdge);
							newInterSt.addIncomingStates(interSt, newInterEdge);
						} else {
							newInterSt = new InterAutoState(
									masterNextState.getId() + "@"
											+ slaveNextState.getId(),
									masterNextState, slaveNextState);
							InterAutoEdge newInterEdge = new InterAutoEdge(
									interSt.getId() + "$" + newInterSt.getId());
							if (newInterSt.buildAsFinal()) {
								newInterSt.setFinalState();
								addFinalState(newInterSt);
							}
							interSt.addOutgoingStates(newInterSt, newInterEdge);
							newInterSt.addIncomingStates(interSt, newInterEdge);
							states.add(newInterSt);
							intersectAnnot(masterNextState, slaveNextState,
									newInterSt, annots);
						}
						// }
					}

				} else {
					Set<AutoState> slaveNextStates = slaveSt
							.outgoingStatesInvLookup(masterNextEdge);
					if (slaveNextStates == null)
						continue; // cannot find that edge
					// can find the edge
					for (AutoState slaveNxtSt : slaveNextStates) {
						CGAutoState slaveNextState = (CGAutoState) slaveNxtSt;
						InterAutoState newInterSt = containMasterandSlaveState(
								masterNextState, slaveNextState);
						if (newInterSt != null) {
							InterAutoEdge newInterEdge = new InterAutoEdge(
									interSt.getId() + "$" + newInterSt.getId());
							interSt.addOutgoingStates(newInterSt, newInterEdge);
							newInterSt.addIncomingStates(interSt, newInterEdge);
						} else {

							boolean toCreate = true;
							if (annots.containsKey(masterNextState)) {
								Map<AutoState, Boolean> annot = annots
										.get(masterNextState);
								if (annot.containsKey(slaveNextState)) {
									toCreate = annot.get(slaveNextState);
									if (!toCreate)
										continue; // jump to the next master
								}
							}

							newInterSt = new InterAutoState(
									masterNextState.getId() + "@"
											+ slaveNextState.getId(),
									masterNextState, slaveNextState);
							InterAutoEdge newInterEdge = new InterAutoEdge(
									interSt.getId() + "$" + newInterSt.getId());
							if (newInterSt.buildAsFinal()) {
								newInterSt.setFinalState();
								addFinalState(newInterSt);
							}
							interSt.addOutgoingStates(newInterSt, newInterEdge);
							newInterSt.addIncomingStates(interSt, newInterEdge);
							states.add(newInterSt);
							intersectAnnot(masterNextState, slaveNextState,
									newInterSt, annots);
						}
					}
				}
			}
		}
	}

	// garbage methods

	// this method is an old version which will not be used
	// intersect method with optimization
	protected void intersectWithOpt(AutoState masterState,
			AutoState slaveState, InterAutoState buildState,
			Map<AutoState, Map<AutoState, Boolean>> annotations) {
		// most of this method is the same to the unoptimized version intersect
		// except for the isDotEdge branch
		Iterator<AutoState> masterStatesIt = masterState
				.outgoingStatesIterator();
		while (masterStatesIt.hasNext()) {
			RegAutoState masterNextState = (RegAutoState) masterStatesIt.next();
			Set<AutoEdge> masterNextEdges = masterState
					.outgoingStatesLookup(masterNextState);
			if (masterNextEdges == null)
				continue;
			Iterator<AutoEdge> masterNextEdgesIt = masterNextEdges.iterator();
			while (masterNextEdgesIt.hasNext()) {
				AutoEdge masterNextEdge = (AutoEdge) masterNextEdgesIt.next();
				if (masterNextEdge.isDotEdge()) {
					// if this is a .* edge in regular expr fsm
					// try to optimize
					// first we try to optimize by checking the status
					boolean checkResult = true; // to be sound
					if (annotations.containsKey(masterState)) { // just make
																// sure
						Map<AutoState, Boolean> annot = annotations
								.get(masterState);
						if (annot.containsKey(slaveState)) { // just make sure
							checkResult = annot.get(slaveState);
							if (!checkResult)
								continue;
						}
					}
					// if check result is true, we should do the expansion
					Iterator<AutoState> slaveNextStatesIt = slaveState
							.outgoingStatesIterator();
					while (slaveNextStatesIt.hasNext()) {
						CGAutoState slaveNextState = (CGAutoState) slaveNextStatesIt
								.next();
						Set<AutoEdge> slaveNextEdges = slaveState
								.outgoingStatesLookup(slaveNextState);
						if (slaveNextEdges == null)
							continue;
						Iterator<AutoEdge> slaveNextEdgesIt = slaveNextEdges
								.iterator();
						while (slaveNextEdgesIt.hasNext()) {
							AutoEdge slaveNextEdge = (AutoEdge) slaveNextEdgesIt
									.next();
							InterAutoState sp = containMasterandSlaveState(
									masterNextState, slaveNextState);
							if (sp != null) {
								// the new statepair is in the states in this
								// machine
								// JUST add an edge between buildState and the
								// new statepair
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStates(buildState, slaveNextEdge);
							} else {
								// the new statepair is not in the states in
								// this machine
								// new a statepair and then add an edge between
								// them
								// and then add the new statepair in this
								// machine
								sp = new InterAutoState(masterNextState.getId()
										.toString()
										+ slaveNextState.getId().toString(),
										masterNextState, slaveNextState);
								if (sp.buildAsFinal()) {
									sp.setFinalState();
									addFinalState(sp);
								}
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStates(buildState, slaveNextEdge);
								states.add(sp); // this machine is moved to the
												// new statepair
								intersectWithOpt(masterNextState,
										slaveNextState, sp, annotations);
								// recursive call
							}
						}
					}
				} else {
					// if this is not a .* edge in regular expr fsm
					// we do not optimize for this case
					Set<AutoState> slaveNextStates = slaveState
							.outgoingStatesInvLookup(masterNextEdge);
					if (slaveNextStates == null)
						continue;
					Iterator<AutoState> slaveNextStatesIt = slaveNextStates
							.iterator();
					while (slaveNextStatesIt.hasNext()) {
						CGAutoState slaveNextState = (CGAutoState) slaveNextStatesIt
								.next();
						InterAutoState sp = containMasterandSlaveState(
								masterNextState, slaveNextState);
						if (sp != null) {
							buildState.addOutgoingStates(sp, masterNextEdge);
							sp.addIncomingStates(buildState, masterNextEdge);
						} else {
							sp = new InterAutoState(masterNextState.getId()
									.toString()
									+ slaveNextState.getId().toString(),
									masterNextState, slaveNextState);
							if (sp.buildAsFinal()) {
								sp.setFinalState();
								addFinalState(sp);
							}
							buildState.addOutgoingStates(sp, masterNextEdge);
							sp.addIncomingStates(buildState, masterNextEdge);
							states.add(sp);
							intersectWithOpt(masterNextState, slaveNextState,
									sp, annotations);
						}
					}
				}
			}
		}
	}

	// this method is an old version which will not be used
	// masterState: the current state of master machine, i.e., regular expr fsm
	// slaveState: the current state of slave machine, i.e., call graph fsm
	// buildState: the current state of the building machine
	// this function does three things:
	// 1. lead the master machine to a new state
	// 2. lead the slave machine to a new state according to the behavior of
	// master
	// 3. make a new state of the building machine, if it is a new state, then
	// call
	// the same function to the newly created states
	// if not a new state, just add an edge from current state to the old state
	protected void intersectWithoutOpt(AutoState masterState,
			AutoState slaveState, InterAutoState buildState) {
		// if masterState has no outgoingStates, return
		// this state must be final
		// (actually we do not need this code because the following plays the
		// same role)
		// if (masterState.outgoingStates().isEmpty())
		// return;
		Iterator<AutoState> masterStatesIt = masterState
				.outgoingStatesIterator();
		while (masterStatesIt.hasNext()) {
			RegAutoState masterNextState = (RegAutoState) masterStatesIt.next();
			Set<AutoEdge> masterNextEdges = masterState
					.outgoingStatesLookup(masterNextState);
			if (masterNextEdges == null)
				continue;
			Iterator<AutoEdge> masterNextEdgesIt = masterNextEdges.iterator();
			while (masterNextEdgesIt.hasNext()) {
				AutoEdge masterNextEdge = (AutoEdge) masterNextEdgesIt.next();
				if (masterNextEdge.isDotEdge()) {
					// if this is a .* edge in regular expr fsm
					Iterator<AutoState> slaveNextStatesIt = slaveState
							.outgoingStatesIterator();
					while (slaveNextStatesIt.hasNext()) {
						CGAutoState slaveNextState = (CGAutoState) slaveNextStatesIt
								.next();
						Set<AutoEdge> slaveNextEdges = slaveState
								.outgoingStatesLookup(slaveNextState);
						if (slaveNextEdges == null)
							continue;
						Iterator<AutoEdge> slaveNextEdgesIt = slaveNextEdges
								.iterator();
						while (slaveNextEdgesIt.hasNext()) {
							AutoEdge slaveNextEdge = (AutoEdge) slaveNextEdgesIt
									.next();
							InterAutoState sp = containMasterandSlaveState(
									masterNextState, slaveNextState);
							if (sp != null) {
								// the new statepair is in the states in this
								// machine
								// JUST add an edge between buildState and the
								// new statepair
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStates(buildState, slaveNextEdge);
							} else {
								// the new statepair is not in the states in
								// this machine
								// new a statepair and then add an edge between
								// them
								// and then add the new statepair in this
								// machine
								sp = new InterAutoState(masterNextState.getId()
										.toString()
										+ slaveNextState.getId().toString(),
										masterNextState, slaveNextState);
								if (sp.buildAsFinal()) {
									sp.setFinalState();
									addFinalState(sp);
								}
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStates(buildState, slaveNextEdge);
								states.add(sp); // this machine is moved to the
												// new statepair
								intersectWithoutOpt(masterNextState,
										slaveNextState, sp); // recursive call
							}
						}
					}
				} else {
					// if this is not a .* edge in regular expr fsm
					Set<AutoState> slaveNextStates = slaveState
							.outgoingStatesInvLookup(masterNextEdge);
					if (slaveNextStates == null)
						continue;
					Iterator<AutoState> slaveNextStatesIt = slaveNextStates
							.iterator();
					while (slaveNextStatesIt.hasNext()) {
						CGAutoState slaveNextState = (CGAutoState) slaveNextStatesIt
								.next();
						InterAutoState sp = containMasterandSlaveState(
								masterNextState, slaveNextState);
						if (sp != null) {
							buildState.addOutgoingStates(sp, masterNextEdge);
							sp.addIncomingStates(buildState, masterNextEdge);
						} else {
							sp = new InterAutoState(masterNextState.getId()
									.toString()
									+ slaveNextState.getId().toString(),
									masterNextState, slaveNextState);
							if (sp.buildAsFinal()) {
								sp.setFinalState();
								addFinalState(sp);
							}
							buildState.addOutgoingStates(sp, masterNextEdge);
							sp.addIncomingStates(buildState, masterNextEdge);
							states.add(sp);
							intersectWithoutOpt(masterNextState,
									slaveNextState, sp);
						}
					}
				}
			}
		}
	}

	/*
	 * // given a regular expression fsm // find all the states that might be
	 * optimized which are mapped // to a set of edges // each key is a state
	 * that has a dot edge // if that key cannot be optimized, the set is empty
	 * (not null) // if that key can be optimized, the set contains the edges
	 * 
	 * // build the finite state machine which combines // a call graph state
	 * machine and a regular expr state machine public void
	 * buildWithoutOpt(Automaton masterFSM, Automaton slaveFSM) {
	 * Iterator<AutoState> masterInitStatesIt = masterFSM.initStatesIterator();
	 * Iterator<AutoState> slaveInitStatesIt = slaveFSM.initStatesIterator();
	 * while (masterInitStatesIt.hasNext()) { AutoState masterInitState =
	 * (AutoState) masterInitStatesIt.next(); while
	 * (slaveInitStatesIt.hasNext()) { AutoState slaveInitState = (AutoState)
	 * slaveInitStatesIt.next(); InterAutoState sp = new
	 * InterAutoState(masterInitState.getId() .toString() +
	 * slaveInitState.getId().toString(), masterInitState, slaveInitState);
	 * states.add(sp); initStates.add(sp); sp.setInitState();
	 * intersectWithoutOpt(masterInitState, slaveInitState, sp); } } }
	 * 
	 * public void buildWithOpt(Automaton masterFSM, Automaton slaveFSM) { //
	 * first parse the masterFSM to get the keyEdges set as the regExprOpts
	 * Map<AutoState, Set<AutoEdge>> regExprOpts = ((RegAutomaton) masterFSM)
	 * .find(); // then get the annotations for slaveFSM Map<AutoState,
	 * Map<AutoState, Boolean>> annotations = ((CGAutomaton) slaveFSM)
	 * .annotate(regExprOpts); // call the intersectWithOpt method to build the
	 * intersection machine Iterator<AutoState> masterInitStatesIt =
	 * masterFSM.initStatesIterator(); Iterator<AutoState> slaveInitStatesIt =
	 * slaveFSM.initStatesIterator(); while (masterInitStatesIt.hasNext()) {
	 * AutoState masterInitState = (AutoState) masterInitStatesIt.next(); while
	 * (slaveInitStatesIt.hasNext()) { AutoState slaveInitState = (AutoState)
	 * slaveInitStatesIt.next(); InterAutoState sp = new
	 * InterAutoState(masterInitState.getId() .toString() +
	 * slaveInitState.getId().toString(), masterInitState, slaveInitState);
	 * states.add(sp); initStates.add(sp); sp.setInitState();
	 * intersectWithOpt(masterInitState, slaveInitState, sp, annotations); } } }
	 */

}
