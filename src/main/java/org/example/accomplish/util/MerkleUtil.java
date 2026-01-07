package org.example.accomplish.util;

import java.util.*;
import org.example.accomplish.util.CryptoUtil;

public class MerkleUtil {
    public static String computeMerkleRoot(List<String> leaves) {
        if (leaves == null || leaves.isEmpty()) return "";
        List<String> layer = new ArrayList<>(leaves);
// use sha256 of leaf content
        for (int i=0;i<layer.size();i++) {
            layer.set(i, CryptoUtil.sha256Hex(layer.get(i)));
        }
        while (layer.size() > 1) {
            List<String> next = new ArrayList<>();
            for (int i=0;i<layer.size();i+=2) {
                if (i+1 < layer.size()) {
                    next.add(CryptoUtil.sha256Hex(layer.get(i)+layer.get(i+1)));
                } else {
                    next.add(layer.get(i)); // odd leaf carry forward
                }
            }
            layer = next;
        }
        return layer.get(0);
    }
}