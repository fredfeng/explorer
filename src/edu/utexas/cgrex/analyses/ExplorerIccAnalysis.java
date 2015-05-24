package edu.utexas.cgrex.analyses;

import soot.*;

import shord.program.Program;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import shord.project.ClassicProject;
import chord.project.Chord;
import chord.util.tuple.object.Pair;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.SootUtils;


/**
 * Entry point for Explorer(Refine callgraph on demand).
 * @author Yu Feng (yufeng@cs.stanford.edu)
 */

@Chord(name="explorerIcc")
public class ExplorerIccAnalysis extends JavaAnalysis
{
	
    QueryManager qm; 

    QueryManager qmCha; 

    //maximum queries.
    int max = 1000;

    // for icc.
    double totalTimeOnCha = 0;
    double totalTimeOnCipa = 0;
    double totalTimeOnNoOpt = 0;
    double totalTimeNormal = 0;
    double totalNoCut = 0.0;
    int iccCha = 0;
    int iccExp = 0;
    int iccCipa = 0;
    int totalIcc = 0;

    public void run()
    {
        
        // for oopsla15
        PackManager.v().getPack("wjpp").apply();
        PackManager.v().getPack("cg").apply();

        qm = new QueryManager(Scene.v().getCallGraph(), Program.g().getMainMethod());
        qmCha = new QueryManager(SootUtils.getCha(), Program.g().getMainMethod());
        //exp1: icc query.
        iccQuery();
        //dump summary info.
        System.out.println("----------ICC report-------------------------");
        System.out.println("Total ICC queries: " + totalIcc);
        System.out.println("Total refutations(ICC-cha): " + iccCha);
        System.out.println("Total refutations(ICC-exp): " + iccExp);
        System.out.println("Total refutations(ICC-cipa): " + iccCipa);
        System.out.println("Total time on Normal: " + totalTimeNormal/1e6);
        System.out.println("Total time on no cut: " + totalNoCut/1e6);
        System.out.println("Total time on CHA: " + (totalTimeOnCha/1e6));
        System.out.println("Total time on cipa: " + (totalTimeOnCipa/1e6));
        System.out.println("Total time w/o look ahead: " + (totalTimeOnNoOpt/1e6));
        
    }

    // generate Icc query based on SrcTgt rel.
    private void iccQuery()
    {
        System.out.println("ICC exp:-----------------");
        ProgramRel relSrcTgt = (ProgramRel) ClassicProject.g().getTrgt("SrcTgt");
        if (!relSrcTgt.isOpen())
            relSrcTgt.load();

	    int cnt = 1;
        Iterable<Pair<SootMethod, SootMethod>> it = relSrcTgt.getAry2ValTuples();
        for (Pair<SootMethod, SootMethod> ele : it) {
            if(cnt > max)
                break;
            cnt++;
            totalIcc = cnt;
            SootMethod m1 = ele.val0;
            SootMethod m2 = ele.val1;
            String query = ".*" + m1.getSignature() + ".*" + m2.getSignature();

            long startNormal = System.nanoTime();
            String regx = qm.getValidExprBySig(query);
            boolean res1 = qm.queryRegx(regx);
            long endNormal = System.nanoTime();
            totalTimeNormal += (endNormal - startNormal);
        
            /*
            long startNoOpt = System.nanoTime();
            boolean res2 = qm.queryRegxNoLookahead(regx);
            long endNoOpt = System.nanoTime();
            totalTimeOnNoOpt += (endNoOpt - startNoOpt);
        
            long startNoCut = System.nanoTime();
            boolean res4 = qm.queryRegxNoMincut(regx);
            long endNoCut = System.nanoTime();
            totalNoCut += (endNoCut - startNoCut);
            */

            long startCipa = System.nanoTime();
            boolean res5 = qm.queryWithoutRefine(regx);
            long endCipa = System.nanoTime();
            totalTimeOnCipa += (endCipa - startCipa);

            /*
            long startCha = System.nanoTime();
            String regxCha = qmCha.getValidExprBySig(query);
            boolean res3 = qmCha.queryWithoutRefine(regxCha);
            long endCha = System.nanoTime();
            totalTimeOnCha += (endCha - startCha);
            
            if(!res3)
                iccCha++;
            */

            if(!res5)
                iccCipa++;

            if(!res1) {
                iccExp++;
            }
        }
    }

}
