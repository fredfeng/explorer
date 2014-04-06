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
}
