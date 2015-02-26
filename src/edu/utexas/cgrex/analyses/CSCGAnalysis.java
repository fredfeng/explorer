package edu.utexas.cgrex.analyses;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
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
import edu.utexas.cgrex.utils.SootUtils;
import edu.utexas.cgrex.utils.StringUtil;

/**
 * @author Yu Feng
 */
@Chord(name = "cg-java", consumes = {"CSMM","reachM"})
public class CSCGAnalysis extends JavaAnalysis {

	private ProgramRel relCSMM;
    protected ProgramRel relReachM;
    protected QueryManager qmIcc;
    protected QueryManager qmPerf;

    double totalTimeSen = 0;
    int totalIcc = 0;
    int iccSen = 0;
    
    double totalTimePerfSen = 0;
    int totalPerf = 0;
    int perfSen = 0;
    final int max = 500;

	// total pairs;
	public void run() {
		relCSMM = (ProgramRel) ClassicProject.g().getTrgt("CSMM");
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
		
		qmIcc = new QueryManager(Scene.v().getMainMethod(), setMeths, set,
				false);
		qmPerf = new QueryManager(Scene.v().getMainMethod(), setMeths, set,
				true);

		
		iccQuery();
		
		perf();
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
                break;
            totalIcc++;
            SootMethod m1 = ele.val0;
            SootMethod m2 = ele.val1;
            String query = ".*" + m1.getSignature() + ".*" + m2.getSignature();

            long startNormal = System.nanoTime();
            String regx = qmIcc.getValidExprBySig(query);
            boolean res1 = qmIcc.queryWithoutRefine(regx);
            long endNormal = System.nanoTime();
            totalTimeSen += (endNormal - startNormal);
            if(!res1)
            	iccSen++;
        }
        
        System.out.println("----------ICC report-------------------------");
        System.out.println("Total ICC queries: " + totalIcc);
        System.out.println("Total refutations(ICC-sen): " + iccSen);
        System.out.println("Total time on Sen: " + totalTimeSen/1e6);
    }
    
    private void perf() 
    {

        System.out.println("Perf exp for kobj/kcfa:-----------------");
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

        String dummyMain = Scene.v().getMainMethod().getSignature();
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

		for (String query : perfSet) {
			if (totalPerf > max)
				break;

			totalPerf++;
			long startNormal = System.nanoTime();
			String regx = qmPerf.getValidExprBySig(query);
			boolean res1 = qmPerf.queryWithoutRefine(regx);
			long endNormal = System.nanoTime();
			totalTimePerfSen += (endNormal - startNormal);

			if (!res1)
				perfSen++;
		}

        long sNormal = System.nanoTime();
        String regxBig = qmPerf.getValidExprBySig(bigQuery);
        boolean res11 = qmPerf.queryWithoutRefine(regxBig);
        long eNormal = System.nanoTime();
        StringUtil.reportSec("Big query(SEN):", sNormal, eNormal);
        System.out.println("Result(SEN):" + res11);
        System.out.println("----------Perf report(kobj/kcfa)-------------------------");
        System.out.println("Total Perf queries(sen): " + totalPerf);
        System.out.println("Total refutations(Perf-sen): " + perfSen);
        System.out.println("Total time on sen: " + totalTimePerfSen/1e6);
  
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
