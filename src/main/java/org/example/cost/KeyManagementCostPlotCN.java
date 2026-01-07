package org.example.cost;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.None;

/**
 * KeyManagementCostPlotCN_1to500
 *
 * - N 范围：1..500，步长 5
 * - 输出 CSV 并绘制三张图（总成本、各构成、百分比节省）
 * - 在总成本图中尝试自动标注交叉点（若存在）
 *
 * 依赖：XChart (org.knowm.xchart:xchart:3.8.1)
 *
 * 编译（假设 xchart-3.8.1.jar 在当前目录）：
 * Windows:
 *   javac -cp .;xchart-3.8.1.jar KeyManagementCostPlotCN_1to500.java
 *   java -cp .;xchart-3.8.1.jar KeyManagementCostPlotCN_1to500
 * Linux / Mac:
 *   javac -cp .:xchart-3.8.1.jar KeyManagementCostPlotCN_1to500.java
 *   java -cp .:xchart-3.8.1.jar KeyManagementCostPlotCN_1to500
 */
public class KeyManagementCostPlotCN {

    // ========== 可调整的仿真参数（按需修改） ==========
    // 资源密文大小（字节），示例：10 MB
    static long resourceCipherSizeBytes = 10L * 1024 * 1024; // 10 MB

    // wrap_DEK 大小（字节）
    static int wrapDekSizeBytes = 512;

    // DEK 纯密钥大小（字节），用于估算（可不直接使用）
    static int dekSizeBytes = 32;

    // 存储/传输 单价（货币单位 / 字节）
    static double costStoragePerByte = 1e-8;   // 约 $0.01/GB
    static double costTransferPerByte = 2e-8;  // 示例

    // 密钥管理成本参数
    static double costPerKey = 5e-4;   // 方案 A 每接收方密钥管理边际成本
    static double costPerWrap = 2e-4;  // 方案 B 每个 wrap_DEK 的边际成本
    static double costKmsFixed = 0.1;  // 方案 B 固定 KMS 成本

    // N 范围与步长（关注 1..500）
    static int Nmin = 1;
    static int Nmax = 500;
    static int Nstep = 2;

    // 输出文件名
    static String csvFile = "comparison_results_1to500.csv";
    static String pngCosts = "cost_plot_1to500_costs.png";
    static String pngComponents = "cost_plot_1to500_components.png";
    static String pngReduction = "cost_plot_1to500_reduction.png";

    // 结果类
    public static class Result {
        public int N;
        public double storageABytes;
        public double storageBBytes;
        public double transferABytes;
        public double transferBBytes;
        public double storageCostA;
        public double storageCostB;
        public double transferCostA;
        public double transferCostB;
        public double keyMgmtCostA;
        public double keyMgmtCostB;
        public double costA;
        public double costB;
        public double percentReduction;
    }

    // 仿真
    public static Result simulate(int N) {
        Result r = new Result();
        r.N = N;

        // 存储与传输字节量
        r.storageABytes = (double) N * resourceCipherSizeBytes;
        r.transferABytes = (double) N * resourceCipherSizeBytes;

        r.storageBBytes = resourceCipherSizeBytes + (double) N * wrapDekSizeBytes;
        r.transferBBytes = (double) N * wrapDekSizeBytes;

        // 成本计算
        r.storageCostA = r.storageABytes * costStoragePerByte;
        r.storageCostB = r.storageBBytes * costStoragePerByte;

        r.transferCostA = r.transferABytes * costTransferPerByte;
        r.transferCostB = r.transferBBytes * costTransferPerByte;

        r.keyMgmtCostA = costPerKey * N;
        r.keyMgmtCostB = costKmsFixed + costPerWrap * N;

        r.costA = r.storageCostA + r.transferCostA + r.keyMgmtCostA;
        r.costB = r.storageCostB + r.transferCostB + r.keyMgmtCostB;

        if (r.costA <= 0.0) {
            r.percentReduction = 0.0;
        } else {
            r.percentReduction = (r.costA - r.costB) / r.costA * 100.0;
        }
        return r;
    }

    // 字节可读化
    public static String humanReadableBytes(double bytes) {
        String[] units = {"B","KB","MB","GB","TB"};
        double b = bytes;
        int i = 0;
        while (b >= 1024.0 && i < units.length-1) { b /= 1024.0; i++; }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(b) + " " + units[i];
    }

