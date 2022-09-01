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
package org.openhab.binding.freeboxos.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.login.Session.Permission;

/**
 * Exception for errors when Session require missing permission
 *
 * @author ben12 - Initial contribution
 */
@NonNullByDefault
public class MissingPermissionException extends FreeboxException {
    private static final long serialVersionUID = 3965810786699311126L;

    private Permission permission;

    public MissingPermissionException(Permission permission, Exception cause, String format, Object... args) {
        super(cause, format, args);
        this.permission = permission;
    }

    public MissingPermissionException(Permission permission, String format, Object... args) {
        super(format, args);
        this.permission = permission;
    }

    public MissingPermissionException(Permission permission, String msg) {
        super(msg);
        this.permission = permission;
    }

    public Permission getPermission() {
        return permission;
    }
}
