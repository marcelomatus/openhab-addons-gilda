/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.siemenshvac.internal.metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.siemenshvac.internal.constants.SiemensHvacBindingConstants;
import org.openhab.binding.siemenshvac.internal.handler.SiemensHvacBridgeConfig;
import org.openhab.binding.siemenshvac.internal.network.SiemensHvacCallback;
import org.openhab.binding.siemenshvac.internal.network.SiemensHvacConnector;
import org.openhab.binding.siemenshvac.internal.type.SiemensHvacChannelGroupTypeProvider;
import org.openhab.binding.siemenshvac.internal.type.SiemensHvacChannelTypeProvider;
import org.openhab.binding.siemenshvac.internal.type.SiemensHvacConfigDescriptionProvider;
import org.openhab.binding.siemenshvac.internal.type.SiemensHvacException;
import org.openhab.binding.siemenshvac.internal.type.SiemensHvacThingTypeProvider;
import org.openhab.binding.siemenshvac.internal.type.UidUtils;
import org.openhab.core.OpenHAB;
import org.openhab.core.config.core.ConfigDescriptionBuilder;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelDefinitionBuilder;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.StateChannelTypeBuilder;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class SiemensHvacMetadataRegistryImpl implements SiemensHvacMetadataRegistry {

    private final Logger logger = LoggerFactory.getLogger(SiemensHvacMetadataRegistryImpl.class);

    // A map contains data point config read from Api and/or WebPages
    private Map<String, SiemensHvacMetadata> dptMap = new Hashtable<String, SiemensHvacMetadata>();
    private @Nullable SiemensHvacMetadata root = null;
    private @Nullable ArrayList<SiemensHvacMetadataDevice> devices = null;

    private static final String JSON_DIR = OpenHAB.getUserDataFolder() + File.separatorChar + "jsondb";

    private @Nullable SiemensHvacThingTypeProvider thingTypeProvider;
    private @Nullable SiemensHvacChannelTypeProvider channelTypeProvider;
    private @Nullable SiemensHvacChannelGroupTypeProvider channelGroupTypeProvider;
    private @Nullable SiemensHvacConfigDescriptionProvider configDescriptionProvider;
    private @Nullable SiemensHvacConnector hvacConnector;

    private final HashMap<String, SiemensHvacMetadataUser> userList;

    public SiemensHvacMetadataRegistryImpl() {
        userList = new HashMap<String, SiemensHvacMetadataUser>();
    }

    @Reference
    protected void setSiemensHvacConnector(SiemensHvacConnector hvacConnector) {
        this.hvacConnector = hvacConnector;
    }

    protected void unsetSiemensHvacConnector(SiemensHvacConnector hvacConnector) {
        this.hvacConnector = null;
    }

    @Reference
    protected void setThingTypeProvider(SiemensHvacThingTypeProvider thingTypeProvider) {
        this.thingTypeProvider = thingTypeProvider;
    }

    protected void unsetThingTypeProvider(SiemensHvacThingTypeProvider thingTypeProvider) {
        this.thingTypeProvider = null;
    }

    @Reference
    protected void setChannelTypeProvider(SiemensHvacChannelTypeProvider channelTypeProvider) {
        this.channelTypeProvider = channelTypeProvider;
    }

    protected void unsetChannelTypeProvider(SiemensHvacChannelTypeProvider channelTypeProvider) {
        this.channelTypeProvider = null;
    }

    //
    @Reference
    protected void setChannelGroupTypeProvider(SiemensHvacChannelGroupTypeProvider channelGroupTypeProvider) {
        this.channelGroupTypeProvider = channelGroupTypeProvider;
    }

    protected void unsetChannelGroupTypeProvider(SiemensHvacChannelGroupTypeProvider channelGroupTypeProvider) {
        this.channelGroupTypeProvider = null;
    }

    @Reference
    protected void setConfigDescriptionProvider(SiemensHvacConfigDescriptionProvider configDescriptionProvider) {
        this.configDescriptionProvider = configDescriptionProvider;
    }

    protected void unsetConfigDescriptionProvider(SiemensHvacConfigDescriptionProvider configDescriptionProvider) {
        this.configDescriptionProvider = null;
    }

    @Override
    public @Nullable SiemensHvacConnector getSiemensHvacConnector() {
        return this.hvacConnector;
    }

    @Override
    public @Nullable SiemensHvacChannelTypeProvider getChannelTypeProvider() {
        return this.channelTypeProvider;
    }

    @Override
    public @Nullable ArrayList<SiemensHvacMetadataDevice> getDevices() {
        return devices;
    }

    /**
     * Initializes the type generator.
     */
    @Override
    @Activate
    public void initialize() {
    }

    public void initDptMap(@Nullable SiemensHvacMetadata node) {
        if (node == null) {
            return;
        }

        if (node.getClass() == SiemensHvacMetadataMenu.class) {
            SiemensHvacMetadataMenu mInformation = (SiemensHvacMetadataMenu) node;

            for (SiemensHvacMetadata child : mInformation.getChilds().values()) {
                initDptMap(child);
            }
        }

        if (!node.getLongDesc().isEmpty()) {
            dptMap.put("byName" + node.getLongDesc(), node);
        }
        if (!node.getShortDesc().isEmpty()) {
            dptMap.put("byName" + node.getShortDesc(), node);
        }

        dptMap.put("byId" + node.getId(), node);
        dptMap.put("bySubId" + node.getSubId(), node);

        if (node.getClass() == SiemensHvacMetadataDataPoint.class) {
            SiemensHvacMetadataDataPoint dpi = (SiemensHvacMetadataDataPoint) node;
            dptMap.put("byDptId" + dpi.getDptId(), node);
        }
    }

    class ResolveCount {
        private int resolveCount = 0;

        public ResolveCount(int count) {
            resolveCount = count;
        }

        public void decreaseResolveCount() {
            resolveCount--;
        }

        public int getResolveCount() {
            return resolveCount;
        }
    }

    public void resolveDetails(int unresolveCountP) {
        ResolveCount rv = new ResolveCount(unresolveCountP);

        for (String key : dptMap.keySet()) {
            if (key.indexOf("byId") < 0) {
                continue;
            }

            SiemensHvacMetadata node = dptMap.get(key);
            if (node != null) {
                if (node.getClass() == SiemensHvacMetadataDataPoint.class) {
                    SiemensHvacMetadataDataPoint dpi = (SiemensHvacMetadataDataPoint) node;
                    if (!dpi.getDetailsResolved()) {
                        resolveDptDetails(dpi, rv);
                    }
                }
            }
        }
    }

    public int unresolveCount() {
        int count = 0;
        for (String key : dptMap.keySet()) {
            if (key.indexOf("byId") < 0) {
                continue;
            }

            SiemensHvacMetadata node = dptMap.get(key);
            if (node != null) {
                if (node.getClass() == SiemensHvacMetadataDataPoint.class) {
                    SiemensHvacMetadataDataPoint dpi = (SiemensHvacMetadataDataPoint) node;
                    if (!dpi.getDetailsResolved()) {
                        count++;
                    }
                }
            }

        }

        return count;
    }

    @Override
    public @Nullable SiemensHvacMetadataMenu getRoot() {
        return (SiemensHvacMetadataMenu) root;
    }

    @Override
    public void readMeta() throws SiemensHvacException {
        ArrayList<SiemensHvacMetadataDevice> lcDevices = devices;
        SiemensHvacConnector lcHvacConnector = hvacConnector;

        if (root != null) {
            return;
        }

        if (lcHvacConnector == null) {
            logger.debug("SiemensHvacMetadataRegistryImpl:ReadMeta() : lHvacConnector not initialize.");
            return;
        }

        readUserInfo();

        SiemensHvacBridgeConfig config = lcHvacConnector.getBridgeConfiguration();
        if (config == null) {
            logger.debug("SiemensHvacMetadataRegistryImpl:ReadMeta() : config not initialize.");
            return;
        }

        SiemensHvacMetadataUser user = null;

        String userName = config.userName;
        if (userList.containsKey(userName)) {
            user = userList.get(userName);
        }

        if (user == null) {
            logger.error("SiemensHvacMetadataRegistryImpl:ReadMeta() : cannot find user, aborting.");
            return;
        }

        logger.trace("siemensHvac:Initialization():Begin_0001");

        logger.trace("siemensHvac:Initialization():ReadCache");
        loadMetaDataFromCache();

        // increase the timeout during this phase
        // because we queued a lot of request
        // and timeout start to run when request is queued (not executed)

        Instant start = Instant.now();
        lcHvacConnector.setTimeOut(600);

        logger.trace("siemensHvac:Initialization():ReadDeviceList");
        readDeviceList();

        if (root == null) {
            logger.trace("siemensHvac:Initialization():No cache information, root==null, reading metadata from device");

            logger.trace("siemensHvac:Initialization():BeginReadMenu");
            root = new SiemensHvacMetadataMenu();

            changeLanguage(user, 1);
            readMetaData(root, -1, false);
            lcHvacConnector.waitNoNewRequest();
            lcHvacConnector.waitAllPendingRequest();

            changeLanguage(user, user.getLanguageId());
            readMetaData(root, -1, true);
            lcHvacConnector.waitNoNewRequest();
            lcHvacConnector.waitAllPendingRequest();

            logger.trace("siemensHvac:Initialization():EndReadMenu");
        }

        if (root != null) {
            logger.trace("siemensHvac:Initialization():BeginInitDptMap");
            initDptMap(root);
            logger.trace("siemensHvac:Initialization():EndInitDptMap");
        }

        int unresolveCount = unresolveCount();

        while (unresolveCount > 0) {
            logger.trace("siemensHvac:Initialization():BeginResolveDtpMap {}", unresolveCount);
            resolveDetails(unresolveCount);
            lcHvacConnector.waitAllPendingRequest();
            unresolveCount = unresolveCount();
        }

        Instant end = Instant.now();
        lcHvacConnector.setTimeOut(30);

        long elapseTime = Duration.between(start, end).toSeconds();
        logger.trace("siemensHvac:Initialization():ReadMetadata in {} s", elapseTime);

        logger.trace("siemensHvac:Initialization():SaveCache");
        saveMetaDataToCache();

        logger.trace("siemensHvac:Initialization():InitThing");
        getRoot();
        lcDevices = devices;
        if (lcDevices != null) {
            for (SiemensHvacMetadataDevice device : lcDevices) {
                generateThingsType(device);
            }
        }

        logger.trace("siemensHvac:InitDptMap():end");
    }

    private void generateThingsType(SiemensHvacMetadataDevice device) {
        SiemensHvacThingTypeProvider lcThingTypeProvider = thingTypeProvider;
        logger.debug("Generate thing types for device: {} / {}", device.getName(), device.getSerialNr());
        if (lcThingTypeProvider != null) {
            ThingTypeUID thingTypeUID = UidUtils.generateThingTypeUID(device);
            ThingType tt = null;

            tt = lcThingTypeProvider.getInternalThingType(thingTypeUID);

            if (tt == null) {
                List<ChannelGroupType> groupTypes = new ArrayList<>();

                int treeId = device.getTreeId();
                if (dptMap.containsKey("byId" + treeId)) {
                    SiemensHvacMetadataMenu menu = (SiemensHvacMetadataMenu) dptMap.get("byId" + treeId);

                    if (menu != null) {
                        var childs = menu.getChilds().values();
                        for (SiemensHvacMetadata child : childs) {
                            generateThingsType(child, groupTypes, menu);
                        }

                    }

                }

                tt = createThingType(device, groupTypes);
                lcThingTypeProvider.addThingType(tt);
            }
        }
    }

    private void generateThingsType(SiemensHvacMetadata child, List<ChannelGroupType> groupTypes,
            SiemensHvacMetadataMenu menu) {
        SiemensHvacChannelTypeProvider lcChannelTypeProvider = channelTypeProvider;
        SiemensHvacChannelGroupTypeProvider lcChannelGroupTypeProvider = channelGroupTypeProvider;

        if (child instanceof SiemensHvacMetadataMenu subMenu) {
            List<ChannelDefinition> channelDefinitions = new ArrayList<>();

            for (SiemensHvacMetadata childDt : subMenu.getChilds().values()) {

                try {
                    if (childDt instanceof SiemensHvacMetadataMenu) {
                        generateThingsType(childDt, groupTypes, menu);
                    }
                    if (childDt instanceof SiemensHvacMetadataDataPoint metadataDataPoint) {
                        SiemensHvacMetadataDataPoint dataPoint = metadataDataPoint;

                        if (dataPoint.getDptType().isEmpty()) {
                            continue;
                        }

                        ChannelTypeUID channelTypeUID = UidUtils.generateChannelTypeUID(dataPoint);

                        ChannelType channelType = null;

                        if (channelTypeProvider != null && lcChannelTypeProvider != null) {
                            channelType = lcChannelTypeProvider.getInternalChannelType(channelTypeUID);
                            if (channelType == null) {
                                channelType = createChannelType(dataPoint, channelTypeUID);
                                lcChannelTypeProvider.addChannelType(channelType);
                            }
                        }

                        SiemensHvacMetadataDataPoint dpt = ((SiemensHvacMetadataDataPoint) childDt);

                        Map<String, String> props = new Hashtable<String, String>();
                        props.put("dptId", "" + dpt.getDptId());
                        props.put("id", "" + dpt.getId());
                        props.put("subId", "" + dpt.getSubId());
                        props.put("groupdId", "" + dpt.getGroupId());

                        String id = dataPoint.getId() + "-" + UidUtils.sanetizeId(dataPoint.getShortDesc());

                        if (channelType != null) {
                            ChannelDefinition channelDef = new ChannelDefinitionBuilder(id, channelType.getUID())
                                    .withLabel(dataPoint.getShortDesc()).withDescription(dataPoint.getLongDesc())
                                    .withProperties(props).build();

                            channelDefinitions.add(channelDef);
                        }
                    }
                } catch (Exception ex) {
                    logger.warn("Unable to create channel for: {}", childDt);
                }
            }

            // generate group
            ChannelGroupTypeUID groupTypeUID = UidUtils.generateChannelGroupTypeUID(subMenu);
            ChannelGroupType groupType = null;

            if (lcChannelGroupTypeProvider != null) {
                groupType = lcChannelGroupTypeProvider.getInternalChannelGroupType(groupTypeUID);

                if (groupType == null) {
                    String groupLabel = subMenu.getShortDesc();
                    groupType = ChannelGroupTypeBuilder.instance(groupTypeUID, groupLabel)
                            .withChannelDefinitions(channelDefinitions).withCategory("")
                            .withDescription(menu.getLongDesc()).build();
                    lcChannelGroupTypeProvider.addChannelGroupType(groupType);
                    groupTypes.add(groupType);
                }
            }

        }
    }

    private ChannelType createChannelType(SiemensHvacMetadataDataPoint dpt, ChannelTypeUID channelTypeUID) {
        ChannelType channelType;

        String itemType = getItemType(dpt);
        String category = getCategory(dpt);
        String label = itemType;
        String description = "";

        StateDescriptionFragmentBuilder stateFragment = StateDescriptionFragmentBuilder.create();

        List<StateOption> options = new ArrayList<StateOption>();
        if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_ENUM)) {
            StringBuilder descBuilder = new StringBuilder();
            descBuilder.append("Enum:");
            List<SiemensHvacMetadataPointChild> childs = dpt.getChild();
            int idx = 0;

            for (SiemensHvacMetadataPointChild opt : childs) {
                StateOption stOpt = new StateOption(opt.getValue(), opt.getText());
                options.add(stOpt);
                if (idx > 0) {
                    descBuilder.append("_");
                }

                descBuilder.append(String.format("(%s:%s)", opt.getValue(), opt.getText()));
                idx++;
            }
            description = descBuilder.toString();
            label = channelTypeUID.getId();
        }

        if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_NUMERIC)) {
            BigDecimal min = new BigDecimal(dpt.getMin());
            BigDecimal max = new BigDecimal(dpt.getMax());
            BigDecimal step = new BigDecimal(dpt.getResolution());

            stateFragment = stateFragment.withMinimum(min).withMaximum(max).withStep(step).withReadOnly(false);

            description = channelTypeUID.toString();
            label = channelTypeUID.getId();
        } else {
            stateFragment = stateFragment.withPattern(getStatePattern(dpt)).withReadOnly(!dpt.getWriteAccess());
        }

        if (!options.isEmpty()) {
            stateFragment = stateFragment.withOptions(options);
        }

        boolean isAdvanced = false;
        if (channelTypeUID.getId().contains("-y")) {
            isAdvanced = true;
        }
        if (channelTypeUID.getId().contains("-k")) {
            isAdvanced = true;
        }
        if (channelTypeUID.getId().contains("histo")) {
            isAdvanced = true;
        }
        if (channelTypeUID.getId().contains("-qx")) {
            isAdvanced = true;
        }

        final StateChannelTypeBuilder channelTypeBuilder = ChannelTypeBuilder.state(channelTypeUID, label, itemType)
                .withStateDescriptionFragment(stateFragment.build());

        channelType = channelTypeBuilder.isAdvanced(isAdvanced).withDescription(description).withCategory(category)
                .build();

        return channelType;
    }

    /**
     * Creates the ThingType for the given device.
     */
    private ThingType createThingType(SiemensHvacMetadataDevice device, List<ChannelGroupType> groupTypes) {
        SiemensHvacConfigDescriptionProvider lcConfigDescriptionProvider = configDescriptionProvider;
        String name = device.getName();
        String description = device.getName();

        List<String> supportedBridgeTypeUids = new ArrayList<>();
        supportedBridgeTypeUids.add(SiemensHvacBindingConstants.THING_TYPE_OZW672.toString());
        ThingTypeUID thingTypeUID = UidUtils.generateThingTypeUID(device);

        Map<String, String> properties = new HashMap<>();
        properties.put(Thing.PROPERTY_VENDOR, SiemensHvacBindingConstants.PROPERTY_VENDOR_NAME);
        properties.put(Thing.PROPERTY_MODEL_ID, device.getType());

        URI configDescriptionURI = getConfigDescriptionURI(device);
        if (lcConfigDescriptionProvider != null
                && lcConfigDescriptionProvider.getInternalConfigDescription(configDescriptionURI) == null) {
            generateConfigDescription(device, groupTypes, configDescriptionURI);
        }

        List<ChannelGroupDefinition> groupDefinitions = new ArrayList<>();
        for (ChannelGroupType groupType : groupTypes) {
            String id = groupType.getUID().getId();
            groupDefinitions.add(new ChannelGroupDefinition(id, groupType.getUID()));
        }

        return ThingTypeBuilder.instance(thingTypeUID, name).withSupportedBridgeTypeUIDs(supportedBridgeTypeUids)
                .withDescription(description).withChannelGroupDefinitions(groupDefinitions).withProperties(properties)
                .withRepresentationProperty(Thing.PROPERTY_MODEL_ID).withConfigDescriptionURI(configDescriptionURI)
                .withCategory(SiemensHvacBindingConstants.CATEGORY_THING_HVAC).build();
    }

    private URI getConfigDescriptionURI(SiemensHvacMetadataDevice device) {
        return URI.create((String.format("%s:%s", SiemensHvacBindingConstants.CONFIG_DESCRIPTION_URI_THING_PREFIX,
                UidUtils.generateThingTypeUID(device))));
    }

    private void generateConfigDescription(SiemensHvacMetadataDevice device, List<ChannelGroupType> groupTypes,
            URI configDescriptionURI) {
        SiemensHvacConfigDescriptionProvider lcConfigDescriptionProvider = configDescriptionProvider;
        List<ConfigDescriptionParameter> parms = new ArrayList<>();
        List<ConfigDescriptionParameterGroup> groups = new ArrayList<>();

        if (lcConfigDescriptionProvider != null) {
            lcConfigDescriptionProvider.addConfigDescription(ConfigDescriptionBuilder.create(configDescriptionURI)
                    .withParameters(parms).withParameterGroups(groups).build());
        }
    }

    public String getItemType(SiemensHvacMetadataDataPoint dpt) {
        if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_STRING)) {
            return SiemensHvacBindingConstants.ITEM_TYPE_STRING;
        } else if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_NUMERIC)) {
            return SiemensHvacBindingConstants.ITEM_TYPE_NUMBER;
        } else if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_ENUM)) {
            return SiemensHvacBindingConstants.ITEM_TYPE_ENUMERATION;
        } else if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_DATE_TIME)) {
            return SiemensHvacBindingConstants.ITEM_TYPE_DATETIME;
        } else if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_TIME)) {
            return SiemensHvacBindingConstants.ITEM_TYPE_DATETIME;
        } else if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_RADIO)) {
            return SiemensHvacBindingConstants.ITEM_TYPE_CONTACT;
        } else if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_SCHEDULER)) {
            return SiemensHvacBindingConstants.ITEM_TYPE_DATETIME;
        } else if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_CALENDAR)) {
            return SiemensHvacBindingConstants.ITEM_TYPE_DATETIME;
        } else {
            logger.debug("unknow type in getItemType()");

        }

        return "";
    }

    /**
     * Determines the category for the given Datapoint.
     */
    public static String getCategory(SiemensHvacMetadataDataPoint dp) {
        String dpType = dp.getDptType();
        String dptUnit = dp.getDptUnit();

        if (dptUnit == null) {
            return "";
        } else if (dptUnit.contains("°C")) {
            return SiemensHvacBindingConstants.CATEGORY_CHANNEL_PROPS_TEMP;
        } else if (dpType.contains(SiemensHvacBindingConstants.DPT_TYPE_DATE_TIME)) {
            return SiemensHvacBindingConstants.CATEGORY_CHANNEL_PROPS_TIME;
        } else if (dpType.contains(SiemensHvacBindingConstants.DPT_TYPE_TIME)) {
            return SiemensHvacBindingConstants.CATEGORY_CHANNEL_PROPS_TIME;
        } else if (dpType.contains(SiemensHvacBindingConstants.DPT_TYPE_ENUM)) {
            return SiemensHvacBindingConstants.CATEGORY_CHANNEL_WIDGETS_SWITCH;
        } else if (dpType.contains(SiemensHvacBindingConstants.DPT_TYPE_RADIO)) {
            return SiemensHvacBindingConstants.CATEGORY_CHANNEL_WIDGETS_SWITCH;
        } else if (dpType.contains(SiemensHvacBindingConstants.DPT_TYPE_NUMERIC)) {
            return SiemensHvacBindingConstants.CATEGORY_CHANNEL_WIDGETS_NUMBER;
        } else {
            return SiemensHvacBindingConstants.CATEGORY_CHANNEL_CONTROL_HEATING;
        }
    }

    /**
     * Returns the state pattern metadata string with unit for the given Datapoint.
     */
    public static String getStatePattern(SiemensHvacMetadataDataPoint dpt) {
        String unit = dpt.getDptUnit();

        if ("%".equals(unit)) {
            return "%d %%";
        }

        if (unit != null && !unit.isEmpty()) {
            if (dpt.getDptType().equals(SiemensHvacBindingConstants.DPT_TYPE_NUMERIC)) {
                return String.format("%s %s", "%d", "%unit%");
            }
        }

        return "";
    }

    public void readUserInfo() throws SiemensHvacException {
        try {
            SiemensHvacConnector lcHvacConnector = hvacConnector;
            String request = "main.app?section=settings&subsection=user";

            if (lcHvacConnector != null) {
                String response = lcHvacConnector.doBasicRequest(request);

                if (response != null) {
                    String st = response;
                    st = st.replace("\n", "");

                    Pattern pattern1 = Pattern.compile("table class=\\\"user_table\\\".*?>(.*?)<\\/table>");
                    Matcher matcher1 = pattern1.matcher(st);

                    if (matcher1.find()) {
                        String userTable = matcher1.group(1);

                        Pattern pattern2 = Pattern.compile("<tr.*?>(.*?)<\\/tr>");
                        Matcher matcher2 = pattern2.matcher(userTable);

                        int idx = 0;
                        while (matcher2.find()) {
                            String line = matcher2.group(1);

                            if (idx > 0) {
                                Pattern pattern3 = Pattern.compile("<td(.*?)>(.*?)<\\/td>");
                                Matcher matcher3 = pattern3.matcher(line);

                                int idxCell = 0;
                                String userName = "";
                                String userEdit = "";
                                String userId = "";
                                while (matcher3.find()) {
                                    String cell = matcher3.group(2);
                                    String header = matcher3.group(1);

                                    if (idxCell == 0) {
                                        userName = cell;
                                    } else if (idxCell == 5) {
                                        userEdit = header;
                                    }
                                    idxCell++;
                                }

                                if ("".equals(userName)) {
                                    continue;
                                }

                                Pattern pattern4 = Pattern.compile("userid=(.+?)");
                                Matcher matcher4 = pattern4.matcher(userEdit);

                                SiemensHvacMetadataUser user = new SiemensHvacMetadataUser();
                                user.setName(userName);

                                if (matcher4.find()) {
                                    userId = matcher4.group(1);
                                    user.setId(Integer.parseInt(userId));
                                } else {
                                    userId = null;
                                    user.setId(-1);
                                }

                                request = "main.app?section=settings&subsection=user&action=modify";
                                if (userId != null) {
                                    request = request + "&userid=" + userId;
                                }
                                response = lcHvacConnector.doBasicRequest(request);

                                Pattern pattern5 = Pattern.compile(
                                        "<select name=\\\"language\\\".*>((.*|\\n)*?)</select>", Pattern.MULTILINE);
                                Matcher matcher5 = pattern5.matcher(response);

                                if (matcher5.find()) {
                                    String optionsList = matcher5.group(1);

                                    Pattern pattern6 = java.util.regex.Pattern.compile(
                                            "<option value=\\\"([^ ]*)\\\"(.*)>(.*)</option>", Pattern.MULTILINE);
                                    Matcher matcher6 = pattern6.matcher(optionsList);

                                    while (matcher6.find()) {
                                        String id = matcher6.group(1);
                                        String opt = matcher6.group(2);
                                        String lang = matcher6.group(3);

                                        if (opt.indexOf("selected") >= 0) {
                                            user.setLanguage(lang);
                                            user.setLanguageId(Integer.parseInt(id));
                                        }
                                    }
                                }

                                userList.put(userName, user);
                            }

                            idx++;

                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("siemensHvac:ResolveDpt:Error during reading user info: {}", e.getLocalizedMessage());
            throw new SiemensHvacException("Error durring reading user info", e);
            // Reset sessionId so we redone _auth on error
        }
    }

    public void changeLanguage(SiemensHvacMetadataUser user, int lang) {
        try {
            SiemensHvacConnector lcHvacConnector = hvacConnector;
            String request = "main.app?section=settings&subsection=user&action=modify";
            if (user.getId() != -1) {
                request = request + "&userid=" + user.getId();
            }
            request = request + "&language=" + lang + "&submit=OK";
            if (lcHvacConnector != null) {
                lcHvacConnector.doBasicRequest(request);
                lcHvacConnector.resetSessionId(null, false);
                lcHvacConnector.resetSessionId(null, true);
            }

        } catch (

        Exception e) {
            logger.error("siemensHvac:ResolveDpt:Error during dp reading: {}", e.getLocalizedMessage());
            // Reset sessionId so we redone _auth on error
        }
    }

    public void readDeviceList() {
        try {
            SiemensHvacConnector lcHvacConnector = hvacConnector;
            ArrayList<SiemensHvacMetadataDevice> lcDevices = devices;

            lcDevices = new ArrayList<SiemensHvacMetadataDevice>();
            devices = lcDevices;
            String request = "api/devicelist/list.json?";

            JsonObject response = null;
            if (lcHvacConnector != null) {
                response = lcHvacConnector.doRequest(request);
            }
            JsonArray devicesList = null;
            if (response != null) {
                devicesList = response.getAsJsonArray("Devices");
            }

            if (devicesList == null) {
                return;
            }

            for (JsonElement device : devicesList) {

                JsonObject obj = (JsonObject) device;
                String name = "";
                String addr = "";
                String type = "";
                String serialNr = "";
                String treeDate = "";
                String treeTime = "";
                boolean treeGenerated = false;

                if (obj.has("Name")) {
                    name = obj.get("Name").getAsString();
                }

                if (obj.has("Addr")) {
                    addr = obj.get("Addr").getAsString();
                }

                if (obj.has("Type")) {
                    type = obj.get("Type").getAsString();
                }

                if (obj.has("SerialNr")) {
                    serialNr = obj.get("SerialNr").getAsString();
                }

                if (obj.has("TreeDate")) {
                    treeDate = obj.get("TreeDate").getAsString();
                }

                if (obj.has("TreeTime")) {
                    treeTime = obj.get("TreeTime").getAsString();
                }

                if (obj.has("TreeGenerated")) {
                    treeGenerated = obj.get("TreeGenerated").getAsBoolean();
                }

                SiemensHvacMetadataDevice deviceObj = new SiemensHvacMetadataDevice();
                deviceObj.setName(name);
                deviceObj.setAddr(addr);
                deviceObj.setSerialNr(serialNr);
                deviceObj.setType(type);
                deviceObj.setTreeDate(treeDate);
                deviceObj.setTreeTime(treeTime);
                deviceObj.setTreeGenerated(treeGenerated);

                String request2 = "api/menutree/device_root.json?TreeName=Web&SerialNumber=" + serialNr;
                if (lcHvacConnector != null) {
                    JsonObject response2 = lcHvacConnector.doRequest(request2);

                    if (response2 != null && response2.has("TreeItem")) {
                        JsonObject tree = response2.getAsJsonObject("TreeItem");
                        if (tree.has("Id")) {
                            int treeId = tree.get("Id").getAsInt();
                            deviceObj.setTreeId(treeId);
                        }
                    }
                }

                lcDevices.add(deviceObj);
            }

        } catch (Exception e) {
            logger.error("siemensHvac:ResolveDpt:Error during dp reading: {}", e.getLocalizedMessage());
            // Reset sessionId so we redone _auth on error
        }
    }

    public void readMetaData(@Nullable SiemensHvacMetadata parent, int id, boolean localized) {
        try {
            SiemensHvacConnector lcHvacConnector = hvacConnector;
            String request = "api/menutree/list.json?";
            if (id != -1) {
                request = request + "&Id=" + id;
            }

            if (lcHvacConnector != null) {
                lcHvacConnector.doRequest(request, new SiemensHvacCallback() {

                    @Override
                    public void execute(URI uri, int status, @Nullable Object response) {
                        logger.debug("response for {}, status {}:", uri, status);
                        if (response instanceof JsonObject) {
                            decodeMetaDataResult((JsonObject) response, parent, id, localized);
                        } else {
                            logger.debug("error status {}: {}", uri, status);
                        }
                    }
                });
            }

        } catch (Exception e) {
            logger.error("siemensHvac:ResolveDpt:Error during dp reading: {} ; {}", id, e.getLocalizedMessage());
            // Reset sessionId so we redone _auth on error
        }
    }

    @SuppressWarnings("unused")
    private static int nbDpt = 0;

    public void decodeMetaDataResult(JsonObject resultObj, @Nullable SiemensHvacMetadata parent, int id,
            boolean localized) {
        SiemensHvacConnector lcHvacConnector = hvacConnector;
        if (resultObj.has("MenuItems")) {
            if (parent != null) {
                logger.debug("Decode menuItem for: {}", parent.getShortDesc());
            }
            SiemensHvacMetadata childNode;
            JsonArray menuItems = resultObj.getAsJsonArray("MenuItems");

            for (JsonElement child : menuItems) {
                JsonObject menuItem = child.getAsJsonObject();

                int itemId = -1;
                if (menuItem.has("Id")) {
                    itemId = menuItem.get("Id").getAsInt();
                }

                SiemensHvacMetadataMenu menu = (SiemensHvacMetadataMenu) parent;

                if (menu.hasChild(itemId)) {
                    childNode = menu.getChild(itemId);
                } else {
                    childNode = new SiemensHvacMetadataMenu();
                    childNode.setId(itemId);
                    childNode.setParent(parent);

                    if (parent != null) {
                        menu.addChild(childNode);
                    }
                }

                if (menuItem.has("Text")) {
                    JsonObject descObj = menuItem.getAsJsonObject("Text");

                    int catId = -1;
                    int groupId = -1;
                    int subItemId = -1;
                    String longDesc = "";
                    String shortDesc = "";

                    if (descObj.has("CatId")) {
                        catId = descObj.get("CatId").getAsInt();
                    }
                    if (descObj.has("GroupId")) {
                        groupId = descObj.get("GroupId").getAsInt();
                    }
                    if (descObj.has("Id")) {
                        subItemId = descObj.get("Id").getAsInt();
                    }

                    if (descObj.has("Long")) {
                        longDesc = descObj.get("Long").getAsString();
                    }
                    if (descObj.has("Short")) {
                        shortDesc = descObj.get("Short").getAsString();
                    }

                    childNode.setSubId(subItemId);
                    childNode.setCatId(catId);
                    childNode.setGroupId(groupId);
                    if (!localized) {
                        childNode.setShortDescEn(shortDesc);
                        childNode.setLongDescEn(longDesc);
                    } else {
                        childNode.setShortDesc(shortDesc);
                        childNode.setLongDesc(longDesc);
                    }

                    readMetaData(childNode, itemId, localized);
                }

            }
        }
        if (resultObj.has("DatapointItems"))

        {
            if (parent != null) {
                logger.debug("Decode dp for : {}", parent.getShortDesc());
            }

            SiemensHvacMetadata childNode;
            JsonArray dptItems = resultObj.getAsJsonArray("DatapointItems");

            Map<String, SiemensHvacMetadataDataPoint> idMap = new Hashtable<String, SiemensHvacMetadataDataPoint>();

            for (JsonElement child : dptItems) {
                JsonObject dptItem = child.getAsJsonObject();

                nbDpt++;

                int nodeId = -1;
                int dpSubKey = -1;
                boolean hasWriteAccess = false;
                String address = "";

                if (dptItem.has("Id")) {
                    nodeId = dptItem.get("Id").getAsInt();
                }

                SiemensHvacMetadataMenu menu = (SiemensHvacMetadataMenu) parent;

                if (menu.hasChild(nodeId)) {
                    childNode = menu.getChild(nodeId);
                } else {
                    childNode = new SiemensHvacMetadataDataPoint();
                    childNode.setId(nodeId);
                    childNode.setParent(parent);

                    menu.addChild(childNode);
                }

                if (dptItem.has("Address")) {
                    address = dptItem.get("Address").getAsString();
                }
                if (dptItem.has("DpSubKey")) {
                    dpSubKey = dptItem.get("DpSubKey").getAsInt();
                }
                if (dptItem.has("WriteAccess")) {
                    hasWriteAccess = dptItem.get("WriteAccess").getAsBoolean();
                }

                SiemensHvacMetadataDataPoint dptChild = (SiemensHvacMetadataDataPoint) childNode;

                dptChild.setId(nodeId);
                dptChild.setAddress(address);
                dptChild.setDptSubKey(dpSubKey);
                dptChild.setWriteAccess(hasWriteAccess);

                idMap.put("" + nodeId, dptChild);

                if (dptItem.has("Text")) {
                    JsonObject descObj = dptItem.getAsJsonObject("Text");

                    int catId = -1;
                    int groupId = -1;
                    int subItemId = -1;
                    String longDesc = "";
                    String shortDesc = "";

                    if (descObj.has("CatId")) {
                        catId = descObj.get("CatId").getAsInt();
                    }
                    if (descObj.has("GroupId")) {
                        groupId = descObj.get("GroupId").getAsInt();
                    }
                    if (descObj.has("Id")) {
                        subItemId = descObj.get("Id").getAsInt();
                    }
                    if (descObj.has("Long")) {
                        longDesc = descObj.get("Long").getAsString();
                    }
                    if (descObj.has("Short")) {
                        shortDesc = descObj.get("Short").getAsString();
                    }

                    childNode.setSubId(subItemId);
                    childNode.setCatId(catId);
                    childNode.setGroupId(groupId);

                    if (!localized) {
                        childNode.setShortDescEn(shortDesc);
                        childNode.setLongDescEn(longDesc);
                    } else {
                        childNode.setShortDesc(shortDesc);
                        childNode.setLongDesc(longDesc);
                    }
                }

            }

            String request2 = "main.app?section=popcard&idtype=4";
            if (id != -1) {
                request2 = request2 + "&id=" + id;
            }

            if (lcHvacConnector != null) {
                lcHvacConnector.doRequest(request2, new SiemensHvacCallback() {

                    @Override
                    public void execute(URI uri, int status, @Nullable Object response) {
                        if (response != null) {
                            String st = (String) response;
                            st = st.replace("\n", "");

                            Pattern pattern = Pattern
                                    .compile("td class=\\\"dp_linenumber\\\".*?>(.*?)<\\/td>.+?(?=id)id=\"dp(.+?)\"");
                            Matcher matcher = pattern.matcher(st);

                            while (matcher.find()) {
                                String id = matcher.group(2);
                                String dptId = matcher.group(1);

                                if (id != null && dptId != null && !id.isEmpty() && !dptId.isEmpty()) {
                                    if (idMap.containsKey(id)) {
                                        SiemensHvacMetadataDataPoint child = idMap.get(id);
                                        if (child != null) {
                                            child.setDptId(dptId);
                                        }
                                    }

                                }
                            }
                        }
                    }
                });
            }

        }
    }

    @Override
    public @Nullable SiemensHvacMetadata getDptMap(@Nullable String key) {
        if (key == null) {
            return null;
        }

        if (dptMap.containsKey("byMenu" + key)) {
            return dptMap.get("byMenu" + key);
        }
        if (dptMap.containsKey("byName" + key)) {
            return dptMap.get("byName" + key);
        }
        if (dptMap.containsKey("byDptId" + key)) {
            return dptMap.get("byDptId" + key);
        }
        if (dptMap.containsKey("byId" + key)) {
            return dptMap.get("byId" + key);
        }

        return null;
    }

    public void loadMetaDataFromCache() {
        SiemensHvacConnector lcHvacConnector = hvacConnector;
        File file = null;

        try {
            file = new File(JSON_DIR + File.separator + "siemens.json");

            if (!file.exists()) {
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            String js = new String(bytes, StandardCharsets.UTF_8);

            if (lcHvacConnector != null) {
                root = lcHvacConnector.getGsonWithAdapter().fromJson(js, SiemensHvacMetadataMenu.class);
            }
        } catch (IOException ioe) {
            logger.warn("Couldn't read Siemens MetaData information from file '{}'.", file.getAbsolutePath());

        }
    }

    public void saveMetaDataToCache() {
        SiemensHvacConnector lcHvacConnector = hvacConnector;
        File file = null;

        try {
            file = new File(JSON_DIR + File.separator + "siemens.json");

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            try (FileOutputStream os = new FileOutputStream(file)) {
                if (lcHvacConnector != null) {
                    String js = lcHvacConnector.getGsonWithAdapter().toJson(root);

                    byte[] bt = js.getBytes();
                    os.write(bt);
                    os.flush();
                }
            }

        } catch (IOException ioe) {
            logger.warn("Couldn't write Siemens MetaData information to file '{}'.", file.getAbsolutePath());

        }
    }

    public void resolveDptDetails(SiemensHvacMetadataDataPoint dpt, ResolveCount rv) {
        SiemensHvacConnector lcHvacConnector = hvacConnector;
        if (dpt.getDetailsResolved()) {
            return;
        }

        String request = "api/menutree/datapoint_desc.json?Id=" + dpt.getId();
        if (lcHvacConnector != null) {
            lcHvacConnector.doRequest(request, new SiemensHvacCallback() {

                @Override
                public void execute(URI uri, int status, @Nullable Object response) {
                    if (response instanceof JsonObject) {
                        rv.decreaseResolveCount();
                        logger.debug("siemensHvac:Initialization():ToResolve() {}", rv.getResolveCount());
                        dpt.resolveDptDetails((JsonObject) response);
                    } else {
                        logger.debug("Invalid response from Siemens gateway, result is not a JsonObject");
                    }
                }
            });
        }
    }

    @Override
    public void invalidate() {
        root = null;
        SiemensHvacConnector lcHavConnector = hvacConnector;
        SiemensHvacChannelGroupTypeProvider lcChannelGroupTypeProvider = channelGroupTypeProvider;
        SiemensHvacThingTypeProvider lcThingTypeProvider = thingTypeProvider;
        SiemensHvacChannelTypeProvider lcChannelTypeProvider = channelTypeProvider;
        SiemensHvacConfigDescriptionProvider lcConfigDescriptionProvider = configDescriptionProvider;

        if (lcHavConnector != null) {
            lcHavConnector.invalidate();
        }

        if (lcChannelGroupTypeProvider != null) {
            lcChannelGroupTypeProvider.invalidate();
        }

        if (lcThingTypeProvider != null) {
            lcThingTypeProvider.invalidate();
        }

        if (lcChannelTypeProvider != null) {
            lcChannelTypeProvider.invalidate();
        }

        if (lcConfigDescriptionProvider != null) {
            lcConfigDescriptionProvider.invalidate();
        }
    }
}
