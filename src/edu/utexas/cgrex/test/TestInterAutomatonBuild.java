package edu.utexas.cgrex.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.InterAutoOpts;
import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutomaton;

public class TestInterAutomatonBuild {

	public static void main(String[] args) {
		test();
	}

	public static void test() {
		RegAutomaton reg = TestRegAutomatonOneStepAnnot.test();
		CGAutomaton call = TestBuildCGStateSCC.test();

		reg.buildOneStepAnnot(); // annotate reg automaton
		// get the annotations
		Map<AutoState, Set<AutoEdge>> regAnnots = reg.getOneStepAnnot();

		call.buildCGStatesSCC(); // consider scc
		call.annotateOneStep(regAnnots); // annotate call automaton
		// get the annotations

		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		myoptions.put("two", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);
		InterAutomaton inter = new InterAutomaton(myopts, reg, call);
		inter.build();

		inter.dumpFile();
		System.out.println("Finished");
	}
}
