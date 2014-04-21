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
	public AutoState endState;

	
	public CutEntity(AutoState state, AutoEdge edge, AutoState endState) {
		this.state = state;
		this.edge = edge;
		this.endState = endState;
	}
	
	@Override
	public String toString() {
		return state + "->" + edge;
	}

}
