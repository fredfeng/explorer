package test.util;

import java.util.*;
import soot.SootMethod;

public class Edge {
	// id for Edge class is a 
	// SootMethod for call graph finite state machine
	// String for regular expression finite state machine
	private Id id; 
	
	public Edge(Id id) {
		this.id = id;
	}
	
	public boolean isDot() {
		return false;
	}
	
	public Id getId() { return id; }
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Edge && id.equals( ((Edge)other).getId() ) ? true : false;
	}
	
	@Override 
	public int hashCode() { return id.hashCode(); }
	
	@Override 
	public String toString() { return id.toString(); }
}
