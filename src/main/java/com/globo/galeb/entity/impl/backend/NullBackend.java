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

import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.request.RemoteUser;

/**
 * Class NullBackend.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 18, 2014.
 */
public class NullBackend implements IBackend {

    /** The host. */
    private final String host;

    /** The port. */
    private final int port;

    /**
     * Instantiates a new null backend.
     */
    public NullBackend() {
        this.host = "0.0.0.0";
        this.port = 0;
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
        return Integer.MAX_VALUE;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setConnectionTimeout(java.lang.Integer)
     */
    @Override
    public IBackend setConnectionTimeout(Integer timeout) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#isKeepalive()
     */
    @Override
    public Boolean isKeepalive() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setKeepAlive(boolean)
     */
    @Override
    public IBackend setKeepAlive(boolean keepalive) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getKeepAliveMaxRequest()
     */
    @Override
    public Long getKeepAliveMaxRequest() {
        return Long.MAX_VALUE;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setKeepAliveMaxRequest(java.lang.Long)
     */
    @Override
    public IBackend setKeepAliveMaxRequest(Long maxRequestCount) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getKeepAliveTimeOut()
     */
    @Override
    public Long getKeepAliveTimeOut() {
        return Long.MAX_VALUE;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setKeepAliveTimeOut(java.lang.Long)
     */
    @Override
    public IBackend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getMaxPoolSize()
     */
    @Override
    public Integer getMaxPoolSize() {
        return 1;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#setMaxPoolSize(java.lang.Integer)
     */
    @Override
    public IBackend setMaxPoolSize(Integer maxPoolSize) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#isUsePooledBuffers()
     */
    @Override
    public Boolean isUsePooledBuffers() {
        return false;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getSendBufferSize()
     */
    @Override
    public Integer getSendBufferSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getReceiveBufferSize()
     */
    @Override
    public Integer getReceiveBufferSize() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#isPipelining()
     */
    @Override
    public Boolean isPipelining() {
        return true;
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
    public IBackend setMinSessionPoolSize(int minPoolSize) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.impl.backend.IBackend#setMaxConn(int)
     */
    @Override
    public IBackend setMaxConn(int maxConn) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#connect(com.globo.galeb.core.RemoteUser)
     */
    @Override
    public HttpClient connect(RemoteUser remoteUser) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#close(java.lang.String)
     */
    @Override
    public void close(String remoteUser) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#getActiveConnections()
     */
    @Override
    public int getActiveConnections() {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#isClosed(java.lang.String)
     */
    @Override
    public boolean isClosed(String remoteUser) {
        return true;
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
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IBackend#closeAll()
     */
    @Override
    public void closeAll() {
        //
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return IJsonable.UNDEF;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.IJsonable#toJson()
     */
    @Override
    public JsonObject toJson() {
        return new JsonObject().putString(ID_FIELDNAME, UNDEF);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.impl.backend.IBackend#setMetricPrefix(java.lang.String)
     */
    @Override
    public IBackend setMetricPrefix(String prefix) {
        return this;
    }

}
