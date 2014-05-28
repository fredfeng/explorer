rm -r out/
rm -r ../out/eclipse.back
cp -r ../out/eclipse ../out/eclipse.back
rm -r ../out/eclipse
rm -r ../sootified/eclipse
cp -r ../benchmarks/eclipse ../sootified/
java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar eclipse -s default 
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar eclipse -s default 
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar eclipse -s default 
cp -r out/ ../out/eclipse
java -Xmx4G -cp ../soot/soot-nightly.jar soot.Main -w -app -p cg.spark enabled -p cg reflection-log:../out/eclipse/refl.log -cp ../lib/jce.jar:../lib/rt.jar:../lib/eclipse.jar:../out/eclipse -include org.apache. -include org.w3c. -main-class Harness -d ../sootified/eclipse Harness 
#java -Xmx4G -javaagent:pia.jar -jar dacapo-9.12-bach.jar avrora -s default
