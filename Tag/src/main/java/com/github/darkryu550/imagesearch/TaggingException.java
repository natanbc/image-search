package com.github.darkryu550.imagesearch;

public class TaggingException extends Exception {
    public TaggingException() {
    }

    public TaggingException(String message) {
        super(message);
    }

    public TaggingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaggingException(Throwable cause) {
        super(cause);
    }
}
