package edu.utexas.cgrex;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Transform;
import edu.utexas.cgrex.analyses.CGregxTransformer;

public class Harness {

	public static int benchmarkSize = 500;

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
		String targetLoc = "benchmarks/sablecc-3.7/classes/";
		// 0: interactive mode; 1: benchmark mode
		try {

			StringBuilder options = new StringBuilder();
			PackManager
					.v()
					.getPack("wjtp")
					.add(new Transform("wjtp.regularPT",
							new CGregxTransformer()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
							"-no-bodies-for-excluded", "-exclude", "java",
							"-exclude", "javax", "-output-format", "none",
							"-p", "jb", "use-original-names:true", });

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
