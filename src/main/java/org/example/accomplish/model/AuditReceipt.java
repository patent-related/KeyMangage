package org.example.accomplish.model;

public class AuditReceipt {
    private String batchId;
    private String merkleRoot;
    private String chainTxId;
    private int confirmations;
    private long timestamp;
    private String auditSignature;

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getMerkleRoot() { return merkleRoot; }
    public void setMerkleRoot(String merkleRoot) { this.merkleRoot = merkleRoot; }
    public String getChainTxId() { return chainTxId; }
    public void setChainTxId(String chainTxId) { this.chainTxId = chainTxId; }
    public int getConfirmations() { return confirmations; }
    public void setConfirmations(int confirmations) { this.confirmations = confirmations; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getAuditSignature() { return auditSignature; }
    public void setAuditSignature(String auditSignature) { this.auditSignature = auditSignature; }
}