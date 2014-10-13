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
package com.globo.galeb.metrics;

import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.logging.Logger;

public class StatsdClient {
    private static final String PATTERN_COUNT = "%s:%s|c";
    private static final String PATTERN_TIME  = "%s:%s|ms";
    private static final String PATTERN_GAUGE = "%s:%s|g";
    private static final String PATTERN_SET   = "%s:%s|s";

    public static enum TypeStatsdMessage {
        COUNT(PATTERN_COUNT),
        TIME(PATTERN_TIME),
        GAUGE(PATTERN_GAUGE),
        SET(PATTERN_SET);

        private final String pattern;
        private TypeStatsdMessage(String pattern) {
            this.pattern = pattern;
        }
        public String getPattern() {
            return this.pattern;
        }
    }

    private String statsDhost;
    private Integer statsDPort;
    private String prefix;
    private final Logger log;
    private final DatagramSocket socket;

    public StatsdClient(String statsDhost, Integer statsDPort, String prefix,
                        final DatagramSocket socket, final Logger log) {
        this.statsDhost = statsDhost;
        this.statsDPort = statsDPort;
        this.prefix = "".equals(prefix) ? "stats" : prefix;
        this.log = log;
        this.socket = socket;
    }

    public StatsdClient(final DatagramSocket socket, final Logger log) {
        this("localhost", 8125, "", socket, log);
    }

    public void send(final TypeStatsdMessage type, String message) {
        String[] data = message.split(":");
        String key = data[0];
        String value = data[1];
        try {
            String id = String.format("".equals(prefix) ? "%s%s": "%s.%s", prefix, key);
            socket.send(String.format(type.getPattern(), id, value), statsDhost, statsDPort, null);
        } catch (io.netty.channel.ChannelException e) {
            log.error("io.netty.channel.ChannelException: Failed to open a socket.");
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
    }
}
