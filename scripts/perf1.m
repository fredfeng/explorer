function perf1()
figure();
t = 30;
otf = [13.186; 4.533; 1.696; 1.517];
cha = [23.004; 8.945; 26.870; 2.872];

%mydata = [otf, cha];
bar_h=bar(cha, 0.5,'stack');
mycolor=[1 0.6 0.9; 0.4 1 0.8; 0.4 1 0.8; 0.4 1 0.8];
colormap(mycolor);

% bar_child=cell2mat(get(bar_h,'Children'));
% for i=1:size(bar_child,1)    
%     set(bar_child(i),'CData',mydata(:,i));
%     set(bar_child(i),'CDataMapping','direct');
% end
% set(bar_child(1), 'CData',[0,0.2,0.3,0.4]);
% set(bar_child(1), 'CData',[0,1,0,1]);

axis([0.5,4.5,0,t]);
% lable setting
xlabel('Benchmark','FontName','Times New Roman','FontSize',16);
ylabel('Run time (seconds)','FontName','Times New Roman','FontSize',16);
% legend setting
% legend('On-the-fly','CHA', 'Location','NorthEast');
legend('CHA', 'Location','NorthEast');

% axis setting
set(gca,'FontName','Times New Roman','FontSize',16);
% title setting
title('Run time for different benchmarks','FontName','Times New Roman','FontSize',18);
% gtext('21976.49');
% gtext('209.46');
set(gca,'XTickLabel',{'soot-j','rhino','kawa-c','schroeder'});
grid on;

