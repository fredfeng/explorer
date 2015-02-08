package edu.utexas.cgrex.benchmarks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.SootUtils;

/**
 * The harness for dead code detection.
 * @author yufeng
 *
 */
public class DeadCodeHarness extends SceneTransformer {

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
		String targetLoc = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/lusearch/classes";
		String cp = "lib/rt.jar:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/shared/dacapo-9.12/classes:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/lusearch/jar/lucene-core-2.4.jar";
		String targetMain = "org.dacapo.harness.ChordHarness";
		
//		String prefix = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/";
//		String targetLoc = prefix + "benchmarks/antlr/classes";
//		String cp = "lib/rt.jar:"
//				+ prefix + "/shared/dacapo-2006-10-MR2/classes:" + prefix
//				+ "benchmarks/antlr/jar/antlr.jar:";
//		String targetMain = "dacapo.antlr.Main";

		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.dead", new DeadCodeHarness()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-allow-phantom-refs", "-soot-classpath", cp,
							"-main-class", targetMain,
							// "-no-bodies-for-excluded",
							"-p", "cg.spark", "enabled:true",

					});

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}

	}
	
	static String[] arr = {
		"<org.apache.commons.cli.Parser: java.util.List getRequiredOptions()>",
		"<org.apache.lucene.index.MultiSegmentReader: org.apache.lucene.index.TermEnum terms()>",
		"<org.apache.lucene.search.DisjunctionSumScorer: void <init>(java.util.List)>",
		"<org.apache.lucene.store.IndexInput: void <init>()>",
		"<org.apache.commons.cli.Options: org.apache.commons.cli.Options addOption(org.apache.commons.cli.Option)>",
		"<org.apache.lucene.store.ChecksumIndexInput: long getFilePointer()>",
		"<org.apache.lucene.index.MultiSegmentReader$MultiTermPositions: void <init>(org.apache.lucene.index.IndexReader[],int[])>",
		"<org.dacapo.harness.DacapoException: void <init>(java.lang.String)>",
		"<org.dacapo.parser.ConfigFileTokenManager: void <init>(org.dacapo.parser.SimpleCharStream)>",
		"<org.apache.commons.cli.ParseException: void <init>(java.lang.String)>",
		"<org.apache.lucene.index.SegmentInfos: long generationFromSegmentsFileName(java.lang.String)>",
		"<org.apache.lucene.index.SegmentReader: org.apache.lucene.index.SegmentReader get(boolean,org.apache.lucene.store.Directory,org.apache.lucene.index.SegmentInfo,org.apache.lucene.index.SegmentInfos,boolean,boolean,int,boolean)>",
		"<org.dacapo.harness.CommandLineArgs: void defineCallback()>",
		"<org.dacapo.parser.SimpleCharStream: void <init>(java.io.Reader,int,int,int)>",
		"<org.apache.lucene.util.ScorerDocQueue$HeapedScorerDoc: void <init>(org.apache.lucene.util.ScorerDocQueue,org.apache.lucene.search.Scorer)>",
		"<org.apache.lucene.index.CorruptIndexException: void <init>(java.lang.String)>",
		"<org.apache.lucene.search.IndexSearcher: void <init>(org.apache.lucene.index.IndexReader,boolean)>",
		"<org.apache.commons.cli.Option: void clearValues()>",
		"<org.apache.lucene.analysis.CharArraySet: boolean contains(java.lang.Object)>",
		"<org.apache.lucene.search.MultiTermQuery: boolean equals(java.lang.Object)>",
		"<org.apache.lucene.index.DirectoryIndexReader$1: void <init>(org.apache.lucene.store.Directory,boolean,org.apache.lucene.index.IndexDeletionPolicy,boolean)>"
	};

	@Override
	protected void internalTransform(String phaseName,
			Map<String, String> options) {
		CallGraph cicg = Scene.v().getCallGraph();
		SootMethod main = Scene.v().getMainMethod();
		QueryManager qm = new QueryManager(cicg, main);
		Set<String> querySet = new HashSet<String>();

		for (SootMethod meth : SootUtils.getChaReachableMethods()) {
			if (meth.isJavaLibraryMethod()
					|| Scene.v().getEntryPoints().contains(meth))
				continue;
			
			String query = main.getSignature() + ".*" + meth.getSignature();
			querySet.add(query);
		}
		
		
		// Set<String> remain = new HashSet<String>();
		// for (String tmp : Arrays.asList(arr)) {
		// String my = main.getSignature() + ".*" + tmp;
		// my = qm.getValidExprBySig(my);
		// boolean res11 = qm.queryRegx(my);
		// if (!res11)
		// remain.add(tmp);
		// }
		//
		// for (String re : remain) {
		// System.out.println("Fail:" + re);
		// }
		// assert false : remain.size();

		int falseCnt = 0;
		int cnt = 0;
		for (String q : querySet) {
			cnt++;
			String regx = qm.getValidExprBySig(q);
			regx = regx.replaceAll("\\s+", "");
			boolean res1 = qm.queryRegx(regx);
			if (!res1) {
				falseCnt++;
				System.out.println(falseCnt + " || " + cnt + "--****-out of---"
						+ querySet.size());
				System.out.println("unreach:" + q);
			}
			if(cnt > 100)
				break;
		}
		//dump info.
		System.out.println("total time on product: " + totalInter);
		System.out.println("total time on cut: " + totalCut);
		System.out.println(falseCnt + " out of 100");
	}
	
	public static double totalCut = 0.0;
	public static double totalInter = 0.0;
}
