package edu.utexas.cgrex.analyses;

import java.util.Set;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.Stmt;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.SetUtils;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import edu.utexas.cgrex.QueryManager;

/**
 * @author Yu Feng
 */
@Chord(name = "cg-java", consumes = {"CSMM","reachM"})
public class CSCGAnalysis extends JavaAnalysis {

	private ProgramRel relCSMM;
    protected ProgramRel relRootM;
    protected ProgramRel relReachM;
    protected QueryManager qm;
    double totalTimeSen = 0;
    int totalIcc = 0;
    int iccSen = 0;
    final int max = 500;

	// total pairs;
	public void run() {
		relCSMM = (ProgramRel) ClassicProject.g().getTrgt("CSMM");
		relRootM = (ProgramRel) ClassicProject.g().getTrgt("rootM");
		relReachM = (ProgramRel) ClassicProject.g().getTrgt("reachM");

		if (!relCSMM.isOpen())
			relCSMM.load();

		if (!relReachM.isOpen())
			relReachM.load();

		Iterable<Trio<SootMethod, Stmt, SootMethod>> it = relCSMM.getView()
				.getAry3ValTuples();
		Set<Trio<SootMethod, Stmt, SootMethod>> set = SetUtils.iterableToSet(
				it, relCSMM.size());

		Iterable<SootMethod> itm = relReachM.getView().getAry1ValTuples();
		Set<SootMethod> setMeths = SetUtils
				.iterableToSet(itm, relReachM.size());
		
		qm = new QueryManager(setMeths, set);
		qm.setMainMethod(Scene.v().getMainMethod());
		
		iccQuery();
	}
	
    private void iccQuery()
    {
        System.out.println("ICC exp for kobj/kcfa:-----------------");
        ProgramRel relSrcTgt = (ProgramRel) ClassicProject.g().getTrgt("SrcTgt");
        if (!relSrcTgt.isOpen())
            relSrcTgt.load();

        Iterable<Pair<SootMethod, SootMethod>> it = relSrcTgt.getAry2ValTuples();
        for (Pair<SootMethod, SootMethod> ele : it) {
            if(totalIcc > max)
                return;
            totalIcc++;
            SootMethod m1 = ele.val0;
            SootMethod m2 = ele.val1;
            String query = ".*" + m1.getSignature() + ".*" + m2.getSignature();

            long startNormal = System.nanoTime();
            String regx = qm.getValidExprBySig(query);
            boolean res1 = qm.queryWithoutRefine(regx);
            long endNormal = System.nanoTime();
            totalTimeSen += (endNormal - startNormal);
            if(!res1)
            	iccSen++;
        }
    }
	
}
