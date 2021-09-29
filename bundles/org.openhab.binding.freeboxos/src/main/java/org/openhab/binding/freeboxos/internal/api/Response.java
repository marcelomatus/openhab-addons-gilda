/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.freeboxos.internal.api;

import javax.validation.Valid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.api.login.Session.Permission;

import com.google.gson.annotations.SerializedName;

/**
 * Defines an API result that returns a single object
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class Response<T> {
    public static enum ErrorCode {
        NONE,
        @SerializedName("auth_required")
        AUTHORIZATION_REQUIRED,
        @SerializedName("internal_error")
        INTERNAL_ERROR,
        @SerializedName("invalid_token")
        INVALID_TOKEN;
    }

    private boolean success;
    private ErrorCode errorCode = ErrorCode.NONE;
    private String msg = "";

    private @Nullable Permission missingRight;

    @Valid
    private @NonNullByDefault({}) T result;

    public T getResult() {
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public @Nullable Permission getMissingRight() {
        return missingRight;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMsg() {
        return msg;
    }

    public static Response<?> of(ErrorCode code) {
        Response<?> response = new Response<Object>();
        response.success = false;
        response.errorCode = code;
        return response;
    }
}
