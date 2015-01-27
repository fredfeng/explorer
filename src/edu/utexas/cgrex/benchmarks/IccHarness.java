package edu.utexas.cgrex.benchmarks;

import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.CompilationDeathException;
import soot.PackManager;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeStmt;
import soot.options.Options;

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
		
		//prefer Android APK files// -src-prec apk
		Options.v().set_src_prec(Options.src_prec_apk);

		//output as APK, too//-f J
		Options.v().set_output_format(Options.output_format_dex);

		// resolve the PrintStream and System soot-classes
		Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
		Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
		
		try {

			PackManager
					.v()
					.getPack("jtp")
					.add(new Transform("jtp.myInstrumenter",
							new BodyTransformer() {
								@Override
								protected void internalTransform(
										final Body b,
										String phaseName,
										@SuppressWarnings("rawtypes") Map options) {
									final PatchingChain units = b.getUnits();
									// important to use snapshotIterator here
									for (Iterator<Unit> iter = units
											.snapshotIterator(); iter.hasNext();) {
										final Unit u = iter.next();
										u.apply(new AbstractStmtSwitch() {
											public void caseInvokeStmt(
													InvokeStmt stmt) {
												// code here
												System.out
														.println("invoke stmt:"
																+ stmt);
											}

										});
									}
								}
							}));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-allow-phantom-refs", "-android-jars", sdk});

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
