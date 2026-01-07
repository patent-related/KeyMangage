package org.example.cost;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 * 简单仿真：比较传统方案（方案A）与本发明方案（方案B）在 N 个接收方场景下的存储/传输/密钥管理成本估算。
 *
 * 使用说明：
 * - 可修改 main 中的参数 resourceCipherSizeBytes, dekSizeBytes, wrapDekSizeBytes, costPerKey, costPerWrap, costKmsFixed 等。
 * - 运行后将在控制台打印若干 N 的对比，并将全量结果写入 "comparison_results.csv"
 *
 * 注意：此仿真为概念验证估算工具，实际部署需用真实测量数据替换参数。
 */

public class KeyManagementCostSimulation {
    // 单位：字节（Bytes）
    static long resourceCipherSizeBytes = 10L * 1024 * 1024; // 10 MB 资源密文示例
    static int dekSizeBytes = 32; // DEK 假设 256-bit = 32 bytes
    static int wrapDekSizeBytes = 512; // wrap_DEK 包含元数据与签名，示例假设 512 bytes
    // 密钥管理成本（任意货币单位或相对成本单位）
    static double costPerKey = 0.001; // 方案 A 每个密钥的管理单位成本（示例）
    static double costPerWrap = 0.0005; // 方案 B 每个 wrap_DEK 的管理单位成本（示例）
    static double costKmsFixed = 1.0; // 方案 B 的固定 KMS 运行成本（示例）
    static double costStoragePerByte = 0.00000001; // 存储单位成本（货币/字节），示例仅用于将字节转换为成本
    static double costTransferPerByte = 0.00000002; // 传输单位成本（货币/字节）
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
        // 方案 A（传统）：假设为每个接收方保存一份密文（最坏/常见情况）
        r.storageABytes = (double) N * resourceCipherSizeBytes; // N * resourceCipherSize
        r.transferABytes = (double) N * resourceCipherSizeBytes; // 初次下发 N 份密文
        // 方案 B（本发明）：只保存一份密文 + 为每个接收方保存 wrap_DEK（如果需要存储）
        r.storageBBytes = resourceCipherSizeBytes + (double) N * wrapDekSizeBytes;
        r.transferBBytes = (double) N * wrapDekSizeBytes; // 初次只传 wrap_DEK 给每个接收方
        // 成本估算模型（简化）
        double storageCostA = r.storageABytes * costStoragePerByte;
        double storageCostB = r.storageBBytes * costStoragePerByte;
        double transferCostA = r.transferABytes * costTransferPerByte;
        double transferCostB = r.transferBBytes * costTransferPerByte;
        double keyMgmtCostA = costPerKey * N; // 管理 N 个密钥（简化）
        double keyMgmtCostB = costKmsFixed + costPerWrap * N; // 固定 KMS 成本 + per-wrap 成本
        r.costA = storageCostA + transferCostA + keyMgmtCostA;
        r.costB = storageCostB + transferCostB + keyMgmtCostB;
        // 防止除零
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
        // 可调整的 N 值列表
        List<Integer> Ns = Arrays.asList(1, 5, 10, 50, 100, 500, 1000, 5000, 10000);
        System.out.println("仿真参数：");
        System.out.println("资源密文大小(resourceCipherSizeBytes) = " + humanReadableBytes(resourceCipherSizeBytes));
        System.out.println("wrap_DEK 大小(wrapDekSizeBytes) = " + wrapDekSizeBytes + " bytes");
        System.out.println("costPerKey = " + costPerKey + ", costPerWrap = " + costPerWrap + ", costKmsFixed = " + costKmsFixed);
        System.out.println("costStoragePerByte = " + costStoragePerByte + ", costTransferPerByte = " + costTransferPerByte);
        System.out.println();
        System.out.printf("%8s | %12s | %12s | %12s | %12s | %12s | %12s | %8s\n",
                "N", "StorA", "StorB", "TransA", "TransB", "CostA", "CostB", "Red%");
        DecimalFormat cf = new DecimalFormat("#0.000");
        // 输出到 CSV 文件，便于绘图
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
            }
            writer.flush();
            System.out.println("\n仿真结果已写入 comparison_results.csv");
        }
        // 另外演示当 N 大时的比例增长（可视化估算）
        int Nlarge = 100000;
        Result rLarge = simulate(Nlarge);
        System.out.println();
        System.out.println("示例：N = " + Nlarge);
        System.out.println("方案 A 存储 (bytes) = " + rLarge.storageABytes + " (" + humanReadableBytes(rLarge.storageABytes) + ")");
        System.out.println("方案 B 存储 (bytes) = " + rLarge.storageBBytes + " (" + humanReadableBytes(rLarge.storageBBytes) + ")");
        System.out.println("成本 A = " + cf.format(rLarge.costA) + " , 成本 B = " + cf.format(rLarge.costB) + " , 降低百分比 = " + cf.format(rLarge.percentReduction) + "%");
    }
}
