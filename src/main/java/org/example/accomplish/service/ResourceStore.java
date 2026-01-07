package org.example.accomplish.service;

import org.example.accomplish.model.Resource;
import org.example.accomplish.util.CryptoUtil;
import org.example.accomplish.util.IdUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceStore {
    private Map<String, Resource> store = new ConcurrentHashMap<>();

    public Resource createResource(String resourceId, byte[] plain) {
        Resource r = new Resource();
        r.setResourceId(resourceId);
// generate random DEK
        byte[] dek = CryptoUtil.randomBytes(16);
        r.setDek(dek);
// encrypt resource with DEK - here we just simulate by storing plain as cipher (not secure)
        r.setCipher(plain);
        r.setResourceFingerprint(CryptoUtil.sha256Hex(new String(plain)));
        store.put(resourceId, r);
        return r;
    }

    public Resource getResource(String resourceId) {
        return store.get(resourceId);
    }
}