package edu.utexas.cgrex.analyses;

import java.util.Map;
import java.util.Scanner;

import soot.Scene;
import soot.SceneTransformer;
import soot.jimple.spark.pag.PAG;
import edu.utexas.cgrex.QueryManager;

/**
 * Entry point of the whole analysis.
 * @author yufeng
 *
 */
public class CGregxTransformer extends SceneTransformer {

    protected void internalTransform(String phaseName,
    @SuppressWarnings("rawtypes") Map options) {  
		// TODO Auto-generated method stub
		// read default pag from soot.
		PAG pag = (PAG) Scene.v().getPointsToAnalysis();
		// build xinyu's AutoPAG
		AutoPAG me = new AutoPAG(pag);
		me.build();
		
		//test case: (\u6162|\u6155).*\u0097
		//test case: (\u6162|\u6155).*\u6102

		QueryManager qm = new QueryManager(me);
		String regx = "";
		
		qm.doQuery(regx);

//		while (true) {
//			Scanner in = new Scanner(System.in);
//			System.out.println("Please Enter a string:");
//			regx = in.nextLine();
//			//press "q" to exit the program
//			if(regx.equals("q"))  				
//				System.exit(0);
//			else {
//				System.out.println("You entered string " + regx);
//				qm.doQuery(regx);
//			}
//		}

    }
}
