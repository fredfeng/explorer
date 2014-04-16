package edu.utexas.cgrex.test;

import java.util.Iterator;
import java.util.Map;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootField;
import soot.Transform;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.VarNode;


public class TestAutoPAG extends SceneTransformer{
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
        System.out.println("********************TestAutoPAG*************");
        //read default pag from soot.
		PAG pag = (PAG) Scene.v().getPointsToAnalysis();

		//FIXME:don't support match by default.
        System.out.println("-------match-------");
//        Iterator<Object> it = me.matchSourcesIterator();
//        while (it.hasNext()) {
//        	VarNode from = (VarNode)it.next();
//        	Node[] tos = me.matchLookup(from);
//        	for (int i = 0; i < tos.length; i ++) {
//        		VarNode to = (VarNode)tos[i];
//        		System.out.println(from.getNumber() + " " + to.getNumber());
//        	}
//        }
        
        System.out.println("--------info-------");  
        System.out.println("--------store info---------");
        Iterator<Object> it = pag.storeSourcesIterator();
        while (it.hasNext()) {
        	VarNode from = (VarNode)it.next();
        	Node[] tos = pag.storeLookup(from);
        	for (int i = 0; i < tos.length; i ++) {
        		FieldRefNode to = (FieldRefNode)tos[i];
        		if ( !(to.getField() instanceof SootField)) continue;
        		System.out.println(from.getNumber() + " is stored to " 
        				+ to.getBase().getNumber() + " " 
        				+ to.getNumber() + " "
        				+ ((SootField)to.getField()).getName());
        	}
        }
        System.out.println("---------load info-----------");
        it = pag.loadSourcesIterator(); 
        while (it.hasNext()) {
        	FieldRefNode from = (FieldRefNode)it.next();
    		if ( !(from.getField() instanceof SootField)) continue;

        	Node[] tos = pag.loadLookup(from);
        	for (int i = 0; i < tos.length; i ++) {
        		VarNode to = (VarNode)tos[i];
        		System.out.println(from.getBase().getNumber() + " "
        				+ from.getNumber() + " " 
        				+ ((SootField)from.getField()).getName() + " is loaded to "
        				+ to.getNumber());
        	}
        }
        
        // dump the DAG into output_dir
        //PAGDumper dumper = new PAGDumper( mypag, output_dir );
        //dumper.dump();
        
        
        System.out.println("this is the test transformer function.");
	}
	
	public static void main(String[] args) {
		String targetLoc = 
				//"/Users/xwang/oopsla/CallsiteResolver/benchmarks/CFLexamples/bin/";
				"benchmarks/CFLexamples/bin";
		try {
			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.test", new TestAutoPAG()));

			soot.Main.v().run(new String[] {
					"-W",
					"-process-dir", targetLoc,
					"-src-prec", "java",
					"-allow-phantom-refs",
					"-no-bodies-for-excluded",
					"-exclude", "java",
					"-exclude", "javax",
					"-output-format", "jimple",
					"-p", "jb", "use-original-names:true",
					//"-p", "cg.cha", "on",
					"-p", "cg.spark", "on",
					"-debug"} );

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}
		
		System.out.println("This is in the test main function.");
	}
}
