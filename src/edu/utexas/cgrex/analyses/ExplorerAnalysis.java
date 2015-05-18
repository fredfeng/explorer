package edu.utexas.cgrex.analyses;

import soot.*;
import soot.jimple.*;
import soot.util.*;

import shord.program.Program;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramDom;
import shord.project.analyses.ProgramRel;
import shord.project.ClassicProject;

import stamp.app.Component;
import stamp.app.App;
import stamp.app.IntentFilter;
import stamp.app.Data;

import chord.project.Chord;

import java.util.jar.*;
import java.io.*;
import java.util.*;

import chord.util.tuple.object.Pair;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.StringUtil;

import soot.jimple.toolkits.callgraph.Edge;
import edu.utexas.cgrex.utils.SootUtils;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;


/**
 * Entry point for Explorer(Refine callgraph on demand).
 * @author Yu Feng (yufeng@cs.stanford.edu)
 */

@Chord(name="explorer")
public class ExplorerAnalysis extends JavaAnalysis
{
	
    QueryManager qmPerf; 
    QueryManager qmPerfCha; 


    //maximum queries.
    int max = 1000;

    // for perf.
    double totalTimeOnCha2 = 0;
    double totalTimeOnNoOpt2 = 0;
    double totalTimeOnCipa2 = 0;
    double totalTimeNormal2 = 0;
    double totalNoCut2 = 0.0;
    int perfCha = 0;
    int perfExp = 0;
    int perfCipa = 0;
    int totalPerf = 0;
 

    public void run()
    {
        
        // for oopsla15
        PackManager.v().getPack("wjpp").apply();
        PackManager.v().getPack("cg").apply();

        System.out.println("hook up with stamp...");
        System.out.println("step1:");
        qmPerf = new QueryManager(Scene.v().getCallGraph(), Program.g().getMainMethod(),true);
        System.out.println("step2:");
        qmPerfCha = new QueryManager(SootUtils.getCha(), Program.g().getMainMethod(),true);
        //exp2: performance detection
        perf();
        //dump summary info.
        System.out.println("----------Perf report-------------------------");
        System.out.println("Total Perf queries: " + totalPerf);
        System.out.println("Total refutations(Perf-cha): " + perfCha);
        System.out.println("Total refutations(Perf-exp): " + perfExp);
        System.out.println("Total refutations(Perf-cipa): " + perfCipa);
        System.out.println("Total time on Normal: " + totalTimeNormal2/1e6);
        System.out.println("Total time on no cut: " + totalNoCut2/1e6);
        System.out.println("Total time on CHA: " + (totalTimeOnCha2/1e6));
        System.out.println("Total time on cipa: " + (totalTimeOnCipa2/1e6));
        System.out.println("Total time w/o look ahead: " + (totalTimeOnNoOpt2/1e6));
    }

    private void perf() 
    {

        System.out.println("Performance exp:-----------------");
        String[] cbList = { "onCreate", "onResume", "onPause", "onClick" };
        Set<String> callbacks = new HashSet<String>();
        Iterator<SootMethod> it = Scene.v().getMethodNumberer().iterator();

        while (it.hasNext()) {
            SootMethod cb = it.next();
            SootClass clz = cb.getDeclaringClass();

            if (Arrays.asList(cbList).contains(cb.getName())) {
                SootClass actClz = Scene.v().getSootClass("android.app.Activity");
                if (!clz.getName().startsWith("android.") && SootUtils.subTypesOf(actClz).contains(clz))
                    callbacks.add(cb.getSignature());
            }

        }

        String dummyMain = Program.g().getMainMethod().getSignature();
    	int cnt = 0;
        // n*m queries.
        System.out.println("*****total queries: " + callbacks.size() * lenMeths.size());
    	Set<String> perfSet = new HashSet<String>();
        String bigQuery = "";
        for (String src : callbacks) {
            for (String tgt : lenMeths) {
                String tmp = ".*" + src + ".*" + tgt;
                String query = dummyMain + tmp;
                bigQuery += "(" + tmp + ")|";
		        perfSet.add(query);
            }
        }

        bigQuery = bigQuery.substring(0, bigQuery.length()-1);
        System.out.println("bigQuery:" + bigQuery);

        for (String query : perfSet) {
            if(cnt > max)
                break;

	    System.out.println("query " + cnt + "---> " + query);
            cnt++;
            totalPerf = cnt;
            long startNormal = System.nanoTime();
            String regx = qmPerf.getValidExprBySig(query);
            boolean res1 = qmPerf.queryRegx(regx);
            long endNormal = System.nanoTime();
            totalTimeNormal2 += (endNormal - startNormal);

            /*
            long startNoOpt = System.nanoTime();
            boolean res2 = qmPerf.queryRegxNoLookahead(regx);
            long endNoOpt = System.nanoTime();
            totalTimeOnNoOpt2 += (endNoOpt - startNoOpt);
        
            long startNoCut = System.nanoTime();
            boolean res4 = qmPerf.queryRegxNoMincut(regx);
            long endNoCut = System.nanoTime();
            totalNoCut2 += (endNoCut - startNoCut);

	        long startCha = System.nanoTime();
            String regxCha = qmPerfCha.getValidExprBySig(query);
            boolean res3 = qmPerfCha.queryWithoutRefine(regxCha);
            long endCha = System.nanoTime();
            totalTimeOnCha2 += (endCha - startCha);
            */

            long startCipa = System.nanoTime();
            boolean res5 = qmPerf.queryWithoutRefine(regx);
            long endCipa = System.nanoTime();
            totalTimeOnCipa2 += (endCipa - startCipa);

            /*
            if(!res3)
            perfCha++;
            */

            if(!res5)
            perfCipa++;

            if(!res1) {
                perfExp++;
            } else {
                System.out.println("false positive:" + query);
            }
        }

        long sNormal = System.nanoTime();
        String regxBig = qmPerf.getValidExprBySig(bigQuery);
        boolean res11 = qmPerf.queryRegx(regxBig);
        long eNormal = System.nanoTime();
        StringUtil.reportSec("Big query(EXP):", sNormal, eNormal);
        System.out.println("Result(EXP):" + res11);

        long sCipa = System.nanoTime();
        boolean res55 = qmPerf.queryWithoutRefine(regxBig);
        long eCipa = System.nanoTime();
        StringUtil.reportSec("Big query(CIPA):", sCipa, eCipa);
        System.out.println("Result(CIPA):" + res55);
 

        /*
        long sCha = System.nanoTime();
        String regxBigCha = qmPerfCha.getValidExprBySig(bigQuery);
        boolean res33 = qmPerfCha.queryWithoutRefine(regxBigCha);
        long eCha = System.nanoTime();
        StringUtil.reportSec("Big query(CHA):", sCha, eCha);
        System.out.println("Result(CHA):" + res33);
        */
  
    }


