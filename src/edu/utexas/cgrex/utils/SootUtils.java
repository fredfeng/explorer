package edu.utexas.cgrex.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import soot.AnySubType;
import soot.ArrayType;
import soot.FastHierarchy;
import soot.G;
import soot.MethodOrMethodContext;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.util.queue.QueueReader;

/**
 * @author Saswat Anand
 **/
public class SootUtils {

	private static HashMap<SootClass, Set<SootClass>> classToSubtypes = new HashMap<SootClass, Set<SootClass>>();
	
	private static CallGraph cha;
	
	private static Set<SootMethod> chaReachableMethods = new HashSet<SootMethod>();

	public static void reportTime(String desc, Date start, Date end) {
		long time = end.getTime() - start.getTime();
		G.v().out
				.println("[CGregx] " + desc + " in " + time + " milliseconds.");
	}

	/*
	 * Given a sootClass and one of its method, return all sub-types that do not
	 * override this method
	 */
	public static List<Type> compatibleTypeList(SootClass cl, SootMethod meth) {
		HashSet<Type> subTypes = new HashSet<Type>();
		LinkedList<SootClass> worklist = new LinkedList<SootClass>();
		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();

		if (subTypes.add(cl.getType()))
			worklist.add(cl);
		while (!worklist.isEmpty()) {
			cl = worklist.removeFirst();
			if (cl.isInterface()) {
				for (Iterator<SootClass> cIt = fh
						.getAllImplementersOfInterface(cl).iterator(); cIt
						.hasNext();) {
					final SootClass c = cIt.next();
					if (subTypes.add(c.getType()))
						worklist.add(c);
				}
			} else {
				for (Iterator<SootClass> cIt = fh.getSubclassesOf(cl)
						.iterator(); cIt.hasNext();) {
					final SootClass c = cIt.next();
					if (!c.declaresMethod(meth.getName(),
							meth.getParameterTypes())
							&& subTypes.add(c.getType()))
						worklist.add(c);
				}
			}
		}

		List<Type> list = new ArrayList<Type>();
		list.addAll(subTypes);
		return list;
	}

	public static Set<SootClass> subTypesOf(SootClass cl) {
		Set<SootClass> subTypes = classToSubtypes.get(cl);
		if (subTypes != null)
			return subTypes;

		classToSubtypes.put(cl, subTypes = new HashSet<SootClass>());

		subTypes.add(cl);

		LinkedList<SootClass> worklist = new LinkedList<SootClass>();
		HashSet<SootClass> workset = new HashSet<SootClass>();
		FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();

		if (workset.add(cl))
			worklist.add(cl);
		while (!worklist.isEmpty()) {
			cl = worklist.removeFirst();
			if (cl.isInterface()) {
				for (Iterator<SootClass> cIt = fh
						.getAllImplementersOfInterface(cl).iterator(); cIt
						.hasNext();) {
					final SootClass c = cIt.next();
					if (workset.add(c))
						worklist.add(c);
				}
			} else {
				if (cl.isConcrete()) {
					subTypes.add(cl);
				}
				for (Iterator<SootClass> cIt = fh.getSubclassesOf(cl)
						.iterator(); cIt.hasNext();) {
					final SootClass c = cIt.next();
					if (workset.add(c))
						worklist.add(c);
				}
			}
		}
		return subTypes;
	}


	public static String getSootSubsigFor(String chordSubsig) {
		String name = chordSubsig.substring(0, chordSubsig.indexOf(':'));
		String retType = chordSubsig.substring(chordSubsig.indexOf(')') + 1);
		String paramTypes = chordSubsig.substring(chordSubsig.indexOf('(') + 1,
				chordSubsig.indexOf(')'));
		return parseDesc(retType) + " " + name + "(" + parseDesc(paramTypes)
				+ ")";
	}

	static String parseDesc(String desc) {
		StringBuilder params = new StringBuilder();
		String param = null;
		char c;
		int arraylevel = 0;
		boolean didone = false;

		int len = desc.length();
		for (int i = 0; i < len; i++) {
			c = desc.charAt(i);
			if (c == 'B') {
				param = "byte";
			} else if (c == 'C') {
				param = "char";
			} else if (c == 'D') {
				param = "double";
			} else if (c == 'F') {
				param = "float";
			} else if (c == 'I') {
				param = "int";
			} else if (c == 'J') {
				param = "long";
			} else if (c == 'S') {
				param = "short";
			} else if (c == 'Z') {
				param = "boolean";
			} else if (c == 'V') {
				param = "void";
			} else if (c == '[') {
				arraylevel++;
				continue;
			} else if (c == 'L') {
				int j;
				j = desc.indexOf(';', i + 1);
				param = desc.substring(i + 1, j);
				// replace '/'s with '.'s
				param = param.replace('/', '.');
				i = j;
			} else
				assert false;

			if (didone)
				params.append(',');
			params.append(param);
			while (arraylevel > 0) {
				params.append("[]");
				arraylevel--;
			}
			didone = true;
		}
		return params.toString();
	}

