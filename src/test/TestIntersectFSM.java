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
		Edge expr_edge_bar = new Edge(new refsmId("bar"));
		dotEdge expr_edge_dot = new dotEdge(new refsmId("dot"));
		
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
		Edge call_edge_foo = new Edge(new cgfsmId("foo"));
		Edge call_edge_baz = new Edge(new cgfsmId("baz"));
		Edge call_edge_main = new Edge(new cgfsmId("main"));
		Edge call_edge_x = new Edge(new cgfsmId("x"));
		Edge call_edge_y = new Edge(new cgfsmId("y"));
		Edge call_edge_z = new Edge(new cgfsmId("z"));
		
		cgfsmState call_init = new cgfsmState(new cgfsmId(1), true, false, null);
		cgfsmState call_state_1 = new cgfsmState(new cgfsmId(2), false, true, call_edge_x);
		cgfsmState call_state_2 = new cgfsmState(new cgfsmId(3), false, true, call_edge_y);
		cgfsmState call_state_3 = new cgfsmState(new cgfsmId(4), false, true, call_edge_z);
		cgfsmState call_state_4 = new cgfsmState(new cgfsmId(5), false, true, call_edge_main);
		cgfsmState call_state_5 = new cgfsmState(new cgfsmId(6), false, true, call_edge_foo);
		cgfsmState call_final = new cgfsmState(new cgfsmId(7), false, true, call_edge_baz);
			
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
		
		//System.out.println("Hello world!");
		expr.dump();
		call.dump();
		
		intersectFSM comb_without_opt = new intersectFSM();
		comb_without_opt.buildWithoutOpt(expr, call);
		comb_without_opt.dump();
		
		Map<State, Set<Edge>> opts = expr.find();
		System.out.println(opts);
		Map<State, Map<State, Boolean>> annotations = call.annotate(opts);
		System.out.println(annotations);
		
		intersectFSM comb_with_opt = new intersectFSM();
		comb_with_opt.buildWithOpt(expr, call);
		comb_with_opt.dump();
	}
}
