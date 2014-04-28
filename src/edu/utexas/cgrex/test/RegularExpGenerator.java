package edu.utexas.cgrex.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.automaton.AutoEdge;

/**
 * Generating regular expressions automatically
 * based on current call graph.
 * @author yufeng
 *
 */
public class RegularExpGenerator {
	
	//total templates we can choose
	private int templateNum = 3;
	
	Map<SootMethod, AutoEdge> methToEdgeMap;
	
	private QueryManager manager;
	
	List<SootMethod> reachableMethods = new ArrayList<SootMethod>();

	
	public RegularExpGenerator(	QueryManager manager) {
		this.methToEdgeMap = manager.getMethToEdgeMap();
		this.manager = manager;
	}
	
	public String genRegx() {
		Random randomizer = new Random();
		int ran = randomizer.nextInt(templateNum);
		
		switch(ran) {
		case 0:
			return template1();
		case 1:
			return template2();
		case 2: 
			return template3();
		default:
			return template2();
		}
	}
	
	//Template 2: .*(_|__).*_, replace _ with random methods.
	private String template3() {
		SootMethod meth1 = pickupMethod();
		String uid1 = (String)methToEdgeMap.get(meth1).getId();
		
		SootMethod meth2 = pickupMethod();
		String uid2 = (String)methToEdgeMap.get(meth2).getId();
		
		SootMethod meth3 = pickupMethod();
		String uid3 = (String)methToEdgeMap.get(meth3).getId();
		
		SootMethod meth4 = pickupMethod();
		String uid4 = (String)methToEdgeMap.get(meth4).getId();
		
		String template = ".*(" +uid1 + "|" + uid2 + uid3 + ").*" + uid4;

		return template;
	}
	
	//Template 3: .*_.*_.*(_|_), replace _ with random methods.
	private String template2() {
		SootMethod meth1 = pickupMethod();
		String uid1 = (String)methToEdgeMap.get(meth1).getId();
		
		SootMethod meth2 = pickupMethod();
		String uid2 = (String)methToEdgeMap.get(meth2).getId();
		
		SootMethod meth3 = pickupMethod();
		String uid3 = (String)methToEdgeMap.get(meth3).getId();
		
		SootMethod meth4 = pickupMethod();
		String uid4 = (String)methToEdgeMap.get(meth4).getId();
		
		String template = ".*" + uid1 + ".*" +uid2 +".*(" +uid3 + "|" + uid4 + ")";
		
		return template;
	}
	
	//Template 1: (_|_).*_, replace _ with random methods.
	private String template1() {
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
			Iterator<MethodOrMethodContext> mIt = manager
					.getReachableMethods().listener();
			while(mIt.hasNext()) reachableMethods.add((SootMethod)mIt.next());
		}
		
		SootMethod ranMethod = reachableMethods.get(randomizer
				.nextInt(reachableMethods.size()));
		
		return ranMethod;
	}

}
