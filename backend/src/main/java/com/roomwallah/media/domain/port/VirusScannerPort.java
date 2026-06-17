package com.roomwallah.media.domain.port;

public interface VirusScannerPort {
    boolean scan(byte[] content);
}
