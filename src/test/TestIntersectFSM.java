package test;

import java.util.*;

import test.util.*;

public class TestIntersectFSM {
	public static void main(String[] args) {
		cgfsm call = new cgfsm();
		refsm expr = new refsm(); 
		
		/** construct a simulated regular expr fsm */
		Edge expr_edge_foo = new Edge(new refsmId("foo"));
		Edge expr_edge_bar = new Edge(new refsmId("bar"));
		Edge expr_edge_baz = new Edge(new refsmId("baz"));	
		
		refsmState expr_init = new refsmState(new refsmId(1), true, false);
		refsmState expr_state_1 = new refsmState(new refsmId(2), false, false);
		refsmState expr_state_2 = new refsmState(new refsmId(3), false, false);
		refsmState expr_state_3 = new refsmState(new refsmId(4), false, false);
		refsmState expr_final = new refsmState(new refsmId(5), false, true);
		expr.addInitState(expr_init);
		expr.addFinalState(expr_final);
		expr_init.addOutgoingStates(expr_final, expr_edge_foo);
		expr_final.addIncomingStates(expr_init, expr_edge_bar);
		expr.addStates(expr_init);
		expr.addStates(expr_final);
		
		/** construct a simulated call graph fsm */
		Edge call_main = new Edge(new cgfsmId("foo"));
		Edge call_foo = new Edge(new cgfsmId("foo"));
		cgfsmState call_init = new cgfsmState(new cgfsmId(1), true, false, null);
		cgfsmState call_a = new cgfsmState(new cgfsmId(2), false, false, call_main);
		cgfsmState call_final = new cgfsmState(new cgfsmId(3), false, true, call_foo);
		call_init.addOutgoingStates(call_a, call_main);
		call_a.addOutgoingStates(call_final, call_foo);
		call.addInitState(call_init);
		call.addFinalState(call_final);
		call.addStates(call_init);
		call.addStates(call_a);
		call.addStates(call_final);
		
		System.out.println("Hello world!");
		expr.dump();
		call.dump();
		
		intersectFSM comb = new intersectFSM();
		comb.build(expr, call);
		comb.dump();
		
	}
}
