package edu.utexas.cgrex.test;

import java.util.Set;

import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.utils.GraphUtil;

public class TestFindRoots {
	public static void main(String[] args) {
		CGAutomaton call = TestBuildCGStateSCC.test();

		call.dump();
		Set<AutoState> roots = GraphUtil.findRoots(call);
		System.out.println(roots);
	}
}
