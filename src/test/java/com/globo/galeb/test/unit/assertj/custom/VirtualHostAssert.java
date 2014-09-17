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
package com.globo.galeb.test.unit.assertj.custom;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.Virtualhost;

import org.assertj.core.api.AbstractAssert;
import org.vertx.java.core.json.JsonObject;

public class VirtualHostAssert extends AbstractAssert<VirtualHostAssert, Virtualhost> {

    protected VirtualHostAssert(Virtualhost actual) {
        super(actual, VirtualHostAssert.class);
    }

    public static VirtualHostAssert assertThat(Virtualhost actual) {
        return new VirtualHostAssert(actual);
    }

    public VirtualHostAssert hasActionFail(boolean actionOk) {
        isNotNull();
        if (actionOk) {
            failWithMessage("Action not fail.", "");
        }
        return this;
    }

    public VirtualHostAssert hasActionOk(boolean actionOk) {
        isNotNull();
        if (!actionOk) {
            failWithMessage("Action fail.", "");
        }
        return this;
    }

    public VirtualHostAssert hasSize(Integer size, boolean backendOk) {
        isNotNull();
        if (actual.getBackends(backendOk).size() != size) {
            failWithMessage("Expected size to be <%s> but was <%s>", size, actual.getBackends(backendOk).size());
        }
        return this;
    }

    public VirtualHostAssert containsBackend(JsonObject backend, boolean backendOk) {
        isNotNull();
        if (!actual.getBackends(backendOk).contains(new Backend(backend, null))) {
            failWithMessage("%s not found at %s", backend.getString(IJsonable.jsonIdFieldName), actual.getVirtualhostName());
        }
        return this;
    }

    public VirtualHostAssert doesNotContainsBackend(JsonObject backend, boolean backendOk) {
        isNotNull();
        if (actual.getBackends(backendOk).contains(new Backend(backend, null))) {
            failWithMessage("%s found at %s", backend.getString(IJsonable.jsonIdFieldName), actual.getVirtualhostName());
        }
        return this;
    }

    public VirtualHostAssert hasProperty(String property) {
        isNotNull();
        if (!actual.getProperties().containsField(property)) {
            failWithMessage("%s haven't the %s property", actual.getVirtualhostName(), property);
        }
        return this;
    }

    public VirtualHostAssert haventProperty(String property) {
        isNotNull();
        if (!actual.getProperties().containsField(property)) {
            failWithMessage("%s has the %s property", actual.getVirtualhostName(), property);
        }
        return this;
    }
}
