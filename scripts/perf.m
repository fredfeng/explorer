function perf()
figure();
% x axis
x = 0:500:2000;
% On-the-fly call graph building time
sootjCHA = 0.733;
% CHA-based call graph building time
sootjOTF = 233.594;
% on-the-fly query run time
sootjCHA1 = [sootjCHA,sootjCHA+[127.298, 257.398, 392.674, 513.794]];
% CHA query run time
sootjOTF1 = [sootjOTF,sootjOTF+[81.986, 159.422, 235.433, 310.181]];


rhinoCHA = 0.587;
rhinoOTF = 59.278;
rhinoCHA1 = [rhinoCHA,rhinoCHA+[46.343, 92.933, 135.421, 175.725]];
rhinoOTF1 = [rhinoOTF,rhinoOTF+[21.937, 40.945, 58.979, 77.425]];


plot(x,sootjCHA1,'d-r','MarkerSize', 10, 'MarkerFaceColor', 'r', 'LineWidth', 1.01, 'MarkerFaceColor', 'w');
hold on;
plot(x,sootjOTF1,'s-b','MarkerSize', 10, 'MarkerFaceColor', 'b','LineWidth', 1.1, 'MarkerFaceColor', 'w');
hold on;
plot(x,rhinoCHA1,'+-g','MarkerSize', 10, 'MarkerFaceColor', 'g', 'LineWidth', 1.01, 'MarkerFaceColor', 'w');
hold on;
plot(x,rhinoOTF1,'*-m','MarkerSize', 10, 'MarkerFaceColor', 'm', 'LineWidth', 1.01, 'MarkerFaceColor', 'w');
hold on;

axis([-10,2010,0,600]);
% lable setting
xlabel('Number of queries','FontName','Times New Roman','FontSize',14);
ylabel('Run time (seconds)','FontName','Times New Roman','FontSize',14);
% legend setting
legend('soot-j CHA', 'soot-j On-the-fly', 'rhino CHA', 'rhino On-the-fly', 'Location','NorthWest');
% axis setting
set(gca,'FontName','Times New Roman','FontSize',10);
% title setting
title('Run time for different number of queries','FontName','Times New Roman','FontSize',18);
% gtext('21976.49');
% gtext('209.46');
grid on;