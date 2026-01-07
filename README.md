# Patent Key Management - 链上策略驱动的密钥管理系统

[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.x-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 📋 项目简介

这是一个基于区块链的密钥管理系统（Key Management System, KMS）的概念验证实现和性能测试项目。该系统通过链上策略驱动的方式，实现了数据共享场景下的密钥封装（wrap_DEK）和分发机制，相比传统方案显著降低了存储和传输成本。

### 核心特性

- **链上策略驱动**：授权策略上链，KMS 根据链上策略决策访问控制
- **按接收方密钥封装**：每个接收方获得定制的 wrap_DEK，而非完整资源副本
- **DID 身份管理**：基于去中心化身份（DID）的身份验证
- **TEE 可信执行环境**：模拟 TEE 环境中的密钥解封和资源访问
- **审计追溯**：完整的访问请求审计日志，支持批量聚合上链
- **动态撤销机制**：支持链上授权撤销并实时同步

## 🏗️ 项目结构

```
patent-KeyManagement/
├── src/main/java/org/example/
│   ├── accomplish/              # 方案实现模块
│   │   ├── model/              # 数据模型
│   │   │   ├── AuditReceipt.java       # 审计回执
│   │   │   ├── DidReceipt.java         # DID 查询回执
│   │   │   ├── Evidence.java           # 审计凭证
│   │   │   ├── Request.java            # 访问请求
│   │   │   ├── Resource.java           # 资源对象
│   │   │   └── WrapDek.java            # 封装的数据加密密钥
│   │   ├── service/            # 核心服务
│   │   │   ├── AuditService.java       # 审计服务
│   │   │   ├── DidRegistry.java        # DID 注册与查询
│   │   │   ├── KmsService.java         # 密钥管理服务
│   │   │   ├── PolicyContract.java     # 链上策略合约
│   │   │   ├── ResourceStore.java      # 资源存储
│   │   │   └── TeeEnvironment.java     # TEE 环境模拟
│   │   ├── util/               # 工具类
│   │   │   ├── CryptoUtil.java         # 加密工具
│   │   │   ├── IdUtil.java             # ID 生成工具
│   │   │   └── MerkleUtil.java         # Merkle 树工具
│   │   └── MainDemo.java               # 完整流程演示
│   └── cost/                   # 成本分析模块
│       ├── KeyManagementCostSimulation.java        # 成本仿真
│       ├── KeyManagementCostPlot.java              # 成本可视化（英文）
│       ├── KeyManagementCostPlotCN.java            # 成本可视化（中文）
│       └── KeyManagementCostSingleShot_Nmax500.java # 单次测试
├── pom.xml                     # Maven 配置
├── comparison_results.csv      # 仿真结果数据
└── README.md                   # 项目文档
```

## 🚀 快速开始

### 环境要求

- **Java**: JDK 8 或更高版本
- **Maven**: 3.x 或更高版本
- **依赖**: XChart 3.8.1 (用于图表绘制)

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd patent-KeyManagement
```

2. **构建项目**
```bash
mvn clean compile
```

3. **安装依赖**

依赖会通过 Maven 自动下载：
- `org.knowm.xchart:xchart:3.8.1` - 图表绘制库

## 💡 使用示例

### 1. 运行完整流程演示

演示链上策略驱动的 wrap_DEK 完整流程：

```bash
mvn exec:java -Dexec.mainClass="org.example.accomplish.MainDemo"
```

**演示内容包括：**
- 资源创建与 DEK 生成
- DID 注册
- 链上授权发布
- 访问请求构建
- KMS 请求处理（DID 验证、授权检查、attestation 验证）
- 审计凭证聚合与上链
- wrap_DEK 下发
- TEE 环境中使用 wrap_DEK 解封并访问资源
- 链上授权撤销与 wrap_DEK 撤销

### 2. 运行成本仿真分析

比较传统方案与本发明方案在不同接收方数量下的成本差异：

```bash
mvn exec:java -Dexec.mainClass="org.example.cost.KeyManagementCostSimulation"
```

**输出：**
- 控制台打印不同 N 值（接收方数量）下的成本对比表格
- 生成 `comparison_results.csv` 文件，包含详细数据

### 3. 生成成本对比图表

生成可视化的成本对比图表（PNG 格式）：

```bash
# 英文版
mvn exec:java -Dexec.mainClass="org.example.cost.KeyManagementCostPlot"

# 中文版
mvn exec:java -Dexec.mainClass="org.example.cost.KeyManagementCostPlotCN"
```

**输出文件：**
- `comparison_costs.png` - 成本曲线对比图
- `comparison_reduction.png` - 成本降低百分比图

### 4. 单次成本测试（N ≤ 500）

快速测试较小规模场景：

```bash
mvn exec:java -Dexec.mainClass="org.example.cost.KeyManagementCostSingleShot_Nmax500"
```

## 📊 成本模型说明

### 仿真参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `resourceCipherSizeBytes` | 10 MB | 资源密文大小 |
| `dekSizeBytes` | 32 bytes | DEK 大小（256-bit） |
| `wrapDekSizeBytes` | 512 bytes | wrap_DEK 大小（含元数据与签名） |
| `costPerKey` | 0.001 | 方案 A 每个密钥管理成本 |
| `costPerWrap` | 0.0005 | 方案 B 每个 wrap_DEK 管理成本 |
| `costKmsFixed` | 1.0 | 方案 B KMS 固定运行成本 |
| `costStoragePerByte` | 0.00000001 | 存储单位成本 |
| `costTransferPerByte` | 0.00000002 | 传输单位成本 |

### 方案对比

**方案 A（传统方案）：**
- 为每个接收方保存一份完整资源密文副本
- 存储成本：`N × resourceCipherSize`
- 传输成本：`N × resourceCipherSize`

**方案 B（本发明方案）：**
- 保存单份资源密文 + N 个 wrap_DEK
- 存储成本：`resourceCipherSize + N × wrapDekSize`
- 传输成本：`N × wrapDekSize`

### 成本计算公式

```
总成本 = 存储成本 + 传输成本 + 密钥管理成本

成本降低百分比 = (CostA - CostB) / CostA × 100%
```

## 🔧 关键技术点

### 1. 链上策略驱动

```java
// 链上发布授权
String authorizationId = policy.publishAuthorization(resourceId, requesterDid, validitySeconds);

// KMS 检查链上授权
boolean isValid = policy.isAuthorizationValid(authorizationId, requesterDid);
```

### 2. wrap_DEK 生成与绑定

每个 wrap_DEK 包含：
- 加密的 DEK（使用接收方公钥加密）
- 接收方 TEE ID 和公钥指纹
- 有效期（validFrom/validTo）
- 使用约束（usageConstraints）
- 绑定的授权 ID 哈希
- 绑定的审计回执哈希
- KMS 签名

### 3. 审计凭证聚合

```java
// 提交访问凭证
audit.submitEvidence(evidence);

// 批量聚合并上链
audit.flushBatchToChain();
```

### 4. TEE 环境模拟

```java
// TEE 从 KMS 获取 wrap_DEK
WrapDek wrap = kms.fetchWrapForTee(requestId);

// 解封 DEK 并访问资源
tee.simulateUse(requestId, requesterDid);
```

## 📈 性能分析结果示例

基于默认参数，当接收方数量 N = 10000 时：

| 指标 | 方案 A | 方案 B | 降低比例 |
|------|--------|--------|----------|
| 存储 | 95.37 GB | 15.00 MB | ~99.98% |
| 传输 | 95.37 GB | 4.88 MB | ~99.99% |
| 总成本 | 2.055 | 1.015 | ~50.6% |

*注：具体数值取决于仿真参数配置*

## 🔐 安全考虑

**本项目为概念验证实现，仅用于性能测试和方案演示。实际生产环境需要：**

1. **真实加密算法**：当前使用简化的 XOR 模拟，需替换为 AES-GCM、RSA 等标准算法
2. **真实区块链集成**：集成实际的区块链网络（如 Ethereum、Hyperledger Fabric）
3. **真实 TEE 集成**：集成 Intel SGX、AMD SEV 或 ARM TrustZone
4. **完整的密钥生命周期管理**：密钥轮换、备份、恢复机制
5. **健壮的错误处理**：网络故障、链上交易失败等异常场景处理
6. **完善的访问控制**：细粒度的权限管理和策略引擎

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发建议

1. 遵循 Java 代码规范
2. 添加单元测试覆盖新功能
3. 更新文档说明变更内容
4. **异常诊断**：出现问题时，请增强日志输出，打印完整上下文信息以便诊断


**注意**：本项目仅用于研究和教育目的，不建议直接用于生产环境。
