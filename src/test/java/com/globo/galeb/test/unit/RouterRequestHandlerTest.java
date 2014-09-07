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

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.handlers.RouterRequestHandler;
import com.globo.galeb.test.unit.util.FakeLogger;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
//import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

public class RouterRequestHandlerTest {

    private RouterRequestHandler routerRequestHandler;
//    private HttpServerRequest httpServerRequest;
    private Verticle verticle;
    private Vertx vertx;
    private Container container;
    private LogDelegate logDelegate;
    private FakeLogger logger;
    private Map<String, Virtualhost> virtualhosts;

    @Before
    public void setUp() {
//        httpServerRequest = mock(HttpServerRequest.class);
        verticle = mock(Verticle.class);
        vertx = mock(Vertx.class);
        container = mock(Container.class);
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);

        when(verticle.getVertx()).thenReturn(vertx);
        when(verticle.getVertx().eventBus()).thenReturn(null);
        when(verticle.getContainer()).thenReturn(container);
        when(verticle.getContainer().logger()).thenReturn(logger);

        routerRequestHandler = new RouterRequestHandler(vertx, container, virtualhosts, null);
    }

    @Test
    public void headersWithHttpVersion10() {
        MultiMap headersWithConnectionKeepAlive = new CaseInsensitiveMultiMap();
        MultiMap headersWithConnectionClose = new CaseInsensitiveMultiMap();
        MultiMap headersEmpty = new CaseInsensitiveMultiMap();

        headersWithConnectionKeepAlive.set("Connection", "keep-alive");
        headersWithConnectionClose.set("Connection", "close");


        boolean isKeepAliveWithConnectionKeepAlive = routerRequestHandler.isHttpKeepAlive(headersWithConnectionKeepAlive, HttpVersion.HTTP_1_0);
        boolean isKeepAliveWithConnectionClose = routerRequestHandler.isHttpKeepAlive(headersWithConnectionClose, HttpVersion.HTTP_1_0);
        boolean isKeepAliveWithoutConnectionHeader = routerRequestHandler.isHttpKeepAlive(headersEmpty, HttpVersion.HTTP_1_0);

        assertThat(isKeepAliveWithConnectionKeepAlive).as("isKeepAliveWithConnectionKeepAlive").isTrue();
        assertThat(isKeepAliveWithConnectionClose).as("isKeepAliveWithConnectionClose").isFalse();
        assertThat(isKeepAliveWithoutConnectionHeader).as("isKeepAliveWithoutConnectionHeader").isFalse();

    }

    @Test
    public void headersWithHttpVersion11() {
        MultiMap headersWithConnectionKeepAlive = new CaseInsensitiveMultiMap();
        MultiMap headersWithConnectionClose = new CaseInsensitiveMultiMap();
        MultiMap headersEmpty = new CaseInsensitiveMultiMap();

        headersWithConnectionKeepAlive.set("Connection", "keep-alive");
        headersWithConnectionClose.set("Connection", "close");

        boolean isKeepAliveWithConnectionKeepAlive = routerRequestHandler.isHttpKeepAlive(headersWithConnectionKeepAlive, HttpVersion.HTTP_1_1);
        boolean isKeepAliveWithConnectionClose = routerRequestHandler.isHttpKeepAlive(headersWithConnectionClose, HttpVersion.HTTP_1_1);
        boolean isKeepAliveWithoutConnectionHeader = routerRequestHandler.isHttpKeepAlive(headersEmpty, HttpVersion.HTTP_1_1);

        assertThat(isKeepAliveWithConnectionKeepAlive).as("isKeepAliveWithConnectionKeepAlive").isTrue();
        assertThat(isKeepAliveWithConnectionClose).as("isKeepAliveWithConnectionClose").isFalse();
        assertThat(isKeepAliveWithoutConnectionHeader).as("isKeepAliveWithoutConnectionHeader").isTrue();

    }

}
