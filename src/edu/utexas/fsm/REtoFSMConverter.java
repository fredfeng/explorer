package edu.utexas.fsm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import test.util.Edge;
import test.util.refsm;
import test.util.refsmId;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

/**
 * Converting a regular expression into our regFSM.
 * Notice it may not handle .* properly.
 * @author yufeng
 *
 */
public class REtoFSMConverter implements Serializable{
	
	Map<State, test.util.State> toFSMstate = new HashMap();
	//Regular expression -> JSA FSM -> our RegFSM
	public void doConvert(String reg) {
		System.out.println("begin to convert...." + reg);
		
		RegExp r = new RegExp(reg);
		Automaton auto = r.toAutomaton(); 
		refsm regFsm = new refsm();
		
		Set<test.util.State> finiteStates = new HashSet();
		
		Set<State> states = auto.getStates();
		int number = 1;
		
		for (State s : states) {
			Map incomeStates = new HashMap(); // Map<State, Edge>
			refsmId id = new refsmId(new Integer(number++));
			test.util.State mystate = new test.util.State(id, false, false);
			mystate.setOutgoingStatesInv(incomeStates);

			//use number to represent state id.
			toFSMstate.put(s, mystate);
		}
	
		for (State s : states) {
			test.util.State fsmState = toFSMstate.get(s);
			Map outgoingStates = new HashMap(); // Map<State, Edge>

			
			if (s.isAccept()) {
				fsmState.setFinalState();
				regFsm.addFinalState(fsmState);
			} else {//normal states.
				
			}
				
			if (s.equals(auto.getInitialState())) {
				fsmState.setInitState();
				regFsm.addInitState(fsmState);
			}		
					
			for (Transition t : s.getTransitions()) {
				test.util.State tgtState = toFSMstate.get(t.getDest());
				Map tgtIncome = tgtState.getOutgoingStatesInv();
				//using edge label as id.
				Edge outEdge = new Edge(new refsmId(t.getMax()));
				
				Set tmpin = new HashSet();
				tmpin.add(fsmState);
				tgtIncome.put(outEdge, tmpin);

				Set tmpout = new HashSet();
				tmpout.add(outEdge);
				outgoingStates.put(tgtState, tmpout);
			}
			fsmState.setOutgoingStates(outgoingStates);
			finiteStates.add(fsmState);
			
			regFsm.addStates(fsmState);
		}	
		
		//dump current result.
		regFsm.dump();	
		
	}
	
	public static void main(String[] args) {
		String reg = "ab(c+d)";
		new REtoFSMConverter().doConvert(reg);
	}
}
