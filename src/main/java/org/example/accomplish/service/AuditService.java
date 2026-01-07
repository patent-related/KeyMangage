package org.example.accomplish.service;

import org.example.accomplish.model.*;
import org.example.accomplish.util.CryptoUtil;
import org.example.accomplish.util.IdUtil;
import org.example.accomplish.util.MerkleUtil;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AuditService {
    private Queue<Evidence> queue = new ConcurrentLinkedQueue<>();
    private List<AuditReceipt> receipts = Collections.synchronizedList(new ArrayList<>());

    public void submitEvidence(Evidence e) {
        queue.add(e);
        System.out.println("[审计服务] 收到审计证据: " + e.getEvidenceId());
    }

    // simulate periodic batch aggregation and on-chain anchor
    public AuditReceipt flushBatchToChain() {
        List<Evidence> batch = new ArrayList<>();
        Evidence e;
        while ((e = queue.poll()) != null) {
            batch.add(e);
        }
        if (batch.isEmpty()) {
            System.out.println("[审计服务] 无待聚合证据，无需上链");
            return null;
        }
        // compute merkle root by hashing evidence ids (simplified)
        List<String> leaves = new ArrayList<>();
        for (Evidence ev : batch) {
            leaves.add(ev.getEvidenceId());
        }
        String merkleRoot = MerkleUtil.computeMerkleRoot(leaves);
        AuditReceipt receipt = new AuditReceipt();
        receipt.setBatchId(IdUtil.randomUUID());
        receipt.setMerkleRoot(merkleRoot);
        receipt.setChainTxId("chainTx-" + IdUtil.randomUUID());
        receipt.setConfirmations(1);
        receipt.setTimestamp(System.currentTimeMillis());
        receipt.setAuditSignature("audit-sig-simulated");
        receipts.add(receipt);
        System.out.println("[审计服务] 已聚合批次并上链。batchId=" + receipt.getBatchId() + " merkleRoot=" + merkleRoot);
        return receipt;
    }

    public AuditReceipt latestReceipt() {
        if (receipts.isEmpty()) {
            return null;
        }
        return receipts.get(receipts.size() - 1);
    }
}