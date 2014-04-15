package edu.utexas.cgrex.test;

import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutomaton;

public class TestInterAutomatonDelete {
	public static void main(String[] args) {
		GenInterAutomaton.gen();
		InterAutomaton inter = GenInterAutomaton.getInterAutomaton();
		RegAutomaton reg = GenInterAutomaton.getRegAutomaton();
		reg.dump();
	}
}
