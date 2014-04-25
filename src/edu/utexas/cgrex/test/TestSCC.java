package edu.utexas.cgrex.test;

import java.util.Set;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.utils.SCCBuilder;

public class TestSCC {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CGAutomaton cg = new TestSCC().gen();
		SCCBuilder sb = new SCCBuilder(cg, cg.getInitStates());
		for (Set<Object> list : sb.getComponents()) {
			System.out.println(list);
		}

		cg.dump();
	}

	public CGAutomaton gen() {
		CGAutomaton call = new CGAutomaton();

		/** construct a simulated call graph fsm */
		AutoEdge call_edge_1 = new AutoEdge("1", 16, "16");
		AutoEdge call_edge_2 = new AutoEdge("2", 13, "13");
		AutoEdge call_edge_3 = new AutoEdge("3", 12, "12");
		AutoEdge call_edge_4 = new AutoEdge("4", 4, "4");
		AutoEdge call_edge_5 = new AutoEdge("5", 9, "9");
		AutoEdge call_edge_6 = new AutoEdge("6", 14, "14");
		AutoEdge call_edge_7 = new AutoEdge("7", 7, "7");
		AutoEdge call_edge_8 = new AutoEdge("8", 20, "20");
		AutoEdge call_edge_9 = new AutoEdge("9", 4, "4");

		CGAutoState call_init = new CGAutoState("s", true, false);
		CGAutoState call_state_1 = new CGAutoState("v1", false, false);
		CGAutoState call_state_2 = new CGAutoState("v2", false, false);
		CGAutoState call_state_3 = new CGAutoState("v3", false, false);
		CGAutoState call_state_4 = new CGAutoState("v4", false, false);
		CGAutoState call_final = new CGAutoState("t", false, true);

		call.addStates(call_init);
		call.addStates(call_state_1);
		call.addStates(call_state_2);
		call.addStates(call_state_3);
		call.addStates(call_state_4);
		call.addStates(call_final);

		call.addInitState(call_init);
		call.addFinalState(call_final);

		call_init.addOutgoingStates(call_state_1, call_edge_1);
		call_init.addOutgoingStates(call_state_2, call_edge_2);

		call_state_1.addOutgoingStates(call_state_3, call_edge_3);

		call_state_2.addOutgoingStates(call_state_1, call_edge_4);
		call_state_2.addOutgoingStates(call_state_4, call_edge_6);

		call_state_3.addOutgoingStates(call_state_2, call_edge_5);
		call_state_3.addOutgoingStates(call_final, call_edge_8);

		call_state_4.addOutgoingStates(call_state_3, call_edge_7);
		call_state_4.addOutgoingStates(call_final, call_edge_9);

		return call;

	}
}
