function precision()
t = 100;
cha = [63;64;22;44];
otf = [t;t;t;t] - cha;
mydata = [cha, otf];
bar_h=bar(mydata, 0.5,'stack');
mycolor=[1 0.6 1; 0.4 1 0.8; 0.4 1 0.8; 0.4 1 0.8];
colormap(mycolor);
bar_child=cell2mat(get(bar_h,'Children'));
for i=1:size(bar_child,1)    
    set(bar_child(i),'CData',mydata(:,i));
    set(bar_child(i),'CDataMapping','direct');
end
set(bar_child(1), 'CData',[0,0.2,0.3,0.4]);

axis([0.5,4.5,0,t + 10]);
% lable setting
xlabel('Benchmark','FontName','Times New Roman','FontSize',16);
ylabel('Number of Yes Answers','FontName','Times New Roman','FontSize',16);
% legend setting
legend('on-the-fly','CGregx', 'Location','SouthWest');
% axis setting
set(gca,'FontName','Times New Roman','FontSize',16);
% title setting
title('Precision','FontName','Times New Roman','FontSize',18);
% gtext('21976.49');
% gtext('209.46');
set(gca,'XTickLabel',{'soot-j','rhino','kawa-c','schroeder'});
grid on;

% x = 0.96;
% text(x,t,'x','FontSize',25);
% text(1+x,t,'x','FontSize',25);
% text(2+x,t,'x','FontSize',25);
% text(3+x,t,'x','FontSize',25);
% str = 'inf';
% text(0.34,t,str,'FontName','Times New Roman','FontSize',16);
