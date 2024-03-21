package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class WebhookHeaderResponse {

    @JsonProperty("x-request-start")
    private List<String> requestStart;
    @JsonProperty("x-envoy-expected-rq-timeout-ms")
    private List<String> timeout;
    @JsonProperty("x-request-id")
    private List<String> requestId;
    @JsonProperty("x-envoy-external-address")
    private List<String> ecternalAddress;
    @JsonProperty("call-ref")
    private List<String> callRef;
    @JsonProperty("signature")
    private List<String> signature;
    @JsonProperty("published-timestamp")
    private List<String> publishedTimestamp;
    @JsonProperty("content-type")
    private List<String> contentType;
    @JsonProperty("content-length")
    private List<String> contentLength;
    @JsonProperty("x-amzn-trace-id")
    private List<String> traceId;
    @JsonProperty("x-forwarded-port")
    private List<String> forwardedPort;
    @JsonProperty("x-forwarded-proto")
    private List<String> forwardedProto;
    @JsonProperty("x-forwarded-for")
    private List<String> forwardedFor;
    @JsonProperty("host")
    private List<String> host;
    @JsonProperty("webhooks-key")
    private List<String> webhooksKey;
    @JsonProperty("accept")
    private List<String> accept;
    @JsonProperty("tracestate")
    private List<String> tracestate;
    @JsonProperty("traceparent")
    private List<String> traceparent;

    public List<String> getRequestStart() {
        return requestStart;
    }

    public void setRequestStart(List<String> requestStart) {
        this.requestStart = requestStart;
    }

    public List<String> getTimeout() {
        return timeout;
    }

    public void setTimeout(List<String> timeout) {
        this.timeout = timeout;
    }

    public List<String> getRequestId() {
        return requestId;
    }

    public void setRequestId(List<String> requestId) {
        this.requestId = requestId;
    }

    public List<String> getEcternalAddress() {
        return ecternalAddress;
    }

    public void setEcternalAddress(List<String> ecternalAddress) {
        this.ecternalAddress = ecternalAddress;
    }

    public List<String> getCallRef() {
        return callRef;
    }

    public void setCallRef(List<String> callRef) {
        this.callRef = callRef;
    }

    public List<String> getSignature() {
        return signature;
    }

    public void setSignature(List<String> signature) {
        this.signature = signature;
    }

    public List<String> getPublishedTimestamp() {
        return publishedTimestamp;
    }

    public void setPublishedTimestamp(List<String> publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
    }

    public List<String> getContentType() {
        return contentType;
    }

    public void setContentType(List<String> contentType) {
        this.contentType = contentType;
    }

    public List<String> getContentLength() {
        return contentLength;
    }

    public void setContentLength(List<String> contentLength) {
        this.contentLength = contentLength;
    }

    public List<String> getTraceId() {
        return traceId;
    }

    public void setTraceId(List<String> traceId) {
        this.traceId = traceId;
    }

    public List<String> getForwardedPort() {
        return forwardedPort;
    }

    public void setForwardedPort(List<String> forwardedPort) {
        this.forwardedPort = forwardedPort;
    }

    public List<String> getForwardedProto() {
        return forwardedProto;
    }

    public void setForwardedProto(List<String> forwardedProto) {
        this.forwardedProto = forwardedProto;
    }

    public List<String> getForwardedFor() {
        return forwardedFor;
    }

    public void setForwardedFor(List<String> forwardedFor) {
        this.forwardedFor = forwardedFor;
    }

    public List<String> getHost() {
        return host;
    }

    public void setHost(List<String> host) {
        this.host = host;
    }

    public List<String> getWebhooksKey() {
        return webhooksKey;
    }

    public void setWebhooksKey(List<String> webhooksKey) {
        this.webhooksKey = webhooksKey;
    }

    public List<String> getAccept() {
        return accept;
    }

    public void setAccept(List<String> accept) {
        this.accept = accept;
    }

    public List<String> getTracestate() {
        return tracestate;
    }

    public void setTracestate(List<String> tracestate) {
        this.tracestate = tracestate;
    }

    public List<String> getTraceparent() {
        return traceparent;
    }

    public void setTraceparent(List<String> traceparent) {
        this.traceparent = traceparent;
    }
}
