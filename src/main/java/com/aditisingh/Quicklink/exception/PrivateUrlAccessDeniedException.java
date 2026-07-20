package com.aditisingh.Quicklink.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PrivateUrlAccessDeniedException extends RuntimeException {
}
