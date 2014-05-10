package edu.utexas.cgrex.benchmarks;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Transform;
import edu.utexas.cgrex.benchmarks.OnTheFlyTransformer;

public class GenRegxHarness {

	public static int benchmarkSize = 100;
	
	//we will collect the running time at each interval.
	public static int interval = 5;


	// 0: interactive mode; 1: benchmark mode
	public static int mode = 1;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String targetLoc = args[0];
		String targetMain = args[1];
		System.out.println("Gen regx for benchmark----------" + targetLoc);
		System.out.println("Main method----------" + targetMain);
		// 0: interactive mode; 1: benchmark mode
		try {

			StringBuilder options = new StringBuilder();
			PackManager
					.v()
					.getPack("wjtp")
					.add(new Transform("wjtp.regularPT",
							new QueryGenTransformer()));
			

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
                            "-main-class", targetMain,
							"-src-prec", "java", "-allow-phantom-refs",
							"-no-bodies-for-excluded", 
                            "-exclude", "java",
							"-exclude", "javax", 
                            "-output-format", "none",
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
