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

import soot.FastHierarchy;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

/**
 * @author Saswat Anand
 **/
public class SootUtils {

	private static HashMap<SootClass, Set<SootClass>> classToSubtypes = new HashMap();

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
				// if (cl.isConcrete()) {
				// subTypes.add(cl);
				// }
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

		List<Type> list = new ArrayList();
		list.addAll(subTypes);
		return list;
	}

	public static Set<SootClass> subTypesOf(SootClass cl) {
		Set<SootClass> subTypes = classToSubtypes.get(cl);
		if (subTypes != null)
			return subTypes;

		classToSubtypes.put(cl, subTypes = new HashSet());

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

	// src equals tgt, or, tgt is the superclass of src.
	public static boolean compatibleWith(SootMethod src, SootMethod tgt) {
		boolean b = false;
		if (src.equals(tgt))
			b = true;
        return b;
		/*SootClass srcClazz = src.getDeclaringClass();
		while (srcClazz.hasSuperclass()) {
			SootClass superClazz = srcClazz.getSuperclass();
			if (superClazz.declaresMethod(tgt.getName(),
					tgt.getParameterTypes()))
				return true;

			srcClazz = superClazz;
		}

		return b;*/
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
}
