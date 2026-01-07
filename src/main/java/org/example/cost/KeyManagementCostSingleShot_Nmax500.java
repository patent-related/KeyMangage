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
 * KeyManagementCostSingleShot_Nmax500
 *
 * 单次视角成本模型（Nmax = 500），更精确的轮换建模：
 * - 单次（single-shot）视角：只计算一次分发/生成/传输/存储的成本
 * - 轮换通过 rotationCount 精确建模：totalCalls = baseCalls * (1 + rotationCount) * (1 + retryRate)
 * - 可配置轮换时是否重新加密资源密文（rotateResourceCipherOnRotation）
 * - 支持在轮换时仅重新生成 wrap（rotateWrapOnly）
 *
 * N 范围：1..500，步长 5（可调整）
 *
 * 依赖：XChart (org.knowm.xchart:xchart:3.8.1)
 *
 * 编译（假设 xchart-3.8.1.jar 在当前目录）：
 * Windows:
 *   javac -cp .;xchart-3.8.1.jar KeyManagementCostSingleShot_Nmax500.java
 *   java -cp .;xchart-3.8.1.jar KeyManagementCostSingleShot_Nmax500
 * Linux / Mac:
 *   javac -cp .:xchart-3.8.1.jar KeyManagementCostSingleShot_Nmax500.java
 *   java -cp .:xchart-3.8.1.jar KeyManagementCostSingleShot_Nmax500
 */
public class KeyManagementCostSingleShot_Nmax500 {

    // ========== 基本数据与成本参数（可按需调整） ==========
    // 资源密文大小（字节）
    static long resourceCipherSizeBytes = 10L * 1024 * 1024; // 10 MB

    // wrap_DEK 大小（字节）
    static int wrapDekSizeBytes = 512;

    // 存储/传输 单价（货币单位 / 字节）
    static double costStoragePerByte = 1e-8;   // 约 $0.01/GB
    static double costTransferPerByte = 2e-8;  // 示例

    // 原有密钥管理成本参数（后续管理边际费）
    static double costPerKeyMgmtA = 5e-4;   // 方案 A：每接收方的密钥管理边际成本（不含生成费用）
    static double costPerWrapMgmtB = 2e-4;  // 方案 B：每个 wrap 的后续管理边际成本（不含生成费用）
    static double costKmsFixed = 0.1;       // 方案 B 固定 KMS 成本（一次性/按本次摊入）

    // 新增：wrap 生成 / 调用 / 计算成本（每次）
    static double costPerWrapGenerate = 0.0002; // 每次生成 wrap 的 API/计算费用（示例）

    // 新增：方案 A 中每个接收方生成完整密文/分发的生成费用（例如每个接收方单独加密调用）
    static double costPerKeyGenerateA = 0.0005; // 每个接收方生成/分发 DEK 的生成费用（示例）

    // 轮换、重试、审计、备份、运维（单次视角）
    static double rotationCount = 0.0;     // 轮换次数（在本次生命周期内额外执行的完全轮换次数，0 表示不轮换）
    static boolean rotateResourceCipherOnRotation = false; // 轮换时是否需要重新加密资源密文本身
    static boolean rotateWrapOnly = true;  // 若 true 则轮换仅重新生成 wrap（默认）；若 false 且 rotateResourceCipherOnRotation=true，则需重新加密资源
    static double retryRate = 0.05;        // 重试率（5%）
    static double auditBytesPerWrap = 200; // 每次 wrap 产生的审计日志字节数
    static double backupFactor = 1.0;      // 备份份数（例如 1 表示再保留一份副本）
    static double opsOneTimeCost = 100.0;  // 一次性的开发/集成/运维费用（本次分摊）
    // 若想按 N 分摊可在 simulate 中除以 N 或按方案分配

    // 可选：预期泄露损失参数（用于风险成本估计）
    static double breachProbabilityPerShot = 0.0; // 单次事件视角下的泄露概率示例（通常难以量化）
    static double lossPerBreach = 10000.0;        // 每次泄露预计损失（货币单位）

