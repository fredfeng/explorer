package edu.utexas.cgrex.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import edu.utexas.cgrex.automaton.AutoEdge;

/**
 * Generating regular expressions automatically
 * based on current call graph.
 * @author yufeng
 *
 */
public class RegularExpGenerator {
	
	Map<SootMethod, AutoEdge> methToEdgeMap;
	
	List<SootMethod> reachableMethods = new ArrayList<SootMethod>();
	
	//Template 1: (_|_).*_, replace _ with random methods.
	public RegularExpGenerator(	Map<SootMethod, AutoEdge> methToEdgeMap) {
		this.methToEdgeMap = methToEdgeMap;
	}
	
	public String genRegx() {
		SootMethod meth1 = pickupMethod();
		String uid1 = (String)methToEdgeMap.get(meth1).getId();
		
		SootMethod meth2 = pickupMethod();
		String uid2 = (String)methToEdgeMap.get(meth2).getId();
		
		SootMethod meth3 = pickupMethod();
		String uid3 = (String)methToEdgeMap.get(meth3).getId();
		String template = "(" + uid1 + "|" +uid2 +").*" +uid3;
		
		return template;
	}
	
	private SootMethod pickupMethod() {
		Random randomizer = new Random();
		
		if (reachableMethods.size() == 0) {
			Iterator<MethodOrMethodContext> mIt = Scene.v()
					.getReachableMethods().listener();
			while(mIt.hasNext()) reachableMethods.add((SootMethod)mIt.next());
		}
		
		SootMethod ranMethod = reachableMethods.get(randomizer
				.nextInt(reachableMethods.size()));
		
		return ranMethod;
	}

}
