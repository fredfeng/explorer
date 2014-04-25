package edu.utexas.cgrex.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.utils.GraphUtil;

public class TestBuildCGStateSCC {

	// construct a call graph which includes strongly connected components
	// then dump the SCC information of the CGAutomaton
	public static void main(String[] args) {

		CGAutomaton call = new CGAutomaton();

		/** construct a simulated call graph fsm */
		AutoEdge edge_foo = new AutoEdge("foo");
		AutoEdge edge_baz = new AutoEdge("baz");
		AutoEdge edge_main = new AutoEdge("main");
		AutoEdge edge_x = new AutoEdge("x");
		AutoEdge edge_y = new AutoEdge("y");
		AutoEdge edge_z = new AutoEdge("z");

		CGAutoState state_init = new CGAutoState(1, true, false);
		CGAutoState state_x = new CGAutoState(2, false, true);
		CGAutoState state_y = new CGAutoState(3, false, true);
		CGAutoState state_z = new CGAutoState(4, false, true);
		CGAutoState state_main = new CGAutoState(5, false, true);
		CGAutoState state_foo = new CGAutoState(6, false, true);
		CGAutoState state_baz_final = new CGAutoState(7, false, true);

		call.addStates(state_init);
		call.addStates(state_x);
		call.addStates(state_y);
		call.addStates(state_z);
		call.addStates(state_main);
		call.addStates(state_foo);
		call.addStates(state_baz_final);

		call.addEdge(state_init, state_x, edge_x);
		call.addEdge(state_x, state_x, edge_x);
		call.addEdge(state_x, state_y, edge_y);
		call.addEdge(state_z, state_y, edge_y);
		call.addEdge(state_y, state_foo, edge_foo);
		call.addEdge(state_foo, state_foo, edge_foo);
		call.addEdge(state_z, state_main, edge_main);
		call.addEdge(state_baz_final, state_z, edge_z);
		call.addEdge(state_main, state_x, edge_x);
		call.addEdge(state_foo, state_baz_final, edge_baz);

		call.buildCGStatesSCC();
		call.dumpSCCInfo();

		call.dump();

		test1();

	}

	public static CGAutomaton test() {
		CGAutomaton call = new CGAutomaton();

		/** construct a simulated call graph fsm */
		AutoEdge edge_zoo = new AutoEdge("zoo");
		AutoEdge edge_bar = new AutoEdge("bar");
		AutoEdge edge_main = new AutoEdge("main");
		AutoEdge edge_x = new AutoEdge("x");
		AutoEdge edge_y = new AutoEdge("y");
		AutoEdge edge_z = new AutoEdge("z");

		CGAutoState state_init = new CGAutoState(1, true, false);
		CGAutoState state_x = new CGAutoState(2, false, true);
		CGAutoState state_y = new CGAutoState(3, false, true);
		CGAutoState state_z = new CGAutoState(4, false, true);
		CGAutoState state_main = new CGAutoState(5, false, true);
		CGAutoState state_zoo_final = new CGAutoState(6, false, true);
		CGAutoState state_bar = new CGAutoState(7, false, true);

		call.addStates(state_init);
		call.addStates(state_x);
		call.addStates(state_y);
		call.addStates(state_z);
		call.addStates(state_main);
		call.addStates(state_bar);
		call.addStates(state_zoo_final);

		call.addEdge(state_init, state_bar, edge_bar);
		call.addEdge(state_bar, state_x, edge_x);
		call.addEdge(state_bar, state_x, edge_x);
		call.addEdge(state_x, state_z, edge_z);
		call.addEdge(state_z, state_zoo_final, edge_zoo);
		call.addEdge(state_bar, state_main, edge_main);
		call.addEdge(state_main, state_main, edge_main);
		call.addEdge(state_main, state_y, edge_y);

		return call;
	}

	public static void test1() {

		CGAutomaton call = new CGAutomaton();

		/** construct a simulated call graph fsm */
		AutoEdge edge_foo = new AutoEdge("foo");
		AutoEdge edge_baz = new AutoEdge("baz");
		AutoEdge edge_main = new AutoEdge("main");
		AutoEdge edge_x = new AutoEdge("x");
		AutoEdge edge_y = new AutoEdge("y");
		AutoEdge edge_z = new AutoEdge("z");
		AutoEdge edge_test = new AutoEdge("test");

		CGAutoState state_init = new CGAutoState(1, true, false);
		CGAutoState state_x = new CGAutoState(2, false, true);
		CGAutoState state_y = new CGAutoState(3, false, true);
		CGAutoState state_z = new CGAutoState(4, true, false);
		CGAutoState state_main = new CGAutoState(5, false, true);
		CGAutoState state_foo = new CGAutoState(6, false, true);
		CGAutoState state_baz_final = new CGAutoState(7, false, true);
		CGAutoState state_test = new CGAutoState(8, false, true);

		call.addStates(state_init);
		call.addStates(state_x);
		call.addStates(state_y);
		call.addStates(state_z);
		call.addStates(state_main);
		call.addStates(state_foo);
		call.addStates(state_baz_final);
		call.addStates(state_test);

		call.addEdge(state_init, state_x, edge_x);
		call.addEdge(state_x, state_x, edge_x);
		call.addEdge(state_x, state_y, edge_y);
		call.addEdge(state_y, state_z, edge_z);
		call.addEdge(state_y, state_foo, edge_foo);
		call.addEdge(state_foo, state_foo, edge_foo);
		call.addEdge(state_z, state_main, edge_main);
		call.addEdge(state_baz_final, state_z, edge_z);
		call.addEdge(state_main, state_x, edge_x);
		call.addEdge(state_foo, state_baz_final, edge_baz);
		call.addEdge(state_init, state_z, edge_z);
		call.addEdge(state_test, state_init, edge_test);

		Set roots = call.getStates();
		Map nodeToPreds = new HashMap<Object, Set<Object>>();
		Map nodeToSuccs = new HashMap<Object, Set<Object>>();

		for (AutoState s : call.getStates()) {
			nodeToPreds.put(s, s.getIncomingStatesKeySet());
			nodeToSuccs.put(s, s.getOutgoingStatesKeySet());
		}
		Object sccListTmp = GraphUtil.doAnalysis(roots, nodeToPreds,
				nodeToSuccs);
		System.out.println((List<Set<AutoState>>) sccListTmp);

		call.dump();
	}

}
