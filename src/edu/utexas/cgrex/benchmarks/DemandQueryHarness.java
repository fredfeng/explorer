package edu.utexas.cgrex.benchmarks;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Transform;

/**
 * The harness for wrapping up both CHA&OTF algorithms.
 * 
 * @author yufeng
 * 
 */
public class DemandQueryHarness {

	public static int benchmarkSize = 1000;

	// we will collect the running time at each interval.
	public static int interval = 5;

	// 0: interactive mode; 1: benchmark mode
	public static int mode = 1;

	public static String queryLoc = "";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// String targetLoc = args[0];
		// String targetMain = args[1];
		// queryLoc = args[2];
		// String alg = args[3];

		queryLoc = "scripts/sootj_regx_2k.txt";

		// String targetLoc = "benchmarks/CFLexamples/bin/";
		// String targetLoc = "../CFLexamples/bin/";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesHardTestSuite/benchmarks/javazoom/classes";
		String targetLoc = "benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/soot-j/classes";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/javasrc-p/classes";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/gj/classes";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/jpat-p/classes";

		// String targetLoc = "benchmarks/sablecc-3.7/classes/";
		// 0: interactive mode; 1: benchmark mode

		String targetMain = "ca.mcgill.sable.soot.jimple.Main";

		System.out.println("benchmark----------" + targetLoc);
		try {

			PackManager
					.v()
					.getPack("wjtp")
					.add(new Transform("wjtp.iff", new DemandQueryTransformer()));

			/*
			 * if(alg.equals("cha")) PackManager .v() .getPack("wjtp") .add(new
			 * Transform("wjtp.regularPT", new CHATransformer())); else
			 * PackManager .v() .getPack("wjtp") .add(new
			 * Transform("wjtp.regularPT", new OnTheFlyTransformer()));
			 */

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
							"-main-class", targetMain,
							 "-soot-class-path", "lib/rt.jar:lib/jce.jar",
							/*"-no-bodies-for-excluded", "-exclude", "java",
							"-exclude", "javax",*/
							"-output-format", "none" });

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
