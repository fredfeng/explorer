package edu.utexas.cgrex.test;

import java.util.HashMap;
import java.util.Map;

import edu.utexas.cgrex.automaton.AnnotTwoStepsWrapper;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.InterAutoOpts;
import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutomaton;

public class TestInterAutomatonBuildTwoSteps {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		RegAutomaton reg = TestRegAutomatonTwoStepAnnot.test1();
		CGAutomaton call = TestBuildCGStateSCC.test2();

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

		// call.dumpSCCInfo();
		call.dumpAnnot2Info();

		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		// myoptions.put("two", true);
		myoptions.put("one", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);
		InterAutomaton inter = new InterAutomaton(myopts, reg, call);
		inter.build();

		inter.dumpFile();
		System.out.println("Finished");
	}
}
