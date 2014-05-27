rm -r out/
rm -r ../out/avrora
rm -r ../sootified/avrora
cp -r ../benchmarks/avrora ../sootified/
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar avrora -s default 
#java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar avrora -s default 
java -Xmx4G -javaagent:../tamiflex/poa.jar -jar ../../dacapo-9.12-bach.jar avrora -s default 
cp -r out/ ../out/avrora
java -Xmx4G -cp ../soot/soot-nightly.jar soot.Main -w -app -p cg.spark enabled -p cg reflection-log:../out/avrora/refl.log -cp ../lib/jce.jar:../lib/rt.jar:../lib/commons-cli-1.2.jar:../lib/avrora-cvs-20091224.jar:../out/avrora -include org.apache. -include org.w3c. -include org.xml. -include java. -include javax. -main-class Harness -d ../sootified/avrora Harness 
#java -Xmx4G -javaagent:pia.jar -jar dacapo-9.12-bach.jar avrora -s default
