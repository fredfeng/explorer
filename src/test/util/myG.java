package test.util;

import soot.G;

public class myG extends G {
	private static myG instance = new myG();
	
	public static myG v() { return instance; }
}
