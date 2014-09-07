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
package com.globo.galeb.loadbalance.impl;

import java.util.List;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;

public class RoundRobinPolicy implements ILoadBalancePolicy {

    private int pos = -1;

    @Override
    public Backend getChoice(final List<Backend> backends, final RequestData requestData) {

        int size = backends.size();
        pos = pos+1>=size ? 0 : pos+1;

       return backends.get(pos);
    }

    @Override
    public String toString() {
        return RoundRobinPolicy.class.getSimpleName();
    }

    @Override
    public boolean isDefault() {
        return false;
    }
}
