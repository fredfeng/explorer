package edu.utexas.cgrex.test;

import java.util.*;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.InterAutoOpts;
import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutoState;
import edu.utexas.cgrex.automaton.RegAutomaton;
import edu.utexas.cgrex.utils.GraphUtil;

public class TestInterAutomatonOpts {
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

		// System.out.println("Hello world!");
		expr.dump();
		call.dump();
		
		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);
		
		InterAutomaton comb_without_opt = new InterAutomaton(myopts, expr, call);
		comb_without_opt.build();
		comb_without_opt.dump();
		
		/*
		Map<AutoState, Set<AutoEdge>> opts = expr.find();
		System.out.println(opts);
		Map<AutoState, Map<AutoState, Boolean>> annotations = call
				.annotate(opts);
		System.out.println(annotations);

		InterAutomaton comb_with_opt = new InterAutomaton(myopts, expr, call);
		comb_with_opt.build();
		comb_with_opt.dump();

		// test scc doAnalysis 
		CGAutomaton scc = new CGAutomaton();
		AutoEdge scc_edge_main = new AutoEdge("main");
		AutoEdge scc_edge_foo = new AutoEdge("foo");
		AutoEdge scc_edge_bar = new AutoEdge("bar");
		AutoEdge scc_edge_baz = new AutoEdge("baz");
		AutoEdge scc_edge_goo = new AutoEdge("goo");
		AutoEdge scc_edge_zoo = new AutoEdge("zoo");
		AutoEdge scc_edge_bug = new AutoEdge("bug");

		CGAutoState scc_init = new CGAutoState(1, true, false, null);
		CGAutoState scc_main = new CGAutoState(2, false, true, scc_edge_main);
		CGAutoState scc_foo = new CGAutoState(3, false, true, scc_edge_foo);
		CGAutoState scc_bar = new CGAutoState(4, false, true, scc_edge_bar);
		CGAutoState scc_baz = new CGAutoState(5, false, true, scc_edge_baz);
		CGAutoState scc_goo = new CGAutoState(6, false, true, scc_edge_goo);
		CGAutoState scc_zoo = new CGAutoState(7, false, true, scc_edge_zoo);
		CGAutoState scc_bug = new CGAutoState(8, false, true, scc_edge_bug);

		scc.addStates(scc_init);
		scc.addStates(scc_main);
		scc.addStates(scc_foo);
		scc.addStates(scc_bar);
		scc.addStates(scc_baz);
		scc.addStates(scc_goo);
		scc.addStates(scc_zoo);
		scc.addStates(scc_bug);

		scc.addInitState(scc_init);
		scc.addFinalState(scc_main);
		scc.addFinalState(scc_foo);
		scc.addFinalState(scc_bar);
		scc.addFinalState(scc_baz);
		scc.addFinalState(scc_goo);
		scc.addFinalState(scc_zoo);
		scc.addFinalState(scc_bug);

		scc_init.addOutgoingStates(scc_main, scc_edge_main);
		scc_main.addOutgoingStates(scc_foo, scc_edge_foo);
		scc_foo.addOutgoingStates(scc_bar, scc_edge_bar);
		scc_foo.addOutgoingStates(scc_foo, scc_edge_foo);
		scc_bar.addOutgoingStates(scc_baz, scc_edge_baz);
		scc_bar.addOutgoingStates(scc_zoo, scc_edge_zoo);
		scc_bar.addOutgoingStates(scc_goo, scc_edge_goo);
		scc_goo.addOutgoingStates(scc_foo, scc_edge_foo);
		scc_goo.addOutgoingStates(scc_bug, scc_edge_bug);
		scc_bug.addOutgoingStates(scc_goo, scc_edge_goo);
		scc_zoo.addOutgoingStates(scc_zoo, scc_edge_zoo);
		
		scc.buildCGStatesSCC();
		scc.dump();
		
		for (AutoState s : scc.getStates()) {
			CGAutoState st = (CGAutoState) s;
			System.out.println("AutoState " + st);
			System.out.println("statesInTheSameSCC: " + st.getStatesInTheSameSCC());
			System.out.println("edgesInTheSameSCC: " + st.getEdgesInTheSameSCC());
			System.out.println("outgoingStatesOfSCC: " + st.getOutgoingStatesOfSCC());
			System.out.println("outgoingEdgesOfSCC: " + st.getOutgoingEdgesOfSCC());
		}
		*/
		
		/*
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
		*/
		
		/*
		InterAutomaton test_scc_with_opt = new InterAutomaton(myopts, expr, scc);
		InterAutomaton test_scc_without_opt = new InterAutomaton(myopts, expr, scc);
		opts = expr.find();
		System.out.println(opts);
		annotations = scc.annotate(opts);
		System.out.println(annotations);
		test_scc_with_opt.build();
		test_scc_with_opt.dump();
		*/

	}
}
