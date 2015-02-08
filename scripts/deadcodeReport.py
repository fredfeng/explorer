import ConfigParser
import os
import sys
from subprocess import PIPE, Popen
from sets import Set

global str

benName = ''

def main():
    print "begin generate report for deadcode experiment"
    cgLoc = '/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/' + benName + '/cgoutput.txt'
    chordLoc = '/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/' + benName + '/chord_output/reachableCM.txt'
    assert os.path.exists(cgLoc)
    assert os.path.exists(chordLoc)
    chordSet = []
    cgYesSet = []
    cgNoSet = []
    with open(chordLoc) as f:
        for line in f:
            if len(line) == 1:
                continue

            mystr = line.split(',')[1].replace('\n','')
            if not mystr in chordSet:
                chordSet.append(mystr)

    with open(cgLoc) as f:
        for line in f:
            if 'yesreach:' in line:
                line = line.split('.*')[1].replace('\n','')
                cgYesSet.append(line)

            if 'unreach:' in line:
                line = line.split('.*')[1].replace('\n','')
                cgNoSet.append(line)
        
    ## check soundness in noSet
    unsound = 0
    for no in cgNoSet:
        arr = no.split(' ')
        assert len(arr) == 3
        clzName = arr[0]
        clzName = clzName[1:-1]
        methName = arr[2]
        methName = methName.split('(')[0]
        #print clzName + ' ' + methName

        for chord in chordSet:
            #print chord
            arr = chord.split('@')
            assert len(arr) == 2
            chordClz = arr[1]
            chordClz = chordClz[:-1]
            chordMeth = arr[0]
            chordMeth = chordMeth.split(':')[0]
            if (clzName == chordClz) and (methName == chordMeth):
                unsound = unsound + 1
                print 'maybe unsound:' + no
                break

    ## check precision in yesSet
    imprecise = 0
    for yes in cgYesSet:
        arr = yes.split(' ')
        assert len(arr) == 3
        clzName = arr[0]
        clzName = clzName[1:-1]
        methName = arr[2]
        methName = methName.split('(')[0]
        flag = True
        #print clzName + ' ' + methName

        for chord in chordSet:
            #print chord
            arr = chord.split('@')
            assert len(arr) == 2
            chordClz = arr[1]
            chordClz = chordClz[:-1]
            chordMeth = arr[0]
            chordMeth = chordMeth.split(':')[0]
            if (clzName == chordClz) and (methName == chordMeth):
                flag = False
                break

        if flag:
            imprecise = imprecise + 1


    print '------------------------------------------'
    print 'Summary information for ' + benName
    print 'unsound: ' + str(unsound) + ' out of ' + str(len(cgNoSet))
    print 'precise: ' + str(imprecise) + ' out of ' + str(len(cgYesSet))
    with open(cgLoc) as f:
        for line in f:
            if 'Total' in line:
                line = line.replace('\n','')
                print line


if __name__ == "__main__":
    benName = sys.argv[1]
    print benName
    main()

