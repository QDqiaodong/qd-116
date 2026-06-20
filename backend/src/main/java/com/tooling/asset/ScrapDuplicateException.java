package com.tooling.asset;

import lombok.Getter;

@Getter
public class ScrapDuplicateException extends RuntimeException {

    private final ScrapRecord existingRecord;

    public ScrapDuplicateException(String message, ScrapRecord existingRecord) {
        super(message);
        this.existingRecord = existingRecord;
    }
}
