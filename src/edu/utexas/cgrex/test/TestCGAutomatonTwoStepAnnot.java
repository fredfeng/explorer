package edu.utexas.cgrex.test;

import java.util.Map;

import edu.utexas.cgrex.automaton.AnnotTwoStepsWrapper;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.RegAutomaton;

public class TestCGAutomatonTwoStepAnnot {
	public static void main(String[] args) {

		RegAutomaton reg = TestRegAutomatonTwoStepAnnot.test();
		CGAutomaton call = TestBuildCGStateSCC.test();

		reg.dump();
		call.dump();
		
		reg.buildTwoStepAnnot(); // annotate reg automaton
		// get the annotations
		Map<AutoState, AnnotTwoStepsWrapper> regAnnots = reg.getTwoStepAnnot();

		call.buildCGStatesSCC(); // consider scc
		call.annotateTwoSteps(regAnnots); // annotate call automaton
		// get the annotations
		Map<AutoState, Map<AutoState, Boolean>> callAnnots = call
				.getAnnotTwoSteps();

		
		//call.dumpSCCInfo();
		call.dumpAnnot2Info();

	}
}
