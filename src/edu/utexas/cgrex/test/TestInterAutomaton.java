package edu.utexas.cgrex.test;

import java.util.*;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutoState;
import edu.utexas.cgrex.automaton.RegAutomaton;
import edu.utexas.cgrex.utils.GraphUtil;

public class TestInterAutomaton {
	public static void main(String[] args) {
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

		CGAutoState call_init = new CGAutoState(1, true, false, null);
		CGAutoState call_state_1 = new CGAutoState(2, false, true, call_edge_x);
		CGAutoState call_state_2 = new CGAutoState(3, false, true, call_edge_y);
		CGAutoState call_state_3 = new CGAutoState(4, false, true, call_edge_z);
		CGAutoState call_state_4 = new CGAutoState(5, false, true,
				call_edge_main);
		CGAutoState call_state_5 = new CGAutoState(6, false, true,
				call_edge_foo);
		CGAutoState call_final = new CGAutoState(7, false, true, call_edge_baz);

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

		// System.out.println("Hello world!");
		expr.dump();
		call.dump();

		InterAutomaton comb_without_opt = new InterAutomaton();
		comb_without_opt.buildWithoutOpt(expr, call);
		comb_without_opt.dump();

		Map<AutoState, Set<AutoEdge>> opts = expr.find();
		System.out.println(opts);
		Map<AutoState, Map<AutoState, Boolean>> annotations = call
				.annotate(opts);
		System.out.println(annotations);

		InterAutomaton comb_with_opt = new InterAutomaton();
		comb_with_opt.buildWithOpt(expr, call);
		comb_with_opt.dump();

		/** test scc doAnalysis */
		RegAutomaton scc = new RegAutomaton();
		AutoEdge scc_edge_1 = new AutoEdge("edge1");
		AutoEdge scc_edge_2 = new AutoEdge("edge2");
		AutoEdge scc_edge_3 = new AutoEdge("edge3");
		AutoEdge scc_edge_4 = new AutoEdge("edge4");
 
		RegAutoState scc_init = new RegAutoState(1, true, false);
		RegAutoState scc_final = new RegAutoState(2, false, true);
		RegAutoState scc_state_1 = new RegAutoState(3);

		scc.addStates(scc_init);
		scc.addStates(scc_final);
		scc.addStates(scc_state_1);

		scc.addInitState(scc_init);
		scc.addFinalState(scc_final);

		scc_init.addOutgoingStates(scc_final, scc_edge_1);
		scc_init.addIncomingStates(scc_state_1, scc_edge_2);
		scc_final.addOutgoingStates(scc_state_1, scc_edge_3);
		scc_final.addIncomingStates(scc_state_1, scc_edge_4);
		scc_final.addIncomingStates(scc_init, scc_edge_1);
		scc_state_1.addOutgoingStates(scc_final, scc_edge_4);
		scc_state_1.addOutgoingStates(scc_init, scc_edge_2);
		scc_state_1.addIncomingStates(scc_final, scc_edge_3);
		
		scc.dump();

		Set roots = scc.getInitStates();
		Map nodeToPreds = new HashMap<Object, Set<Object>>();
		Map nodeToSuccs = new HashMap<Object, Set<Object>>();
		Iterator<AutoState> it = scc.statesIterator();
		while (it.hasNext()) {
			AutoState s = it.next();
			nodeToPreds.put(s, s.getIncomingStatesKeySet());
			nodeToSuccs.put(s, s.getOutgoingStatesKeySet());
		}
		System.out.println(nodeToPreds);
		System.out.println(nodeToSuccs);

		List<Set<Object>> sccList = GraphUtil.doAnalysis(roots, nodeToPreds,
				nodeToSuccs);
		System.out.println(sccList);
		
		InterAutomaton test_scc_with_opt = new InterAutomaton();
		test_scc_with_opt.buildWithOpt(scc, call);
	}
}
