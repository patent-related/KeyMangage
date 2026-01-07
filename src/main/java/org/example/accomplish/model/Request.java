package org.example.accomplish.model;

public class Request {
    private String requestId;
    private String requesterDid;
    private String resourceId;
    private String authorizationTxId; // chain authorization identifier
    private String attestationSummaryHash;
    private String usageParameters;
    private long timestamp;
    private String signature;

    // getters / setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getRequesterDid() { return requesterDid; }
    public void setRequesterDid(String requesterDid) { this.requesterDid = requesterDid; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getAuthorizationTxId() { return authorizationTxId; }
    public void setAuthorizationTxId(String authorizationTxId) { this.authorizationTxId = authorizationTxId; }
    public String getAttestationSummaryHash() { return attestationSummaryHash; }
    public void setAttestationSummaryHash(String attestationSummaryHash) { this.attestationSummaryHash = attestationSummaryHash; }
    public String getUsageParameters() { return usageParameters; }
    public void setUsageParameters(String usageParameters) { this.usageParameters = usageParameters; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}