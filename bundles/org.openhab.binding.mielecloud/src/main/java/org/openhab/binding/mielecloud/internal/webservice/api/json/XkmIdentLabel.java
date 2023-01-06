/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.mielecloud.internal.webservice.api.json;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Immutable POJO representing the XKM (Miele communication module) identification. Queried from the Miele REST API.
 *
 * @author Björn Lange - Initial contribution
 */
@NonNullByDefault
public class XkmIdentLabel {
    @Nullable
    private String techType;
    @Nullable
    private String releaseVersion;

    public Optional<String> getTechType() {
        return Optional.ofNullable(techType);
    }

    public Optional<String> getReleaseVersion() {
        return Optional.ofNullable(releaseVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(releaseVersion, techType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        XkmIdentLabel other = (XkmIdentLabel) obj;
        return Objects.equals(releaseVersion, other.releaseVersion) && Objects.equals(techType, other.techType);
    }

    @Override
    public String toString() {
        return "XkmIdentLabel [techType=" + techType + ", releaseVersion=" + releaseVersion + "]";
    }
}
