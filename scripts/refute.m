function refute()
fig = figure();
cha = [
        19,10,0 
        11,8,0
        49,42,0
        30,17,0
        76,75,0
];
%mydata = [otf, cha];
bar_h=bar(cha);
mycolor=[0 0 1; 1 1 0; 1 0 0];
colormap(mycolor);


%axis([0.5,4.5,0,t]);
% lable setting
%xlabel('Benchmark','FontName','Times New Roman','FontSize',16);
ylabel('Number of Queries','FontName','Times New Roman','FontSize',16);

legend('1Obj', 'Explorer','CHA','Location','northwest');

% axis setting
set(gca,'FontName','Times New Roman','FontSize',16);
% title setting
title('Number of refutations on different benchmarks','FontName','Times New Roman','FontSize',18);
set(gca,'XTickLabel',{'pmd','antlr','lusearch','luindex','avrora'});
saveas(fig, 'refute.png');
