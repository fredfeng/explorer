package edu.utexas.cgrex.utils;

import soot.FastHierarchy;
import soot.util.NumberedString;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Saswat Anand
**/
public class SootUtils
{   
    private static HashMap<SootClass,Set<SootClass>> classToSubtypes = new HashMap();
    
    protected static void reportTime( String desc, Date start, Date end ) {
        long time = end.getTime()-start.getTime();
        G.v().out.println( "[CGregx] "+desc+" in "+time/1000+"."+(time/100)%10+" seconds." );
    }

    public static Set<SootClass> subTypesOf(SootClass cl)
    {
        Set<SootClass> subTypes = classToSubtypes.get(cl);
        if(subTypes != null) 
            return subTypes;
        
        classToSubtypes.put(cl, subTypes = new HashSet());

        subTypes.add(cl);

        LinkedList<SootClass> worklist = new LinkedList<SootClass>();
        HashSet<SootClass> workset = new HashSet<SootClass>();
        FastHierarchy fh = Scene.v().getOrMakeFastHierarchy();

        if(workset.add(cl)) worklist.add(cl);
        while(!worklist.isEmpty()) {
            cl = worklist.removeFirst();
            if(cl.isInterface()) {
                for(Iterator<SootClass> cIt = fh.getAllImplementersOfInterface(cl).iterator(); cIt.hasNext();) {
                    final SootClass c = cIt.next();
                    if(workset.add(c)) worklist.add(c);
                }
            } else {
                if(cl.isConcrete()) {
                    subTypes.add(cl);
                }
                for(Iterator<SootClass> cIt = fh.getSubclassesOf(cl).iterator(); cIt.hasNext();) {
                    final SootClass c = cIt.next();
                    if(workset.add(c)) worklist.add(c);
                }
            }
        }
        return subTypes;
    }
    
    public static String getSootSubsigFor(String chordSubsig)
    {
        String name = chordSubsig.substring(0, chordSubsig.indexOf(':'));
        String retType = chordSubsig.substring(chordSubsig.indexOf(')')+1);
        String paramTypes = chordSubsig.substring(chordSubsig.indexOf('(')+1, chordSubsig.indexOf(')'));
        return parseDesc(retType) + " " + name + "(" + parseDesc(paramTypes) + ")";
    }

    static String parseDesc(String desc) 
    {
        StringBuilder params = new StringBuilder();
        String param = null;
        char c;
        int arraylevel=0;
        boolean didone = false;

        int len = desc.length();
        for (int i=0; i < len; i++) {
            c = desc.charAt(i);
            if (c =='B') {
                param = "byte";
            } else if (c =='C') {
                param = "char";
            } else if (c == 'D') {
                param = "double";
            } else if (c == 'F') {
                param = "float";
            } else if (c == 'I') {
                param = "int";
            } else if (c == 'J') {
                param = "long";
            } else if (c == 'S') {
                param = "short";
            } else if (c == 'Z') {
                param = "boolean";
            } else if (c == 'V') {
                param = "void";
            } else if (c == '[') {
                arraylevel++;
                continue;
            } else if (c == 'L') {
                int j;
                j = desc.indexOf(';',i+1);
                param = desc.substring(i+1,j);
                // replace '/'s with '.'s
                param = param.replace('/','.');
                i = j;
            } else
                assert false;

            if (didone) params.append(',');
            params.append(param);
            while (arraylevel>0) {
                params.append("[]");
                arraylevel--;
            }
            didone = true;
        }
        return params.toString();
    }
}