    // N 范围与步长（Nmax = 500）
    static int Nmin = 1;
    static int Nmax = 500;
    static int Nstep = 5;

    // 输出文件
    static String csvFile = "single_shot_comparison_results_1to500.csv";
    static String pngCosts = "single_shot_cost_plot_1to500_costs.png";
    static String pngComponents = "single_shot_cost_plot_1to500_components.png";
    static String pngReduction = "single_shot_cost_plot_1to500_reduction.png";

    // 结果容器
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
        public double auditCostA;
        public double auditCostB;
        public double opsCostA;
        public double opsCostB;
        public double expectedBreachCostA;
        public double expectedBreachCostB;
        public double totalCostA;
        public double totalCostB;
        public double percentReduction;
    }

    // 模拟计算（单次视角，精确轮换建模）
    public static Result simulate(int N) {
        Result r = new Result();
        r.N = N;

        // ========== 字节量（考虑备份） ==========
        // 方案 A：为每个接收方存储一份资源密文（N 份），每份可能有备份
        r.storageABytes = (double) N * resourceCipherSizeBytes * (1.0 + backupFactor);

        // 方案 B：仅存资源密文 1 份（含备份） + 每个接收方一个 wrap（wrap 也可能备份）
        r.storageBBytes = resourceCipherSizeBytes * (1.0 + backupFactor) + (double) N * wrapDekSizeBytes * (1.0 + backupFactor);

        // ========== 传输字节量（单次分发 + 轮换导致的额外传输） ==========
        // 方案 A:
        // 初次分发：N * resourceCipherSizeBytes
        double initialTransferA = (double) N * resourceCipherSizeBytes;
        // 轮换处理：
        // 如果轮换需要重新为每接收方生成/分发（常见场景），则每次轮换会再传输 N * resourceCipherSizeBytes
        double rotationTransferA = 0.0;
        if (rotationCount > 0.0) {
            // 轮换时通常需要向每个接收方重新分发（是否可以只重新派发 key 取决于方案）
            rotationTransferA = rotationCount * (double) N * resourceCipherSizeBytes * (rotateResourceCipherOnRotation ? 1.0 : 0.0);
            // 如果 rotateResourceCipherOnRotation==false, 但仍需要对每接收方产生新的密钥/wrap等（按具体策略）
            // 这里默认：若不重新加密资源密文，则不传输资源密文，但仍可能传输密钥信息（在 A 模式通常是完整密文，所以此处按业务再确认）
        }
        r.transferABytes = (initialTransferA + rotationTransferA) * (1.0 + retryRate); // 含重试放大

        // 方案 B:
        // 初次分发：仅传输 wrap_DEK 给每个接收方（资源密文只存一次）
        double initialTransferB = (double) N * wrapDekSizeBytes;
        // 轮换处理：
        double rotationTransferB = 0.0;
        if (rotationCount > 0.0) {
            // 如果轮换仅为 wrap（常见）则每次轮换需要再传输 N * wrapDekSizeBytes
            if (rotateWrapOnly) {
                rotationTransferB = rotationCount * (double) N * wrapDekSizeBytes;
            } else {
                // 若轮换时需要重新加密资源密文并重新存储/分发（较少见但可能），则还需要传输资源密文
                if (rotateResourceCipherOnRotation) {
                    rotationTransferB = rotationCount * (double) N * resourceCipherSizeBytes;
                } else {
                    rotationTransferB = rotationCount * (double) N * wrapDekSizeBytes; // fallback
                }
            }
        }
        r.transferBBytes = (initialTransferB + rotationTransferB) * (1.0 + retryRate);

        // ========== 存储与传输成本 ==========
        r.storageCostA = r.storageABytes * costStoragePerByte;
        r.storageCostB = r.storageBBytes * costStoragePerByte;

        r.transferCostA = r.transferABytes * costTransferPerByte;
        r.transferCostB = r.transferBBytes * costTransferPerByte;

        // ========== 密钥生成 / 管理 成本（单次模型） ==========
        // 方案 A:
        // 初次生成调用次数（例如每接收方一份生成/分发）
        double baseGenCallsA = (double) N;
        double totalGenCallsA = baseGenCallsA * (1.0 + rotationCount) * (1.0 + retryRate);
        double genCostA = totalGenCallsA * costPerKeyGenerateA;
        double mgmtCostA = (double) N * costPerKeyMgmtA;
        r.keyMgmtCostA = genCostA + mgmtCostA;

        // 方案 B:
        // 初次生成 wrap 的调用：N 次（每接收方一个 wrap）
        double baseGenCallsB = (double) N;
        double totalGenCallsB = baseGenCallsB * (1.0 + rotationCount) * (1.0 + retryRate);
        double genCostB = totalGenCallsB * costPerWrapGenerate;
        double mgmtCostB = (double) N * costPerWrapMgmtB + costKmsFixed; // 包含固定 KMS 成本（一次性，本次摊入）
        r.keyMgmtCostB = genCostB + mgmtCostB;

        // ========== 审计 / 日志成本 ==========
        // 假设每次 wrap 生成/分发会产生审计日志；方案 A 生成/分发也产生日志
        double auditCallsA = totalGenCallsA;
        double auditCallsB = totalGenCallsB;
        r.auditCostA = auditCallsA * auditBytesPerWrap * costStoragePerByte;
        r.auditCostB = auditCallsB * auditBytesPerWrap * costStoragePerByte;

        // ========== 运维/固定成本分摊（一次性成本按本次任务计入，可按 N 分摊或按方案分配） ==========
        // 这里示例：将 opsOneTimeCost 平均分配到每个接收方再乘以 N（也可以直接把其作为固定的一次性成本独立加入）
        // 若希望直接加入一次性成本（而非每接收方分摊），可把 opsCostA/opsCostB 改成 opsOneTimeCost/2 等
        r.opsCostA = opsOneTimeCost / 2.0;
        r.opsCostB = opsOneTimeCost / 2.0;

        // ========== 预期泄露损失（风险成本，单次视角） ==========
        r.expectedBreachCostA = breachProbabilityPerShot * lossPerBreach;
        r.expectedBreachCostB = breachProbabilityPerShot * lossPerBreach;

        // ========== 总成本合计（单次视角） ==========
        r.totalCostA = r.storageCostA + r.transferCostA + r.keyMgmtCostA + r.auditCostA + r.opsCostA + r.expectedBreachCostA;
        r.totalCostB = r.storageCostB + r.transferCostB + r.keyMgmtCostB + r.auditCostB + r.opsCostB + r.expectedBreachCostB;

        if (r.totalCostA <= 0.0) {
            r.percentReduction = 0.0;
        } else {
            r.percentReduction = (r.totalCostA - r.totalCostB) / r.totalCostA * 100.0;
        }

        return r;
    }

    // 人类可读字节表示
    public static String humanReadableBytes(double bytes) {
        String[] units = {"B","KB","MB","GB","TB"};
        double b = bytes;
        int i = 0;
        while (b >= 1024.0 && i < units.length-1) { b /= 1024.0; i++; }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(b) + " " + units[i];
    }

    // 在 costA 与 costB 的数组中寻找第一个交叉点（插值），若无则返回 -1
    public static double findCrossingN(List<Integer> Ns, List<Double> costA, List<Double> costB) {
        for (int i = 1; i < Ns.size(); i++) {
            double a0 = costA.get(i-1), a1 = costA.get(i);
            double b0 = costB.get(i-1), b1 = costB.get(i);
            double d0 = a0 - b0;
            double d1 = a1 - b1;
            if (d0 == 0.0) {
                return Ns.get(i-1);
            }
            if (d0 * d1 <= 0.0) {
                double t = (0.0 - d0) / (d1 - d0 + 1e-15);
                double n0 = Ns.get(i-1);
                double n1 = Ns.get(i);
                return n0 + t * (n1 - n0);
            }
        }
        return -1.0;
    }

    // 插值函数
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

        // 各构成成本数据（用于拆分图）
        List<Double> storageCostAData = new ArrayList<>();
        List<Double> storageCostBData = new ArrayList<>();
        List<Double> transferCostAData = new ArrayList<>();
        List<Double> transferCostBData = new ArrayList<>();
        List<Double> keyMgmtCostAData = new ArrayList<>();
        List<Double> keyMgmtCostBData = new ArrayList<>();
        List<Double> auditCostAData = new ArrayList<>();
        List<Double> auditCostBData = new ArrayList<>();

        System.out.println("单次视角仿真参数（主要）:");
        System.out.println("资源密文大小 = " + humanReadableBytes(resourceCipherSizeBytes));
        System.out.println("wrap_DEK 大小 = " + wrapDekSizeBytes + " bytes");
        System.out.println("存储单价 = " + costStoragePerByte + " /byte, 传输单价 = " + costTransferPerByte + " /byte");
        System.out.println("costPerWrapGenerate = " + costPerWrapGenerate + ", costPerKeyGenerateA = " + costPerKeyGenerateA);
        System.out.println("rotationCount=" + rotationCount + ", rotateWrapOnly=" + rotateWrapOnly + ", rotateResourceCipherOnRotation=" + rotateResourceCipherOnRotation);
        System.out.println("retryRate=" + retryRate + ", auditBytesPerWrap=" + auditBytesPerWrap);
        System.out.println("backupFactor=" + backupFactor + ", opsOneTimeCost=" + opsOneTimeCost);
        System.out.println();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            writer.write("N,storageABytes,storageBBytes,transferABytes,transferBBytes,storageCostA,storageCostB,transferCostA,transferCostB,keyMgmtCostA,keyMgmtCostB,auditCostA,auditCostB,opsCostA,opsCostB,expectedBreachCostA,expectedBreachCostB,totalCostA,totalCostB,percentReduction\n");

            DecimalFormat df = new DecimalFormat("#0.00000");

            for (int n : Ns) {
                Result r = simulate(n);

                writer.write(String.format("%d,%.0f,%.0f,%.0f,%.0f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.6f\n",
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
                        r.auditCostA,
                        r.auditCostB,
                        r.opsCostA,
                        r.opsCostB,
                        r.expectedBreachCostA,
                        r.expectedBreachCostB,
                        r.totalCostA,
                        r.totalCostB,
                        r.percentReduction
                ));

                System.out.printf("N=%4d | 成本A=%10s 成本B=%10s 降幅=%7s%%\n",
                        r.N,
                        df.format(r.totalCostA),
                        df.format(r.totalCostB),
                        new DecimalFormat("#0.00").format(r.percentReduction));

                // push 数据
                xData.add((double) n);
                costAData.add(r.totalCostA);
                costBData.add(r.totalCostB);
                reductionData.add(r.percentReduction);

                storageCostAData.add(r.storageCostA);
                storageCostBData.add(r.storageCostB);
                transferCostAData.add(r.transferCostA);
                transferCostBData.add(r.transferCostB);
                keyMgmtCostAData.add(r.keyMgmtCostA);
                keyMgmtCostBData.add(r.keyMgmtCostB);
                auditCostAData.add(r.auditCostA);
                auditCostBData.add(r.auditCostB);
            }
            writer.flush();
            System.out.println("\nCSV 已写入：" + csvFile);
        }

        // 计算交叉点
        List<Integer> NsInt = new ArrayList<>(Ns);
        double crossingN = findCrossingN(NsInt, costAData, costBData);
        if (crossingN > 0) {
            System.out.println(String.format("检测到交叉点（近似）：N ≈ %.3f", crossingN));
        } else {
            System.out.println("未检测到交叉点（在给定范围内）。");
        }

        // ---------- 绘制总成本图（含交叉点） ----------
        XYChart chartCosts = new XYChartBuilder()
                .width(1200).height(800)
                .title("单次视角：方案总成本对比（N=1..500）")
                .xAxisTitle("接收方数量 N")
                .yAxisTitle("成本（单位）")
                .build();
        chartCosts.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chartCosts.getStyler().setChartTitleFont(new Font("SansSerif", Font.BOLD, 14));
        chartCosts.getStyler().setLegendFont(new Font("SansSerif", Font.PLAIN, 12));
        chartCosts.getStyler().setMarkerSize(6);

        chartCosts.addSeries("方案 A 总成本", xData, costAData).setMarker(new None()).setLineColor(Color.RED).setLineStyle(new BasicStroke(2.0f));
        chartCosts.addSeries("方案 B 总成本", xData, costBData).setMarker(new None()).setLineColor(Color.BLUE).setLineStyle(new BasicStroke(2.0f));

        if (crossingN > 0) {
            double costAtCross = interpolateAt(crossingN, NsInt, costAData);
            List<Double> xs = new ArrayList<>(); xs.add(crossingN);
            List<Double> ys = new ArrayList<>(); ys.add(costAtCross);
            chartCosts.addSeries(String.format("交叉点 N≈%.1f", crossingN), xs, ys)
                    .setMarker(org.knowm.xchart.style.markers.SeriesMarkers.CIRCLE)
                    .setLineStyle(new BasicStroke(0.0f));
        }

        BitmapEncoder.saveBitmap(chartCosts, pngCosts, BitmapFormat.PNG);
        System.out.println("已保存：" + pngCosts);

        // ---------- 绘制成本构成拆分图 ----------
        XYChart chartComp = new XYChartBuilder()
                .width(1400).height(900)
                .title("单次视角：成本构成拆分（存储 / 传输 / 密钥管理 / 审计）")
                .xAxisTitle("接收方数量 N")
                .yAxisTitle("成本（单位）")
                .build();
        chartComp.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chartComp.getStyler().setMarkerSize(6);

        chartComp.addSeries("存储成本 A", xData, storageCostAData).setMarker(new None()).setLineColor(new Color(200,50,50));
        chartComp.addSeries("存储成本 B", xData, storageCostBData).setMarker(new None()).setLineColor(new Color(200,150,50));
        chartComp.addSeries("传输成本 A", xData, transferCostAData).setMarker(new None()).setLineColor(new Color(50,100,200));
        chartComp.addSeries("传输成本 B", xData, transferCostBData).setMarker(new None()).setLineColor(new Color(50,200,150));
        chartComp.addSeries("密钥生成+管理 A", xData, keyMgmtCostAData).setMarker(new None()).setLineColor(new Color(120,120,120));
        chartComp.addSeries("密钥生成+管理 B", xData, keyMgmtCostBData).setMarker(new None()).setLineColor(new Color(100,0,200));
        chartComp.addSeries("审计成本 A", xData, auditCostAData).setMarker(new None()).setLineColor(new Color(160,80,160));
        chartComp.addSeries("审计成本 B", xData, auditCostBData).setMarker(new None()).setLineColor(new Color(0,150,150));

        BitmapEncoder.saveBitmap(chartComp, pngComponents, BitmapFormat.PNG);
        System.out.println("已保存：" + pngComponents);

        // ---------- 绘制百分比节省图 ----------
        XYChart chartReduction = new XYChartBuilder()
                .width(1200).height(800)
                .title("单次视角：节省百分比（从 方案A 到 方案B）")
                .xAxisTitle("接收方数量 N")
                .yAxisTitle("节省百分比 (%)")
                .build();
        chartReduction.getStyler().setLegendPosition(LegendPosition.InsideNE);
        chartReduction.getStyler().setMarkerSize(6);

        chartReduction.addSeries("节省百分比", xData, reductionData).setMarker(new None()).setLineColor(new Color(34,139,34));

        BitmapEncoder.saveBitmap(chartReduction, pngReduction, BitmapFormat.PNG);
        System.out.println("已保存：" + pngReduction);

        System.out.println("完成。CSV 与 PNG 已生成。若需我用实际的供应商价格（如 KMS 每次调用费、存储/传输单价、轮换策略、审计日志大小）代入并重新运行，请把这些参数发给我，我会替您跑一次并报告新的交叉点和敏感性分析。");
    }
}
