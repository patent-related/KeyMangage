package org.example.accomplish.model;

public class DidReceipt {
    private String did;
    private String publicKeyFingerprint;
    private long timestamp;
    private String receiptId; // simulated

    public String getDid() { return did; }
    public void setDid(String did) { this.did = did; }
    public String getPublicKeyFingerprint() { return publicKeyFingerprint; }
    public void setPublicKeyFingerprint(String publicKeyFingerprint) { this.publicKeyFingerprint = publicKeyFingerprint; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getReceiptId() { return receiptId; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }
}