package edu.utexas.cgrex.analyses;

import java.util.HashMap;
import java.util.Map;

import soot.Scene;
import soot.SceneTransformer;
import edu.utexas.cgrex.QueryManager;

/**
 * Entry point of the whole analysis.
 * 
 * @author yufeng
 * 
 */
public class QueryTransformer extends SceneTransformer {

	public boolean debug = true;

	// CHA.
	QueryManager qm;

	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		qm = new QueryManager(Scene.v().getCallGraph(), Scene.v()
				.getMainMethod());
		this.runRegression();
	}
	
	// perform regression tests.
	protected void runRegression() {
		HashMap<String, Boolean> testbed = new HashMap<String, Boolean>();
		String mainName = Scene.v().getMainClass().getName();
		if (mainName.equals("Test1")) {
			testbed.put(".*<Test1: void main2()>.*<B: void bar()>", true);
			testbed.put(".*<Test1: void main1()>.*<A: void bar()>", true);

			testbed.put(".*<Test1: void main2()>.*<A: void bar()>", false);
			testbed.put(".*<Test1: void main1()>.*<B: void bar()>", false);
		} else if (mainName.equals("Test2")) {
			testbed.put(
					"<Test2: void main(java.lang.String[])>.*<A: void goo()>",
					false);
			testbed.put(
					"<Test2: void main(java.lang.String[])>.*<B: void goo()>",
					true);
		} else {
			testbed.put(
					"<Test3: void main(java.lang.String[])>.*<B: void bar()>",
					true);
		}
		for (String key : testbed.keySet()) {
			String regx = qm.getValidExprBySig(key);
			boolean value = testbed.get(key);
			boolean res = qm.queryRegx(regx);
			assert res == value;
		}
		System.out.println("PASS all regression tests!");
	}
}
