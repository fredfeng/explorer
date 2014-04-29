package edu.utexas.cgrex.utils;

import java.util.Date;

import soot.G;

public class StringUtil {
	public static String appendChar(char c, StringBuilder b) {
		if (c >= 0x21 && c <= 0x7e && c != '\\' && c != '"')
			b.append(c);
		else {
			b.append("\\u");
			String s = Integer.toHexString(c);
			if (c < 0x10)
				b.append("000").append(s);
			else if (c < 0x100)
				b.append("00").append(s);
			else if (c < 0x1000)
				b.append("0").append(s);
			else
				b.append(s);
		}
		return b.toString();
	}
	
	public static void reportSec(String desc, long start, long end) {
		double difference = (end - start)/1e6;
		G.v().out.println("[CGrex] " + desc + " in " + difference + " ms.");
	}
	
	public static void reportTime(String desc, Date start, Date end) {
		long time = end.getTime() - start.getTime();
		G.v().out.println("[CGrex] " + desc + " in " + time / 1000 + "."
				+ (time / 100) % 10 + " seconds.");
	}
	
	public static void reportRefineFail(String desc) {
		G.v().out.println("[CGrex] "
				+ "Regular expression causes Refinement failure:" + desc);
	}
	
	public static void reportInfo(String desc) {
		G.v().out.println("[CGrex] " + desc);
	}

}
