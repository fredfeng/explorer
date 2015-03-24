echo "Begin to run icc experiment."

for entry in /home/yufeng/research/benchmarks/icc/*
do
    echo Analyzing------------ $entry
  ./stamp analyze "$entry"
done

#echo Analyzing------------ "facebook"
#./stamp analyze /home/yufeng/research/benchmarks/icc/com.facebook.katana.apk

#echo Analyzing------------ "pinterest"
#./stamp analyze /home/yufeng/research/benchmarks/icc/com.pinterest.apk

for entry in /home/yufeng/research/benchmarks/icc-ext/*
do
    echo Analyzing------------ $entry
  ./stamp analyze "$entry"
done


