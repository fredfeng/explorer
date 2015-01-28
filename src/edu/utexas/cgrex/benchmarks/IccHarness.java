package edu.utexas.cgrex.benchmarks;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import edu.utexas.cgrex.android.SetupApplication;
import soot.CompilationDeathException;
import soot.Scene;
import soot.SootMethod;
import soot.util.queue.QueueReader;

/**
 * The harness for Inter-component communication in Android.
 * @author yufeng
 *
 */
public class IccHarness {

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
		String targetLoc = "/home/yufeng/workspace/CgTestSet/malware/00621E015191863041E78726B863B7E1374B17FDA690367878D1272B0E44B232.apk";
		System.out.println("benchmark----------" + targetLoc);
		String sdk = "/home/yufeng/research/others/android-platforms/";
		
		try {
			runAnalysis(targetLoc, sdk);

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}

	}

	private static void runAnalysis(final String fileName,
			final String androidJar) {
		try {

			final SetupApplication app = new SetupApplication(androidJar,
					fileName);

			app.calculateEntryPoints();

			app.printEntrypoints();
			QueueReader qr = Scene.v().getReachableMethods().listener();
			while(qr.hasNext()) {
				SootMethod meth = (SootMethod)qr.next();
				System.out.println("reachable methods:" + meth);
			}

		} catch (IOException ex) {
			System.err.println("Could not read file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (XmlPullParserException ex) {
			System.err.println("Could not read Android manifest file: "
					+ ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

}
