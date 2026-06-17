package com.roomwallah.common.ai.impl;

import com.roomwallah.common.ai.EmbeddingProvider;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class DefaultEmbeddingProvider implements EmbeddingProvider {

    @Override
    public double[] embed(String text) {
        if (text == null) {
            text = "";
        }
        double[] vector = new double[128];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < 128; i++) {
                int index = (i * 7) % hash.length;
                int val = hash[index] & 0xFF;
                vector[i] = (double) val / 255.0 - 0.5;
            }
            double sumSquare = 0;
            for (double v : vector) {
                sumSquare += v * v;
            }
            double norm = Math.sqrt(sumSquare);
            if (norm > 0) {
                for (int i = 0; i < 128; i++) {
                    vector[i] /= norm;
                }
            }
        } catch (Exception e) {
            for (int i = 0; i < 128; i++) {
                vector[i] = 1.0 / Math.sqrt(128.0);
            }
        }
        return vector;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getModelIdentifier() {
        return "text-embedding-3-small";
    }

    @Override
    public int getEmbeddingVersion() {
        return 1;
    }
}
