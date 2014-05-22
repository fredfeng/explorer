package edu.utexas.cgrex.benchmarks;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Transform;

public class DemandDrivenHarness {

	public static int benchmarkSize = 1;

	// 0: interactive mode; 1: benchmark mode
	public static int mode = 1;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// String targetLoc = args[0];
		// System.out.println("begin to run benchmark----------" + targetLoc);
		// String targetLoc = "benchmarks/CFLexamples/bin/";
		// String targetLoc = "../CFLexamples/bin/";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesHardTestSuite/benchmarks/javazoom/classes";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/soot-j/classes";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/javasrc-p/classes";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/gj/classes";
		// String targetLoc =
		// "benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/jpat-p/classes";

		String targetLoc = "benchmarks/sablecc-3.7/classes/";
		// 0: interactive mode; 1: benchmark mode
		try {

			PackManager.v().getPack("wjtp").add(new Transform("wjtp.ddd",
			// new PTAnalysisAndCacheTransformer()));
					new CacheAndChainedCacheTransformer2()));

			String mainClassName = "JPATTest";

			// String mainClassName = "ca.mcgill.sable.soot.jimple.Main";
			// String mainClassName = "javasrc.app.JavaSrc";
			// String mainClassName = "gjc.Main";

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
							// "-main-class", mainClassName,
							"-no-bodies-for-excluded", "-exclude", "java",
							"-exclude", "javax", "-output-format", "none" });

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
