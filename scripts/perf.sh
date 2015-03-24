echo "Begin to run performance detection."
for entry in /home/yufeng/research/benchmarks/perf/*
do
  echo Analyzing------------ "$entry"
  ./stamp analyze "$entry"
done

echo Analyzing-------------- MyTracks
./stamp analyze /home/yufeng/research/benchmarks/oopsla15/mytracks/MyTracks

