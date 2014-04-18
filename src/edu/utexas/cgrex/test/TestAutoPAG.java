package edu.utexas.cgrex.test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.CompilationDeathException;
import soot.Local;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.ArrayElement;
import soot.jimple.spark.pag.ClassConstantNode;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.GlobalVarNode;
import soot.jimple.spark.pag.LocalVarNode;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.PAGDumper;
import soot.jimple.spark.pag.StringConstantNode;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.utils.SootUtils;

public class TestAutoPAG extends SceneTransformer {
	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		System.out.println("********************TestAutoPAG*************");
		// read default pag from soot.
		PAG pag = (PAG) Scene.v().getPointsToAnalysis();

		AutoPAG me = new AutoPAG(pag);
		me.build();
		me.dump();
		me.dumpFlow();

		// test query method
		// StringBuilder b = new StringBuilder("");
		// for (Object obj : me.flowInvSources()) {
		// VarNode v = (VarNode) obj;
		// b.append("VarNode: " + v.getNumber() + " points to: ");
		// // Date start = new Date();
		// Set<AllocNode> s = me.queryTest(v);
		// // Date end = new Date();
		// // SootUtils.reportTime("TEST Query", start, end);
		// for (AllocNode node : s) {
		// b.append(node.getNumber() + " ");
		// }
		// b.append("\n");
		// }
		//
		// try {
		// BufferedWriter bufw = new BufferedWriter(new FileWriter(
		// "sootOutput/ptAnalysis"));
		// bufw.write(b.toString());
		// bufw.close();
		// } catch (Exception e) {
		// e.printStackTrace();
		// System.exit(0);
		// }

		for (Iterator cIt = Scene.v().getClasses().iterator(); cIt.hasNext();) {
			final SootClass c = (SootClass) cIt.next();

			Type p = null;
			List<Value> toQuery = new ArrayList<Value>();
			for (Iterator mIt = c.methodIterator(); mIt.hasNext();) {
				SootMethod m = (SootMethod) mIt.next();
				if (!m.isConcrete())
					continue;
				if (!m.hasActiveBody())
					continue;
				Body body = m.getActiveBody();

				for (Local l : body.getLocals()) {
					if (l.getType() instanceof RefType
							|| l.getType() instanceof FieldRef)
						toQuery.add(l);
					p = l.getType();
				}
			}
			Date start = new Date();
			// System.out.println(me.insensitiveQuery(toQuery, p));
			System.out.println(me.sensitiveQuery(toQuery, p));
			Date end = new Date();
			SootUtils.reportTime("Query Time:", start, end);
		}

		String output_dir = "sootOutput";
		PAGDumper dumper = new PAGDumper(pag, output_dir);
		dumper.dump();

		// me.printTypeInfo();

		// print map info
		// printMap(pag);
		// printVarNodes(pag);
		// printFields(pag);
		// printAllocNodes(pag);

