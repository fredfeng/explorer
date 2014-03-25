package edu.utexas.FullFS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Context;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootField;
import soot.jimple.spark.ondemand.AllocAndContextSet;
import soot.jimple.spark.ondemand.HeuristicType;
import soot.jimple.spark.ondemand.WrappedPointsToSet;
import soot.jimple.spark.ondemand.genericutil.Propagator;
import soot.jimple.spark.ondemand.genericutil.Stack;
import soot.jimple.spark.ondemand.pautil.ContextSensitiveInfo;
import soot.jimple.spark.ondemand.pautil.SootUtil;
import soot.jimple.spark.ondemand.pautil.ValidMatches;
import soot.jimple.spark.ondemand.pautil.SootUtil.FieldToEdgesMap;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.sets.EmptyPointsToSet;
import soot.jimple.spark.sets.PointsToSetInternal;

/*
 * RegularPT: Field-sensitive points-to analysis from Manu's paper in OOPSLA05
 * Some codes are adopted from DemandCSPointsTo.java
 *  yufeng@cs.utexas.edu
 */

public class FullFS implements PointsToAnalysis {

    protected ValidMatches vMatches;
    
    protected final int maxPasses;
    
    protected final ContextSensitiveInfo csInfo;
    
    private final boolean lazy;
    
    protected HeuristicType heuristicType;
    
    protected FieldToEdgesMap fieldToLoads;

    protected FieldToEdgesMap fieldToStores;
    
    protected final int maxNodesPerPass;

    protected Map<Local,PointsToSet> reachingObjectsCache, reachingObjectsCacheNoCGRefinement;

    protected boolean useCache;
    
    protected final PAG pag;
    
    protected AllocAndContextSet pointsTo = null;
    
    public PointsToSet reachingObjects(Local l) {
        return doReachingObjects(l);
    }
    
    private void init() {
        this.fieldToStores = SootUtil.storesOnField(pag);
        this.fieldToLoads = SootUtil.loadsOnField(pag);
        this.vMatches = new ValidMatches(pag, fieldToStores);
    }
    
    public PointsToSet doReachingObjects(Local l) {
        //lazy initialization
        if(fieldToStores==null) {
            init();
        }
        PointsToSet result = computeReachingObjects(l);     
        return result;
    }
    
    /**
     * Computes the possibly refined set of reaching objects for l.
     */
    protected PointsToSet computeReachingObjects(Local l) {
        VarNode v = pag.findLocalVarNode(l);
        if (v == null) {
          //no reaching objects
          return EmptyPointsToSet.v();
        }
        pointsTo = new AllocAndContextSet();
        
        final Set<VarNode> marked = nodesPropagatedThrough(v, v.getP2Set());

        return new WrappedPointsToSet(v.getP2Set());

    }

    @Override
    public PointsToSet reachingObjects(Context c, Local l) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public PointsToSet reachingObjects(SootField f) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public PointsToSet reachingObjects(PointsToSet s, SootField f) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public PointsToSet reachingObjects(Local l, SootField f) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public PointsToSet reachingObjects(Context c, Local l, SootField f) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    @Override
    public PointsToSet reachingObjectsOfArrayElement(PointsToSet s) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
    
    protected Set<VarNode> nodesPropagatedThrough(final VarNode source,
            final PointsToSetInternal allocs) {
        final Set<VarNode> marked = new HashSet<VarNode>();
        final Stack<VarNode> worklist = new Stack<VarNode>();
        Propagator<VarNode> p = new Propagator<VarNode>(marked, worklist);
        p.prop(source);
        while (!worklist.isEmpty()) {
            VarNode curNode = worklist.pop();
            Node[] assignSources = pag.simpleInvLookup(curNode);
            for (int i = 0; i < assignSources.length; i++) {
                VarNode assignSrc = (VarNode) assignSources[i];
                if (assignSrc.getP2Set().hasNonEmptyIntersection(allocs)) {
                    p.prop(assignSrc);
                }
            }
            Set<VarNode> matchSources = vMatches.vMatchInvLookup(curNode);
            for (VarNode matchSrc : matchSources) {
                if (matchSrc.getP2Set().hasNonEmptyIntersection(allocs)) {
                    p.prop(matchSrc);
                }
            }
        }
        return marked;
    }

    public FullFS(ContextSensitiveInfo csInfo, PAG pag,
            int maxTraversal, int maxPasses, boolean lazy) {
        this.csInfo = csInfo;
        this.pag = pag;
        this.maxPasses = maxPasses;
        this.lazy = lazy;
        this.maxNodesPerPass = maxTraversal / maxPasses;
        this.heuristicType = HeuristicType.INCR;
        this.reachingObjectsCache = new HashMap<Local, PointsToSet>();
        this.reachingObjectsCacheNoCGRefinement = new HashMap<Local, PointsToSet>();
        this.useCache = true;
    }
    
    public static FullFS makeWithBudget(int maxTraversal,
            int maxPasses, boolean lazy) {
        PAG pag = (PAG) Scene.v().getPointsToAnalysis();
        ContextSensitiveInfo csInfo = new ContextSensitiveInfo(pag);
        return new FullFS(csInfo, pag, maxTraversal, maxPasses, lazy);
    }

}
