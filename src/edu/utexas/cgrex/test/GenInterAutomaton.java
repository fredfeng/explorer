package edu.utexas.cgrex.test;

import java.util.HashMap;
import java.util.Map;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.InterAutoOpts;
import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutoState;
import edu.utexas.cgrex.automaton.RegAutomaton;

public class GenInterAutomaton {

	protected static CGAutomaton cgAutomaton;

	protected static RegAutomaton regAutomaton;

	protected static InterAutomaton interAutomaton;

	public static InterAutomaton getInterAutomaton() {
		return interAutomaton;
	}

	public static CGAutomaton getCGAutomaton() {
		return cgAutomaton;
	}

	public static RegAutomaton getRegAutomaton() {
		return regAutomaton;
	}

	public static void gen() {
		CGAutomaton call = new CGAutomaton();
		RegAutomaton expr = new RegAutomaton();

		/** construct a simulated regular expr fsm */
		AutoEdge expr_edge_foo = new AutoEdge("foo");
		AutoEdge expr_edge_baz = new AutoEdge("baz");
		AutoEdge expr_edge_bar = new AutoEdge("bar");
		AutoEdge expr_edge_dot = new AutoEdge("dot", true);

		RegAutoState expr_init = new RegAutoState(1, true, false);
		RegAutoState expr_state_1 = new RegAutoState(2);
		RegAutoState expr_state_2 = new RegAutoState(3);
		RegAutoState expr_state_3 = new RegAutoState(4);
		RegAutoState expr_final = new RegAutoState(5, false, true);

		expr.addStates(expr_init);
		expr.addStates(expr_final);
		expr.addStates(expr_state_1);
		expr.addStates(expr_state_2);
		expr.addStates(expr_state_3);

		expr.addInitState(expr_init);
		expr.addFinalState(expr_final);

		expr_init.addOutgoingStates(expr_state_1, expr_edge_foo);
		expr_init.addOutgoingStates(expr_state_2, expr_edge_bar);
		expr_init.addOutgoingStates(expr_init, expr_edge_dot);
		expr_init.addIncomingStates(expr_init, expr_edge_dot);
		expr_state_1.addOutgoingStates(expr_state_3, expr_edge_baz);
		expr_state_1.addOutgoingStates(expr_state_1, expr_edge_dot);
		expr_state_1.addIncomingStates(expr_init, expr_edge_foo);
		expr_state_1.addIncomingStates(expr_state_1, expr_edge_dot);
		expr_state_2.addOutgoingStates(expr_state_3, expr_edge_baz);
		expr_state_2.addOutgoingStates(expr_state_2, expr_edge_dot);
		expr_state_2.addIncomingStates(expr_init, expr_edge_bar);
		expr_state_2.addIncomingStates(expr_state_2, expr_edge_dot);
		expr_state_3.addOutgoingStates(expr_final, expr_edge_baz);
		expr_state_3.addIncomingStates(expr_state_1, expr_edge_baz);
		expr_state_3.addIncomingStates(expr_state_2, expr_edge_baz);
		expr_final.addOutgoingStates(expr_final, expr_edge_dot);
		expr_final.addOutgoingStates(expr_final, expr_edge_dot);

		/** construct a simulated call graph fsm */
		AutoEdge call_edge_foo = new AutoEdge("foo");
		AutoEdge call_edge_baz = new AutoEdge("baz");
		AutoEdge call_edge_main = new AutoEdge("main");
		AutoEdge call_edge_x = new AutoEdge("x");
		AutoEdge call_edge_y = new AutoEdge("y");
		AutoEdge call_edge_z = new AutoEdge("z");

		CGAutoState call_init = new CGAutoState(1, true, false);
		CGAutoState call_state_1 = new CGAutoState(2, false, true);
		CGAutoState call_state_2 = new CGAutoState(3, false, true);
		CGAutoState call_state_3 = new CGAutoState(4, false, true);
		CGAutoState call_state_4 = new CGAutoState(5, false, true);
		CGAutoState call_state_5 = new CGAutoState(6, false, true);
		CGAutoState call_final = new CGAutoState(7, false, true);

		call.addStates(call_init);
		call.addStates(call_state_1);
		call.addStates(call_state_2);
		call.addStates(call_state_3);
		call.addStates(call_state_4);
		call.addStates(call_state_5);
		call.addStates(call_final);

		call.addInitState(call_init);
		call.addFinalState(call_final);

		call_init.addOutgoingStates(call_state_1, call_edge_x);
		call_init.addOutgoingStates(call_state_4, call_edge_main);
		call_state_1.addOutgoingStates(call_state_2, call_edge_y);
		call_state_1.addOutgoingStates(call_state_3, call_edge_z);
		call_state_4.addOutgoingStates(call_state_5, call_edge_foo);
		call_state_5.addOutgoingStates(call_final, call_edge_baz);
		call_final.addOutgoingStates(call_final, call_edge_baz);

		regAutomaton = expr;
		cgAutomaton = call;

		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);

		interAutomaton = new InterAutomaton(myopts, expr, call);
		interAutomaton.build();
		
		regAutomaton.deleteOneEdge(expr_state_1, expr_state_3, expr_edge_baz);
		regAutomaton.deleteOneEdge(expr_state_1, expr_state_1, expr_edge_dot);
		regAutomaton.deleteOneEdge(expr_state_2, expr_state_3, expr_edge_baz);
		
	}

}
