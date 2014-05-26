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

		// original DaCapo benchmarks
		// String targetLoc = "benchmarks/dacapo/dacapo-9.12-bach/jar/avrora/";

		// sootified folder (after transforming)
		// String targetLoc = "benchmarks/dacapo/tranformed/sootified/avrora";
		// String targetLoc = "benchmarks/dacapo/tranformed/sootified/batik";
		// String targetLoc = "benchmarks/dacapo/tranformed/sootified/luindex";
		// String targetLoc = "benchmarks/dacapo/tranformed/sootified/lusearch";
		// String targetLoc = "benchmarks/dacapo/tranformed/sootified/h2";
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/sunflow";
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/pmd";
		String targetLoc = "benchmarks/dacapo/transformed/sootified/eclipse";

		// 0: interactive mode; 1: benchmark mode
		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.ddd", new DaCapoTransformer()));

			String mainClassName = "Harness";
			// String mainClassName = "avrora.Main";

			StringBuilder sootClassPath = new StringBuilder();
			// java class path
			sootClassPath.append("benchmarks/dacapo/tranformed/lib/jce.jar");
			sootClassPath.append(":benchmarks/dacapo/tranformed/lib/rt.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/commons-cli-1.2.jar");

			// avrora class path
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/avrora-cvs-20091224.jar");

			// batik class path
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/xml-apis-ext.jar");
			sootClassPath
					.append(":benchmarks/dacapo/transformed/lib/xml-apis.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/crimson-1.1.3.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/xalan-2.6.0.jar");
			sootClassPath.append(":benchmarks/dacapo/tranformed/lib/ui.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/com.springsource.org.mozilla.javascript-1.7.0.R2.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/classes.jar");

			// luindex and lusearch class path
			sootClassPath
					.append(":benchmarks/dacapo/dacapo-9.12-bach/jar/old/lucene-core-2.4.jar");
			sootClassPath
					.append(":benchmarks/dacapo/dacapo-9.12-bach/jar/old/lucene-demos-2.4.jar");

			// h2 class paht
			sootClassPath
					.append(":benchmarks/dacapo/dacapo-9.12-bach/jar/old/derbyTesting.jar");
			sootClassPath
					.append(":benchmarks/dacapo/dacapo-9.12-bach/jar/old/junit-3.8.1.jar");
			sootClassPath
					.append(":benchmarks/dacapo/dacapo-9.12-bach/jar/old/tpcc.jar");
			sootClassPath
					.append(":benchmarks/dacapo/dacapo-9.12-bach/jar/old/h2-1.2.121.jar");

			// pmd class path
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/pmd-4.2.5.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/jaxen-1.1.1.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/asm-3.1.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/junit-3.8.1.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/xml-apis.jar");

			// eclipse
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/eclipse.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/org.eclipse.jdt.core-3.6.2.v_A76_R36x.jar");
			sootClassPath
					.append(":benchmarks/dacapo/tranformed/lib/org.eclipse.text_3.5.0.jar");

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
//							"-p jb.tr use-older-type-assigner:true",
//							"-p jb.tr ignore-wrong-staticness:true",
							"use-older-type-assigner:true",
							"-main-class", mainClassName, "-soot-class-path",
							sootClassPath.toString(),
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
