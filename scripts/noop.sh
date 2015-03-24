#echo "analyzing-----luindex"
#ant observe -Dtarget.name=luindex
#echo "analyzing-----lusearch"
#ant observe -Dtarget.name=lusearch
#echo "analyzing-----antlr"
#ant observe -Dtarget.name=antlr
#echo "analyzing-----avrora"
#ant observe -Dtarget.name=avrora
#echo "analyzing-----pmd"
#ant observe -Dtarget.name=pmd
#echo "analyzing-----chart"
#ant observe -Dtarget.name=chart
#echo "analyzing-----bloat"
#ant observe -Dtarget.name=bloat
#echo "analyzing-----xalan"
#ant observe -Dtarget.name=xalan
#echo "analyzing-----hsqldb"
#ant observe -Dtarget.name=hsqldb
echo "analyzing-----batik"
ant observe -Dtarget.name=batik -Dtarget.type=NOOPT
echo "analyzing-----sunflow"
ant observe -Dtarget.name=sunflow -Dtarget.type=NOOPT
echo "analyzing-----fop"
ant observe -Dtarget.name=fop -Dtarget.type=NOOPT
echo "analyzing-----weka"
ant observe -Dtarget.name=weka -Dtarget.type=NOOPT
echo "analyzing-----jmeter"
ant observe -Dtarget.name=jmeter -Dtarget.type=NOOPT
