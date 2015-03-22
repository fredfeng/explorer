package edu.utexas.cgrex.benchmarks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.CompilationDeathException;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.queue.QueueReader;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.SootUtils;

/**
 * The harness for dead code detection.
 * @author yufeng
 *
 */
public class ObserverHarness extends SceneTransformer {

	static String outLoc = "";
	
	private double totalTimeOnCha = 0;
	
	private double totalTimeOnCipa = 0;
	
	private double totalTimeOnNoOpt = 0;
	
	private double totalTimeNormal = 0;
	
	private double totalNoCut = 0.0;
    
	protected int maxQueries = 300;
	
	protected static boolean compareKobj = false;
	
	protected static String benName = "";
	
	protected static long timeout = 7200 * 1000;	
	
	protected static enum RunType {
		NORMAL, CHA, NOCUT, NOOPT;
	}
	
	protected static RunType currType = RunType.NORMAL;
	
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
			currType = RunType.valueOf(args[1]);
			outLoc = outLoc + benName + "/cgoutput-3-18.txt";
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
			} else if (benName.equals("chart")) {
				targetMain = "dacapo.chart.Main";
				targetLoc = prefix + "benchmarks/chart/classes";
				cp = "lib/rt.jar:" + prefix
						+ "shared/dacapo-2006-10-MR2/classes:" + prefix
						+ "benchmarks/chart/jar/chart.jar:" + prefix
						+ "benchmarks/chart/jar/lowagie.jar";

			} else if (benName.equals("fop")) {
				targetLoc = prefix + "benchmarks/fop/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/fop/jar/fop.jar:" + prefix
						+ "benchmarks/fop/jar/serializer-2.7.0.jar:" + prefix
						+ "benchmarks/fop/jar/avalon-framework-4.2.0.jar:"
						+ prefix + "benchmarks/fop/jar/commons-io-1.3.1.jar:"
						+ prefix
						+ "benchmarks/fop/jar/xmlgraphics-commons-1.3.1.jar:"
						+ prefix
						+ "benchmarks/fop/jar/commons-logging-1.0.4.jar:"
						+ prefix + "benchmarks/fop/jar/xml-apis-ext.jar";

			} else if (benName.equals("bloat")) {
				targetLoc = prefix + "benchmarks/bloat/classes";
				targetMain = "dacapo.bloat.Main";
				cp = "lib/rt.jar:" + prefix
						+ "shared/dacapo-2006-10-MR2/classes:" + prefix
						+ "benchmarks/bloat/jar/bloat.jar";
			} else if (benName.equals("hsqldb")) {
				targetMain = "dacapo.hsqldb.Main";
				targetLoc = prefix + "benchmarks/hsqldb/classes";
				cp = "lib/rt.jar:" + prefix
						+ "shared/dacapo-2006-10-MR2/classes:" + prefix
						+ "benchmarks/hsqldb/jar/hsqldb.jar";
			} else if (benName.equals("xalan")) {
				targetLoc = prefix + "benchmarks/xalan/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/xalan/jar/xalan.jar:" + prefix
						+ "benchmarks/xalan/jar/serializer.jar";
			} else if (benName.equals("batik")) {
				targetLoc = prefix + "benchmarks/batik/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/batik/jar/batik-all.jar:"
						+ prefix + "benchmarks/batik/jar/crimson-1.1.3.jar:"
						+ prefix + "benchmarks/batik/jar/xml-apis-ext.jar";
			} else if (benName.equals("sunflow")) {
				targetLoc = prefix + "benchmarks/sunflow/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/sunflow/jar/sunflow-0.07.2.jar:"
						+ prefix + "benchmarks/sunflow/jar/janino-2.5.12.jar";
			} else if (benName.equals("eclipse")) {
				targetLoc = prefix + "benchmarks/eclipse/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/eclipse/jar/eclipse.jar";
			} else if (benName.equals("weka")) {
				targetLoc = prefix + "benchmarks/weka/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/weka/jar/weka.jar";
			} else if (benName.equals("findbugs")) {
				targetLoc = prefix + "benchmarks/findbugs/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/findbugs/jar/bcel-6.0-SNAPSHOT.jar:"
					+ prefix
					+ "benchmarks/findbugs/jar/asm-debug-all-5.0.2.jar:"
						+ prefix + "benchmarks/findbugs/jar/findbugs.jar";
		} else if (benName.equals("jmeter")) {
			targetLoc = prefix + "benchmarks/jmeter/classes";
			cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
					+ prefix + "benchmarks/jmeter/jar/ApacheJMeter.jar:"
					+ prefix + "benchmarks/jmeter/jar/ApacheJMeter_core.jar:"
					+ prefix + "benchmarks/jmeter/jar/xstream-1.4.8.jar:"
					+ prefix + "benchmarks/jmeter/jar/jorphan.jar:" + prefix
					+ "benchmarks/jmeter/jar/avalon-framework-4.1.4.jar:"
					+ prefix + "benchmarks/jmeter/jar/commons-logging-1.2.jar:"
					+ prefix + "benchmarks/jmeter/jar/commons-io-2.4.jar:"
					+ prefix + "benchmarks/jmeter/jar/logkit-2.0.jar:" + prefix
					+ "benchmarks/jmeter/jar/commons-collections-3.2.1.jar:"
					+ prefix + "benchmarks/jmeter/jar/commons-lang3-3.3.2.jar:"
					+ prefix
					+ "benchmarks/jmeter/jar/rsyntaxtextarea-2.5.6.jar:"
					+ prefix + "benchmarks/jmeter/jar/oro-2.0.8.jar:";
			// + prefix + "benchmarks/jmeter/jar/jcharts-0.7.5.jar";
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
					.add(new Transform("wjtp.scc", new ObserverHarness()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-allow-phantom-refs",
							"-soot-classpath", cp,
							"-main-class", targetMain,
							// "-no-bodies-for-excluded",
							"-p", "cg.spark", "enabled:true",
							"-p", "cg.spark", "simulate-natives:false",

					});

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}

	}
	
	@Override
	protected void internalTransform(String phaseName,
			Map<String, String> options) {
		CallGraph cicg = Scene.v().getCallGraph();
		SootMethod main = Scene.v().getMainMethod();
		QueryManager qm = new QueryManager(cicg, main);
		if(currType == RunType.CHA)
			qm = new QueryManager(SootUtils.getCha(), main);

		QueueReader<MethodOrMethodContext> queue = Scene.v()
				.getReachableMethods().listener();
		Set<String> list = new HashSet<String>(Arrays.asList(include));

		// get listeners from custom classes.
		for (SootClass sc : Scene.v().getClasses()) {
			if (sc.isJavaLibraryClass())
				continue;

			if (sc.getName().contains("Listener")) {
				list.add(sc.getName());
			}
		}
		Set<Edge> edges = new HashSet<Edge>();

		Set<SootMethod> srcs = new HashSet<SootMethod>();
		Set<SootMethod> tgts = new HashSet<SootMethod>();

		while (queue.hasNext()) {
			SootMethod meth = (SootMethod) queue.next();
			String ms = meth.getSignature();
			if (meth.isConstructor() || ms.contains("clinit"))
				continue;

			if (SootUtils.isObserver(ms)) {
				tgts.add(meth);
				for (Iterator<Edge> it = cicg.edgesInto(meth); it.hasNext();) {
					Edge e = it.next();
					SootMethod ca = (SootMethod) e.getSrc();
					SootMethod ce = (SootMethod) e.getTgt();
					if (ca.getDeclaringClass().equals(ce.getDeclaringClass()))
						continue;
					if (ca.getName().equals(ce.getName()))
						continue;
					srcs.add(ca);
					edges.add(e);
				}
			}

			SootClass clz = meth.getDeclaringClass();
			/* anonymous class dominates the listener. */
			if (clz.getName().contains("$")) {
				for (String su : list) {
					if (Scene.v().containsClass(su)) {
						Set<SootClass> subs = SootUtils.subTypesOf(Scene.v()
								.getSootClass(su));
						if (subs.contains(clz)) {
							tgts.add(meth);
							for (Iterator<Edge> it = cicg.edgesInto(meth); it
									.hasNext();) {
								Edge e = it.next();
								SootMethod ca = (SootMethod) e.getSrc();
								SootMethod ce = (SootMethod) e.getTgt();
								if (SootUtils.isTrivial(ca, ce))
									continue;

								srcs.add(ca);
								edges.add(e);
							}
							break;
						}
					}
				}
			}
		}

		Set<String> queries = new HashSet<String>();
		Set<String> pairs = new HashSet<String>();

		for (SootMethod src : srcs) {
			String srcClz = src.getDeclaringClass().getName();
			String srcStr = src.getSignature();
			for (SootMethod tgt : tgts) {
				String event = "";
				if (srcStr.contains("fireStateChanged")) {
					event = "java.awt.event.ChangeListener";
				} else if (srcStr.contains("fireActionEvent")) {
					event = "java.awt.event.ActionListener";
				} else if (srcStr.contains("fireActionPerformed")) {
					event = "java.awt.event.ActionListener";
				} else if (srcStr.contains("fireItemStateChanged")) {
					event = "java.awt.event.ItemListener";
				} else if (srcStr.contains("processWindowEvent")) {
					event = "java.awt.event.WindowListener";
				} else if (srcStr.contains("fireRemoveUpdate")
						|| srcStr.contains("fireInsertUpdate")) {
					event = "javax.swing.event.DocumentListener";
				} else if (srcStr.contains("fireAncestor")) {
					event = "javax.swing.event.AncestorListener";
				} else if (srcStr.contains("baseValueChanged")) {
					event = "org.apache.batik.dom.anim.AnimationTargetListener";
				} else if (srcStr.contains("fireEventListeners")) {
					event = "org.w3c.dom.events.EventListener";
				} else if (srcStr
						.contains("dispatchContentSelectionChangedEvent")) {
					event = "org.apache.batik.bridge.svg12.ContentSelectionChangedListener";
				} else if (srcStr.contains("firePropertiesChangedEvent")) {
					event = "org.apache.batik.css.engine.CSSEngineListener";
				} else if (srcClz.equals("javax.imageio.ImageWriter")) {
					event = "javax.imageio.event.IIOWriteProgressListener";
				} else if (srcClz.equals("javax.imageio.ImageReader")) {
					event = "javax.imageio.event.IIOReadProgressListener";
				}

				if (!event.equals("")) {
					SootClass clzz = Scene.v().getSootClass(event);
					if (SootUtils.subTypesOf(clzz).contains(
							tgt.getDeclaringClass())) {
						queries.add(srcStr + tgt.getSignature());
						StringBuffer sb = new StringBuffer("");
						sb.append(src.getName()).append("@")
								.append(src.getDeclaringClass().getName())
								.append(",").append(tgt.getName()).append("@")
								.append(tgt.getDeclaringClass().getName());
						pairs.add(sb.toString());
					}
				}
			}
		}
		
		System.out.println("isil:" + queries.size());
		for (Edge e : edges) {
			SootMethod src = (SootMethod) e.getSrc();
			SootMethod tgt = (SootMethod) e.getTgt();
			String query = src.getSignature() + tgt.getSignature();
			queries.add(query);
			
			StringBuffer sb = new StringBuffer("");
			sb.append(src.getName()).append("@")
					.append(src.getDeclaringClass().getName())
					.append(",").append(tgt.getName()).append("@")
					.append(tgt.getDeclaringClass().getName());
			pairs.add(sb.toString());
		}
		System.out.println("yu:" + queries.size());
		System.out.println("totals: " + pairs.size());
		
		if (compareKobj) {
			Set<String> kobj = SootUtils.getKobjResult(benName);
			kobj.retainAll(pairs);
			System.out.println("Valid query by 1obj: " + kobj.size());
		}

		int falseCi = 0;
		int falseExp = 0;
		int cnt = 0;
		long start = System.currentTimeMillis();
		long end = start + timeout; // 60 seconds * 1000 ms/sec

		for (String partial : queries) {
			String qq = main.getSignature() + ".*" + partial;
			String regx = qm.getValidExprBySig(qq);
			
			System.out.println("Query counter: " + cnt);
			if (System.currentTimeMillis() > end) {
				System.out.println("Timeout at query " + cnt);
				break;
			}
			
			if(maxQueries == cnt) {
				break;
			}
			
			if (currType == RunType.NORMAL) {
				long startNormal = System.nanoTime();
				boolean res = qm.queryRegx(regx);
				long endNormal = System.nanoTime();
				totalTimeNormal += (endNormal - startNormal);

				long startCipa = System.nanoTime();
				boolean res2 = qm.queryWithoutRefine(regx);
				long endCipa = System.nanoTime();
				totalTimeOnCipa += (endCipa - startCipa);
				if (!res) {
					falseExp++;
					System.out.println("refute: " + qq);
				} else {
					System.out.println("truth: " + qq);
				}
				if (!res2)
					falseCi++;
			} else if (currType == RunType.CHA) {
				long startCha = System.nanoTime();
				boolean res3 = qm.queryWithoutRefine(regx);
				long endCha = System.nanoTime();
				totalTimeOnCha += (endCha - startCha);
				if (!res3)
					falseCi++;
			} else if (currType == RunType.NOCUT) {

			} else if (currType == RunType.NOOPT) {
				long startNoOpt = System.nanoTime();
				boolean res3 = qm.queryRegxNoLookahead(regx);
				long endNoOpt = System.nanoTime();
				totalTimeOnNoOpt += (endNoOpt - startNoOpt);
				if (!res3)
					falseCi++;
			}
			cnt++;
		}
		// dump info.
		System.out
				.println("----------ObserverExp report-------------------------");
		System.out.println("Total queries: " + cnt);
		System.out.println("Total refutations(Explorer): " + falseExp);
		System.out.println("Total refutations(cipa): " + falseCi);
		System.out.println("Total time on Explorer: " + totalTimeNormal / 1e6);
		System.out.println("Total time on no Ci: " + totalTimeOnCipa / 1e6);
		System.out.println("Total time on no cut: " + totalNoCut / 1e6);
		System.out.println("Total time on CHA: " + (totalTimeOnCha / 1e6));
		System.out.println("Total time w/o look ahead: "
				+ (totalTimeOnNoOpt / 1e6));
	}
	
	String[] include = { 
			"java.util.EventListener",
			"javax.servlet.http.HttpSessionBindingListener",
			"javax.servlet.http.HttpSessionAttributeListener",
			"java.awt.image.ImageObserver",
			"javax.swing.AbstractAction", 
			"java.util.Observable",
			"java.util.Observer",
			"javax.faces.event.PhaseListener"
			};
	
	String[] queries = {
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<javax.swing.JViewport$1: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<javax.swing.ToolTipManager$insideTimerAction: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<org.apache.fop.render.awt.viewer.GoToPageDialog$2: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<javax.swing.ToolTipManager$stillInsideTimerAction: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<javax.swing.ToolTipManager$outsideTimerAction: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<org.apache.fop.render.awt.viewer.PreviewDialogAboutBox: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<javax.swing.Autoscroller: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<javax.swing.JComboBox: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<javax.swing.text.DefaultCaret$Handler: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<org.apache.fop.render.awt.viewer.GoToPageDialog$1: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<org.apache.fop.render.awt.viewer.PreviewDialog$9: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<javax.swing.AbstractButton$Handler: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.AbstractButton: void fireActionPerformed(java.awt.event.ActionEvent)>.*<org.apache.fop.render.awt.viewer.Command: void actionPerformed(java.awt.event.ActionEvent)>",
			"<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<javax.swing.JComboBox: void fireActionEvent()>.*<org.apache.fop.render.awt.viewer.PreviewDialog$9: void actionPerformed(java.awt.event.ActionEvent)>" };
	
}
