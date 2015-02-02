package edu.utexas.cgrex.analyses;

import soot.Context;
import soot.jimple.InvokeExpr;

public class CgContext implements Context{
	InvokeExpr callsite;
	
	public CgContext(InvokeExpr stmt) {
		this.callsite = stmt;
	}
	
	public InvokeExpr getCallsite() {
		return callsite;
	}
	
	public String toString() {
		return callsite.toString();
	}
	
}
