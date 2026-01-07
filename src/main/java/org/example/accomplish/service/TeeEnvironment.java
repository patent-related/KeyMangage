package org.example.accomplish.service;

import org.example.accomplish.model.*;
import org.example.accomplish.util.*;

import java.util.*;

public class TeeEnvironment {
    private AuditService audit;
    private KmsService kms;
    private ResourceStore resourceStore;
    private Map<String, Session> sessions = new HashMap<>();
    private String teeId = "tee:example:alice:1";

    static class Session {
        String sessionId;
        byte[] dek; // unwrapped DEK held in TEE memory (non-exportable in real)
        String requestId;
        int usageCount = 0;
        int maxCalls = 0;
        boolean active = true;
    }

    public TeeEnvironment(AuditService audit, KmsService kms, ResourceStore resourceStore) {
        this.audit = audit;
        this.kms = kms;
        this.resourceStore = resourceStore;
    }

    public String getTeeIdForDid(String did) {
// in real system mapping DID -> TEE id; here return teeId for simplicity
        return teeId;
    }

    // simulate TEE fetching wrap_DEK from KMS and unwrapping DEK
    public boolean acceptWrapAndUnwrap(String requestId) {
        WrapDek wrap = kms.fetchWrapForTee(requestId);
        if (wrap == null) {
            System.out.println("[TEE] 未找到 wrap（可能已撤销或尚未准备好），requestId=" + requestId);
            return false;
        }
// verify kmsSignature (simulated)
        if (wrap.getKmsSignature() == null) {
            System.out.println("[TEE] wrap 尚未由 KMS 完成签名，无法解封；requestId=" + requestId);
            return false;
        }
// check validity window
        long now = System.currentTimeMillis();
        if (now < wrap.getValidFrom() || now > wrap.getValidTo()) {
            System.out.println("[TEE] wrap 已过期或尚未生效；requestId=" + requestId);
            return false;
        }
// unencrypt dek (simulate)
        byte[] dek = CryptoUtil.randomBytes(16); // 模拟解封得到的 DEK
        Session s = new Session();
        s.sessionId = IdUtil.randomUUID();
        s.dek = dek;
        s.requestId = requestId;
        s.maxCalls = Integer.parseInt(wrap.getUsageConstraints().getOrDefault("maxCalls", "1"));
        sessions.put(requestId, s);
        System.out.println("[TEE] 已解封 DEK 并建立会话 sessionId=" + s.sessionId + "，requestId=" + requestId);
        return true;
    }

    // simulate usage: decrypt resource partially and submit usage evidence
    public void simulateUse(String requestId, String requesterDid) {
        Session s = sessions.get(requestId);
        if (s == null || !s.active) {
// try accept wrap first
            boolean ok = acceptWrapAndUnwrap(requestId);
            if (!ok) {
                System.out.println("[TEE] 无法访问资源，requestId=" + requestId);
                return;
            }
            s = sessions.get(requestId);
        }
        if (s.usageCount >= s.maxCalls) {
            System.out.println("[TEE] 使用次数已达上限，requestId=" + requestId);
            return;
        }
        if (!s.active) {
            System.out.println("[TEE] 会话已失效，requestId=" + requestId);
            return;
        }
// simulate decrypt & compute outputHash
        Resource r = resourceStore.getResource("resource-001");
        byte[] plain = r.getCipher(); // in real TEE would decrypt with dek
        String outputHash = CryptoUtil.sha256Hex(new String(plain) + ":" + s.usageCount);

// generate signed usage evidence (simulated)
        Evidence ev = new Evidence();
        ev.setEvidenceId(IdUtil.randomUUID());
        ev.setRequestId(requestId);
        ev.setRequesterDid(requesterDid);
        ev.setAttestationSummaryHash(CryptoUtil.sha256Hex("tee-attestation-sim"));
        ev.setDecisionResult("TEE_USAGE_OK");
        ev.setTimestamp(System.currentTimeMillis());
        ev.setTeeSignature("tee-sig-simulated");
        audit.submitEvidence(ev);

        s.usageCount++;
        System.out.println("[TEE] 已执行第 " + s.usageCount + " 次使用，outputHash=" + outputHash + "，evidenceId=" + ev.getEvidenceId());
    }

    // simulate receiving revoke command from KMS
    public void onRevokeWrap(String wrapId) {
// find any session associated and zeroize DEK
        for (Map.Entry<String, Session> ent : sessions.entrySet()) {
            Session s = ent.getValue();
            s.active = false;
            s.dek = null;
            System.out.println("[TEE] 会话 " + s.sessionId + " 已被撤销并销毁 DEK (requestId=" + s.requestId + ")");
// report anomaly evidence
            Evidence ev = new Evidence();
            ev.setEvidenceId(IdUtil.randomUUID());
            ev.setRequestId(s.requestId);
            ev.setRequesterDid("unknown");
            ev.setAttestationSummaryHash(CryptoUtil.sha256Hex("tee-revoke-evidence"));
            ev.setDecisionResult("TEE_REVOKE_ACK");
            ev.setTimestamp(System.currentTimeMillis());
            ev.setTeeSignature("tee-sig-revoke");
            audit.submitEvidence(ev);
        }
    }
}