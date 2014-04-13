package edu.utexas.cgrex;

import java.util.Set;

import soot.Local;
import soot.Type;

public class QueryManager {

	public QueryManager() {
		buildRegAutomaton();
		buildRegAutomaton();
		buildInterAutomaton();
	}
	
	private void init() {
		
	}
	
	private void buildRegAutomaton() {
		
	}
	
	private void buildCGAutomaton() {
		
	}
	
	private void buildInterAutomaton() {
		
	}
	
	private void grepMinCut() {
		
	}
	
	private boolean doPointsToQuery(Type type, Set<Local> locals) {
		return false;
	}
	
	private void refineCallgraph() {
		
	}
	
	//entry method for the query.
	public boolean doQuery(String regx) {
		return false;
	}
	
}
