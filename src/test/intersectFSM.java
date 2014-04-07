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
	
	// build the finite state machine which combines 
	// a call graph state machine and a regular expr state machine
	public void build(fsm masterFSM, fsm slaveFSM) {
		Iterator<Object> masterInitStatesIt = masterFSM.initStatesIterator();
		Iterator<Object> slaveInitStatesIt = slaveFSM.initStatesIterator();
		while (masterInitStatesIt.hasNext()) {
			State masterInitState = (State) masterInitStatesIt.next();
			while (slaveInitStatesIt.hasNext()) {
				System.out.println("here");
				State slaveInitState = (State) slaveInitStatesIt.next();
				StatePair sp = new StatePair(new intersectFSMId(masterInitState.getId().toString() 
						+ slaveInitState.getId().toString()), masterInitState, slaveInitState);
				states.add(sp);
				initStates.add(sp);
				sp.setInitState();
				intersect(masterInitState, slaveInitState, sp);
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
	protected void intersect(State masterState, State slaveState, StatePair buildState) {
		// if masterState has no outgoingStates, return
		// this state must be final 
		if (masterState.outgoingStates().isEmpty()) 
			return;
		
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
								intersect(masterNextState, slaveNextState, sp); // recursive call
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
							intersect(masterNextState, slaveNextState, sp);
						}
					}
				}
			}
		}
	}
}
