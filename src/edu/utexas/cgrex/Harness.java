package edu.utexas.cgrex;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Transform;
import edu.utexas.cgrex.analyses.QueryTransformer;

public class Harness {

	public static int benchmarkSize = 25;
	
	//we will collect the running time at each interval.
	public static int interval = 5;


	// 0: interactive mode; 1: benchmark mode
	public static int mode = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// String targetLoc = args[0];
		// System.out.println("begin to run benchmark----------" + targetLoc);
		// String targetLoc = "benchmarks/CFLexamples/bin/";
		String targetLoc = "/home/yufeng/workspace/CgTestSet/classes/";
		// 0: interactive mode; 1: benchmark mode
		try {

			PackManager
					.v()
					.getPack("wjtp")
					.add(new Transform("wjtp.regularPT",
							new QueryTransformer()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
					// "-no-bodies-for-excluded",
							"-main-class", "Test3",
							"-p", "cg.spark", "enabled:true" });

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}

	}

	public void run(String s) {

	}

}
