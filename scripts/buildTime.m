function buildTime()

cha = [2.24;2.25;8.194;8.445];
otf = [20;20;20;20] - cha;
mydata = [cha, otf];
bar_h=bar(mydata, 0.5,'stack');
mycolor=[1 0.6 1; 1 1 1; 1 1 1; 0.4 1 0.8];
colormap(mycolor);
bar_child=cell2mat(get(bar_h,'Children'));
for i=1:size(bar_child,1)    
    set(bar_child(i),'CData',mydata(:,i));
    set(bar_child(i),'CDataMapping','direct');
end
set(bar_child(1), 'CData',[0,0.2,0.3,0.4]);
set(bar_child(1), 'CData',[0,1,0,1]);


% lable setting
xlabel('Benchmark','FontName','Times New Roman','FontSize',16);
ylabel('Run time (second)','FontName','Times New Roman','FontSize',16);
% legend setting
legend('CHA','On-the-fly', 'Location','West');
% axis setting
set(gca,'FontName','Times New Roman','FontSize',16);
% title setting
title('Time to build call graph (including library)','FontName','Times New Roman','FontSize',18);
% gtext('21976.49');
% gtext('209.46');
set(gca,'XTickLabel',{'soot-j','rhino','kawa-c','schroeder'});
grid on;

x = 0.96;
text(x,20,'x','FontSize',25);
text(1+x,20,'x','FontSize',25);
text(2+x,20,'x','FontSize',25);
text(3+x,20,'x','FontSize',25);
