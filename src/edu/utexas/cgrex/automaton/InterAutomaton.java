package edu.utexas.cgrex.automaton;

import java.util.*;

import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;

public class InterAutomaton extends Automaton {

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

	// given a regular expression fsm
	// find all the states that might be optimized which are mapped
	// to a set of edges
	// each key is a state that has a dot edge
	// if that key cannot be optimized, the set is empty (not null)
	// if that key can be optimized, the set contains the edges

	// build the finite state machine which combines
	// a call graph state machine and a regular expr state machine
	public void buildWithoutOpt(Automaton masterFSM, Automaton slaveFSM) {
		Iterator<AutoState> masterInitStatesIt = masterFSM.initStatesIterator();
		Iterator<AutoState> slaveInitStatesIt = slaveFSM.initStatesIterator();
		while (masterInitStatesIt.hasNext()) {
			AutoState masterInitState = (AutoState) masterInitStatesIt.next();
			while (slaveInitStatesIt.hasNext()) {
				AutoState slaveInitState = (AutoState) slaveInitStatesIt.next();
				InterAutoState sp = new InterAutoState(masterInitState.getId()
						.toString() + slaveInitState.getId().toString(),
						masterInitState, slaveInitState);
				states.add(sp);
				initStates.add(sp);
				sp.setInitState();
				intersectWithoutOpt(masterInitState, slaveInitState, sp);
			}
		}
	}

	public void buildWithOpt(Automaton masterFSM, Automaton slaveFSM) {
		// first parse the masterFSM to get the keyEdges set as the regExprOpts
		Map<AutoState, Set<AutoEdge>> regExprOpts = ((RegAutomaton) masterFSM)
				.find();
		// then get the annotations for slaveFSM
		Map<AutoState, Map<AutoState, Boolean>> annotations = ((CGAutomaton) slaveFSM)
				.annotate(regExprOpts);
		// call the intersectWithOpt method to build the intersection machine
		Iterator<AutoState> masterInitStatesIt = masterFSM.initStatesIterator();
		Iterator<AutoState> slaveInitStatesIt = slaveFSM.initStatesIterator();
		while (masterInitStatesIt.hasNext()) {
			AutoState masterInitState = (AutoState) masterInitStatesIt.next();
			while (slaveInitStatesIt.hasNext()) {
				AutoState slaveInitState = (AutoState) slaveInitStatesIt.next();
				InterAutoState sp = new InterAutoState(masterInitState.getId()
						.toString() + slaveInitState.getId().toString(),
						masterInitState, slaveInitState);
				states.add(sp);
				initStates.add(sp);
				sp.setInitState();
				intersectWithOpt(masterInitState, slaveInitState, sp,
						annotations);
			}
		}
	}

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
								if (sp.buildAsFinal())
									sp.setFinalState();
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStates(buildState, slaveNextEdge);
								states.add(sp); // this machine is moved to the
												// new statepair
								intersectWithOpt(masterNextState,
										slaveNextState, sp, annotations); // recursive
																			// call
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
							if (sp.buildAsFinal())
								sp.setFinalState();
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
								if (sp.buildAsFinal())
									sp.setFinalState();
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
							if (sp.buildAsFinal())
								sp.setFinalState();
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

}
