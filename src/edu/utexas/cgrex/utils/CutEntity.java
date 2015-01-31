package edu.utexas.cgrex.utils;

import soot.jimple.Stmt;
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
	protected Stmt srcStmt;


	
	public CutEntity(AutoState state, AutoEdge edge, AutoState endState) {
		this.state = state;
		this.edge = edge;
		this.endState = endState;
		srcStmt = edge.getSrcStmt();
	}
	
	public AutoState getSrc() {
		return state;
	}
	
	public AutoState getTgt() {
		return endState;
	}
	
	public Stmt getStmt() {
		return srcStmt;
	}
	
	@Override
	public String toString() {
		return state + "->(" + srcStmt + ")" + endState;
	}

}
