/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.tplinksmarthome.internal.model;

/**
 * Interface for data objects that have an error code in their response.
 *
 * @author Hilbrand Bouwkamp - Initial contribution
 */
@FunctionalInterface
public interface HasErrorResponse {
    /**
     * @return returns the object containing the error response
     */
    ErrorResponse getErrorResponse();
}
