package com.fsmonitor.app.entity;

public enum CheckMethod {
    TCP,    // Simple TCP socket connection
    HTTP,   // HTTP GET request
    PING    // ICMP/TCP ping
}
