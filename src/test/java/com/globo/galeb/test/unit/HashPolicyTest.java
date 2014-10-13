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
import static com.globo.galeb.consistenthash.HashAlgorithm.HashType;
import static org.mockito.Mockito.*;

import java.util.EnumSet;
import java.util.Set;

import com.globo.galeb.consistenthash.HashAlgorithm;
import com.globo.galeb.core.Backend;
import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.loadbalance.impl.HashPolicy;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;

public class HashPolicyTest {

    Virtualhost virtualhost;
    int numBackends = 10;

    @Before
    public void setUp() throws Exception {
        Vertx vertx = mock(DefaultVertx.class);

        JsonObject virtualhostProperties = new JsonObject()
            .putString(Virtualhost.LOADBALANCE_POLICY_FIELDNAME, HashPolicy.class.getSimpleName());
        JsonObject virtualhostJson = new JsonObject()
            .putString(IJsonable.ID_FIELDNAME, "test.localdomain")
            .putObject(IJsonable.PROPERTIES_FIELDNAME, virtualhostProperties);
        virtualhost = new Virtualhost(virtualhostJson, vertx);

        for (int x=0; x<numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
        }
    }

    @Test
    public void checkPersistentChoice() {
        long numTests = 256L*256L;

        for (int counter=0; counter<numTests; counter++) {

            RequestData requestData1 = new RequestData(Long.toString(counter), null);
            Backend backend1 = virtualhost.getChoice(requestData1);
            RequestData requestData2 = new RequestData(Long.toString(counter), null);
            Backend backend2 = virtualhost.getChoice(requestData2);
            RequestData requestData3 = new RequestData(Long.toString(counter), null);
            Backend backend3 = virtualhost.getChoice(requestData3);

            assertThat(backend1).isEqualTo(backend2);
            assertThat(backend1).isEqualTo(backend3);
        }
    }

    @Test
    public void checkUniformDistribution() {
        long samples = 100000L;
        int rounds = 5;
        double percentMarginOfError = 0.5;
        Set<HashType> hashs = EnumSet.allOf(HashAlgorithm.HashType.class);

        for (int round=0; round < rounds; round++) {
            System.out.println(String.format("TestHashPolicy.checkUniformDistribution - round %s: %d samples", round+1, samples));

            for (HashType hash: hashs) {

                long sum = 0L;
                long initialTime = System.currentTimeMillis();
                for (int counter=0; counter<samples; counter++) {
                    RequestData requestData = new RequestData(Long.toString(counter), null);
                    virtualhost.getProperties().putString(HashPolicy.HASH_ALGORITHM_FIELDNAME, hash.toString());
                    sum += virtualhost.getChoice(requestData).getPort();
                }
                long finishTime = System.currentTimeMillis();

                double result = (numBackends*(numBackends-1)/2.0) * (samples/numBackends);

                System.out.println(String.format("-> TestHashPolicy.checkUniformDistribution (%s): Time spent (ms): %d. NonUniformDistRatio (smaller is better): %.4f%%",
                        hash, finishTime-initialTime, Math.abs(100.0*(result-sum)/result)));

                double topLimit = sum*(1.0+percentMarginOfError);
                double bottomLimit = sum*(1.0-percentMarginOfError);

                assertThat(result).isGreaterThanOrEqualTo(bottomLimit)
                                  .isLessThanOrEqualTo(topLimit);
            }
        }
    }
}
