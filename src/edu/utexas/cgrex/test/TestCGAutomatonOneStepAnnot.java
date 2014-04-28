package edu.utexas.cgrex.test;

import java.util.Map;
import java.util.Set;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.RegAutomaton;

public class TestCGAutomatonOneStepAnnot {
	public static void main(String[] args) {

		RegAutomaton reg = TestRegAutomatonOneStepAnnot.test();
		CGAutomaton call = TestBuildCGStateSCC.test();

		reg.buildOneStepAnnot(); // annotate reg automaton
		// get the annotations
		Map<AutoState, Set<AutoEdge>> regAnnots = reg.getOneStepAnnot();

		call.buildCGStatesSCC(); // consider scc
		call.annotateOneStep(regAnnots); // annotate call automaton
		// get the annotations
		Map<AutoState, Map<AutoState, Boolean>> callAnnots = call
				.getAnnotOneStep();

		reg.dump();
		call.dump();
		call.dumpSCCInfo();
		call.dumpAnnotInfo();

	}
}
