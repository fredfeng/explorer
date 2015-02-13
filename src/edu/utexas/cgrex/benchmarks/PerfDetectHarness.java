package edu.utexas.cgrex.benchmarks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import soot.Scene;
import soot.SootMethod;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.android.SetupApplication;
import edu.utexas.cgrex.utils.SootUtils;
import edu.utexas.cgrex.utils.StringUtil;

/**
 * The harness for sensitive APIs in Android.
 * 
 * @author yufeng
 *
 */
public class PerfDetectHarness {

	public static final String apiMapping = "apis_perf.txt";

	public static  String sdk = "/home/yufeng/research/others/android-platforms";

	public static final String dummyMain = "<dummyMainClass: void dummyMainMethod()>";

	public static String tmp = "/home/yufeng/research/benchmarks/malware/oopsla15/003af600bae11d6f15e4f936dd47014e608e6487.apk";
	 
	
	 
	 
	// We use Beanbot family as the example.
	// 1. onReceive -> abortBroadcast
	// 2. startService -> getDeviceId, SimSerialNumber, getline1Number

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			tmp = args[0];
		}

		System.out.println("Analyzing app: ------------------" + tmp);
		long startDd = System.nanoTime();
		runAnalysis(tmp, sdk);
		long endDd = System.nanoTime();
		StringUtil.reportSec("Analyzing time:", startDd, endDd);
	}

	private static boolean runAnalysis(final String fileName,
			final String androidJar) {
		boolean val = false;
		try {
			File fin = new File(apiMapping);
			FileInputStream fis = new FileInputStream(fin);
			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			Set<String> lengthyOperations = new HashSet<String>();
			String line = null;
			while ((line = br.readLine()) != null) {
				lengthyOperations.add(line);
			}
			br.close();
			String[] cbList = { "onCreate", "onResume", "onPause", "onStop" };
			Set<String> callbacks = new HashSet<String>();

			System.out.println("Analyzing app:" + fileName);
			SetupApplication app = new SetupApplication(androidJar, fileName);
			app.calculateEntryPoints();
			app.printEntrypoints();
			SootMethod main = Scene.v().getMethod(dummyMain);
			List<SootMethod> entries = new LinkedList<SootMethod>();
			entries.add(main);
			Scene.v().setEntryPoints(entries);

			Iterator<SootMethod> it = Scene.v().getMethodNumberer().iterator();
			while (it.hasNext()) {
				SootMethod cb = it.next();
				if (Arrays.asList(cbList).contains(cb.getName()))
					callbacks.add(cb.getSignature());
			}
			QueryManager qmExplorer = new QueryManager(
					Scene.v().getCallGraph(), main);
			QueryManager qmCha = new QueryManager(SootUtils.getCha(), main);
			// n*m queries.
			int total = 0;
			int diff = 0;
			int chaTrue = 0;
			int exTrue = 0;
			for (String src : callbacks) {
				for (String tgt : lengthyOperations) {
					total++;
					String query = dummyMain + ".*" + src + ".*" + tgt;
					String regx = qmCha.getValidExprBySig(query);
					regx = regx.replaceAll("\\s+", "");
					boolean res1 = qmCha.querySig(regx);
					boolean res2 = qmExplorer.queryRegx(regx);
					if (res1)
						chaTrue++;
					if (res2)
						exTrue++;

					if (res1 && !res2)
						diff++;
				}

			}

			System.out.println("total queries: " + total);
			System.out.println("diff queries**:" + diff + " chaTrue:" + chaTrue
					+ " exTrue:" + exTrue);
			if (diff > 20)
				System.out.println("Good app: " + tmp);

			return val;

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
