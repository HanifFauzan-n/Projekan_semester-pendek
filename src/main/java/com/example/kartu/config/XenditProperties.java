package com.example.kartu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfigurasi integrasi Xendit (SKPL-F05).
 * Nilai diambil dari application.properties yang membacanya dari environment variable.
 * Tidak ada kredensial yang ditulis langsung di kode.
 */
@Component
@ConfigurationProperties(prefix = "xendit")
public class XenditProperties {

    /** Secret API key Xendit. Mode test/sandbox selama penelitian. */
    private String apiKey = "";

    /** Base URL REST API Xendit. */
    private String baseUrl = "https://api.xendit.co";

    /** Token yang dikirim Xendit pada header x-callback-token setiap webhook. */
    private String callbackToken = "";

    /** Lama invoice berlaku dalam detik. Default 24 jam. */
    private int invoiceDurationSeconds = 86400;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCallbackToken() {
        return callbackToken;
    }

    public void setCallbackToken(String callbackToken) {
        this.callbackToken = callbackToken;
    }

    public int getInvoiceDurationSeconds() {
        return invoiceDurationSeconds;
    }

    public void setInvoiceDurationSeconds(int invoiceDurationSeconds) {
        this.invoiceDurationSeconds = invoiceDurationSeconds;
    }
}
