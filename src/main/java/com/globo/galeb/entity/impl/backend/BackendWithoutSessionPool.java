/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.entity.impl.backend;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.bus.ICallbackConnectionCounter;
import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.entity.EntitiesMap;
import com.globo.galeb.request.RemoteUser;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.scheduler.impl.NullScheduler;
import com.globo.galeb.scheduler.impl.VertxPeriodicScheduler;

/**
 * Class Backend without pool.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class BackendWithoutSessionPool extends EntitiesMap<BackendSession> implements ICallbackConnectionCounter, IBackend {

    /** The Constant NUM_CONNECTIONS_INFO. */
    public static final String NUM_CONNECTIONS_INFO            = "numConnections";

    /** The Constant UUID_INFO_ID. */
    public static final String UUID_INFO_ID                    = "uuid";

    /** The Constant CLEANUP_SESSION_TIME. */
    public static final long   CLEANUP_SESSION_TIME            = 1000L;

    /** The host name or IP. */
    private final String host;

    /** The port. */
    private final Integer port;

    /** The my uuid. */
    private final String myUUID;

    /** The queue active connections. */
    private final String queueActiveConnections;

    /** The cleanup session scheduler. */
    private IScheduler cleanupSessionScheduler    = new NullScheduler();

    /** The is locked. */
    private AtomicBoolean isLocked = new AtomicBoolean(false);

    /** The registered. */
    private boolean registered = false;

    /** The num external sessions. */
    private int numExternalSessions = 0;

    /** The prefix. */
    private String prefix = UNDEF;

    /**
     * Class CleanUpSessionHandler.
     *
     * @author See AUTHORS file.
     * @version 1.0.0, Nov 4, 2014.
     */
    class CleanUpSessionHandler implements ISchedulerHandler {

        /** The backend. */
        private final BackendWithoutSessionPool backend;

        /**
         * Instantiates a new clean up session handler.
         *
         * @param backend the backend
         */
        public CleanUpSessionHandler(BackendWithoutSessionPool backend) {
            this.backend = backend;
        }

        /* (non-Javadoc)
         * @see com.globo.galeb.scheduler.ISchedulerHandler#handle()
         */
        @Override
        public void handle() {

            if (!getEntities().isEmpty() && !backend.isLocked.get()) {
                backend.isLocked.set(true);
                Map<String, BackendSession> tmpSessions = new HashMap<>(getEntities());
                for (BackendSession backendSession : tmpSessions.values()) {
                    if (backendSession.isDead()) {
                        backendSession.close();
                    }
                    if (backendSession.isClosed()) {
                        removeEntity(backendSession);
                    }
                }
                backend.isLocked.set(false);
            }

            publishConnection(getEntities().size());
            setNumExternalSessions(0);

        }
    }

    /**
     * Instantiates a new backend.
     *
     * @param json the json
     */
    public BackendWithoutSessionPool(JsonObject json) {
        super(json);

        String[] hostWithPortArray = id!=null ? id.split(":") : null;
        if (hostWithPortArray != null && hostWithPortArray.length>1) {
            this.host = hostWithPortArray[0];
            int myPort;
            try {
                myPort = Integer.parseInt(hostWithPortArray[1]);
            } catch (NumberFormatException e) {
                myPort = 80;
            }
            this.port = myPort;
        } else {
            this.host = id;
            this.port = 80;
        }

        this.queueActiveConnections = String.format("%s%s", IQueueService.QUEUE_BACKEND_CONNECTIONS_PREFIX, this);
        this.myUUID = UUID.randomUUID().toString();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#start()
     */
    @Override
    public void start() {
        registerConnectionsCounter();
        publishConnection(0);
        if (cleanupSessionScheduler instanceof NullScheduler && getPlataform() instanceof Vertx) {
            cleanupSessionScheduler = new VertxPeriodicScheduler((Vertx) getPlataform())
                                            .setPeriod(CLEANUP_SESSION_TIME)
                                            .setHandler(new CleanUpSessionHandler(this))
                                            .start();
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getHost()
     */
    @Override
    public String getHost() {
        return host;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getPort()
     */
    @Override
    public Integer getPort() {
        return port;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getConnectionTimeout()
     */
    @Override
    public Integer getConnectionTimeout() {
        return (Integer) getOrCreateProperty(CONNECTION_TIMEOUT_FIELDNAME, DEFAULT_CONNECTION_TIMEOUT);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setConnectionTimeout(java.lang.Integer)
     */
    @Override
    public IBackend setConnectionTimeout(Integer timeout) {
        properties.putNumber(CONNECTION_TIMEOUT_FIELDNAME, timeout);
        updateModifiedTimestamp();
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#isKeepalive()
     */
    @Override
    public Boolean isKeepalive() {
        return (Boolean) getOrCreateProperty(KEEPALIVE_FIELDNAME, DEFAULT_KEEPALIVE);

    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setKeepAlive(boolean)
     */
    @Override
    public IBackend setKeepAlive(boolean keepalive) {
        properties.putBoolean(KEEPALIVE_FIELDNAME, keepalive);
        updateModifiedTimestamp();
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getKeepAliveMaxRequest()
     */
    @Override
    public Long getKeepAliveMaxRequest() {
        return (Long) getOrCreateProperty(KEEPALIVE_MAXREQUEST_FIELDNAME, DEFAULT_KEEPALIVE_MAXREQUEST);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setKeepAliveMaxRequest(java.lang.Long)
     */
    @Override
    public IBackend setKeepAliveMaxRequest(Long maxRequestCount) {
      properties.putNumber(KEEPALIVE_MAXREQUEST_FIELDNAME, maxRequestCount);
      updateModifiedTimestamp();
      return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getKeepAliveTimeOut()
     */
    @Override
    public Long getKeepAliveTimeOut() {
        return (Long) getOrCreateProperty(KEEPALIVE_TIMEOUT_FIELDNAME, DEFAULT_KEEPALIVE_TIMEOUT);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setKeepAliveTimeOut(java.lang.Long)
     */
    @Override
    public IBackend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        properties.putNumber(KEEPALIVE_TIMEOUT_FIELDNAME, keepAliveTimeOut);
        updateModifiedTimestamp();
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getMaxPoolSize()
     */
    @Override
    public Integer getMaxPoolSize() {
        return (Integer) getOrCreateProperty(MAXPOOL_SIZE_FIELDNAME, DEFAULT_MAX_POOL_SIZE);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setMaxPoolSize(java.lang.Integer)
     */
    @Override
    public IBackend setMaxPoolSize(Integer maxPoolSize) {
        properties.putNumber(MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        updateModifiedTimestamp();
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#isUsePooledBuffers()
     */
    @Override
    public Boolean isUsePooledBuffers() {
        return (Boolean) getOrCreateProperty(USE_POOLED_BUFFERS_FIELDNAME, DEFAULT_USE_POOLED_BUFFERS);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getSendBufferSize()
     */
    @Override
    public Integer getSendBufferSize() {
        return (Integer) getOrCreateProperty(SEND_BUFFER_SIZE_FIELDNAME, DEFAULT_SEND_BUFFER_SIZE);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getReceiveBufferSize()
     */
    @Override
    public Integer getReceiveBufferSize() {
        return (Integer) getOrCreateProperty(RECEIVED_BUFFER_SIZE_FIELDNAME, DEFAULT_RECEIVE_BUFFER_SIZE);

    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#isPipelining()
     */
    @Override
    public Boolean isPipelining() {
        return (Boolean) getOrCreateProperty(PIPELINING_FIELDNAME, DEFAULT_PIPELINING);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getMinSessionPoolSize()
     */
    @Override
    public int getMinSessionPoolSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setMinSessionPoolSize(int)
     */
    @Override
    public BackendWithoutSessionPool setMinSessionPoolSize(int minPoolSize) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#connect(com.globo.galeb.core.RemoteUser)
     */
    @Override
    public HttpClient connect(RemoteUser remoteUser) {
        if (remoteUser==null) {
            return null;
        }

        String remoteUserId = remoteUser.toString();

        BackendSession backendSession = getEntityById(remoteUserId);

        if (backendSession==null) {

            backendSession = new BackendSession(new JsonObject().putString(ID_FIELDNAME, remoteUserId)
                                                .putString(PARENT_ID_FIELDNAME, id)
                                                .putObject(PROPERTIES_FIELDNAME, properties));

            addEntity(backendSession);

            String backendId = this.toString();
            if (!"".equals(parentId) && !"UNDEF".equals(parentId) &&
                    !"".equals(backendId) && !"UNDEF".equals(backendId)) {
                counter.sendActiveSessions(prefix, backendId, 1L);
            }

        }

        return backendSession.connect();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.impl.backend.IBackend#close(java.lang.String)
     */
    @Override
    public void close(String remoteUser) throws RuntimeException {
        if (remoteUser==null || "".equals(remoteUser) || UNDEF.equals(remoteUser)) {
            return;
        }
        BackendSession backendSession = getEntityById(remoteUser);
        if (backendSession!=null) {
            backendSession.mustBeClosed();
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#realClose(java.lang.String)
     */
    @Override
    public void realClose(String remoteUser) throws RuntimeException {
        if (remoteUser==null || "".equals(remoteUser) || UNDEF.equals(remoteUser)) {
            return;
        }
        BackendSession backendSession = getEntityById(remoteUser);

        if (backendSession!=null) {
            removeEntity(remoteUser);
            if (!backendSession.isClosed()) {
                backendSession.close();
            }
            backendSession = null;
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getActiveConnections()
     */
    @Override
    public int getActiveConnections() {
        return getEntities().size() + numExternalSessions;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#isClosed(java.lang.String)
     */
    @Override
    public boolean isClosed(String remoteUser) {
        if (!(remoteUser==null) && getEntityById(remoteUser)!=null) {
            return getEntityById(remoteUser).isClosed();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        prepareJson();
        idObj.putNumber(ACTIVE_CONNECTIONS_FIELDNAME, getActiveConnections());

        return super.toJson();
    }

    /**
     * Register connections counter.
     */
    public void registerConnectionsCounter() {
        if (queueService!=null) {
            queueService.registerConnectionsCounter(this, queueActiveConnections);
        }
    }

    /**
     * Unregister connections counter.
     */
    public void unregisterConnectionsCounter() {
        if (queueService!=null) {
            publishConnection(0);
            queueService.unregisterConnectionsCounter(this, queueActiveConnections);
        }
    }

    /**
     * Publish connection.
     *
     * @param numConnections the num connections
     */
    public void publishConnection(int numConnections) {
        if (queueService!=null) {
            queueService.publishActiveConnections(queueActiveConnections, makeConnectionInfoMessage(numConnections));
        }
    }

    /**
     * Make connection info message.
     *
     * @param numConnection the num connection
     * @return the json object
     */
    public JsonObject makeConnectionInfoMessage(int numConnection) {
        JsonObject myConnections = new JsonObject();
        myConnections.putString(UUID_INFO_ID, myUUID);
        myConnections.putNumber(NUM_CONNECTIONS_INFO, numConnection);
        return myConnections;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackConnectionCounter#setRegistered(boolean)
     */
    @Override
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackConnectionCounter#isRegistered()
     */
    @Override
    public boolean isRegistered() {
        return registered;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackConnectionCounter#callbackGlobalConnectionsInfo(org.(Vertx) getPlataform().java.core.json.JsonObject)
     */
    @Override
    public void callbackGlobalConnectionsInfo(JsonObject message) {
        String uuid = message.getString(UUID_INFO_ID);
        if (uuid != myUUID) {
            int numConnections = message.getInteger(NUM_CONNECTIONS_INFO);
            if (numConnections>=0) {
                numExternalSessions += numConnections;
            } else {
                numExternalSessions = 0;
            }
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#startSessionPool()
     */
    @Override
    public IBackend startSessionPool() {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#closeAllForced()
     */
    @Override
    public void closeAllForced() {
        for (BackendSession backendSession: getEntities().values()) {
            backendSession.close();
        }
        clearEntities();
        cleanupSessionScheduler.cancel();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#closeAll()
     */
    @Override
    public void closeAll() {
        for (String remoteUser: getEntities().keySet()) {
            close(remoteUser);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(IBackend otherBackend) {
        if (otherBackend==null) {
            return 0;
        }
        return this.getActiveConnections()-otherBackend.getActiveConnections();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.impl.backend.IBackend#setMetricPrefix(java.lang.String)
     */
    @Override
    public IBackend setMetricPrefix(String prefix) {
        this.prefix = prefix;
        updateModifiedTimestamp();
        return this;
    }

    /**
     * Sets the num external sessions.
     *
     * @param numExternalSessions the new num external sessions
     */
    private void setNumExternalSessions(int numExternalSessions) {
        this.numExternalSessions = numExternalSessions;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.impl.backend.IBackend#setUsePooledBuffers(boolean)
     */
    @Override
    public IBackend setUsePooledBuffers(boolean usePooledBuffers) {
        properties.putBoolean(USE_POOLED_BUFFERS_FIELDNAME, usePooledBuffers);
        updateModifiedTimestamp();
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.impl.backend.IBackend#setSendBufferSize(int)
     */
    @Override
    public IBackend setSendBufferSize(int sendBufferSize) {
        properties.putNumber(SEND_BUFFER_SIZE_FIELDNAME, sendBufferSize);
        updateModifiedTimestamp();
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.impl.backend.IBackend#setReceiveBufferSize(int)
     */
    @Override
    public IBackend setReceiveBufferSize(int receiveBufferSize) {
        properties.putNumber(RECEIVED_BUFFER_SIZE_FIELDNAME, receiveBufferSize);
        updateModifiedTimestamp();
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.impl.backend.IBackend#setPipelining(boolean)
     */
    @Override
    public IBackend setPipelining(boolean pipelining) {
        properties.putBoolean(PIPELINING_FIELDNAME, pipelining);
        updateModifiedTimestamp();
        return this;
    }

}
