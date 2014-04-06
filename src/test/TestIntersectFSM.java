package test;

import java.util.*;

import test.util.*;

public class TestIntersectFSM {
	public static void main(String[] args) {
		cgfsm call = new cgfsm();
		refsm expr = new refsm();
		
		/** construct a simulated regular expr fsm */
		refsmState expr_init = new refsmState(new refsmId("expr_init"), true, false);
		refsmState expr_final = new refsmState(new refsmId("expr_final"), false, true);
		Edge expr_edge = new Edge(new refsmId("foo()"));
		expr.addInitState(expr_init);
		expr.addFinalState(expr_final);
		expr_init.addOutgoingStates(expr_final, expr_edge);
		expr_final.addIncomingStates(expr_init, expr_edge);
		
		/** construct a simulated call graph fsm */
		cgfsmState call_init = new cgfsmState(new cgfsmId("call_init"), true, false, null);
		Edge call_foo = new Edge(new cgfsmId("foo()"));
		cgfsmState call_final = new cgfsmState(new cgfsmId("foo()"), false, true, call_foo);
		
		
	}
}
