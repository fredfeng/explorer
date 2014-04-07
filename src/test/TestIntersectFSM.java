package test;

import java.util.*;

import test.util.*;

public class TestIntersectFSM {
	public static void main(String[] args) {
		cgfsm call = new cgfsm();
		refsm expr = new refsm(); 
		
		/** construct a simulated regular expr fsm */
		Edge expr_edge_foo = new Edge(new refsmId("foo"));
		Edge expr_edge_baz = new Edge(new refsmId("baz"));	
		
		refsmState expr_init = new refsmState(new refsmId(1), true, false);
		refsmState expr_state_1 = new refsmState(new refsmId(2), false, false);
		refsmState expr_state_2 = new refsmState(new refsmId(3), false, false);
		refsmState expr_state_3 = new refsmState(new refsmId(4), false, false);
		refsmState expr_final = new refsmState(new refsmId(5), false, true);
		
		expr.addStates(expr_init);
		expr.addStates(expr_final);
		expr.addStates(expr_state_1);
		expr.addStates(expr_state_2);
		expr.addStates(expr_state_3);
		
		expr.addInitState(expr_init);
		expr.addFinalState(expr_final);
		
		expr_init.addOutgoingStates(expr_state_1, expr_edge_foo);
		expr_init.addOutgoingStates(expr_state_2, expr_edge_baz);
		expr_state_1.addOutgoingStates(expr_state_3, expr_edge_baz);
		expr_state_1.addIncomingStates(expr_init, expr_edge_foo);
		expr_state_2.addOutgoingStates(expr_state_3, expr_edge_baz);
		expr_state_2.addIncomingStates(expr_init, expr_edge_baz);
		expr_state_3.addOutgoingStates(expr_final, expr_edge_baz);
		expr_state_3.addIncomingStates(expr_state_1, expr_edge_baz);
		expr_state_3.addIncomingStates(expr_state_2, expr_edge_baz);
		
		
		/** construct a simulated call graph fsm */
		Edge call_edge_foo = new Edge(new cgfsmId("foo"));
		Edge call_edge_baz = new Edge(new cgfsmId("baz"));
		
		cgfsmState call_init = new cgfsmState(new cgfsmId(1), true, false, null);
		cgfsmState call_state_1 = new cgfsmState(new cgfsmId(2), false, false, call_edge_foo);
		cgfsmState call_final = new cgfsmState(new cgfsmId(3), false, true, call_edge_baz);
		
		
		call.addStates(call_init);
		call.addStates(call_state_1);
		call.addStates(call_final);
		
		call.addInitState(call_init);
		call.addFinalState(call_final);
		
		call_init.addOutgoingStates(call_state_1, call_edge_foo);
		call_state_1.addOutgoingStates(call_final, call_edge_baz);
		call_final.addOutgoingStates(call_final, call_edge_baz);
		
		//System.out.println("Hello world!");
		expr.dump();
		call.dump();
		
		intersectFSM comb = new intersectFSM();
		comb.build(expr, call);
		comb.dump();
		
	}
}
