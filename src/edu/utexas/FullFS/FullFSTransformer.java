package edu.utexas.FullFS;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import soot.Body;
import soot.CompilationDeathException;
import soot.G;
import soot.Local;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.PAGDumper;
import soot.jimple.spark.solver.SCCCollapser;
import soot.options.Options;
import soot.options.SparkOptions;

/**
 *Transformer for FullFS
 */
public class FullFSTransformer extends SceneTransformer {

    protected void internalTransform(String phaseName,
    @SuppressWarnings("rawtypes") Map options) {
        //set the PointsToAnalysis with phase options
        Map myoptions = new HashMap(options);
        myoptions.put("set-impl", "double");
        myoptions.put("double-set-old", "hybrid");
        myoptions.put("double-set-new", "hybrid");
        myoptions.put("verbose", "true");

        SparkOptions opts = new SparkOptions( myoptions );
        final String output_dir = SourceLocator.v().getOutputDir();

        // Build pointer assignment graph
        ContextInsensitiveBuilder b = new ContextInsensitiveBuilder();
        if( opts.pre_jimplify() ) b.preJimplify();
        Date startBuild = new Date();
        final PAG pag = b.setup( opts );
        b.build();
        Date endBuild = new Date();
        reportTime( "Pointer Assignment Graph", startBuild, endBuild );

        if( opts.verbose() ) {
            G.v().out.println( "VarNodes: "+pag.getVarNodeNumberer().size() );
            G.v().out.println( "FieldRefNodes: "+pag.getFieldRefNodeNumberer().size() );
            G.v().out.println( "AllocNodes: "+pag.getAllocNodeNumberer().size() );
        }

        // Simplify pag
        Date startSimplify = new Date();

        new SCCCollapser( pag, opts.ignore_types_for_sccs() ).collapse();               
        pag.cleanUpMerges();
       
        Date endSimplify = new Date();
        reportTime( "Pointer Graph simplified", startSimplify, endSimplify );

        // Dump pag
        PAGDumper dumper = null;
        if( opts.dump_pag() || opts.dump_solution() ) {
            dumper = new PAGDumper( pag, output_dir );
        }
        if( opts.dump_pag() ) dumper.dump();

        if( opts.verbose() ) {
            G.v().out.println( "[Spark] Number of reachable methods: "
                    +Scene.v().getReachableMethods().size() );
        }

        final int DEFAULT_MAX_PASSES = 1000;
        final int DEFAULT_MAX_TRAVERSAL = 7500000;
        final boolean DEFAULT_LAZY = false;

        Date startOnDemand = new Date();
        PointsToAnalysis onDemandAnalysis = FullFS.makeWithBudget(DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);

        Date endOndemand = new Date();
        System.out.println( "Initialized on-demand context-insensitive analysis" + startOnDemand + endOndemand );
        Scene.v().setPointsToAnalysis(onDemandAnalysis);
        PointsToAnalysis pts = Scene.v().getPointsToAnalysis();

        for (String path : (Collection<String>) Options.v().process_dir()) {
            for (String cl : SourceLocator.v().getClassesUnder(path)) {
                SootClass clazz = Scene.v().getSootClass(cl);
                for(SootMethod m : clazz.getMethods()) {
                    Body body = m.retrieveActiveBody();
                    for(Local l : body.getLocals()) {
                        System.out.println(l + " Points to: " + pts.reachingObjects(l));
                    }
                }
            }
        }

    }


    protected static void reportTime( String desc, Date start, Date end ) {
        long time = end.getTime()-start.getTime();
        G.v().out.println( "[Spark] "+desc+" in "+time/1000+"."+(time/100)%10+" seconds." );
    }

}