    // 在 costA 与 costB 的数组中寻找第一个交叉点（costA > costB 变为 costA <= costB 或反向）
    // 返回交叉点的近似 N（插值），若无交叉则返回 -1
    public static double findCrossingN(List<Integer> Ns, List<Double> costA, List<Double> costB) {
        for (int i = 1; i < Ns.size(); i++) {
            double a0 = costA.get(i-1), a1 = costA.get(i);
            double b0 = costB.get(i-1), b1 = costB.get(i);
            // 检查区间内是否有交叉（符号变化）
            double d0 = a0 - b0;
            double d1 = a1 - b1;
            if (d0 == 0.0) {
                return Ns.get(i-1);
            }
            if (d0 * d1 <= 0.0) { // 存在或接近交叉
                // 线性插值求近似交叉 N
                double t = (0.0 - d0) / (d1 - d0 + 1e-15);
                double n0 = Ns.get(i-1);
                double n1 = Ns.get(i);
                return n0 + t * (n1 - n0);
            }
        }
        return -1.0;
    }

    public static void main(String[] args) throws IOException {
        List<Integer> Ns = new ArrayList<>();
        for (int n = Nmin; n <= Nmax; n += Nstep) {
            Ns.add(n);
        }
        if (!Ns.contains(Nmax)) {
            Ns.add(Nmax);
        }

        // 收集数据
        List<Double> xData = new ArrayList<>();
        List<Double> costAData = new ArrayList<>();
        List<Double> costBData = new ArrayList<>();
        List<Double> reductionData = new ArrayList<>();

        // 各构成成本数据
        List<Double> storageCostAData = new ArrayList<>();
        List<Double> storageCostBData = new ArrayList<>();
        List<Double> transferCostAData = new ArrayList<>();
        List<Double> transferCostBData = new ArrayList<>();
        List<Double> keyMgmtCostAData = new ArrayList<>();
        List<Double> keyMgmtCostBData = new ArrayList<>();

        System.out.println("仿真参数：");
        System.out.println("资源密文大小 = " + humanReadableBytes(resourceCipherSizeBytes));
        System.out.println("wrap_DEK 大小 = " + wrapDekSizeBytes + " bytes");
        System.out.println("存储单价 = " + costStoragePerByte + " /byte");
        System.out.println("传输单价 = " + costTransferPerByte + " /byte");
        System.out.println("costPerKey = " + costPerKey + ", costPerWrap = " + costPerWrap + ", costKmsFixed = " + costKmsFixed);
        System.out.println("N 范围: " + Nmin + " .. " + Nmax + " (step " + Nstep + ")");
        System.out.println();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            writer.write("N,storageABytes,storageBBytes,transferABytes,transferBBytes,storageCostA,storageCostB,transferCostA,transferCostB,keyMgmtCostA,keyMgmtCostB,costA,costB,percentReduction\n");

            DecimalFormat df = new DecimalFormat("#0.00000");

            for (int n : Ns) {
                Result r = simulate(n);
                writer.write(String.format("%d,%.0f,%.0f,%.0f,%.0f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.6f\n",
                        r.N,
                        r.storageABytes,
                        r.storageBBytes,
                        r.transferABytes,
                        r.transferBBytes,
                        r.storageCostA,
                        r.storageCostB,
                        r.transferCostA,
                        r.transferCostB,
                        r.keyMgmtCostA,
                        r.keyMgmtCostB,
                        r.costA,
                        r.costB,
                        r.percentReduction
                ));

                System.out.printf("N=%4d | 存储A=%10s 存储B=%10s | 成本A=%10s 成本B=%10s 降幅=%7s%%\n",
                        r.N,
                        humanReadableBytes(r.storageABytes),
                        humanReadableBytes(r.storageBBytes),
                        df.format(r.costA),
                        df.format(r.costB),
                        new DecimalFormat("#0.00").format(r.percentReduction));

                // push data
                xData.add((double) n);
                costAData.add(r.costA);
                costBData.add(r.costB);
                reductionData.add(r.percentReduction);

                storageCostAData.add(r.storageCostA);
                storageCostBData.add(r.storageCostB);
                transferCostAData.add(r.transferCostA);
                transferCostBData.add(r.transferCostB);
                keyMgmtCostAData.add(r.keyMgmtCostA);
                keyMgmtCostBData.add(r.keyMgmtCostB);
            }
            writer.flush();
            System.out.println("\nCSV 已写入：" + csvFile);
        }

        // 找交叉点
        List<Integer> NsInt = new ArrayList<>(Ns);
        double crossingN = findCrossingN(NsInt, costAData, costBData);
        if (crossingN > 0) {
            System.out.println(String.format("检测到交叉点（近似）：N ≈ %.3f", crossingN));
        } else {
            System.out.println("未检测到交叉点（在给定范围内）。");
        }

        // ---------- 绘制总成本图（含交叉点标注） ----------
        XYChart chartCosts = new XYChartBuilder()
                .width(1200).height(800)
                .title("方案总成本对比（N=1..500）")
                .xAxisTitle("接收方数量 N")
                .yAxisTitle("成本（单位）")
                .build();

