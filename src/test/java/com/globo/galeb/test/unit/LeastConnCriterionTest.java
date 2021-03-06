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
package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.vertx.testtools.VertxAssert.testComplete;

import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.criteria.impl.LeastConnCriterion;
import com.globo.galeb.criteria.impl.LoadBalanceCriterion;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.backend.Backend;
import com.globo.galeb.entity.impl.backend.BackendPool;
import com.globo.galeb.entity.impl.backend.IBackend;
import com.globo.galeb.request.RemoteUser;
import com.globo.galeb.request.RequestData;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public class LeastConnCriterionTest extends TestVerticle {

    private BackendPool backendPool;
    private int numBackends = 10;

    @Test
    public void leastConnection() {

        JsonObject backendPoolProperties = new JsonObject()
            .putString(LoadBalanceCriterion.LOADBALANCE_POLICY_FIELDNAME,
                    LeastConnCriterion.class.getSimpleName().replaceAll(LoadBalanceCriterionFactory.CLASS_SUFFIX, ""));

        JsonObject backendPoolJson = new JsonObject()
            .putString(IJsonable.ID_FIELDNAME, "pool0")
            .putObject(IJsonable.PROPERTIES_FIELDNAME, backendPoolProperties);

        backendPool = (BackendPool) new BackendPool(backendPoolJson).setPlataform(vertx);

        for (int x=0; x<numBackends; x++) {
            backendPool.addEntity(new Backend(new JsonObject().putString(IJsonable.ID_FIELDNAME, String.format("0:%s", x))));
            IBackend backend = backendPool.getEntityById(String.format("0:%s", x));
            for (int c = 1; c <= x+1; c++) {
                backend.connect(new RemoteUser("0",c));
            }
        }

        for (int c=1 ; c<=1000; c++) {

            IBackend backendWithLeastConn = backendPool.getChoice(new RequestData());
            int numConnectionsInBackendWithLeastConn = backendWithLeastConn.getActiveConnections();

            for (IBackend backendSample: backendPool.getEntities().values()) {

                int numConnectionsInBackendSample = backendSample.getActiveConnections();
                if (backendSample!=backendWithLeastConn) {
                    assertThat(numConnectionsInBackendWithLeastConn).isEqualTo(1);
                    assertThat(numConnectionsInBackendWithLeastConn)
                                .isLessThan(numConnectionsInBackendSample);

                }

            }
        }

        testComplete();
    }

}
