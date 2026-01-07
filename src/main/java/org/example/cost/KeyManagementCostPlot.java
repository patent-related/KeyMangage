package org.example.cost;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.markers.None;
import org.knowm.xchart.style.Styler.LegendPosition;

/**
 * KeyManagementCostPlot
 *
 * 1) 进行仿真（比较方案 A 与 方案 B 的存储/传输/总成本）
 * 2) 输出 CSV comparison_results.csv
 * 3) 绘制并保存 PNG comparison_plot.png（包含 CostA、CostB 和 PercentReduction 曲线）
 *
 * 依赖：XChart (org.knowm.xchart:xchart:3.8.1)
 *
 * 编译示例（假设 xchart-3.8.1.jar 与本文件在同一目录）：
 * javac -cp .;xchart-3.8.1.jar KeyManagementCostPlot.java   (Windows)
 * javac -cp .:xchart-3.8.1.jar KeyManagementCostPlot.java   (Linux/Mac)
 *
 * 运行示例：
 * java -cp .;xchart-3.8.1.jar KeyManagementCostPlot   (Windows)
 * java -cp .:xchart-3.8.1.jar KeyManagementCostPlot   (Linux/Mac)
 */
public class KeyManagementCostPlot {

    // 可调参数（单位：字节或货币单位）
    static long resourceCipherSizeBytes = 10L * 1024 * 1024; // 10 MB
    static int dekSizeBytes = 32; // 32 bytes
    static int wrapDekSizeBytes = 512; // wrap_DEK 包含元数据与签名，示例 512 bytes

    // 成本模型参数（可按实际情况调整）
    static double costPerKey = 0.001; // 方案 A 每个密钥管理成本 (示例单位)
    static double costPerWrap = 0.0005; // 方案 B 每个 wrap_DEK 管理成本
    static double costKmsFixed = 1.0; // 方案 B 固定 KMS 成本
    static double costStoragePerByte = 0.00000001; // 存储单位成本
    static double costTransferPerByte = 0.00000002; // 传输单位成本

    // N 列表：可以自定义为等比或等差序列
    static List<Integer> defaultNs = Arrays.asList(1, 5, 10, 50, 100, 500, 1000, 5000, 10000);

    public static class Result {
        public int N;
        public double storageABytes;
        public double storageBBytes;
        public double transferABytes;
        public double transferBBytes;
        public double costA;
        public double costB;
        public double percentReduction; // X%
    }

    public static Result simulate(int N) {
        Result r = new Result();
        r.N = N;

        // 方案 A（每个接收方保存一份密文）
        r.storageABytes = (double) N * resourceCipherSizeBytes;
        r.transferABytes = (double) N * resourceCipherSizeBytes;

        // 方案 B（单密文 + per-recipient wrap_DEK）
        r.storageBBytes = resourceCipherSizeBytes + (double) N * wrapDekSizeBytes;
        r.transferBBytes = (double) N * wrapDekSizeBytes;

        double storageCostA = r.storageABytes * costStoragePerByte;
        double storageCostB = r.storageBBytes * costStoragePerByte;

        double transferCostA = r.transferABytes * costTransferPerByte;
        double transferCostB = r.transferBBytes * costTransferPerByte;

        double keyMgmtCostA = costPerKey * N;
        double keyMgmtCostB = costKmsFixed + costPerWrap * N;

        r.costA = storageCostA + transferCostA + keyMgmtCostA;
        r.costB = storageCostB + transferCostB + keyMgmtCostB;

        if (r.costA <= 0.0) {
            r.percentReduction = 0.0;
        } else {
            r.percentReduction = (r.costA - r.costB) / r.costA * 100.0;
        }
        return r;
    }

