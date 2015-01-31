package edu.utexas.cgrex.automaton;

import soot.jimple.Stmt;

public class CGAutoEdge extends AutoEdge {

	public CGAutoEdge(Object id, Stmt stmt) {
		super(id);
		setSrcStmt(stmt);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof CGAutoEdge))
			return false;

		CGAutoEdge cgEdge = (CGAutoEdge) other;
		
		if(srcStmt == null) {
			//1. native call. 2. main entries
			return id.equals(cgEdge.getId());
		}

		return id.equals(cgEdge.getId()) && srcStmt.equals(cgEdge.getSrcStmt());
	}

	@Override
	public int hashCode() {
		return id.hashCode() + (srcStmt != null ? srcStmt.hashCode() : 0);
	}

	@Override
	public String toString() {
		return id.toString() + "|"
				+ (srcStmt != null ? srcStmt.toString() : "null");
	}
}
