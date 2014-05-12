CGregx
================

Demand-Driven Call Graph construction. 

How to run?
Make sure you have ant and java installed.

First you need to generate queries for a specific benchmark:
ant gen -Dtarget=benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/kawa-c/classes/ -Dmain=kawa.repl -Dalg=cha

To run queries on a specific benchmark such as kawa-c, run:
ant -Dtarget=benchmarks/ashesSuiteCollection/suites/ashesJSuite/benchmarks/kawa-c/classes/ -Dmain=kawa.repl -Dalg=otf -Dquery=scripts/kawa_regx.txt
