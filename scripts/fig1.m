function fig1()
figure();
% x axis
x = 0:2000:28000;
% On-the-fly call graph building time
y11 = 219769.49;
% CHA-based call graph building time
y22 = 209.46;
% on-the-fly query run time
y1 = [y11,y11+[49679.84, 108843.73, 161426.91, 223236.32, 268476.99, 329687.66, 379081.87, 429480.40, 483377.86, 528793.85, 574480.92, 620197.36, 669638.67, 722855.83]];
% CHA query run time
y2 = [y22,y22+[56835.45, 117715.87, 172397.92, 226781.94, 279565.76, 332969.94, 387476.22, 440020.33, 515302.10, 585566.67, 636273.02, 686812.49, 739773.14, 825002.52]];

for i = 1:1:1
    plot(x(1,i),y1(1,i),'d-r','MarkerSize', 10, 'MarkerFaceColor', 'r', 'LineWidth', 1.01, 'MarkerFaceColor', 'w');
    hold on;
    plot(x(1,i),y2(1,i),'s:b','MarkerSize', 10, 'MarkerFaceColor', 'b','LineWidth', 1.1, 'MarkerFaceColor', 'w');
end

str=['2.2'];
text(-1000,250000,str);


plot(x,y1,'d-r','MarkerSize', 10, 'MarkerFaceColor', 'r', 'LineWidth', 1.01, 'MarkerFaceColor', 'w');
hold on;
plot(x,y2,'s:b','MarkerSize', 10, 'MarkerFaceColor', 'b','LineWidth', 1.1, 'MarkerFaceColor', 'w');

% lable setting
xlabel('Number of queries','FontName','Times New Roman','FontSize',14);
ylabel('Run time (ms)','FontName','Times New Roman','FontSize',14);
% legend setting
legend('On-the-fly','CHA', 'Location','NorthWest');
% axis setting
set(gca,'FontName','Times New Roman','FontSize',10);
% title setting
title('Runtime for queries','FontName','Times New Roman','FontSize',18);
% gtext('21976.49');
% gtext('209.46');
grid on;