package edu.utexas.cgrex.analyses;

import java.util.Map;

import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.test.RegularExpGenerator;

/**
 * Entry point of the whole analysis.
 * 
 * @author yufeng
 * 
 */
public class QueryTransformer extends SceneTransformer {

	public boolean debug = true;

	//CHA.
	QueryManager qm;
	
	//on-the-fly.
	QueryManager otfQm;
	
	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		qm = new QueryManager(Scene.v().getCallGraph(), Scene.v().getMainMethod());
		this.runBenchmark();
	}
	
	private void runBenchmark() {
		dumpMethods();
		// picking up samples from CHA-based version.
		RegularExpGenerator generator = new RegularExpGenerator(qm);

//		String regx = generator.genRegx();
		//false;
//		String regx = ".*\u1c02.*\u1c08";
		//true;
		String regx = ".*\u1c02.*\u1c06";

		regx = regx.replaceAll("\\s+", "");
		System.out.println("Random regx------" + regx);
		boolean res1 = qm.queryRegx(regx);
		System.out.println("Query result:" + res1);
	}
	
	void dumpMethods() {
		Map<SootMethod, AutoEdge> map = qm.getMethToEdgeMap();
		for(SootMethod meth : map.keySet()) {
			if(meth.getDeclaringClass().getName().contains("java"))
				continue;
			
			System.out.println(meth + " --> " + map.get(meth).getId());
		}
	}
	
}
