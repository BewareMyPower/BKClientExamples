package com.github.bewaremypower.config;

public class InvalidConfigError extends RuntimeException {
    public InvalidConfigError(String message) {
        super(message);
    }
}
