package edu.utexas.cgrex.benchmarks;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Transform;

/**
 * The harness for wrapping up both CHA&OTF algorithms.
 * @author yufeng
 *
 */
public class BenchmarkHarness {

	public static int benchmarkSize = 10;
	
	//we will collect the running time at each interval.
	public static int interval = 5;


	// 0: interactive mode; 1: benchmark mode
	public static int mode = 1;
	
	public static String queryLoc = "";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String targetLoc = args[0];
		String targetMain = args[1];
		queryLoc = args[2];
		String alg = args[3];

		System.out.println("benchmark----------" + targetLoc);
		try {

			if(alg.equals("cha"))
				PackManager
						.v()
						.getPack("wjtp")
						.add(new Transform("wjtp.regularPT",
								new CHATransformer()));
			else
				PackManager
				.v()
				.getPack("wjtp")
				.add(new Transform("wjtp.regularPT",
						new OnTheFlyTransformer()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
							"-main-class", targetMain,
							/*"-no-bodies-for-excluded", 
							"-exclude", "java",
							"-exclude", "javax", 
							"-output-format", "none",*/
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