    public static String humanReadableBytes(double bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double b = bytes;
        int i = 0;
        while (b >= 1024.0 && i < units.length - 1) {
            b /= 1024.0;
            i++;
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(b) + " " + units[i];
    }

    public static void main(String[] args) throws IOException {
        List<Integer> Ns = new ArrayList<>(defaultNs);

        // 如果需要更密集的点可生成等间隔序列（例如 1..10000 的某些步长）
        // 下面示例向序列中加入更多点（可根据需要打开）
        for (int i = 20000; i <= 100000; i += 20000) {
            Ns.add(i);
        }

        System.out.println("仿真参数：");
        System.out.println("资源密文大小 = " + humanReadableBytes(resourceCipherSizeBytes));
        System.out.println("wrap_DEK 大小 = " + wrapDekSizeBytes + " bytes");
        System.out.println("costPerKey = " + costPerKey + ", costPerWrap = " + costPerWrap + ", costKmsFixed = " + costKmsFixed);
        System.out.println("costStoragePerByte = " + costStoragePerByte + ", costTransferPerByte = " + costTransferPerByte);
        System.out.println();

        System.out.printf("%8s | %12s | %12s | %12s | %12s | %12s | %12s | %8s\n",
                "N", "StorA", "StorB", "TransA", "TransB", "CostA", "CostB", "Red%");

        DecimalFormat cf = new DecimalFormat("#0.000");

        // 结果集合，用于绘图
        List<Double> xData = new ArrayList<>();
        List<Double> costAData = new ArrayList<>();
        List<Double> costBData = new ArrayList<>();
        List<Double> reductionData = new ArrayList<>();

        // 写 CSV
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("comparison_results.csv"))) {
            writer.write("N,storageABytes,storageBBytes,transferABytes,transferBBytes,costA,costB,percentReduction\n");

            for (int n : Ns) {
                Result res = simulate(n);
                System.out.printf("%8d | %12s | %12s | %12s | %12s | %12s | %12s | %7s\n",
                        n,
                        humanReadableBytes(res.storageABytes),
                        humanReadableBytes(res.storageBBytes),
                        humanReadableBytes(res.transferABytes),
                        humanReadableBytes(res.transferBBytes),
                        cf.format(res.costA),
                        cf.format(res.costB),
                        cf.format(res.percentReduction) + "%");

                writer.write(String.format("%d,%.0f,%.0f,%.0f,%.0f,%.6f,%.6f,%.4f\n",
                        res.N,
                        res.storageABytes,
                        res.storageBBytes,
                        res.transferABytes,
                        res.transferBBytes,
                        res.costA,
                        res.costB,
                        res.percentReduction
                ));

                // push to plot arrays
                xData.add((double) n);
                costAData.add(res.costA);
                costBData.add(res.costB);
                reductionData.add(res.percentReduction);
            }
            writer.flush();
            System.out.println("\nCSV 已写入: comparison_results.csv");
        }

        // 使用 XChart 绘图：CostA vs CostB（主 Y 轴），PercentReduction（次 Y 轴）
        XYChart chart = new XYChartBuilder()
                .width(1200).height(800)
                .title("Cost Comparison: Scheme A vs Scheme B")
                .xAxisTitle("Number of recipients (N)")
                .yAxisTitle("Cost (units)")
                .build();

        chart.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chart.getStyler().setMarkerSize(6);

        chart.addSeries("Cost A", xData, costAData).setMarker(new None());
        chart.addSeries("Cost B", xData, costBData).setMarker(new None());

        // second axis for percent reduction: build a second chart overlay
        XYChart chart2 = new XYChartBuilder()
                .width(1200).height(800)
                .title("Percent Reduction vs N")
                .xAxisTitle("Number of recipients (N)")
                .yAxisTitle("Percent Reduction (%)")
                .build();
        chart2.getStyler().setLegendVisible(false);
        chart2.addSeries("PercentReduction", xData, reductionData).setMarker(new None());

        // Save figures
        String pngFile1 = "comparison_costs.png";
        String pngFile2 = "comparison_reduction.png";

        BitmapEncoder.saveBitmap(chart, pngFile1, BitmapFormat.PNG);
        BitmapEncoder.saveBitmap(chart2, pngFile2, BitmapFormat.PNG);

        System.out.println("图像已保存: " + pngFile1 + " , " + pngFile2);
    }
}
