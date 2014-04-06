package test;

import java.util.*;
import test.util.*;

public class intersectFSM extends fsm {
	public intersectFSM() {
		
	}
	
	// given the state of the master FSM and the state of slave FSM
	// and the state of the FSM that we are building
	// do the following things: 
	// 1. get the outgoingStates of the masterState
	// 2. 
	private void intersect(State masterState, State slaveState, State buildState) {
		// if masterState has no outgoingStates, return
		if (masterState.outgoingStates().isEmpty()) 
			return;
		// if 
		if (masterState.outgoingStates().size() == 1) {
			return;
		}
		
		Iterator<Object> it = masterState.outgoingStatesIterator();
		while (it.hasNext()) {
			State masterNext = (State)it.next();
			Edge masterNextEdge = 
		}
	}
}
