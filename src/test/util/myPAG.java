package test.util;

import java.util.HashMap;
import java.util.Map;

import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.options.SparkOptions;

public class myPAG extends PAG {
	// we reuse simple(assign), alloc maps
	// and add another match map
	protected Map<Object, Object> match = new HashMap<Object, Object>();
	public myPAG(final SparkOptions opts) {
		super(opts);
	}
	public final boolean addMatchEdge(Node from, Node to) {
		from = from.getReplacement();
		to = to.getReplacement();
		
		
		
		
		return false;
	}
}