        // 样式
        chartCosts.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chartCosts.getStyler().setChartTitleFont(new Font("SansSerif", Font.BOLD, 14));
        chartCosts.getStyler().setLegendFont(new Font("SansSerif", Font.PLAIN, 12));
        chartCosts.getStyler().setMarkerSize(6);

        chartCosts.addSeries("方案 A 总成本", xData, costAData).setMarker(new None()).setLineColor(Color.RED).setLineStyle(new BasicStroke(2.0f));
        chartCosts.addSeries("方案 B 总成本", xData, costBData).setMarker(new None()).setLineColor(Color.BLUE).setLineStyle(new BasicStroke(2.0f));

        // 在图上用注释（添加一条点）标出交叉点（若存在）
        if (crossingN > 0) {
            // 线性插值得到对应成本值
            double costAtCross = interpolateAt(crossingN, NsInt, costAData);
            // 将交叉点用一个小 marker 表示（XChart 不直接支持单点大注释，这里添加一个短小系列作为标记）
            List<Double> xs = new ArrayList<>(); xs.add(crossingN);
            List<Double> ys = new ArrayList<>(); ys.add(costAtCross);
            chartCosts.addSeries(String.format("交叉点 N≈%.1f", crossingN), xs, ys)
                    .setMarker(org.knowm.xchart.style.markers.SeriesMarkers.CIRCLE)
                    .setLineStyle(new BasicStroke(0.0f));
        }

        // 保存总成本图
        BitmapEncoder.saveBitmap(chartCosts, pngCosts, BitmapFormat.PNG);
        System.out.println("已保存：" + pngCosts);

        // ---------- 绘制各构成成本拆分图 ----------
        XYChart chartComp = new XYChartBuilder()
                .width(1400).height(900)
                .title("成本构成拆分（存储 / 传输 / 密钥管理）")
                .xAxisTitle("接收方数量 N")
                .yAxisTitle("成本（单位）")
                .build();
        chartComp.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chartComp.getStyler().setMarkerSize(6);

        chartComp.addSeries("存储成本 A", xData, storageCostAData).setMarker(new None()).setLineColor(new Color(200,50,50));
        chartComp.addSeries("存储成本 B", xData, storageCostBData).setMarker(new None()).setLineColor(new Color(200,150,50));
        chartComp.addSeries("传输成本 A", xData, transferCostAData).setMarker(new None()).setLineColor(new Color(50,100,200));
        chartComp.addSeries("传输成本 B", xData, transferCostBData).setMarker(new None()).setLineColor(new Color(50,200,150));
        chartComp.addSeries("密钥管理成本 A", xData, keyMgmtCostAData).setMarker(new None()).setLineColor(new Color(120,120,120));
        chartComp.addSeries("密钥管理成本 B", xData, keyMgmtCostBData).setMarker(new None()).setLineColor(new Color(100,0,200));

        BitmapEncoder.saveBitmap(chartComp, pngComponents, BitmapFormat.PNG);
        System.out.println("已保存：" + pngComponents);

        // ---------- 绘制百分比节省图 ----------
        XYChart chartReduction = new XYChartBuilder()
                .width(1200).height(800)
                .title("节省百分比（从 方案A 到 方案B）")
                .xAxisTitle("接收方数量 N")
                .yAxisTitle("节省百分比 (%)")
                .build();
        chartReduction.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chartReduction.getStyler().setMarkerSize(6);

        chartReduction.addSeries("节省百分比", xData, reductionData).setMarker(new None()).setLineColor(new Color(34,139,34));

        BitmapEncoder.saveBitmap(chartReduction, pngReduction, BitmapFormat.PNG);
        System.out.println("已保存：" + pngReduction);

        System.out.println("完成。请查看 CSV 和生成的 PNG 图像。若需我进一步调整参数、改变步长或在图上增加中文注释文本位置，请告诉我。");
    }

    // 插值函数：在给定 N 列表与值列表中，在浮点位置 n 找出插值值
    public static double interpolateAt(double n, List<Integer> Ns, List<Double> values) {
        if (n <= Ns.get(0)) {
            return values.get(0);
        }
        int last = Ns.size() - 1;
        if (n >= Ns.get(last)) {
            return values.get(last);
        }
        for (int i = 1; i < Ns.size(); i++) {
            double n0 = Ns.get(i-1);
            double n1 = Ns.get(i);
            if (n >= n0 && n <= n1) {
                double v0 = values.get(i-1);
                double v1 = values.get(i);
                double t = (n - n0) / (n1 - n0 + 1e-15);
                return v0 + t * (v1 - v0);
            }
        }
        return values.get(values.size()-1);
    }
}
