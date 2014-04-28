package edu.utexas.cgrex.test;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.RegAutoState;
import edu.utexas.cgrex.automaton.RegAutomaton;

public class TestRegAutomatonOneStepAnnot {
	
	// construct a regular expression automaton and test the correctness of the
	// annotation method of RegAutomaton
	public static void main(String[] args) {

		RegAutomaton expr = new RegAutomaton();

		/** construct a simulated regular expr fsm */
		AutoEdge edge_foo = new AutoEdge("foo");
		AutoEdge edge_zoo = new AutoEdge("zoo");
		AutoEdge edge_bar = new AutoEdge("bar");
		AutoEdge edge_dot = new AutoEdge("dot", true);

		RegAutoState state_init = new RegAutoState(1, true, false);
		RegAutoState state_2 = new RegAutoState(2);
		RegAutoState state_3 = new RegAutoState(3, false, true);

		expr.addStates(state_init);
		expr.addStates(state_2);
		expr.addStates(state_3);

		expr.addEdge(state_init, state_2, edge_foo);
		expr.addEdge(state_init, state_2, edge_bar);
		expr.addEdge(state_2, state_2, edge_dot);
		expr.addEdge(state_2, state_3, edge_zoo);
		expr.addEdge(state_2, state_3, edge_dot);

		expr.buildOneStepAnnot();
		expr.dumpFindOneStep();
		expr.dump();

	}
	
	public static RegAutomaton test() {
		RegAutomaton expr = new RegAutomaton();

		/** construct a simulated regular expr fsm */
		AutoEdge edge_foo = new AutoEdge("foo");
		AutoEdge edge_zoo = new AutoEdge("zoo");
		AutoEdge edge_bar = new AutoEdge("bar");
		AutoEdge edge_dot = new AutoEdge("dot", true);

		RegAutoState state_init = new RegAutoState(1, true, false);
		RegAutoState state_2 = new RegAutoState(2);
		RegAutoState state_3 = new RegAutoState(3, false, true);

		expr.addStates(state_init);
		expr.addStates(state_2);
		expr.addStates(state_3);

		expr.addEdge(state_init, state_2, edge_foo);
		expr.addEdge(state_init, state_2, edge_bar);
		expr.addEdge(state_2, state_2, edge_dot);
		expr.addEdge(state_2, state_3, edge_zoo);
		expr.addEdge(state_2, state_3, edge_dot);
		
		return expr;
	}
}
