package com.roomwallah.common.observability;

import org.slf4j.MDC;
import java.util.UUID;

public final class CorrelationContext {

    private static final String MDC_KEY = "correlationId";
    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private CorrelationContext() {}

    public static void set(String correlationId) {
        String id = (correlationId == null || correlationId.isBlank()) 
                ? UUID.randomUUID().toString() 
                : correlationId;
        CORRELATION_ID.set(id);
        MDC.put(MDC_KEY, id);
    }

    public static String get() {
        String id = CORRELATION_ID.get();
        if (id == null) {
            id = UUID.randomUUID().toString();
            set(id);
        }
        return id;
    }

    public static void clear() {
        CORRELATION_ID.remove();
        MDC.remove(MDC_KEY);
    }
}
