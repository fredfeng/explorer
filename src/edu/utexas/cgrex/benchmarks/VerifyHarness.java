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
			cgSet.add(p);
		}
		br.close();
		
		br = new BufferedReader(new FileReader(new File(chord)));
		while ((line = br.readLine()) != null) {
		   // process the line.
			String[] list = line.split(":.*@");
			assert list.length == 2;
			String clzName = list[1];
			String methName = list[0];
			clzName = clzName.substring(0, clzName.length()-1);
			Pair<String, String> p = new Pair<String, String>(clzName, methName);
			chordSet.add(p);
		}
		br.close();
		
		//now begin checking.
		// 1. soundness: any method that is refuted by cg should not exist in
		// chord
		int cnt = 0; 
		for (Pair<String, String> p1 : cgSet) {
			String clzName1 = p1.val0;
			String methName1 = p1.val1;
			for (Pair<String, String> p2 : chordSet) {
				String clzName2 = p2.val0;
				String methName2 = p2.val1;
				if (clzName2.equals(clzName1))
					// assert !methName1.contains(methName2) : clzName1 + ":"
					// + methName1 + "->" + methName2;
					if (methName1.contains(methName2)) {
						cnt++;
						System.out.println(clzName1 + ":" + methName1 + "->"
								+ methName2);
						break;
					}

			}
		}
		
		//TODO: 2. precision.
		System.out.println("PASS verification!" + cgSet.size() + " " + cnt);
	}

}
