# Do NOT uncomment the followings, because getLowPriorityEventTimeout and initEQ should be deleted from the trace file
#rm -r out/
#rm -r ../out/sunflow
rm -r ../sootified/sunflow
cp -r ../benchmarks/sunflow ../sootified/
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar sunflow -s default 
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar sunflow -s default 
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar sunflow -s default 
#cp -r out/ ../out/sunflow
java -Xmx4G -cp ../soot/soot-nightly.jar soot.Main -w -app -p cg.spark enabled -p cg reflection-log:../out/sunflow/refl.log -cp ../lib/jce.jar:../lib/rt.jar:../out/sunflow -include org.apache. -include org.w3c. -main-class Harness -d ../sootified/sunflow Harness 
#java -Xmx4G -javaagent:pia.jar -jar dacapo-9.12-bach.jar sunflow -s default
