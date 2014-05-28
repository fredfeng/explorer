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
		// runnable
//		String targetLoc = "benchmarks/dacapo/transformed/sootified/avrora";
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/sunflow";
		 String targetLoc = "benchmarks/dacapo/transformed/sootified/pmd";
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/tomcat";

		// possible
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/xalan";

		// impossible (unrecognized bytecode instruction: 168)
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/batik";
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/luindex";
		// String targetLoc =
		// "benchmarks/dacapo/transformed/sootified/lusearch";
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/h2";

		// soot type mask error
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/eclipse";
		// local type not allowed in final code
		// maybe this can be solved by removing the wrong class from the jar?
		// String targetLoc = "benchmarks/dacapo/transformed/sootified/fop";

		// wired benchmarks
		// String targetLoc =
		// "benchmarks/dacapo/transformed/sootified/tradebeans";
		// "benchmarks/dacapo/transformed/sootified/tradesoap";

		// 0: interactive mode; 1: benchmark mode
		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.ddd", new DaCapoTransformer()));

			String mainClassName = "Harness";
			// String mainClassName = "avrora.Main";

			StringBuilder sootClassPath = new StringBuilder();

			// java class path
			sootClassPath.append("benchmarks/dacapo/transformed/lib/jce.jar");
			sootClassPath.append(":benchmarks/dacapo/transformed/lib/rt.jar");

			sootClassPath
					.append(":benchmarks/dacapo/transformed/lib/commons-cli-1.2.jar");
			sootClassPath
					.append(":benchmarks/dacapo/transformed/lib/apache-ant-1.8.2.jar");
			// avrora class path
			sootClassPath
					.append(":benchmarks/dacapo/transformed/lib/avrora-cvs-20091224.jar");

			// batik class path
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xml-apis-ext.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xml-apis.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/crimson-1.1.3.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xalan-2.6.0.jar");
//			sootClassPath.append(":benchmarks/dacapo/transformed/lib/ui.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/com.springsource.org.mozilla.javascript-1.7.0.R2.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/classes.jar");
//
//			// luindex and lusearch class path
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/lucene-core-2.4.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/lucene-demos-2.4.jar");
//
//			// h2 class paht
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/derbyTesting.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/junit-3.8.1.jar");
//			sootClassPath.append(":benchmarks/dacapo/transformed/lib/tpcc.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/h2-1.2.121.jar");
//
			// pmd class path
			sootClassPath
					.append(":benchmarks/dacapo/transformed/lib/pmd-4.2.5.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/jaxen-1.1.1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/asm-3.1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/junit-3.8.1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xml-apis.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xercesImpl.jar");

//			// eclipse
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/eclipse.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.jdt.core-3.6.2.v_A76_R36x.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.text_3.5.0.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.core.filesystem-1.3.100.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.core.resources.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.jdt.launching_3.5.1.v20100108_r352.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.core.resources.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.debug.core-3.3.2.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.core.contenttype-3.4.200.v20130326-1255.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/preferences-3.5.100-v20130422-1538.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.core.runtime-3.1.0");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.eclipse.equinox.app-1.3.100.v20130327-1442.jar");
//
//			// sunflow class paht
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/sunflow-0.07.2.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/janino-2.5.12.jar");
//
//			// xalan class path
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xalan-benchmark.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xalan.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xercesImpl.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xml-apis.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/serializer.jar");
//
//			// fop class path
//			sootClassPath.append(":benchmarks/dacapo/transformed/lib/fop.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/avalon-framework-4.2.0.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/batik-all-1.7.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/commons-io-1.3.1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/commons-logging-1.0.4.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/serializer-2.7.0.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/xmlgraphics-commons-1.3.1.jar");
//
//			// jython class path
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/antlr-3.1.3.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/asm-3.1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/asm-commons-3.1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/constantine-0.4.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/jna-posix.jar");
//			sootClassPath.append(":benchmarks/dacapo/transformed/lib/jna.jar");
//
//			// tomcat class path
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/dacapo-tomcat.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/commons-httpcient.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/commons-codec.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/tomcat-juli.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/commons-daemon.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/commons-logging.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/bootstrap.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/catalina-7.0.1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/org.apache.jasper.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/javax.servlet.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/javax.servlet.jsp.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/javax.servlet-3.0.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/javax-ssl-1_1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/shared-7.0.0.1.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/android-7.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/ant-nodeps-1.6.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/javax.ejb.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/jasper-runtime.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/standard-1.0.6.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/javax.persistence.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/javax.annotation.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/javax.servlet.jsp.jstl.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/jasper-compiler-5.5.15.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/coyote-6.0.28.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/tomcat-coyote-7.0.27.jar");
//			sootClassPath
//					.append(":benchmarks/dacapo/transformed/lib/apache-tomcat-util.jar");

			soot.Main
					.v()
					.run(new String[] {
							"-W",
							"-process-dir",
							targetLoc,
							"-src-prec",
							"java",
							"-allow-phantom-refs",
							mainClassName,
							"-soot-class-path",
							sootClassPath.toString(),
//							"-no-bodies-for-excluded",
							// "-exclude", "java",
							// "-exclude", "javax",
//							"-exclude",
//							"org.apache.tomcat.util.http.fileupload.DiskFileItem",
//							"-exclude",
//							"org.apache.tomcat.util.http.fileupload.DiskFileItemFactory",
//							"-exclude",
//							"org.apache.tomcat.util.http.fileupload.ServletFileUpload",
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
