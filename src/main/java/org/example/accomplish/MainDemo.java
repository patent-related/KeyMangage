package org.example.accomplish;

import org.example.accomplish.model.*;
import org.example.accomplish.service.*;
import org.example.accomplish.util.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("启动演示：链上策略驱动的按接收方 wrap_DEK 流程（模拟）");

// 初始化服务与存储
        ResourceStore resourceStore = new ResourceStore();
        PolicyContract policy = new PolicyContract();
        DidRegistry didRegistry = new DidRegistry();
        AuditService audit = new AuditService();
        KmsService kms = new KmsService(policy, didRegistry, audit, resourceStore);
        TeeEnvironment tee = new TeeEnvironment(audit, kms, resourceStore);

// 1. 数据提供方：生成资源与 DEK（DEK 在 demo 中为随机字节）
        Resource resource = resourceStore.createResource("resource-001", "Hello, secret data for sharing".getBytes(StandardCharsets.UTF_8));
        System.out.println("资源已存储：id=" + resource.getResourceId() + " 指纹=" + resource.getResourceFingerprint());

// 2. DID 注册（请求方）
        String requesterDid = "did:example:alice";
        didRegistry.register(requesterDid, "alice-public-key-sim"); // 公钥模拟
        System.out.println("DID 已注册：" + requesterDid);

// 3. 链上发布授权（PolicyContract）
        String authorizationId = policy.publishAuthorization(resource.getResourceId(), requesterDid, 60); // 60s 有效期
        System.out.println("策略已上链发布：authorizationId=" + authorizationId);

// 4. Requester 构建请求并附带 attestationSummary（模拟）
        Request req = new Request();
        req.setRequestId(IdUtil.randomUUID());
        req.setRequesterDid(requesterDid);
        req.setResourceId(resource.getResourceId());
        req.setAuthorizationTxId(authorizationId);
        req.setTimestamp(System.currentTimeMillis());
        req.setAttestationSummaryHash(CryptoUtil.sha256Hex("simulated-attestation-" + requesterDid));
        req.setUsageParameters("maxCalls=3;purpose=analysis");
        req.setSignature("sig-simulated-by-alice");
        System.out.println("访问请求已创建：requestId=" + req.getRequestId());

// 5. KMS 接收请求并处理（包括 DID 查询、链上授权校验、attestation 验证、上报 audit）
        boolean accepted = kms.handleRequest(req, tee.getTeeIdForDid(requesterDid));
        System.out.println("KMS 处理请求结果：" + (accepted ? "接受" : "拒绝"));

// 6. 模拟等待 AuditService 完成批次聚合并生成 finalReceipt
        Thread.sleep(1000);
        audit.flushBatchToChain(); // 强制聚合并上链（模拟）
        Thread.sleep(200); // 等待回执

// 7. KMS 再次尝试下发 wrap_DEK（在 handleRequest 中已做一次尝试，但这里演示完整流程）
        kms.tryIssuePendingWraps();

// 8. TEE 使用 wrap_DEK 解封并访问资源（模拟多次使用以演示 usageConstraints）
        tee.simulateUse(req.getRequestId(), requesterDid);
        tee.simulateUse(req.getRequestId(), requesterDid);
        tee.simulateUse(req.getRequestId(), requesterDid);

// 9. 演示撤销：链上撤销授权 -> KMS 监听并撤销 wrap_DEK -> TEE 收到撤销并销毁 DEK
        System.out.println("\n-- 模拟撤销流程 --");
        policy.revokeAuthorization(authorizationId);
// 通知 KMS（在真实系统中 KMS 监听链上事件，这里直接调用）
        kms.onAuthorizationRevoked(authorizationId);

// 展示最终状态
        System.out.println("演示结束。");
    }
}