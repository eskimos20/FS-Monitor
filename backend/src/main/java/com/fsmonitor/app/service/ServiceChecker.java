package com.fsmonitor.app.service;

import com.fsmonitor.app.entity.Service;

public interface ServiceChecker {
    boolean check(Service service);
}
