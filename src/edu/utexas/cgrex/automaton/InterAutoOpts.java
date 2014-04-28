package edu.utexas.cgrex.automaton;

import java.util.Map;

public class InterAutoOpts {

	private Map<String, Boolean> options;

	/*
	 * ["annot", true/false]
	 */
	public InterAutoOpts(Map<String, Boolean> options) {
		this.options = options;
	}

	public boolean annotated() {
		return options.containsKey("annot")
				&& options.get("annot").equals(true);
	}

	public boolean oneStep() {
		return options.containsKey("one") && options.get("one").equals(true);
	}
	
	public boolean twoStep() {
		return options.containsKey("two") && options.get("two").equals(true);
	}
}
