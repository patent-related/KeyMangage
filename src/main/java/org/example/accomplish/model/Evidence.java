package org.example.accomplish.model;

public class Evidence {
    private String evidenceId;
    private String requestId;
    private String requesterDid;
    private String attestationSummaryHash;
    private String decisionResult; // KMS decision
    private long timestamp;
    private String kmsSignature;
    private String teeSignature; // optional
    private String originalEvidencePointer;

    public String getEvidenceId() { return evidenceId; }
    public void setEvidenceId(String evidenceId) { this.evidenceId = evidenceId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getRequesterDid() { return requesterDid; }
    public void setRequesterDid(String requesterDid) { this.requesterDid = requesterDid; }
    public String getAttestationSummaryHash() { return attestationSummaryHash; }
    public void setAttestationSummaryHash(String attestationSummaryHash) { this.attestationSummaryHash = attestationSummaryHash; }
    public String getDecisionResult() { return decisionResult; }
    public void setDecisionResult(String decisionResult) { this.decisionResult = decisionResult; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getKmsSignature() { return kmsSignature; }
    public void setKmsSignature(String kmsSignature) { this.kmsSignature = kmsSignature; }
    public String getTeeSignature() { return teeSignature; }
    public void setTeeSignature(String teeSignature) { this.teeSignature = teeSignature; }
    public String getOriginalEvidencePointer() { return originalEvidencePointer; }
    public void setOriginalEvidencePointer(String originalEvidencePointer) { this.originalEvidencePointer = originalEvidencePointer; }
}