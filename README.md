CGregx
================

Demand-Driven Call Graph construction. 

How to run?
Make sure you have ant and java installed.

First you need to generate queries for a specific benchmark:
ant gen -Dtarget=test/soot-j/ -Dmain=ca.mcgill.sable.soot.jimple.Main -Dalg=cha

To run queries on a specific benchmark such as kawa-c, run:
ant -Dtarget=test/soot-j/ -Dmain=ca.mcgill.sable.soot.jimple.Main -Dalg=otf -Dquery=test/sootj_regx.txt
