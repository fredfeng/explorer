function deadcode()
fig = figure();
cha = [
        1004,206,5 
596,188,4
     687,189,4
     728,188,3
     613,147,4
];
%mydata = [otf, cha];
bar_h=bar(cha);
mycolor=[0 0 1; 1 1 0; 1 0 0];
colormap(mycolor);


%axis([0.5,4.5,0,t]);
% lable setting
%xlabel('Benchmark','FontName','Times New Roman','FontSize',16);
ylabel('Run time (seconds)','FontName','Times New Roman','FontSize',16);

legend('1Obj', 'Explorer','CHA');

% axis setting
set(gca,'FontName','Times New Roman','FontSize',16);
% title setting
title('Run time for different benchmarks','FontName','Times New Roman','FontSize',18);
set(gca,'XTickLabel',{'pmd','antlr','lusearch','luindex','avrora'});
saveas(fig, 'deadcode.png');