    private List<String> lenMeths = Arrays.asList(new String[] {

	"<java.net.URL: java.net.URLConnection openConnection()>",
	"<java.net.URL: java.io.InputStream openStream()>",
	"<java.net.URL: java.lang.Object getContent()>",
	"<java.net.URL: java.lang.Object getContent(java.lang.Class[])>",
	"<java.net.URLConnection: java.io.InputStream getInputStream()>",
	"<java.net.URLConnection: java.io.OutputStream getOutputStream()>",
	"<java.net.URLConnection: java.lang.Object getContent()>",
	"<java.net.URLConnection: java.lang.Object getContent(java.lang.Class[])>",
	"<java.net.URLConnection: java.lang.String getContentEncoding()>",
	"<java.net.URLConnection: java.lang.String getContentType()>",
	"<java.net.URLConnection: int getContentLength()>",
	"<android.database.sqlite.SQLiteDatabase: void execSQL(java.lang.String)>",
	"<android.database.sqlite.SQLiteDatabase: void execSQL(java.lang.String,java.lang.Object[])>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String,java.lang.String)>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(boolean,java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String,java.lang.String,android.os.CancellationSignal)>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String)>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor query(boolean,java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String,java.lang.String)>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor queryWithFactory(android.database.sqlite.SQLiteDatabase$CursorFactory,boolean,java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String,java.lang.String,android.os.CancellationSignal)>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor queryWithFactory(android.database.sqlite.SQLiteDatabase$CursorFactory,boolean,java.lang.String,java.lang.String[],java.lang.String,java.lang.String[],java.lang.String,java.lang.String,java.lang.String,java.lang.String)>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQuery(java.lang.String,java.lang.String[],android.os.CancellationSignal)>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQuery(java.lang.String,java.lang.String[])>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQueryWithFactory(android.database.sqlite.SQLiteDatabase$CursorFactory,java.lang.String,java.lang.String[],java.lang.String)>",
	"<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQueryWithFactory(android.database.sqlite.SQLiteDatabase$CursorFactory,java.lang.String,java.lang.String[],java.lang.String,android.os.CancellationSignal)>",
	"<android.context.ContextWrapper: java.io.FileInputStream openFileInput(java.lang.String)>",
	"<android.context.ContextWrapper: java.io.FileOutputStream openFileOutput(java.lang.String,int)>",
	"<java.io.Reader: int read()>",
	"<java.io.Reader: int read(char[])>",
	"<java.io.Reader: int read(java.nio.CharBuffer)>",
	"<java.io.InputStreamReader: int read()>",
	"<java.io.InputStreamReader: int read(char[],int,int)>",
	"<java.io.BufferedReader: int read()>",
	"<java.io.BufferedReader: int read(char[],int,int)>",
	"<java.io.BufferedReader: java.lang.String readLine()>",
	"<java.io.Writer: java.io.Writer append(char)>",
	"<java.io.Writer: java.io.Writer append(java.lang.CharSequence)>",
	"<java.io.Writer: java.io.Writer append(java.lang.CharSequence,int,int)>",
	"<java.io.Writer: void write(char[])>",
	"<java.io.Writer: void write(int)>",
	"<java.io.Writer: void write(java.lang.String)>",
	"<java.io.Writer: void write(java.lang.String,int,int)>",
	"<java.io.BufferedWriter: void write(char[],int,int)>",
	"<java.io.BufferedWriter: void write(int)>",
	"<java.io.BufferedWriter: void write(java.lang.String,int,int)>",
	"<android.graphics.BitmapFactory: android.graphics.Bitmap decodeFile(java.lang.String,android.graphics.BitmapFactory$Options)>",
	"<android.graphics.Bitmap: boolean compress(android.graphics.Bitmap$CompressFormat,int,java.io.OutputStream)>"
	    });

}
