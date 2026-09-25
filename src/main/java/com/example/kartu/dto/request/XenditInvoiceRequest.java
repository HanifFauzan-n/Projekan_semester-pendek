package com.example.kartu.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XenditInvoiceRequest {

    private String externalId;
    private BigDecimal amount;
    private String payerEmail;
    private String description;
    private String currency = "IDR";
    private Integer invoiceDuration;
    private String successRedirectUrl;
    private String failureRedirectUrl;
    private Boolean shouldSendEmail = Boolean.FALSE;

    public XenditInvoiceRequest() {
    }

    public XenditInvoiceRequest(String externalId, BigDecimal amount) {
        this.externalId = externalId;
        this.amount = amount;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getInvoiceDuration() {
        return invoiceDuration;
    }

    public void setInvoiceDuration(Integer invoiceDuration) {
        this.invoiceDuration = invoiceDuration;
    }

    public String getSuccessRedirectUrl() {
        return successRedirectUrl;
    }

    public void setSuccessRedirectUrl(String successRedirectUrl) {
        this.successRedirectUrl = successRedirectUrl;
    }

    public String getFailureRedirectUrl() {
        return failureRedirectUrl;
    }

    public void setFailureRedirectUrl(String failureRedirectUrl) {
        this.failureRedirectUrl = failureRedirectUrl;
    }

    public Boolean getShouldSendEmail() {
        return shouldSendEmail;
    }

    public void setShouldSendEmail(Boolean shouldSendEmail) {
        this.shouldSendEmail = shouldSendEmail;
    }
}
