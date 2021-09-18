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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.freeboxos.internal.api.login.Session.Permission;

import com.google.gson.annotations.SerializedName;

/**
 * Base class for all api responses
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class BaseResponse {
    public static enum ErrorCode {
        NONE,
        @SerializedName("auth_required")
        AUTHORIZATION_REQUIRED,
        @SerializedName("invalid_token")
        INTERNAL_ERROR,
        @SerializedName("internal_error")
        INVALID_TOKEN;
    }

    private boolean success;
    private ErrorCode errorCode = ErrorCode.NONE;
    private @Nullable String msg;
    private @Nullable Permission missingRight;

    // protected @Nullable String internalEvaluate() {
    // return !success ? msg : null;
    // }

    // public void evaluate() throws FreeboxException {
    // String error = internalEvaluate();
    // if (error != null) {
    // throw new FreeboxException(this, error);
    // }
    // }

    public @Nullable Permission getMissingRight() {
        return missingRight;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public static BaseResponse of(ErrorCode code) {
        BaseResponse response = new BaseResponse();
        response.success = false;
        response.errorCode = code;
        return response;
    }
}
