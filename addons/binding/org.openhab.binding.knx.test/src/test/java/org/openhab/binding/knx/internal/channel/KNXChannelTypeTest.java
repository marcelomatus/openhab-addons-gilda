package org.openhab.binding.knx.internal.channel;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.types.Command;
import org.junit.Before;
import org.junit.Test;

import tuwien.auto.calimero.exception.KNXFormatException;

public class KNXChannelTypeTest {

    private KNXChannelType ct;

    @Before
    public void setup() {
        ct = new MyKNXChannelType("");
    }

    @Test
    public void testParse_withDPT_multiple_withRead() {
        ChannelConfiguration res = ct.parse("5.001:<1/3/22+0/3/22+<0/8/15");

        assertEquals("5.001", res.getDPT());
        assertEquals("1/3/22", res.getMainGA().getGA());
        assertTrue(res.getMainGA().isRead());
        assertEquals(3, res.getListenGAs().size());
        assertEquals(2, res.getReadGAs().size());
    }

    @Test
    public void testParse_withDPT_multiple_withoutRead() {
        ChannelConfiguration res = ct.parse("5.001:1/3/22+0/3/22+0/8/15");

        assertEquals("5.001", res.getDPT());
        assertEquals("1/3/22", res.getMainGA().getGA());
        assertFalse(res.getMainGA().isRead());
        assertEquals(3, res.getListenGAs().size());
        assertEquals(0, res.getReadGAs().size());
    }

    @Test
    public void testParse_withoutDPT_single_withoutRead() {
        ChannelConfiguration res = ct.parse("1/3/22");

        assertNull(res.getDPT());
        assertEquals("1/3/22", res.getMainGA().getGA());
        assertFalse(res.getMainGA().isRead());
        assertEquals(1, res.getListenGAs().size());
        assertEquals(0, res.getReadGAs().size());
    }

    @Test
    public void testParse_withoutDPT_single_witRead() {
        ChannelConfiguration res = ct.parse("<1/3/22");

        assertNull(res.getDPT());
        assertEquals("1/3/22", res.getMainGA().getGA());
        assertTrue(res.getMainGA().isRead());
        assertEquals(1, res.getListenGAs().size());
        assertEquals(1, res.getReadGAs().size());
    }

    private static class MyKNXChannelType extends KNXChannelType {
        public MyKNXChannelType(String channelTypeID) {
            super(channelTypeID);
        }

        @Override
        protected @NonNull Set<@NonNull String> getAllGAKeys() {
            return Collections.emptySet();
        }

        @Override
        public @Nullable CommandSpec getCommandSpec(@NonNull Configuration configuration, @NonNull Command command)
                throws KNXFormatException {
            return null;
        }

        @Override
        protected @NonNull String getDefaultDPT(@NonNull String gaConfigKey) {
            return "";
        }

    }

}
