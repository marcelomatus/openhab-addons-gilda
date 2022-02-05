/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.vesync.internal.dto.requests;

import org.openhab.binding.vesync.internal.dto.responses.VesyncLoginResponse;
import org.openhab.binding.vesync.internal.exceptions.AuthenticationException;

import com.google.gson.annotations.SerializedName;

/**
 * The {@link VesyncAuthenticatedRequest} is a Java class used as a DTO to hold the Vesync's API's common request data.
 *
 * @author David Goodyear - Initial contribution
 */
public class VesyncAuthenticatedRequest extends VesyncRequest {

    @SerializedName("accountID")
    public String accountId;

    @SerializedName("token")
    public String token;

    public VesyncAuthenticatedRequest() {
        super();
    }

    public VesyncAuthenticatedRequest(final VesyncLoginResponse.VesyncUserSession user) throws AuthenticationException {
        super();
        if (user == null)
            throw new AuthenticationException("User is not logged in");
        this.token = user.getToken();
        this.accountId = user.getAccountId();
    }

    public void ApplyAuthentication(final VesyncLoginResponse.VesyncUserSession userSession)
            throws AuthenticationException {
        if (userSession == null)
            throw new AuthenticationException("User is not logged in");
        this.accountId = userSession.getAccountId();
        this.token = userSession.getToken();
    }
}
