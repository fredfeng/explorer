package edu.utexas.cgrex.utils;

import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;

/**
 * Calculate lines of jimple in current benchmark.
 *
 */
public class LineCounter extends SceneTransformer {

	@Override
	protected void internalTransform(String phaseName,
			Map<String, String> options) {
		Iterator<SootMethod> it = Scene.v().getMethodNumberer().iterator();
		int lines = 0;
		int methods = 0;
		while (it.hasNext()) {
			SootMethod meth = it.next();
			if (meth.hasActiveBody()) {
				Body b = meth.retrieveActiveBody();
				lines += b.getUnits().size();
				methods++;
			}
		}

		System.out.println("total lines: " + lines);
		System.out.println("total methods:" + methods);
	}

}