		// count number of methods
		// int methods = 0;
		// for (Iterator cIt = Scene.v().getClasses().iterator();
		// cIt.hasNext();) {
		// final SootClass c = (SootClass) cIt.next();
		// for (Iterator mIt = c.methodIterator(); mIt.hasNext();) {
		// SootMethod m = (SootMethod) mIt.next();
		// if (!m.isConcrete())
		// continue;
		// if (!m.hasActiveBody())
		// continue;
		// methods++;
		// }
		// }
		// System.out.println("this benchmark has " + methods + " methods.");

	}

	public static void main(String[] args) {
		String targetLoc = // "benchmarks/CFLexamples/bin";
		"benchmarks/sablecc-3.7/classes";
		// "benchmarks/test/bin";
		try {
			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.test", new TestAutoPAG()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
							"-no-bodies-for-excluded", "-exclude", "java",
							"-exclude", "javax", "-output-format", "jimple",
							"-p", "jb", "use-original-names:true",
							// "-p", "cg.cha", "on",
							"-p", "cg.spark", "on", "-debug" });

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}
	}

	/** protected methods */
	// just used to see what kinds of subclasses for AllocNode, FieldRefNode,
	// and VarNode are existing in the pag
	// testing results for benchmark sablecc-3.7
	// VarNode: GlobalVarNode, LocalVarNode
	// FieldRefNode: FieldRefNode
	// AllocNode: AllocNode, StringConstantNode, ClassConstantNode
	protected void printMap(PAG pag) {
		Set<String> x = new HashSet<String>();
		Iterator<Object> it = pag.loadSourcesIterator();
		System.out.println("dddd" + pag.loadSources().size());
		System.out.println("dddd" + pag.storeSources().size());
		System.out.println("dddd" + pag.simpleSources().size());
		System.out.println("dddd" + pag.allocSources().size());
		while (it.hasNext())
			x.add(it.next().toString() + "\n");
		it = pag.storeSourcesIterator();
		while (it.hasNext())
			x.add(it.next().toString() + "\n");
		it = pag.allocSourcesIterator();
		while (it.hasNext())
			x.add(it.next().toString() + "\n");
		it = pag.simpleSourcesIterator();
		while (it.hasNext())
			x.add(it.next().toString() + "\n");
		it = pag.loadInvSourcesIterator();
		while (it.hasNext())
			x.add(it.next().toString() + "\n");
		it = pag.storeInvSourcesIterator();
		while (it.hasNext())
			x.add(it.next().toString() + "\n");
		it = pag.allocInvSourcesIterator();
		while (it.hasNext())
			x.add(it.next().toString() + "\n");
		it = pag.simpleInvSourcesIterator();
		while (it.hasNext())
			x.add(it.next().toString() + "\n");

		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/PAGmapInfo"));
			for (String s : x)
				bufw.write(s + "\n");
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	protected void printFields(PAG pag) {
		Set<String> x = new HashSet<String>();
		int countArrayElement = 0, countSootField = 0;
		int countOthers = 0;
		Iterator<Object> it = pag.loadSourcesIterator();
		while (it.hasNext()) {
			FieldRefNode obj = (FieldRefNode) it.next();
			x.add(obj.getField().toString());
			if (obj.getField() instanceof ArrayElement)
				countArrayElement++;
			else if (obj.getField() instanceof SootField)
				countSootField++;
			else
				countOthers++;
		}
		it = pag.storeInvSourcesIterator();
		while (it.hasNext()) {
			FieldRefNode obj = (FieldRefNode) it.next();
			x.add(obj.getField().toString());
			if (obj.getField() instanceof ArrayElement)
				countArrayElement++;
			else if (obj.getField() instanceof SootField)
				countSootField++;
			else
				countOthers++;
		}
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/PAGmapFieldsInfo"));
			for (String s : x)
				bufw.write(s + "\n");
			bufw.write("Number of ArrayElement: " + countArrayElement + "\n");
			bufw.write("Number of SootField: " + countSootField + "\n");
			bufw.write("Number of Others: " + countOthers + "\n");
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	protected void printVarNodes(PAG pag) {
		Set<String> x = new HashSet<String>();
		int countLocalVarNode = 0, countGlobalVarNode = 0, countOthers = 0;
		Iterator<Object> it = pag.simpleSourcesIterator();
		while (it.hasNext()) {
			Object obj = it.next();
			x.add(obj.toString());
			if (obj instanceof LocalVarNode)
				countLocalVarNode++;
			else
				countGlobalVarNode++;
		}
		it = pag.simpleInvSourcesIterator();
		while (it.hasNext()) {
			Object obj = it.next();
			x.add(obj.toString());
			if (obj instanceof LocalVarNode)
				countLocalVarNode++;
			else
				countGlobalVarNode++;
		}
		it = pag.loadInvSourcesIterator();
		while (it.hasNext()) {
			Object obj = it.next();
			x.add(obj.toString());
			if (obj instanceof LocalVarNode)
				countLocalVarNode++;
			else
				countGlobalVarNode++;
		}
		it = pag.storeSourcesIterator();
		while (it.hasNext()) {
			Object obj = it.next();
			x.add(obj.toString());
			if (obj instanceof LocalVarNode)
				countLocalVarNode++;
			else
				countGlobalVarNode++;
		}
		it = pag.allocInvSourcesIterator();
		while (it.hasNext()) {
			Object obj = it.next();
			x.add(obj.toString());
			if (obj instanceof LocalVarNode)
				countLocalVarNode++;
			else if (obj instanceof GlobalVarNode)
				countGlobalVarNode++;
			else
				countOthers++;
		}

		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/PAGmapVarNodeInfo"));
			for (String s : x)
				bufw.write(s + "\n");
			bufw.write("Number of LocalVarNode: " + countLocalVarNode + "\n");
			bufw.write("Number of GlobalVarNode: " + countGlobalVarNode + "\n");
			bufw.write("Number of otherNodes: " + countOthers + "\n");
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	protected void printAllocNodes(PAG pag) {
		Set<String> x = new HashSet<String>();
		int countAllocNode = 0, countStringConstNode = 0;
		int countClassConstNode = 0, countOthers = 0;
		Iterator<Object> it = pag.allocSourcesIterator();
		while (it.hasNext()) {
			Object obj = it.next();
			x.add(obj.toString());
			if (obj instanceof AllocNode)
				countAllocNode++;
			else if (obj instanceof StringConstantNode)
				countStringConstNode++;
			else if (obj instanceof ClassConstantNode)
				countClassConstNode++;
			else
				countOthers++;
		}
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/PAGmapAllocNodeInfo"));
			for (String s : x)
				bufw.write(s + "\n");
			bufw.write("Number of AllocNode: " + countAllocNode + "\n");
			bufw.write("Number of StringConstantNode: " + countStringConstNode
					+ "\n");
			bufw.write("Number of ClassConstantNode: " + countClassConstNode
					+ "\n");
			bufw.write("Number of otherNodes: " + countOthers + "\n");
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

	}
}
