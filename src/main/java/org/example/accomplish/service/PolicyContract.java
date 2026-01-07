package org.example.accomplish.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.example.accomplish.util.IdUtil;

public class PolicyContract {
    // simple in-memory map to simulate on-chain authorizations
    private Map<String, AuthorizationRecord> auths = new ConcurrentHashMap<>();

    public static class AuthorizationRecord {
        String authorizationId;
        String resourceId;
        String granteeDid;
        long validTo;
        boolean revoked = false;
    }

    public String publishAuthorization(String resourceId, String granteeDid, int validSeconds) {
        AuthorizationRecord rec = new AuthorizationRecord();
        rec.authorizationId = IdUtil.randomUUID();
        rec.resourceId = resourceId;
        rec.granteeDid = granteeDid;
        rec.validTo = System.currentTimeMillis() + validSeconds * 1000L;
        auths.put(rec.authorizationId, rec);
// 模拟链上事件发布（现实中为链交易）
        System.out.println("[策略合约] 已发布授权记录: authorizationId=" + rec.authorizationId + " resourceId=" + resourceId + " grantee=" + granteeDid);
        return rec.authorizationId;
    }

    public boolean isAuthorizationValid(String authorizationId, String granteeDid) {
        AuthorizationRecord rec = auths.get(authorizationId);
        if (rec == null) {
            return false;
        }
        if (rec.revoked) {
            return false;
        }
        if (!rec.granteeDid.equals(granteeDid)) {
            return false;
        }
        return rec.validTo >= System.currentTimeMillis();
    }

    public void revokeAuthorization(String authorizationId) {
        AuthorizationRecord rec = auths.get(authorizationId);
        if (rec != null) {
            rec.revoked = true;
        }
        System.out.println("[策略合约] 授权已撤销: " + authorizationId);
    }
}