	// check whether src can reach tgt.
	public static boolean checkReachable(CallGraph cg, SootMethod src,
			SootMethod tgt) {
		Stack<SootMethod> queue = new Stack<SootMethod>();
		queue.push(src);
		LinkedList<SootMethod> visited = new LinkedList<SootMethod>();

		while (!queue.empty()) {
			SootMethod cur = queue.pop();

			if (!visited.contains(cur)) {
				visited.add(cur);

				for (Iterator<Edge> cIt = cg.edgesOutOf(cur); cIt.hasNext();) {
					Edge tgtEdge = cIt.next();
					SootMethod tgtMeth = (SootMethod) tgtEdge.getTgt();
					queue.push(tgtMeth);
				}
			}
		}
		return visited.contains(tgt);
	}
	
	public static CallGraph getCha() {
		if (cha == null) {
			CallGraphBuilder cg = new CallGraphBuilder(DumbPointerAnalysis.v());
			cg.build();
			System.out.println("CHA Number**** of reachable methods: "
					+ Scene.v().getReachableMethods().size());
			cha = cg.getCallGraph();
		}
		return cha;
	}
	
	public static boolean castSafe(Type castType, Set<Type> types) {
		if (castType instanceof ArrayType) {
			for (Type tgt : types) {
				if (!(tgt instanceof ArrayType)) {
					return false;
				}
			}
		} else if (castType instanceof RefType) {
			SootClass castClass = ((RefType) castType).getSootClass();
			Set<SootClass> subClasses = subTypesOf(castClass);
			for (Type tgt : types) {
				if (tgt instanceof AnySubType) {
					AnySubType any = (AnySubType) tgt;
					Type anyType = any.getBase();
					assert anyType instanceof RefType;
					SootClass anyBaseClz = ((RefType) anyType).getSootClass();
					Set<SootClass> anySubClasses = subTypesOf(anyBaseClz);
					for (SootClass sub : anySubClasses) {
						if (!subClasses.contains(sub))
							return false;
					}
				}
				if(tgt instanceof ArrayType)
					return false;
				assert tgt instanceof RefType : tgt;
				SootClass tgtClz = ((RefType) tgt).getSootClass();
				
				if (!subClasses.contains(tgtClz))
					return false;

			}
		} else {
			assert false : castType;
		}

		return true;
	}
	
	// generate the CHA-based call graph
	public static Set<SootMethod> getChaReachableMethods() {
		if (cha == null) {
			long start = System.nanoTime();
			CallGraphBuilder cg = new CallGraphBuilder(DumbPointerAnalysis.v());
			cg.build();
			System.out.println("CHA Number**** of reachable methods: "
					+ Scene.v().getReachableMethods().size());
			cha = cg.getCallGraph();

			// collect reachable methods based on CHA.
			QueueReader<MethodOrMethodContext> qr = Scene.v()
					.getReachableMethods().listener();
			while (qr.hasNext()) {
				chaReachableMethods.add(qr.next().method());
			}
			long end = System.nanoTime();
			StringUtil.reportSec("CHA Construction Time: ", start, end);
		}
		assert !chaReachableMethods.isEmpty();
		return chaReachableMethods;
	}
	
	public static boolean asyncClass(SootMethod method) {
		SootClass clz = method.getDeclaringClass();
		String name = method.getName();
		Set<SootClass> subclz = new HashSet<SootClass>();
		if (Scene.v().containsClass("android.os.AsyncTask")) {
			SootClass async = Scene.v().getSootClass("android.os.AsyncTask");
			subclz.addAll(subTypesOf(async));
		}
		if (Scene.v().containsClass("java.lang.Runnable")) {
			SootClass async = Scene.v().getSootClass("java.lang.Runnable");
			subclz.addAll(subTypesOf(async));
			if (clz.implementsInterface("java.lang.Runnable"))
				subclz.add(clz);
		}
		if (Scene.v().containsClass("java.lang.Thread")) {
			SootClass async = Scene.v().getSootClass("java.lang.Thread");
			subclz.addAll(subTypesOf(async));
			if (clz.hasSuperclass() && clz.getSuperclass().equals(async))
				subclz.add(clz);
		}

		if (name.equals("run") || name.startsWith("doInBackground")
				|| name.equals("subscribe") || name.equals("<init>"))
			return subclz.contains(clz);
		else
			return false;
	}
	
}
