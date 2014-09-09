/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package com.globo.galeb.core;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import static com.globo.galeb.core.Constants.QUEUE_HEALTHCHECK_FAIL;

public class Backend implements Serializable {

    private final Vertx vertx;
    private final EventBus eb;
    private final ConnectionsCounter connectionsCounter;

    private HttpClient client;

    private final String host;
    private final Integer port;
    private Integer connectionTimeout;
    private boolean keepalive;
    private Long keepAliveMaxRequest;
    private Long keepAliveTimeOut;
    private int backendMaxPoolSize;

    private Long keepAliveTimeMark;
    private Long requestCount;

    @Override
    public String toString() {
        return String.format("%s:%d", this.host, this.port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Backend other = (Backend) obj;
        if (host == null) {
            if (other.host != null) return false;
        } else
            if (!host.equalsIgnoreCase(other.host)) return false;
        if (port == null) {
            if (other.port != null) return false;
        } else
            if (!port.equals(other.port)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public Backend(final String hostWithPort, final Vertx vertx) {
        String[] hostWithPortArray = hostWithPort!=null ? hostWithPort.split(":") : null;
        this.vertx = vertx;
        this.eb = (vertx!=null) ? vertx.eventBus() : null;
        this.client = null;
        if (hostWithPortArray != null && hostWithPortArray.length>1) {
            this.host = hostWithPortArray[0];
            this.port = Integer.parseInt(hostWithPortArray[1]);
        } else {
            this.host = null;
            this.port = null;
        }
        this.connectionTimeout = 60000;
        this.keepalive = true;
        this.keepAliveMaxRequest = Long.MAX_VALUE-1;
        this.keepAliveTimeMark = System.currentTimeMillis();
        this.keepAliveTimeOut = 86400000L; // One day
        this.requestCount = 0L;
        this.connectionsCounter = new ConnectionsCounter(this.toString(), vertx);
    }

    public Backend(JsonObject json, final Vertx vertx) {
        this(String.format("%s:%d",
            json.getString("host", "127.0.0.1"),
            json.getNumber("port", 0)
            ), vertx);
        if (json.containsField("properties")) {
            JsonObject properties = json.getObject("properties");
            this.connectionTimeout = properties.getInteger("connectionTimeout", 60000);
            this.keepalive = properties.getBoolean("keepalive", true);
            this.keepAliveMaxRequest = properties.getLong("keepAliveMaxRequest", Long.MAX_VALUE-1);
            this.keepAliveTimeOut = properties.getLong("keepAliveTimeOut", 86400000L); // One day
        }
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Backend setConnectionTimeout(Integer timeout) {
        this.connectionTimeout = timeout;
        return this;
    }

    public boolean isKeepalive() {
        return keepalive;
    }

    public Backend setKeepAlive(boolean keepalive) {
        this.keepalive = keepalive;
        return this;
    }

    public Long getKeepAliveMaxRequest() {
      return keepAliveMaxRequest;
    }

    public Backend setKeepAliveMaxRequest(Long maxRequestCount) {
      this.keepAliveMaxRequest = maxRequestCount;
      return this;
    }

    public Long getKeepAliveTimeOut() {
        return keepAliveTimeOut;
    }

    public Backend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        this.keepAliveTimeOut = keepAliveTimeOut;
        this.connectionsCounter.setConnectionMapTimeout(getKeepAliveTimeOut());
        return this;
    }

    public boolean isKeepAliveLimit() {
        Long now = System.currentTimeMillis();
        if (requestCount<=keepAliveMaxRequest) {
            requestCount++;
        }
        if ((requestCount>=keepAliveMaxRequest) || (requestCount==Long.MAX_VALUE) ||
                (now-keepAliveTimeMark)>keepAliveTimeOut) {
            keepAliveTimeMark = now;
            requestCount = 0L;
            return true;
        }
        return false;
    }

    public Integer getMaxPoolSize() {
        return backendMaxPoolSize;
    }

    public Backend setMaxPoolSize(Integer maxPoolSize) {
        this.backendMaxPoolSize = maxPoolSize;
        return this;
    }

    // Lazy initialization
    public HttpClient connect(String remoteIP, String remotePort) {
        final String backend = this.toString();
        if (client==null) {
            if (vertx!=null) {
                client = vertx.createHttpClient()
                    .setKeepAlive(keepalive)
                    .setTCPKeepAlive(keepalive)
                    .setConnectTimeout(connectionTimeout)
                    .setMaxPoolSize(backendMaxPoolSize);
                if (host!=null || port!=null) {
                    client.setHost(host)
                          .setPort(port);
                }
                client.exceptionHandler(new Handler<Throwable>() {
                    @Override
                    public void handle(Throwable e) {
                        eb.publish(QUEUE_HEALTHCHECK_FAIL, backend);
                        connectionsCounter.initEventBus();
                    }
                });
                connectionsCounter.registerEventBus();

            }
        }
        connectionsCounter.addConnection(remoteIP, remotePort);
        return client;
    }

    public ConnectionsCounter getSessionController() {
        return connectionsCounter;
    }

    public void close() {
        if (client!=null) {
            try {
                client.close();
            } catch (IllegalStateException e) {
                // Already closed. Ignore exception.
            } finally {
                client=null;
                connectionsCounter.unregisterEventBus();
            }
        }
        connectionsCounter.clearConnectionsMap();
    }

    public boolean isClosed() {
        if (client==null) {
            return true;
        }
        boolean httpClientClosed = false;
        try {
            client.getReceiveBufferSize();
        } catch (IllegalStateException e) {
            httpClientClosed = true;
        }
        return httpClientClosed;
    }

    @Override
    public JsonObject toJson() {
        JsonObject backendJson = new JsonObject();
        backendJson.putString("host", host);
        backendJson.putNumber("port", port);
        JsonObject propertiesJson = new JsonObject();
        propertiesJson.putBoolean("keepalive", isKeepalive());
        propertiesJson.putNumber("connectionTimeout", getConnectionTimeout());
        propertiesJson.putNumber("keepaliveMaxRequest", getKeepAliveMaxRequest());
        propertiesJson.putNumber("keepAliveTimeOut", getKeepAliveTimeOut());
        propertiesJson.putNumber("maxPoolSize", getMaxPoolSize());
        propertiesJson.putNumber("activeConnections", getSessionController().getActiveConnections());
        backendJson.putObject("properties", propertiesJson);

        return backendJson;
    }

}