package com.sms.nexus.common.exception;

public class PermissionDeniedException extends BusinessException {

    public PermissionDeniedException(String message) {
        super(403, message);
    }
}
