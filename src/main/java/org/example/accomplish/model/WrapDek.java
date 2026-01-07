package org.example.accomplish.model;

import java.util.*;

public class WrapDek {
    private String wrapDekId;
    private byte[] encryptedDek; // encrypted DEK for recipient (simulated)
    private String recipientTeeId;
    private String recipientPublicKeyFingerprint;
    private long validFrom;
    private long validTo;
    private Map<String,String> usageConstraints = new HashMap<>();
    private String boundReceiptIdSha256;
    private String boundAuthorizationIdSha256;
    private String kmsSignature; // simulated
    private boolean revoked = false;
    private String requestId;

    // getters / setters
    public String getWrapDekId() { return wrapDekId; }
    public void setWrapDekId(String wrapDekId) { this.wrapDekId = wrapDekId; }
    public byte[] getEncryptedDek() { return encryptedDek; }
    public void setEncryptedDek(byte[] encryptedDek) { this.encryptedDek = encryptedDek; }
    public String getRecipientTeeId() { return recipientTeeId; }
    public void setRecipientTeeId(String recipientTeeId) { this.recipientTeeId = recipientTeeId; }
    public String getRecipientPublicKeyFingerprint() { return recipientPublicKeyFingerprint; }
    public void setRecipientPublicKeyFingerprint(String recipientPublicKeyFingerprint) { this.recipientPublicKeyFingerprint = recipientPublicKeyFingerprint; }
    public long getValidFrom() { return validFrom; }
    public void setValidFrom(long validFrom) { this.validFrom = validFrom; }
    public long getValidTo() { return validTo; }
    public void setValidTo(long validTo) { this.validTo = validTo; }
    public Map<String, String> getUsageConstraints() { return usageConstraints; }
    public void setUsageConstraints(Map<String, String> usageConstraints) { this.usageConstraints = usageConstraints; }
    public String getBoundReceiptIdSha256() { return boundReceiptIdSha256; }
    public void setBoundReceiptIdSha256(String boundReceiptIdSha256) { this.boundReceiptIdSha256 = boundReceiptIdSha256; }
    public String getBoundAuthorizationIdSha256() { return boundAuthorizationIdSha256; }
    public void setBoundAuthorizationIdSha256(String boundAuthorizationIdSha256) { this.boundAuthorizationIdSha256 = boundAuthorizationIdSha256; }
    public String getKmsSignature() { return kmsSignature; }
    public void setKmsSignature(String kmsSignature) { this.kmsSignature = kmsSignature; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}