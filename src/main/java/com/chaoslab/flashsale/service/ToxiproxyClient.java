package com.chaoslab.flashsale.service;

public interface ToxiproxyClient {

    void setProxyEnabled(String proxyName, boolean enabled);

    void addLatencyToxic(String proxyName, int latencyMs, int jitterMs);

    void resetProxy(String proxyName);

    boolean isProxyEnabled(String proxyName);
}
