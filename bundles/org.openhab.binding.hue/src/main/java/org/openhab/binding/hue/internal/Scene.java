/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.hue.internal;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.types.StateOption;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

/**
 * Basic scene information.
 *
 * @author Hengrui Jiang - Initial contribution
 */
public class Scene {
    public static final Type GSON_TYPE = new TypeToken<Map<String, Scene>>() {
    }.getType();

    private String id;
    private String name;
    @SerializedName("lights")
    private List<String> lightIds;
    @SerializedName("group")
    private String groupId;
    private boolean recycle;

    @NonNull
    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the human readable name of the scene. If the name is omitted upon creation, this
     * defaults to the ID.
     *
     * @return human readable name of the scene
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the list of lights that the scene applies to. For group scenes, this list should be identical to the list
     * of all lights that are in the group.
     *
     * @return list of lights that the scene applies to
     */
    @NonNull
    public List<String> getLightIds() {
        return lightIds;
    }

    /**
     * Returns the group that the scene belongs to. This field is optional for scenes that applies to a specific list of
     * lights instead of a group.
     *
     * @return the group that the scene belongs to
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Indicates if the scene can be recycled by the bridge. A recyclable scene is not able to be activated.
     *
     * @return whether the scene can be recycled
     */
    public boolean isRecycle() {
        return recycle;
    }

    public StateOption toStateOption(Map<String, String> groupNames) {
        StringBuilder stateOptionLabel = new StringBuilder(name);
        if (groupId != null && groupNames.containsKey(groupId)) {
            stateOptionLabel.append(" (").append(groupNames.get(groupId)).append(")");
        }
        if (!id.contentEquals(name)) {
            stateOptionLabel.append(" [").append(id).append("]");
        }

        return new StateOption(id, stateOptionLabel.toString());
    }
}
