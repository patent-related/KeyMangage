package org.example.accomplish.service;

import org.example.accomplish.model.DidReceipt;
import org.example.accomplish.util.IdUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DidRegistry {
    private Map<String, String> didToPub = new ConcurrentHashMap<>();
    private Map<String, DidReceipt> receipts = new ConcurrentHashMap<>();

    public void register(String did, String publicKeyFingerprint) {
        didToPub.put(did, publicKeyFingerprint);
        DidReceipt r = new DidReceipt();
        r.setDid(did);
        r.setPublicKeyFingerprint(publicKeyFingerprint);
        r.setTimestamp(System.currentTimeMillis());
        r.setReceiptId(IdUtil.randomUUID());
        receipts.put(did, r);
    }

    public DidReceipt query(String did) {
        return receipts.get(did);
    }
}