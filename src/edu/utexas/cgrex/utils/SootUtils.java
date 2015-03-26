package edu.utexas.cgrex.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
			// mark all methods in async.
			if (subclz.contains(clz))
				return true;
		}
		if (clz.getName().startsWith("de.schildbach.wallet")
				&& name.equals("<clinit>")) {
			return true;
		}
		
		//normal log for printing exception is fine.
		if (clz.getName().startsWith("de.schildbach.wallet.ui.InputParser")
				|| clz.getName().startsWith("ch.qos.logback"))
			return true;
		
		if (Scene.v().containsClass("java.lang.Runnable")) {
			SootClass async = Scene.v().getSootClass("java.lang.Runnable");
			subclz.addAll(subTypesOf(async));
			if (clz.implementsInterface("java.lang.Runnable"))
				subclz.add(clz);
		}
		if (Scene.v().containsClass("android.support.v4.app.DialogFragment")) {
			SootClass async = Scene.v().getSootClass(
					"android.support.v4.app.DialogFragment");
			subclz.addAll(subTypesOf(async));
			if (subclz.contains(clz))
				return true;
		}
		if (Scene.v().containsClass("android.app.AlertDialog$Builder")) {
			SootClass async = Scene.v().getSootClass(
					"android.app.AlertDialog$Builder");
			subclz.addAll(subTypesOf(async));
			if (subclz.contains(clz))
				return true;
		}

		if (Scene.v().containsClass("java.lang.Thread")) {
			SootClass async = Scene.v().getSootClass("java.lang.Thread");
			subclz.addAll(subTypesOf(async));
			if (clz.hasSuperclass() && clz.getSuperclass().equals(async))
				subclz.add(clz);
		}

		if (Scene.v().containsClass("android.os.Handler")) {
			SootClass async = Scene.v().getSootClass("android.os.Handler");
			subclz.addAll(subTypesOf(async));
			if (clz.hasSuperclass() && clz.getSuperclass().equals(async))
				subclz.add(clz);
		}

		if (name.equals("run") || name.startsWith("doInBackground")
				|| name.equals("handleMessage") || name.equals("subscribe")
				|| name.equals("<init>") || name.equals("<clinit>"))
			return subclz.contains(clz);
		else
			return false;
	}
	
	/* checking whether a method is observer.*/
	public static boolean isObserver(String ms) {
		if (ms.contains("actionPerformed")
				|| ms.contains("animatedAttributeChanged")
				|| ms.contains("itemStateChanged")
				|| ms.contains("propertiesChanged")
				|| ms.contains("otherAnimationChanged")
				|| ms.contains("baseValueChanged")
				|| ms.contains("handleEvent")
				|| ms.contains("contentSelectionChanged")
				|| ms.matches(".*void update.*Observable.*")
				|| ms.contains("stateChanged")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean ignoreTopNodes(RefType ref) {
		if (ref.getClassName().equals("java.lang.String")
				|| ref.getClassName().equals("java.lang.Object")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isTrivial(SootMethod ca, SootMethod ce) {
		String caName = ca.getDeclaringClass().getName();
		if (caName.equals("java.awt.Window")
				&& !ca.getName().contains("processWindowEvent")) {
			return true;
		}
		if (ca.isConstructor()) {
			return true;
		}
		if (ca.getName().matches("get.*Size")
				|| ce.getName().matches("get.*Size")) {
			return true;
		}
		if (ca.getName().contains("toString")
				|| ce.getName().contains("toString")) {
			return true;
		}
		Set<String> exlist = new HashSet<String>(Arrays.asList(exclude));
		if (exlist.contains(caName))
			return true;
		
		if (ca.getDeclaringClass().equals(ce.getDeclaringClass())) {
			return true;
		}
		return false;
	}
	
	public static Set<String> getKobjResult(String proj) {
		Set<String> kobj = new HashSet<String>();
		String base = "/home/yufeng/research/CallsiteResolver-observe/kobj/";
		File file = new File(base + proj + "kobj/CICM.txt");
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				// process the line.
				String[] tuple = line.split(",");
				assert tuple.length == 4;
				String caller = tuple[1];
				String caller1 = caller.substring(caller.indexOf("!") + 1,
						caller.indexOf(":"));
				String caller2 = caller.split("@")[1];

				String callee = tuple[3];
				String part1 = callee.split(":")[0];
				String part2 = removeLastChar(callee.split("@")[1]);

				StringBuffer sb = new StringBuffer("");
				sb.append(caller1).append("@").append(caller2).append(",")
						.append(part1).append("@").append(part2);
				kobj.add(sb.toString());
			}
			return kobj;
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
	}
	
	private static String removeLastChar(String str) {
        return str.substring(0,str.length()-1);
    }
	
	public static Set<String> genObsQueries(String benName, boolean compareKobj) {
		CallGraph cicg = Scene.v().getCallGraph();
		SootMethod main = Scene.v().getMainMethod();
		QueueReader<MethodOrMethodContext> queue = Scene.v()
				.getReachableMethods().listener();
		Set<String> list = new HashSet<String>(Arrays.asList(include));
		Set<Edge> edges = new HashSet<Edge>();
		Set<SootMethod> srcs = new HashSet<SootMethod>();
		Set<SootMethod> tgts = new HashSet<SootMethod>();
		Set<String> pairs = new HashSet<String>();
		Set<String> queries = new HashSet<String>();
		// get listeners from custom classes.
		for (SootClass sc : Scene.v().getClasses()) {
			if (sc.isJavaLibraryClass())
				continue;

			if (sc.getName().contains("Listener")) {
				list.add(sc.getName());
			}
		}

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

		for (Edge e : edges) {
			SootMethod src = (SootMethod) e.getSrc();
			SootMethod tgt = (SootMethod) e.getTgt();
			String query = src.getSignature() + tgt.getSignature();
			queries.add(query);

			StringBuffer sb = new StringBuffer("");
			sb.append(src.getName()).append("@")
					.append(src.getDeclaringClass().getName()).append(",")
					.append(tgt.getName()).append("@")
					.append(tgt.getDeclaringClass().getName());
			pairs.add(sb.toString());
		}

		if (compareKobj) {
			Set<String> kobj = SootUtils.getKobjResult(benName);
			kobj.retainAll(pairs);
			System.out.println("Valid query by 1obj: " + kobj.size());
		}
		return queries;
	}
	
	static String[] include = { "java.util.EventListener",
			"javax.servlet.http.HttpSessionBindingListener",
			"javax.servlet.http.HttpSessionAttributeListener",
			"java.awt.image.ImageObserver", "javax.swing.AbstractAction",
			"java.util.Observable", "java.util.Observer",
			"javax.faces.event.PhaseListener" };

	static String[] exclude = { "java.awt.Container", "java.awt.Component",
			"java.awt.AWTEventMulticaster", };
}
