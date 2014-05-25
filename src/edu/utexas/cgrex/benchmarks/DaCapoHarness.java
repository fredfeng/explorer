package edu.utexas.cgrex.benchmarks;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Transform;

public class DaCapoHarness {

	public static int benchmarkSize = 1;

	// 0: interactive mode; 1: benchmark mode
	public static int mode = 1;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// String targetLoc = "benchmarks/dacapo/sootified/avrora-default";
		String targetLoc = "benchmarks/dacapo/sootified/luindex-default";

		// 0: interactive mode; 1: benchmark mode
		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.ddd", new DaCapoTransformer()));

			String mainClassName = "Harness";

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
							"-main-class", mainClassName, "-soot-class-path",
							"lib/rt.jar:lib/jce.jar",
							// "-no-bodies-for-excluded",
							// "-exclude", "java",
							// "-exclude", "javax",
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
