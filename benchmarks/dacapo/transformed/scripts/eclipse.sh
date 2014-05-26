rm -r ../out/eclipse.back
cp -r ../out/eclipse ../out/eclipse.back
rm -r ../out/eclipse
rm -r ../sootified/eclipse
cp -r ../benchmarks/eclipse ../sootified/
java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar eclipse -s default 
java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar eclipse -s default 
java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar eclipse -s default 
mv out/ ../out/eclipse
java -Xmx4G -cp ../soot/soot-nightly.jar soot.Main -w -app -p cg.spark enabled -p cg reflection-log:../out/eclipse/refl.log -cp ../lib/jce.jar:../lib/rt.jar:../lib/commons-cli-1.2.jar:../lib/eclipse.jar:../lib/org.eclipse.jdt.core-3.6.2.v_A76_R36x.jar:../lib/org.eclipse.text_3.5.0.jar:../out/eclipse -include org.apache. -include org.w3c. -include org.xml. -include java. -include javax. -main-class Harness -d ../sootified/eclipse Harness 
#java -Xmx4G -javaagent:pia.jar -jar dacapo-9.12-bach.jar avrora -s default
