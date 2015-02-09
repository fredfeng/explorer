package edu.utexas.cgrex.benchmarks;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
	
	public static String outLoc = "";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String prefix = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/";
		String targetLoc = "", cp = "", targetMain = "org.dacapo.harness.ChordHarness";
		outLoc = prefix + "benchmarks/"; 
		if (args.length > 0) {
			// run from shell.
			String benName = args[0];
			outLoc = outLoc + benName + "/cgoutput.txt";
			if (benName.equals("luindex")) {
				targetLoc = prefix + "benchmarks/luindex/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix
						+ "benchmarks/luindex/jar/lucene-core-2.4.jar:"
						+ prefix
						+ "benchmarks/luindex/jar/lucene-demos-2.4.jar";
			} else if (benName.equals("lusearch")) {
				targetLoc = prefix + "benchmarks/lusearch/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix
						+ "benchmarks/lusearch/jar/lucene-core-2.4.jar";
			} else if (benName.equals("antlr")) {
				targetLoc = prefix + "benchmarks/antlr/classes";
				cp = "lib/rt.jar:" + prefix
						+ "shared/dacapo-2006-10-MR2/classes:" + prefix
						+ "benchmarks/antlr/jar/antlr.jar:";
				targetMain = "dacapo.antlr.Main";
			} else if (benName.equals("avrora")) {
				targetLoc = prefix + "benchmarks/avrora/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix
						+ "benchmarks/avrora/jar/avrora-cvs-20091224.jar";
			} else {
				assert benName.equals("pmd") : "unknown benchmark" + benName;
				targetLoc = prefix + "benchmarks/pmd/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/pmd/jar/asm-3.1.jar:" + prefix
						+ "benchmarks/pmd/jar/jaxen-1.1.1.jar:" + prefix
						+ "benchmarks/pmd/jar/pmd-4.2.5.jar:" + prefix
						+ "benchmarks/pmd/jar/junit-3.8.1.jar:" + prefix
						+ "benchmarks/pmd/jar/ant.jar";
			}
		}


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
		
//		SootMethod testMeth = Scene.v().getMethod(
//				"<org.apache.lucene.index.SegmentTermPositions: void <clinit>()>");
//		Iterator<Edge> it = cicg.edgesInto(testMeth);
//		while(it.hasNext()) {
//			System.out.println(it.next());
//		}
//		assert false;

		int appSize = 0;
		for (SootMethod meth : SootUtils.getChaReachableMethods()) {
			if(!meth.isJavaLibraryMethod()) 
				appSize++;
			if (meth.isJavaLibraryMethod()
					|| Scene.v().getEntryPoints().contains(meth)
					|| meth.isConstructor()
					|| meth.getName().contains("toString")
					|| meth.getName().contains("<clinit>"))
				continue;
			
			String query = main.getSignature() + ".*" + meth.getSignature();
			querySet.add(query);
		}
		
		int falseCnt = 0;
		int cnt = 0;
		Set<String> outSet = new HashSet<String>();
		for (String q : querySet) {
			cnt++;
			String regx = qm.getValidExprBySig(q);
			regx = regx.replaceAll("\\s+", "");
			boolean res1 = qm.queryRegx(regx);
			if (!res1) {
				falseCnt++;
				System.out.println("unreach:" + q);
				outSet.add("unreach:" + q);
			} else {
				System.out.println("yesreach:" + q);
				outSet.add("yesreach:" + q);
			}
				
			if(cnt >= 100)
				break;
		}
		//dump info.
		System.out.println("----------DeadCode report-------------------------");
		System.out.println("Total time on product: " + totalInter);
		System.out.println("Total time on cut: " + totalCut);
		System.out.println("Total methods in App: " + appSize);
		System.out.println("Total refutations: " + falseCnt);
		
		PrintWriter writer;
		try {
			writer = new PrintWriter(outLoc, "UTF-8");
			writer.println("----------DeadCode report-------------------------");
			writer.println("Total time on product: " + totalInter);
			writer.println("Total time on cut: " + totalCut);
			writer.println("Total methods in App: " + appSize);
			writer.println("Total refutations: " + falseCnt);
			writer.println("Method detaisl----");
			for(String out : outSet) {
				writer.println(out);
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

	}
	
	public static double totalCut = 0.0;
	public static double totalInter = 0.0;
}
