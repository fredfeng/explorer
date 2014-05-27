rm -r out/
rm -r ../out/pmd
rm -r ../sootified/pmd
cp -r ../benchmarks/pmd ../sootified/
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar pmd -s default 
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar pmd -s default 
java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar pmd -s default 
cp -r out/ ../out/pmd
java -Xmx4G -cp ../soot/soot-nightly.jar soot.Main -w -app -p cg.spark enabled -p cg reflection-log:../out/pmd/refl.log -cp ../lib/jce.jar:../lib/rt.jar:../lib/pmd-4.2.5.jar:../out/pmd -include org.apache -include org.w3c -main-class Harness -d ../sootified/pmd Harness 
#java -Xmx4G -javaagent:pia.jar -jar dacapo-9.12-bach.jar avrora -s default
