package org.example.accomplish.model;

public class Resource {
    private String resourceId;
    private byte[] cipher; // resource cipher (DEK encrypted content omitted in demo)
    private String resourceFingerprint;
    private byte[] dek; // DEK (in real system must be protected)

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public byte[] getCipher() { return cipher; }
    public void setCipher(byte[] cipher) { this.cipher = cipher; }
    public String getResourceFingerprint() { return resourceFingerprint; }
    public void setResourceFingerprint(String resourceFingerprint) { this.resourceFingerprint = resourceFingerprint; }
    public byte[] getDek() { return dek; }
    public void setDek(byte[] dek) { this.dek = dek; }
}