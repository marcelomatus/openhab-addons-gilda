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
package org.openhab.voice.actiontemplatehli.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.openhab.voice.actiontemplatehli.internal.ActionTemplateInterpreterConstants.SERVICE_ID;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.voice.actiontemplatehli.internal.configuration.ActionTemplateConfiguration;
import org.openhab.voice.actiontemplatehli.internal.configuration.ActionTemplateGroupTargets;
import org.openhab.voice.actiontemplatehli.internal.configuration.ActionTemplatePlaceholder;
import org.openhab.voice.actiontemplatehli.internal.utils.ActionTemplateDiceComparator;
import org.openhab.voice.actiontemplatehli.internal.utils.ActionTemplateTokenComparator;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link ActionTemplateInterpreterTest} class contains the tests for the interpreter
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class ActionTemplateInterpreterTest {
    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;
    private @NonNullByDefault({}) ActionTemplateInterpreter interpreter;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        ObjectMapper mapper = new ObjectMapper();
        // Prepare Switch
        var switchItem = new SwitchItem("testSwitch");
        switchItem.setState(OnOffType.OFF);
        switchItem.setLabel("bedroom light");
        switchItem.addTag("Light");
        Mockito.when(itemRegistryMock.get(switchItem.getName())).thenReturn(switchItem);
        // Prepare Switch Write action
        var switchNPLWriteAction = new ActionTemplateConfiguration();
        switchNPLWriteAction.template = "$onOff $itemLabel";
        switchNPLWriteAction.value = "$onOff";
        var onOffPlaceholder = new ActionTemplatePlaceholder();
        onOffPlaceholder.label = "onOff";
        onOffPlaceholder.values = new String[] { "turn on", "turn off" };
        onOffPlaceholder.mappedValues = Map.of("turn on", "ON", "turn off", "OFF");
        switchNPLWriteAction.placeholders = List.of(onOffPlaceholder);
        // Prepare Switch Read action
        var switchNPLReadAction = new ActionTemplateConfiguration();
        switchNPLReadAction.read = true;
        switchNPLReadAction.template = "how is the $itemLabel";
        switchNPLReadAction.value = "$itemLabel is $state";
        // Prepare Group
        var groupItem = new GroupItem("testGroup");
        groupItem.setLabel("bedroom");
        groupItem.addTag("Location");
        Mockito.when(itemRegistryMock.get(groupItem.getName())).thenReturn(groupItem);
        // TV channel
        var numberItem = new NumberItem("testNumber");
        numberItem.setState(DecimalType.valueOf("1"));
        numberItem.setLabel("channel");
        numberItem.addTag("tv_channel");
        numberItem.setStateDescriptionService((text, locale) -> StateDescriptionFragmentBuilder.create().withOptions(
                List.of(new StateOption("1", "hbo"), new StateOption("2", "fox"), new StateOption("3", "discovery")))
                .build().toStateDescription());
        Mockito.when(itemRegistryMock.get(numberItem.getName())).thenReturn(numberItem);
        // Prepare Group Write action
        var groupNPLWriteAction = new ActionTemplateConfiguration();
        groupNPLWriteAction.template = "turn on $itemLabel lights";
        groupNPLWriteAction.requiredItemTags = new String[] { "Location" };
        groupNPLWriteAction.value = "ON";
        groupNPLWriteAction.memberTargets = new ActionTemplateGroupTargets();
        groupNPLWriteAction.memberTargets.itemType = "Switch";
        groupNPLWriteAction.memberTargets.requiredItemTags = new String[] { "Light" };
        // Prepare Group Read action
        var groupNPLReadAction = new ActionTemplateConfiguration();
        groupNPLReadAction.read = true;
        groupNPLReadAction.requiredItemTags = new String[] { "Location" };
        groupNPLReadAction.template = "how is the light in the $itemLabel";
        groupNPLReadAction.value = "$itemLabel in $groupLabel is $state";
        var statePlaceholder = new ActionTemplatePlaceholder();
        statePlaceholder.label = "state";
        statePlaceholder.mappedValues = Map.of("on", "ON", "off", "OFF");
        groupNPLReadAction.placeholders = List.of(statePlaceholder);
        groupNPLReadAction.memberTargets = new ActionTemplateGroupTargets();
        groupNPLReadAction.memberTargets.itemName = switchItem.getName();
        groupNPLReadAction.memberTargets.requiredItemTags = new String[] { "Light" };
        // Prepare group write action using item option
        var groupNPLOptionWriteAction = new ActionTemplateConfiguration();
        groupNPLOptionWriteAction.template = "set? $itemLabel channel to $itemOption";
        groupNPLOptionWriteAction.requiredItemTags = new String[] { "Location" };
        groupNPLOptionWriteAction.value = "$itemOption";
        groupNPLOptionWriteAction.memberTargets = new ActionTemplateGroupTargets();
        groupNPLOptionWriteAction.memberTargets.itemType = "Number";
        groupNPLOptionWriteAction.memberTargets.requiredItemTags = new String[] { "tv_channel" };
        // Prepare group read action using item option
        var groupNPLOptionReadAction = new ActionTemplateConfiguration();
        groupNPLOptionReadAction.read = true;
        groupNPLOptionReadAction.requiredItemTags = new String[] { "Location" };
        groupNPLOptionReadAction.template = "what channel is on the $itemLabel tv";
        groupNPLOptionReadAction.value = "$groupLabel tv is on $itemOption";
        groupNPLOptionReadAction.memberTargets = new ActionTemplateGroupTargets();
        groupNPLOptionReadAction.memberTargets.itemType = "Number";
        groupNPLOptionReadAction.memberTargets.requiredItemTags = new String[] { "tv_channel" };
        // Add switch member to group
        groupItem.addMember(switchItem);
        // Add number member to group
        groupItem.addMember(numberItem);
        // Prepare string
        var stringItem = new StringItem("testString");
        stringItem.setLabel("message example");
        Mockito.when(itemRegistryMock.get(stringItem.getName())).thenReturn(stringItem);
        var stringItem2 = new StringItem("testString2");
        stringItem2.setLabel("message example 2");
        Mockito.when(itemRegistryMock.get(stringItem2.getName())).thenReturn(stringItem2);
        // Prepare string write action
        var stringNPLWriteAction = new ActionTemplateConfiguration();
        stringNPLWriteAction.template = "send message $* to $contact";
        stringNPLWriteAction.value = "$contact:$*";
        stringNPLWriteAction.silent = true;
        var contactPlaceholder = new ActionTemplatePlaceholder();
        contactPlaceholder.label = "contact";
        contactPlaceholder.values = new String[] { "Mark", "Andrea" };
        contactPlaceholder.mappedValues = Map.of("Mark", "+34000000000", "Andrea", "+34000000001");
        stringNPLWriteAction.placeholders = List.of(contactPlaceholder);
        // Mock metadata for 'testString'
        Mockito.when(metadataRegistryMock.get(new MetadataKey(SERVICE_ID, stringItem.getName())))
                .thenReturn(new Metadata(new MetadataKey(SERVICE_ID, stringItem.getName()), "",
                        mapper.readValue(mapper.writeValueAsString(stringNPLWriteAction), Map.class)));
        // Prepare string write action type dice
        var stringDiceWriteAction = new ActionTemplateConfiguration();
        stringDiceWriteAction.template = "please? send alert|warning $* to $contact";
        stringDiceWriteAction.value = "$contact:$*";
        stringDiceWriteAction.silent = true;
        stringDiceWriteAction.placeholders = List.of(contactPlaceholder);
        stringDiceWriteAction.type = "dice";
        // Mock metadata for 'testString2'
        Mockito.when(metadataRegistryMock.get(new MetadataKey(SERVICE_ID, stringItem2.getName())))
                .thenReturn(new Metadata(new MetadataKey(SERVICE_ID, stringItem2.getName()), "",
                        mapper.readValue(mapper.writeValueAsString(stringDiceWriteAction), Map.class)));
        // Mock items
        Mockito.when(itemRegistryMock.getAll())
                .thenReturn(List.of(switchItem, stringItem, stringItem2, groupItem, numberItem));

        interpreter = new ActionTemplateInterpreter(itemRegistryMock, metadataRegistryMock, eventPublisherMock) {
            @Override
            protected ActionTemplateConfiguration[] getTypeActionConfigs(String itemType) {
                // mock type actions for testing
                if ("Switch".equals(itemType)) {
                    return new ActionTemplateConfiguration[] { switchNPLWriteAction, switchNPLReadAction };
                }
                if ("Group".equals(itemType)) {
                    return new ActionTemplateConfiguration[] { groupNPLWriteAction, groupNPLReadAction,
                            groupNPLOptionReadAction, groupNPLOptionWriteAction };
                }
                return new ActionTemplateConfiguration[] {};
            }
        };
    }

    /**
     * Test type write action
     */
    @Test
    public void switchItemOnOffTest() throws InterpretationException {
        var response = interpreter.interpret(Locale.ENGLISH, "turn on bedroom light");
        assertThat(response, is("Done"));
        Mockito.verify(eventPublisherMock).post(ItemEventFactory.createCommandEvent("testSwitch", OnOffType.ON));
        response = interpreter.interpret(Locale.ENGLISH, "turn off bedroom light");
        assertThat(response, is("Done"));
        Mockito.verify(eventPublisherMock).post(ItemEventFactory.createCommandEvent("testSwitch", OnOffType.OFF));
    }

    /**
     * Test type read action
     */
    @Test
    public void switchItemReadTest() throws InterpretationException {
        var response = interpreter.interpret(Locale.ENGLISH, "how is the bedroom light");
        assertThat(response, is("bedroom light is OFF"));
    }

    /**
     * Test group write action targeting members
     */
    @Test
    public void groupItemMemberOnTest() throws InterpretationException {
        var response = interpreter.interpret(Locale.ENGLISH, "turn on bedroom lights");
        assertThat(response, is("Done"));
        Mockito.verify(eventPublisherMock).post(ItemEventFactory.createCommandEvent("testSwitch", OnOffType.ON));
    }

    /**
     * Test group read action targeting members
     */
    @Test
    public void groupItemMemberReadTest() throws InterpretationException {
        var response = interpreter.interpret(Locale.ENGLISH, "how is the light in the bedroom");
        assertThat(response, is("bedroom light in bedroom is off"));
    }

    /**
     * Test target a group member item using the itemOption placeholder
     */
    @Test
    public void groupItemOptionTest() throws InterpretationException {
        var response = interpreter.interpret(Locale.ENGLISH, "what channel is on the bedroom tv");
        assertThat(response, is("bedroom tv is on hbo"));
        response = interpreter.interpret(Locale.ENGLISH, "set bedroom channel to fox");
        assertThat(response, is("Done"));
        Mockito.verify(eventPublisherMock)
                .post(ItemEventFactory.createCommandEvent("testNumber", new DecimalType("2")));
        // try omitting optional token
        response = interpreter.interpret(Locale.ENGLISH, "bedroom channel to discovery");
        assertThat(response, is("Done"));
        Mockito.verify(eventPublisherMock)
                .post(ItemEventFactory.createCommandEvent("testNumber", new DecimalType("3")));
    }

    /**
     * Test write action using the dynamic label
     */
    @Test
    public void messageTest() throws InterpretationException {
        var response = interpreter.interpret(Locale.ENGLISH, "send message please turn off the bedroom light to mark");
        // silent mode is enabled so no response
        assertThat(response, is(""));
        Mockito.verify(eventPublisherMock).post(ItemEventFactory.createCommandEvent("testString",
                new StringType("+34000000000:please turn off the bedroom light")));
    }

    /**
     * Test write action using the dynamic label and dice comparison
     */
    @Test
    public void diceMessageTest() throws InterpretationException {
        // Note one word is incorrect
        var response = interpreter.interpret(Locale.ENGLISH, "send pallet please turn off the bedroom light to mark");
        // silent mode is enabled so no response
        assertThat(response, is(""));
        Mockito.verify(eventPublisherMock).post(ItemEventFactory.createCommandEvent("testString2",
                new StringType("+34000000000:please turn off the bedroom light")));
    }

    /**
     * Test unitary to lemmas usage while using token comparison
     */
    @Test
    public void lemmaTokenComparisonUnitTest() {
        var comparator = new ActionTemplateTokenComparator(new String[] { "it", "is", "really", "cool" },
                new String[] { "it", "be", "really", "cool" }, new String[] { "" });
        var result = comparator.compare(new String[] { "it", "<lemma>be", "really", "cool" });
        assertThat(result.score, is(100.0));
    }

    /**
     * Test unitary to tags usage while token comparison
     */
    @Test
    public void tagTokenComparisonUnitTest() {
        var comparator = new ActionTemplateTokenComparator(new String[] { "that", "sounds", "good" }, new String[] {},
                new String[] { "DT", "VBZ", "JJ" });
        var result = comparator.compare(new String[] { "<tag>DT", "sounds", "good" });
        assertThat(result.score, is(100.0));
    }

    /**
     * Test unitary to lemmas usage while using dice comparison
     */
    @Test
    public void lemmaDiceComparisonUnitTest() {
        var comparator = new ActionTemplateDiceComparator(new String[] { "she", "is", "really", "cool" },
                new String[] { "she", "be", "really", "cool" }, new String[] { "" }, 50.0);
        var result = comparator.compare(new String[] { "she", "<lemma>be", "really", "cool" });
        assertThat(result.score, is(100.0));
    }

    /**
     * Test unitary to tags usage while dice comparison
     */
    @Test
    public void tagDiceComparisonUnitTest() {
        var comparator = new ActionTemplateDiceComparator(new String[] { "that", "sounds", "good" }, new String[] {},
                new String[] { "DT", "VBZ", "JJ" }, 50.0);
        var result = comparator.compare(new String[] { "<tag>DT", "sounds", "good" });
        assertThat(result.score, is(100.0));
    }

    /**
     * Test unitary to token comparison
     */
    @Test
    public void tokenComparisonUnitTest() {
        var template = new String[] { "play|watch", "something", "on|at", "the?", "television" };
        assertThat(scoreByTokenComparison(new String[] { "play", "something" }, template), is(0.0));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "else" }, template), is(0.0));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "on", "television" }, template),
                is(100.0));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "on", "the", "television" }, template),
                is(100.0));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "at", "television" }, template),
                is(100.0));
        // bad transcription
        assertThat(scoreByTokenComparison(new String[] { "pay", "something", "at", "television" }, template), is(0.0));
    }

    /**
     * Test unitary to dynamic placeholder while token comparison
     */
    @Test
    public void dynamicTokenComparisonUnitTest() {
        var template = new String[] { "play|watch", "$*", "on|at", "the?", "television" };
        assertThat(scoreByTokenComparison(new String[] { "play", "something" }, template), is(0.0));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "else" }, template), is(0.0));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "on", "television" }, template),
                is(99.67));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "else", "on", "television" }, template),
                is(99.67));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "on", "the", "television" }, template),
                is(99.67));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "at", "television" }, template),
                is(99.67));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "else", "on", "television" }, template),
                is(99.67));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "else", "at", "television" }, template),
                is(99.67));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "else", "on", "the", "television" },
                template), is(99.67));
        assertThat(scoreByTokenComparison(new String[] { "play", "something", "else", "at", "the", "television" },
                template), is(99.67));
    }

    /**
     * Test unitary to dice comparison
     */
    @Test
    public void diceComparisonUnitTest() {
        var template = new String[] { "play|watch", "something", "on|at", "the?", "television" };
        assertThat(scoreByDiceComparison(new String[] { "play", "something" }, template), is(0.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "else" }, template), is(0.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "on", "television" }, template),
                is(100.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "on", "the", "television" }, template),
                is(100.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "at", "television" }, template),
                is(100.0));
        // bad transcription
        assertThat(scoreByDiceComparison(new String[] { "pay", "something", "at", "television" }, template), is(94.34));
    }

    /**
     * Test unitary to dynamic placeholder while dice comparison
     */
    @Test
    public void dynamicDiceComparisonUnitTest() {
        var template = new String[] { "play|watch", "$*", "on|at", "the?", "television" };
        assertThat(scoreByDiceComparison(new String[] { "play", "something" }, template), is(0.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "else" }, template), is(0.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "on", "television" }, template), is(99.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "else", "more", "on", "television" },
                template), is(99.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "on", "the", "television" }, template),
                is(99.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "at", "television" }, template), is(99.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "else", "on", "television" }, template),
                is(99.0));
        assertThat(scoreByDiceComparison(new String[] { "play", "something", "else", "at", "television" }, template),
                is(99.0));
    }

    private double scoreByTokenComparison(String[] phrase, String[] template) {
        BigDecimal bd = BigDecimal.valueOf(
                new ActionTemplateTokenComparator(phrase, new String[] {}, new String[] {}).compare(template).score);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private double scoreByDiceComparison(String[] phrase, String[] template) {
        BigDecimal bd = BigDecimal
                .valueOf(new ActionTemplateDiceComparator(phrase, new String[] {}, new String[] {}, 50.0)
                        .compare(template).score);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
