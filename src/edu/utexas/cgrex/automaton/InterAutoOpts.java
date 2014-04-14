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
}
