
/* Soot - a J*va Optimization Framework
 * Copyright (C) 2007 Patrick Lam
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package edu.utexas.UltraCG;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.Local;
import soot.RefLikeType;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JReturnStmt;
import soot.tagkit.StringTag;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

/** LocalNotMayAliasAnalysis attempts to determine if two local
 * variables (at two potentially different program points) definitely
 * point to different objects.
 *
 * The underlying abstraction is that of definition expressions.  When
 * a local variable gets assigned a new object (unlike LocalMust, only
 * NewExprs), the analysis tracks the source of the value. If two
 * variables have different sources, then they are different.
 * 
 * See Sable TR 2007-8 for details.
 * 
 * The modification is to tag the abstract allocation site h that 
 * will not flow out of current procedure. Or, flow out through return stmt.
 * If the allocation site flow into other fields, containers or formal params,
 * we just give up. 
 * Eventually we will add tag for each allocation site indicate whether it's dead.
 * 
 * 
 * @author Patrick Lam
 * @author yufeng
 */
public class LocalMustNotAliasAnalysis extends ForwardFlowAnalysis
{
    protected static final Object UNKNOWN = new Object() {
        public String toString() {
            return "UNKNOWN";
        }
    };
    
    protected Set<Local> locals;

    public LocalMustNotAliasAnalysis(UnitGraph g)
    {
        super(g);
        locals = new HashSet<Local>(); locals.addAll(g.getBody().getLocals());

        for (Local l : (Collection<Local>) g.getBody().getLocals()) {
            if (l.getType() instanceof RefLikeType)
                locals.add(l);
        }

        doAnalysis();
        
        tagMustNotInfo(g);
    }
    
    /*Query the result and tag it to corresponding alloc. */
    private void tagMustNotInfo(UnitGraph g) 
    {
        Iterator it = g.getBody().getUnits().iterator();
        Unit retUnit = g.getBody().getUnits().getLast();
        while (it.hasNext()){
            Stmt stmt = (Stmt)it.next();
            if(stmt instanceof DefinitionStmt) {

                Value rhs = ((DefinitionStmt)stmt).getRightOp();
                Value lhs = ((DefinitionStmt)stmt).getLeftOp();
                
                if( (rhs instanceof NewExpr) && (lhs instanceof Local)) {
            
                    if(getReturnStmt(g) == null) {//void
                        //irrelevant heap
                        //System.out.println("REFUTE******: "+ rhs);
                        stmt.addTag(new StringTag("Y"));
                    } else {//return something
                        
                        JReturnStmt retStmt = (JReturnStmt)retUnit;
                        Local l1 = (Local)lhs;
                        if(retStmt.getOp() instanceof Local) {
                            Local l2 = (Local)retStmt.getOp();
                            boolean mustNot = notMayAlias(l1, stmt, l2, retStmt);
                            if(mustNot) {
                                //System.out.println("REFUTE******: "+ rhs);
                                //irrelevant heap
                                stmt.addTag(new StringTag("Y"));
                            }

                        }
                    }  
                }
            }
        }
    }
    
    private Unit getReturnStmt(UnitGraph g)
    {
        Unit retUnit = g.getBody().getUnits().getLast();
        if(retUnit instanceof JReturnStmt) return retUnit;
        else return null;
    }

    protected void merge(Object in1, Object in2, Object o)
    {
        HashMap inMap1 = (HashMap) in1;
        HashMap inMap2 = (HashMap) in2;
        HashMap outMap = (HashMap) o;

        for (Local l : locals) {
            Set l1 = (Set)inMap1.get(l), l2 = (Set)inMap2.get(l);
            Set out = (Set)outMap.get(l);
            out.clear();
            if (l1.contains(UNKNOWN) || l2.contains(UNKNOWN)) {
                out.add(UNKNOWN);
            } else {
                out.addAll(l1); out.addAll(l2);
            }
        }
    }
    

    protected void flowThrough(Object inValue, Object unit,
            Object outValue)
    {
        HashMap     in  = (HashMap) inValue;
        HashMap     out = (HashMap) outValue;
        Stmt    s   = (Stmt)    unit;

        out.clear();
        out.putAll(in);

        if (s instanceof DefinitionStmt) {
            DefinitionStmt ds = (DefinitionStmt) s;
            Value lhs = ds.getLeftOp();
            Value rhs = ds.getRightOp();
            if (lhs instanceof Local) {
                HashSet lv = new HashSet();
                out.put(lhs, lv);
                if (rhs instanceof NewExpr) {
                    lv.add(rhs);
                } else if (rhs instanceof Local) {
                    lv.addAll((HashSet)in.get(rhs));
                } else lv.add(UNKNOWN);
            }
        }
    }

    protected void copy(Object source, Object dest)
    {
        HashMap sourceMap = (HashMap) source;
        HashMap destMap   = (HashMap) dest;
            
        destMap.putAll(sourceMap);
    }

    protected Object entryInitialFlow()
    {
        HashMap m = new HashMap();
        for (Local l : (Collection<Local>) locals) {
            HashSet s = new HashSet(); s.add(UNKNOWN);
            m.put(l, s);
        }
        return m;
    }
        
    protected Object newInitialFlow()
    {
        HashMap m = new HashMap();
        for (Local l : (Collection<Local>) locals) {
            HashSet s = new HashSet(); 
            m.put(l, s);
        }
        return m;
    }

    /**
     * Returns true if this analysis has any information about local l
     * at statement s (i.e. it is not {@link #UNKNOWN}).
     * In particular, it is safe to pass in locals/statements that are not
     * even part of the right method. In those cases <code>false</code>
     * will be returned.
     * Permits s to be <code>null</code>, in which case <code>false</code> will be returned.
     */
    public boolean hasInfoOn(Local l, Stmt s) {
        HashMap flowBefore = (HashMap) getFlowBefore(s);
        if(flowBefore==null) {
            return false;
        } else {
            Set info = (Set) flowBefore.get(l);
            return info!=null && !info.contains(UNKNOWN);
        }
    }

    /**
     * @return true if values of l1 (at s1) and l2 (at s2) are known
     * to point to different objects
     */
    public boolean notMayAlias(Local l1, Stmt s1, Local l2, Stmt s2) {
        Set l1n = (Set) ((HashMap)getFlowBefore(s1)).get(l1);
        Set l2n = (Set) ((HashMap)getFlowBefore(s2)).get(l2);

        if (l1n.contains(UNKNOWN) || l2n.contains(UNKNOWN))
            return false;

        Set n = new HashSet(); n.addAll(l1n); n.retainAll(l2n);
        return n.isEmpty();
    }
        
}
