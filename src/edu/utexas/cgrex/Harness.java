package edu.utexas.cgrex;

import java.util.Scanner;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Transform;
import edu.utexas.RegularPT.RegularPTTransformer;

public class Harness {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String targetLoc = "benchmarks/CFLexamples/bin/";

		try {

			StringBuilder options = new StringBuilder();
			PackManager
					.v()
					.getPack("wjtp")
					.add(new Transform("wjtp.regularPT",
							new RegularPTTransformer()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-src-prec", "java", "-allow-phantom-refs",
							"-no-bodies-for-excluded", "-exclude", "java",
							"-exclude", "javax", "-output-format", "none",
							"-p", "jb", "use-original-names:true",
							// "-p", "cg.cha", "on",
							"-p", "cg.spark", "on", "-debug" });
			
			String regx = "";
			//test case: (\u6162|\u6155).*\u0097
			QueryManager qm = new QueryManager();
			while (true) {
				Scanner in = new Scanner(System.in);
				System.out.println("Please Enter a string:");
				regx = in.nextLine();
				//press "q" to exit the program
				if(regx.equals("q"))  				
					System.exit(0);
				else {
					System.out.println("You entered string " + regx);
					qm.doQuery(regx);
				}
			}

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}

	}

	public void run(String s) {

	}

}
