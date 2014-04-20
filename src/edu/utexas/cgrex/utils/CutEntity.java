package edu.utexas.cgrex.utils;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;

/**
 * wrapper class for AutoState and AutoEdge
 * @author yufeng
 *
 */
public class CutEntity {
	
	public AutoState state;
	public AutoEdge edge;
	
	public CutEntity(AutoState state, AutoEdge edge) {
		this.state = state;
		this.edge = edge;
	}
	
	@Override
	public String toString() {
		return state + "->" + edge;
	}

}
