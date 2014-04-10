package test;

import java.util.*;

import test.util.*;

public class intersectFSM extends fsm {
	protected Set<Object> allEdges = new HashSet<Object>(); // store all edges in this machine
	private int counter = 1;
	public intersectFSM() {
		
	}
	
	// if this machine contains a statepair consisting of master and slave
	// return that statepair
	// otherwise, return null
	public StatePair containMasterandSlaveState(State master, State slave) {
		for(State stat : states) {
			StatePair s = (StatePair) stat;
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
	public void buildWithoutOpt(fsm masterFSM, fsm slaveFSM) {
		Iterator<Object> masterInitStatesIt = masterFSM.initStatesIterator();
		Iterator<Object> slaveInitStatesIt = slaveFSM.initStatesIterator();
		while (masterInitStatesIt.hasNext()) {
			State masterInitState = (State) masterInitStatesIt.next();
			while (slaveInitStatesIt.hasNext()) {
				State slaveInitState = (State) slaveInitStatesIt.next();
				StatePair sp = new StatePair(new intersectFSMId(masterInitState.getId().toString() 
						+ slaveInitState.getId().toString()), masterInitState, slaveInitState);
				states.add(sp);
				initStates.add(sp);
				sp.setInitState();
				intersectWithoutOpt(masterInitState, slaveInitState, sp);
			}
		}
	}
	
	public void buildWithOpt(fsm masterFSM, fsm slaveFSM) {
		// first parse the masterFSM to get the keyEdges set as the regExprOpts
		Map<State, Set<Edge>> regExprOpts = ((refsm) masterFSM).find();
		// then get the annotations for slaveFSM		
		Map<State, Map<State, Boolean>> annotations = ((cgfsm) slaveFSM).annotate(regExprOpts);
		// call the intersectWithOpt method to build the intersection machine
		Iterator<Object> masterInitStatesIt = masterFSM.initStatesIterator();
		Iterator<Object> slaveInitStatesIt = slaveFSM.initStatesIterator();
		while (masterInitStatesIt.hasNext()) {
			State masterInitState = (State) masterInitStatesIt.next();
			while (slaveInitStatesIt.hasNext()) {
				State slaveInitState = (State) slaveInitStatesIt.next();
				StatePair sp = new StatePair(new intersectFSMId(masterInitState.getId().toString() 
						+ slaveInitState.getId().toString()), masterInitState, slaveInitState);
				states.add(sp);
				initStates.add(sp);
				sp.setInitState();
				intersectWithOpt(masterInitState, slaveInitState, sp, annotations);
			}
		}
	}
	
	// intersect method with optimization
	protected void intersectWithOpt(State masterState, State slaveState, StatePair buildState, 
			Map<State, Map<State, Boolean>> annotations) {
		Iterator<Object> masterStatesIt = masterState.outgoingStatesIterator();
		while (masterStatesIt.hasNext()) {
			refsmState masterNextState = (refsmState) masterStatesIt.next();
			Set<Object> masterNextEdges = masterState.outgoingStatesLookup(masterNextState);
			if (masterNextEdges == null) 
				continue;
			Iterator<Object> masterNextEdgesIt = masterNextEdges.iterator();
			while (masterNextEdgesIt.hasNext()) {
				Edge masterNextEdge = (Edge) masterNextEdgesIt.next();
				if (masterNextEdge.isDot()) {
					// if this is a .* edge in regular expr fsm
					// try to optimize
					// first we try to optimize by checking the status 
					boolean checkResult = true; // to be sound
					if (annotations.containsKey(masterState)) {
						Map<State, Boolean> annot = annotations.get(masterState);
						if (annot.containsKey(slaveState)) {
							checkResult = annot.get(slaveState);
							if (!checkResult) 
								continue;
						}
					}
					// if check result is true, we should do the expansion
					Iterator<Object> slaveNextStatesIt = slaveState.outgoingStatesIterator();
					while (slaveNextStatesIt.hasNext()) {
						cgfsmState slaveNextState = (cgfsmState) slaveNextStatesIt.next();
						Set<Object> slaveNextEdges = slaveState.outgoingStatesLookup(slaveNextState);
						if (slaveNextEdges == null) 
							continue;
						Iterator<Object> slaveNextEdgesIt = slaveNextEdges.iterator();
						while (slaveNextEdgesIt.hasNext()) {
							Edge slaveNextEdge = (Edge) slaveNextEdgesIt.next();
							StatePair sp = containMasterandSlaveState(masterNextState, slaveNextState);
							if (sp != null) {
								// the new statepair is in the states in this machine
								// JUST add an edge between buildState and the new statepair
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStatePairs(buildState, slaveNextEdge);
							} else {
								// the new statepair is not in the states in this machine
								// new a statepair and then add an edge between them
								// and then add the new statepair in this machine
								sp = new StatePair(new intersectFSMId(masterNextState.getId().toString() 
										+ slaveNextState.getId().toString()), masterNextState, slaveNextState);
								if (sp.buildAsFinal())
									sp.setFinalState();
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStatePairs(buildState, slaveNextEdge);
								states.add(sp); // this machine is moved to the new statepair
								intersectWithOpt(masterNextState, slaveNextState, sp, annotations); // recursive call
							}
						}
					}
				} else {
					// if this is not a .* edge in regular expr fsm
					// we do not optimize for this case
					Set<Object> slaveNextStates = slaveState.outgoingStatesInvLookup(masterNextEdge);
					if (slaveNextStates == null) 
						continue;
					Iterator<Object> slaveNextStatesIt = slaveNextStates.iterator();
					while (slaveNextStatesIt.hasNext()) {
						cgfsmState slaveNextState = (cgfsmState) slaveNextStatesIt.next();
						StatePair sp = containMasterandSlaveState(masterNextState, slaveNextState);
						if (sp != null) {
							buildState.addOutgoingStates(sp,masterNextEdge);
							sp.addIncomingStatePairs(buildState, masterNextEdge);
						} else {
							sp = new StatePair(new intersectFSMId(masterNextState.getId().toString() 
									+ slaveNextState.getId().toString()), masterNextState, slaveNextState);
							if (sp.buildAsFinal()) 
								sp.setFinalState();
							buildState.addOutgoingStates(sp, masterNextEdge);
							sp.addIncomingStatePairs(buildState, masterNextEdge);
							states.add(sp);
							intersectWithOpt(masterNextState, slaveNextState, sp, annotations);
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
	// 2. lead the slave machine to a new state according to the behavior of master
	// 3. make a new state of the building machine, if it is a new state, then call
	//    the same function to the newly created states
	//	  if not a new state, just add an edge from current state to the old state
	protected void intersectWithoutOpt(State masterState, State slaveState, StatePair buildState) {
		// if masterState has no outgoingStates, return
		// this state must be final 
		// (actually we do not need this code because the following plays the same role)
		//if (masterState.outgoingStates().isEmpty()) 
			//return;
		
		Iterator<Object> masterStatesIt = masterState.outgoingStatesIterator();
		while (masterStatesIt.hasNext()) {
			refsmState masterNextState = (refsmState) masterStatesIt.next();
			Set<Object> masterNextEdges = masterState.outgoingStatesLookup(masterNextState);
			if (masterNextEdges == null) 
				continue;
			Iterator<Object> masterNextEdgesIt = masterNextEdges.iterator();
			while (masterNextEdgesIt.hasNext()) {
				Edge masterNextEdge = (Edge) masterNextEdgesIt.next();
				if (masterNextEdge.isDot()) {
					// if this is a .* edge in regular expr fsm
					Iterator<Object> slaveNextStatesIt = slaveState.outgoingStatesIterator();
					while (slaveNextStatesIt.hasNext()) {
						cgfsmState slaveNextState = (cgfsmState) slaveNextStatesIt.next();
						Set<Object> slaveNextEdges = slaveState.outgoingStatesLookup(slaveNextState);
						if (slaveNextEdges == null) 
							continue;
						Iterator<Object> slaveNextEdgesIt = slaveNextEdges.iterator();
						while (slaveNextEdgesIt.hasNext()) {
							Edge slaveNextEdge = (Edge) slaveNextEdgesIt.next();
							StatePair sp = containMasterandSlaveState(masterNextState, slaveNextState);
							if (sp != null) {
								// the new statepair is in the states in this machine
								// JUST add an edge between buildState and the new statepair
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStatePairs(buildState, slaveNextEdge);
							} else {
								// the new statepair is not in the states in this machine
								// new a statepair and then add an edge between them
								// and then add the new statepair in this machine
								sp = new StatePair(new intersectFSMId(masterNextState.getId().toString() 
										+ slaveNextState.getId().toString()), masterNextState, slaveNextState);
								if (sp.buildAsFinal())
									sp.setFinalState();
								buildState.addOutgoingStates(sp, slaveNextEdge);
								sp.addIncomingStatePairs(buildState, slaveNextEdge);
								states.add(sp); // this machine is moved to the new statepair
								intersectWithoutOpt(masterNextState, slaveNextState, sp); // recursive call
							}
						}
					}
				} else {
					// if this is not a .* edge in regular expr fsm
					Set<Object> slaveNextStates = slaveState.outgoingStatesInvLookup(masterNextEdge);
					if (slaveNextStates == null) 
						continue;
					Iterator<Object> slaveNextStatesIt = slaveNextStates.iterator();
					while (slaveNextStatesIt.hasNext()) {
						cgfsmState slaveNextState = (cgfsmState) slaveNextStatesIt.next();
						StatePair sp = containMasterandSlaveState(masterNextState, slaveNextState);
						if (sp != null) {
							buildState.addOutgoingStates(sp,masterNextEdge);
							sp.addIncomingStatePairs(buildState, masterNextEdge);
						} else {
							sp = new StatePair(new intersectFSMId(masterNextState.getId().toString() 
									+ slaveNextState.getId().toString()), masterNextState, slaveNextState);
							if (sp.buildAsFinal()) 
								sp.setFinalState();
							buildState.addOutgoingStates(sp, masterNextEdge);
							sp.addIncomingStatePairs(buildState, masterNextEdge);
							states.add(sp);
							intersectWithoutOpt(masterNextState, slaveNextState, sp);
						}
					}
				}
			}
		}
	}

}
