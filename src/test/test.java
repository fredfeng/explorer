package test;

import java.util.*;

import edu.utexas.RegularPT.RegularPTTransformer;
import soot.CompilationDeathException;
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.geom.geomPA.GeomPointsTo;
import soot.jimple.spark.internal.SparkNativeHelper;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.FieldRefNode;
import soot.jimple.spark.pag.MethodPAG;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.PAGDumper;
import soot.jimple.spark.pag.VarNode;
import soot.options.SparkOptions;
import soot.util.queue.QueueReader;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.solver.OnFlyCallGraph;
import soot.jimple.spark.solver.SCCCollapser;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.jimple.toolkits.pointer.util.NativeMethodDriver;
import test.util.*;

public class test extends SceneTransformer{
	protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
		Map myoptions = new HashMap(options);
        myoptions.put("set-impl", "double");
        myoptions.put("double-set-old", "hybrid");
        myoptions.put("double-set-new", "hybrid");
        myoptions.put("verbose", "true");
        System.out.println("********************RegularPT*************");

        SparkOptions opts = new SparkOptions( myoptions );
        final String output_dir = SourceLocator.v().getOutputDir();

        // Build pointer assignment graph
        ContextInsensitiveBuilder b = new ContextInsensitiveBuilder();
        // prJimplify() does with the methods that does not have an active body
        // but should and can have an active body
        if( opts.pre_jimplify() ) b.preJimplify();
        // setup() builds an empty PAG
        // and returns a reference to the PAG
        final PAG pag = b.setup( opts );
        // build() builds the PAG in b
        // so updates PAG
        // this build() uses CHA to build the PAG
        b.build();
        PAGDumper dumper = new PAGDumper( pag, output_dir );
        dumper.dump();
        
        MatchEdges me = new MatchEdges(pag);
        
        System.out.println("-------match-------");
        Iterator<Object> it = me.matchSourcesIterator();
        while (it.hasNext()) {
        	VarNode from = (VarNode)it.next();
        	Node[] tos = me.matchLookup(from);
        	for (int i = 0; i < tos.length; i ++) {
        		VarNode to = (VarNode)tos[i];
        		System.out.println(from.getNumber() + " " + to.getNumber());
        	}
        }
        
        System.out.println("--------info-------");  
        System.out.println("--------store info---------");
        it = pag.storeSourcesIterator();
        while (it.hasNext()) {
        	VarNode from = (VarNode)it.next();
        	Node[] tos = pag.storeLookup(from);
        	for (int i = 0; i < tos.length; i ++) {
        		FieldRefNode to = (FieldRefNode)tos[i];
        		System.out.println(from.getNumber() + " | " 
        				+ to.getBase().getNumber() + " " 
        				+ to.getNumber() + " "
        				+ ((SootField)to.getField()).getName());
        	}
        }
        System.out.println("---------load info-----------");
        it = pag.loadSourcesIterator(); 
        while (it.hasNext()) {
        	FieldRefNode from = (FieldRefNode)it.next();
        	Node[] tos = pag.loadLookup(from);
        	for (int i = 0; i < tos.length; i ++) {
        		VarNode to = (VarNode)tos[i];
        		System.out.println(from.getBase().getNumber() + " "
        				+ from.getNumber() + " " 
        				+ ((SootField)from.getField()).getName() + " | "
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
				"/Users/xwang/oopsla/CallsiteResolver/benchmarks/CFLexamples/bin/";
				//"/Users/xwang/oopsla/CallsiteResolver/CFLexamples/test/bin";
		try {

			StringBuilder options = new StringBuilder();	
			// 
			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.test", new test()));

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
