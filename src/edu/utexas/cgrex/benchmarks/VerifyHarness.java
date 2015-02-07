package edu.utexas.cgrex.benchmarks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import chord.util.tuple.object.Pair;

/**
 * Using Chord to verify soundness.
 * @author yufeng
 *
 */
public class VerifyHarness {

	public static void main(String[] args) throws IOException {
		String cg = "/home/yufeng/research/CallsiteResolver/unreach_cg.txt";
		String chord = "/home/yufeng/research/CallsiteResolver/chord.txt";
		Set<Pair<String, String>> cgSet = new HashSet<Pair<String, String>>();
		Set<Pair<String, String>> chordSet = new HashSet<Pair<String, String>>();

		BufferedReader br = new BufferedReader(new FileReader(new File(cg)));
		String line;
		while ((line = br.readLine()) != null) {
			String[] list = line.split(" ");
			assert list.length == 3;
			String clzName = list[0];
			clzName = clzName.substring(1, clzName.length() - 1);
			String methName = list[2];
			methName = methName.substring(0, methName.indexOf("("));
			Pair<String, String> p = new Pair<String, String>(clzName, methName);
		}
		br.close();
		
		br = new BufferedReader(new FileReader(new File(chord)));
		while ((line = br.readLine()) != null) {
		   // process the line.
			assert false : line;
		}
		br.close();
	}

}
