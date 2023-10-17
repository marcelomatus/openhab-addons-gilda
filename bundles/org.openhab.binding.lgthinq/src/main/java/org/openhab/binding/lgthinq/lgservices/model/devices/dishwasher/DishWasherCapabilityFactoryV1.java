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
package org.openhab.binding.lgthinq.lgservices.model.devices.dishwasher;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lgthinq.internal.errors.LGThinqApiException;
import org.openhab.binding.lgthinq.internal.errors.LGThinqException;
import org.openhab.binding.lgthinq.lgservices.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The {@link DishWasherCapabilityFactoryV1}
 *
 * @author Nemer Daud - Initial contribution
 */
@NonNullByDefault
public class DishWasherCapabilityFactoryV1 extends AbstractDishWasherCapabilityFactory {
    private static final Logger logger = LoggerFactory.getLogger(DishWasherCapabilityFactoryV1.class);

    @Override
    public DishWasherCapability create(JsonNode rootNode) throws LGThinqException {
        DishWasherCapability cap = super.create(rootNode);
        cap.setRemoteStartFeatName("RemoteStart");
        cap.setChildLockFeatName("ChildLock");
        cap.setDoorLockFeatName("DoorLock");
        return cap;
    }

    @Override
    protected String getStateFeatureNodeName() {
        return "State";
    }

    @Override
    protected String getProcessStateNodeName() {
        return "PreState";
    }

    @Override
    protected String getPreStateFeatureNodeName() {
        return "PreState";
    }

    @Override
    protected String getRinseFeatureNodeName() {
        return "RinseOption";
    }

    @Override
    protected String getTemperatureFeatureNodeName() {
        return "WaterTemp";
    }

    @Override
    protected String getSpinFeatureNodeName() {
        return "SpinSpeed";
    }

    @Override
    protected String getSoilWashFeatureNodeName() {
        return "Wash";
    }

    @Override
    protected String getDoorLockFeatureNodeName() {
        // there is no dook lock node in V1.
        return "DUMMY_DOOR_LOCK";
    }

    @Override
    protected MonitoringResultFormat getMonitorDataFormat(JsonNode rootNode) {
        String type = rootNode.path("Monitoring").path("type").textValue();
        return MonitoringResultFormat.getFormatOf(Objects.requireNonNullElse(type, ""));
    }

    @Override
    protected Map<String, CommandDefinition> getCommandsDefinition(JsonNode rootNode) throws LGThinqApiException {
        return getCommandsDefinitionV1(rootNode);
    }

    @Override
    protected String getCommandRemoteStartNodeName() {
        return "OperationStart";
    }

    @Override
    protected String getCommandStopNodeName() {
        return "OperationStop";
    }

    @Override
    protected String getCommandWakeUpNodeName() {
        return "OperationWakeUp";
    }

    @Override
    protected String getDefaultCourseIdNodeName() {
        return "defaultCourseId";
    }

    @Override
    protected String getDryLevelNodeName() {
        return "DryLevel";
    }

    @Override
    protected String getNotSelectedCourseKey() {
        return "0";
    }

    @Override
    protected List<LGAPIVerion> getSupportedAPIVersions() {
        return List.of(LGAPIVerion.V1_0);
    }

    @Override
    protected FeatureDefinition newFeatureDefinition(String featureName, JsonNode featuresNode,
            @Nullable String targetChannelId, @Nullable String refChannelId) {
        JsonNode featureNode = featuresNode.path(featureName);
        if (featureNode.isMissingNode()) {
            return FeatureDefinition.NULL_DEFINITION;
        }
        FeatureDefinition fd = new FeatureDefinition();
        fd.setName(featureName);
        fd.setLabel(featureName);
        fd.setChannelId(Objects.requireNonNullElse(targetChannelId, ""));
        fd.setRefChannelId(Objects.requireNonNullElse(refChannelId, ""));
        // All features from V1 are ENUMs
        fd.setDataType(FeatureDataType.ENUM);
        JsonNode valuesMappingNode = featureNode.path("option");
        if (!valuesMappingNode.isMissingNode()) {

            Map<String, String> valuesMapping = new HashMap<>();
            valuesMappingNode.fields().forEachRemaining(e -> {
                // collect values as:
                //
                // "option":{
                // "0":"@WM_STATE_POWER_OFF_W",
                // to "0" -> "@WM_STATE_POWER_OFF_W"
                valuesMapping.put(e.getKey(), e.getValue().asText());
            });
            fd.setValuesMapping(valuesMapping);
        }

        return fd;
    }

    @Override
    public DishWasherCapability getCapabilityInstance() {
        return new DishWasherCapability();
    }

    @Override
    /*
     * Return the default Course ID.
     * OBS:In the V1, the default course points to the ID of the course list that is the default.
     */
    protected String getDefaultCourse(JsonNode rootNode) {
        return rootNode.path("Config").path("defaultCourseId").textValue();
    }

    @Override
    protected String getRemoteFeatName() {
        return "RemoteStart";
    }

    @Override
    protected String getStandByFeatName() {
        return "Standby";
    }

    @Override
    protected String getConfigCourseType(JsonNode rootNode) {
        if (rootNode.path(getMonitorValueNodeName()).path("APCourse").isMissingNode()) {
            return "Course";
        } else {
            return "APCourse";
        }
    }

    @Override
    protected String getCourseNodeName(JsonNode rootNode) {
        JsonNode refOptions = rootNode.path(getMonitorValueNodeName()).path(getConfigCourseType(rootNode))
                .path("option");
        if (refOptions.isArray()) {
            AtomicReference<String> courseNodeName = new AtomicReference<>("");
            for (JsonNode node : refOptions) {
                return node.asText();
            }
        }
        return "";
    }

    @Override
    protected String getSmartCourseNodeName(JsonNode rootNode) {
        return "SmartCourse";
    }

    @Override
    protected String getConfigSmartCourseType(JsonNode rootNote) {
        return "SmartCourse";
    }

    @Override
    protected String getConfigDownloadCourseType(JsonNode rootNode) {
        // just to ignore because there is no DownloadCourseType in V1
        return "XXXXXXXXXXX";
    }

    @Override
    protected String getMonitorValueNodeName() {
        return "Value";
    }
}
