package org.example.accomplish.service;

import org.example.accomplish.model.*;
import org.example.accomplish.util.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KmsService {
    private PolicyContract policy;
    private DidRegistry didRegistry;
    private AuditService audit;
    private ResourceStore resourceStore;

    // storage for issued wrap_deks, keyed by requestId or wrapId
    private Map<String, WrapDek> wrapsById = new ConcurrentHashMap<>();
    private Map<String, WrapDek> wrapsByRequest = new ConcurrentHashMap<>();

    public KmsService(PolicyContract policy, DidRegistry didRegistry, AuditService audit, ResourceStore resourceStore) {
        this.policy = policy;
        this.didRegistry = didRegistry;
        this.audit = audit;
        this.resourceStore = resourceStore;
    }

    // Entry point for request handling
    public boolean handleRequest(Request req, String recipientTeeId) {
        System.out.println("[KMS] 收到请求: " + req.getRequestId());
// 1. basic checks: signature (simulated) / freshness
        if (req.getSignature() == null) {
            System.out.println("[KMS] 拒绝：缺少签名");
            return false;
        }
// 2. DID query
        DidReceipt didRec = didRegistry.query(req.getRequesterDid());
        if (didRec == null) {
            System.out.println("[KMS] 拒绝：DID 未注册");
            return false;
        }
// 3. chain authorization check
        boolean ok = policy.isAuthorizationValid(req.getAuthorizationTxId(), req.getRequesterDid());
        if (!ok) {
            System.out.println("[KMS] 拒绝：链上授权无效或已过期");
// submit rejection evidence to audit
            Evidence ev = new Evidence();
            ev.setEvidenceId(IdUtil.randomUUID());
            ev.setRequestId(req.getRequestId());
            ev.setRequesterDid(req.getRequesterDid());
            ev.setAttestationSummaryHash(req.getAttestationSummaryHash());
            ev.setDecisionResult("REJECT_AUTHORIZATION");
            ev.setTimestamp(System.currentTimeMillis());
            ev.setKmsSignature("kms-sig-simulated");
            audit.submitEvidence(ev);
            return false;
        }
// 4. attestation validation (simulated by checking attestation summary is non-empty)
        if (req.getAttestationSummaryHash() == null) {
            System.out.println("[KMS] 拒绝：缺少 attestation 证明摘要");
            return false;
        }
// 5. build evidence and submit to audit
        Evidence evidence = new Evidence();
        evidence.setEvidenceId(IdUtil.randomUUID());
        evidence.setRequestId(req.getRequestId());
        evidence.setRequesterDid(req.getRequesterDid());
        evidence.setAttestationSummaryHash(req.getAttestationSummaryHash());
        evidence.setDecisionResult("ALLOW_PENDING_RECEIPT");
        evidence.setTimestamp(System.currentTimeMillis());
        evidence.setKmsSignature("kms-sig-simulated");
        audit.submitEvidence(evidence);

// 6. create pending wrap_DEK but mark as waiting for audit receipt
        WrapDek wrap = new WrapDek();
        wrap.setWrapDekId(IdUtil.randomUUID());
// encryptedDek: simulate by AES-encrypting DEK with recipient public key fingerprint (not real)
        Resource res = resourceStore.getResource(req.getResourceId());
        byte[] encryptedDek = CryptoUtil.xor(res.getDek(), (req.getRequesterDid()+req.getRequestId()).getBytes());
        wrap.setEncryptedDek(encryptedDek);
        wrap.setRecipientTeeId(recipientTeeId);
        wrap.setRecipientPublicKeyFingerprint(didRec.getPublicKeyFingerprint());
        wrap.setValidFrom(System.currentTimeMillis());
        wrap.setValidTo(System.currentTimeMillis() + 60*1000L); // 60s validity
        Map<String,String> usage = new HashMap<>();
        usage.put("maxCalls","3");
        usage.put("purpose","analysis");
        wrap.setUsageConstraints(usage);
        wrap.setBoundAuthorizationIdSha256(CryptoUtil.sha256Hex(req.getAuthorizationTxId()));
        wrap.setRequestId(req.getRequestId());
// kmsSignature will be set when finalizing wrap
        wrapsById.put(wrap.getWrapDekId(), wrap);
        wrapsByRequest.put(req.getRequestId(), wrap);

        System.out.println("[KMS] 已创建待定 wrap_DEK: " + wrap.getWrapDekId() + "，等待审计回执以完成下发");
        return true;
    }

    // periodically called to finalize wraps once audit receipt is available
    public void tryIssuePendingWraps() {
        AuditReceipt receipt = audit.latestReceipt();
        if (receipt == null) {
            System.out.println("[KMS] 当前无审计回执，暂无法下发 wrap_DEK");
            return;
        }
// finalize all pending wraps by embedding receipt hash and signing
        for (WrapDek w : wrapsById.values()) {
            if (w.getKmsSignature() != null) {
                continue; // already finalized
            }
// bind receipt
            w.setBoundReceiptIdSha256(CryptoUtil.sha256Hex(receipt.getBatchId()));
// sign the wrap metadata (simulate)
            String toSign = w.getWrapDekId() + "|" + w.getRecipientTeeId() + "|" + w.getBoundReceiptIdSha256();
            w.setKmsSignature("kms-sig:" + CryptoUtil.sha256Hex(toSign));
            System.out.println("[KMS] 完成 wrap_DEK: " + w.getWrapDekId() + "，已绑定审计回执 batchId=" + receipt.getBatchId());
// 在真实系统中会通过受信通道下发到 TEE；此处仅模拟打印
        }
    }

    public WrapDek getWrapByRequest(String requestId) {
        return wrapsByRequest.get(requestId);
    }

    public void revokeWrapByAuthorizationId(String authorizationId) {
        String sha = CryptoUtil.sha256Hex(authorizationId);
        for (WrapDek w : wrapsById.values()) {
            if (sha.equals(w.getBoundAuthorizationIdSha256())) {
                w.setRevoked(true);
                System.out.println("[KMS] 将 wrap 标记为已撤销: " + w.getWrapDekId());
            }
        }
    }

    public void onAuthorizationRevoked(String authorizationId) {
        revokeWrapByAuthorizationId(authorizationId);
// 在真实系统中：向活跃会话下发撤销命令
    }

    // expose a method for TEE to fetch wrap by request id (simulate secure retrieval)
    public WrapDek fetchWrapForTee(String requestId) {
        WrapDek w = wrapsByRequest.get(requestId);
        if (w == null) {
            return null;
        }
        if (w.isRevoked()) {
            return null;
        }
        return w;
    }
}