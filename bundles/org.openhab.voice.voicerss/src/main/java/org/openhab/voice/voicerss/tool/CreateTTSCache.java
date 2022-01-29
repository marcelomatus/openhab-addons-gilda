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
package org.openhab.voice.voicerss.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.openhab.voice.voicerss.internal.cloudapi.CachedVoiceRSSCloudImpl;

/**
 * This class fills a cache with data from the VoiceRSS TTS service.
 *
 * @author Jochen Hiller - Initial contribution
 */
public class CreateTTSCache {

    public static final int RC_OK = 0;
    public static final int RC_USAGE = 1;
    public static final int RC_INPUT_FILE_NOT_FOUND = 2;
    public static final int RC_API_KEY_MISSING = 3;
    public static final int RC_INVALID_CODEC = 4;

    public static void main(String[] args) throws IOException {
        CreateTTSCache tool = new CreateTTSCache();
        int rc = tool.doMain(args);
        System.exit(rc);
    }

    public int doMain(String[] args) throws IOException {
        if ((args == null) || (args.length < 6)) {
            usage();
            return RC_USAGE;
        }
        if (!args[0].equalsIgnoreCase("--api-key")) {
            usage();
            return RC_API_KEY_MISSING;
        }
        String apiKey = args[1];
        String cacheDir = args[2];
        String locale = args[3];
        String voice = args[4];
        String codec = "MP3";
        if (args.length >= 7) {
            switch (args[6]) {
                case "MP3":
                case "WAV":
                case "OGG":
                case "AAC":
                    codec = args[6];
                    break;
                default:
                    usage();
                    return RC_INVALID_CODEC;
            }
        }
        String format = args.length >= 8 ? args[7] : "44khz_16bit_mono";
        if (args[5].startsWith("@")) {
            String inputFileName = args[5].substring(1);
            File inputFile = new File(inputFileName);
            if (!inputFile.exists()) {
                usage();
                System.err.println("File " + inputFileName + " not found");
                return RC_INPUT_FILE_NOT_FOUND;
            }
            generateCacheForFile(apiKey, cacheDir, locale, voice, codec, format, inputFileName);
        } else {
            String text = args[5];
            generateCacheForMessage(apiKey, cacheDir, locale, voice, codec, format, text);
        }
        return RC_OK;
    }

    private void usage() {
        System.out.println("Usage: java org.openhab.voice.voicerss.tool.CreateTTSCache <args>");
        System.out.println(
                "Arguments: --api-key <key> <cache-dir> <locale> <voice> { <text> | @inputfile } [ <codec> <format> ]");
        System.out.println("  key       the VoiceRSS API Key, e.g. \"123456789\"");
        System.out.println("  cache-dir is directory where the files will be stored, e.g. \"voicerss-cache\"");
        System.out.println("  locale    the language locale, has to be valid, e.g. \"en-us\", \"de-de\"");
        System.out.println("  voice     the voice, \"default\" for the default voice");
        System.out.println("  text      the text to create audio file for, e.g. \"Hello World\"");
        System.out.println(
                "  inputfile a name of a file, where all lines will be translatet to text, e.g. \"@message.txt\"");
        System.out.println("  codec     the audio codec, \"MP3\", \"WAV\", \"OGG\" or \"AAC\", \"MP3\" by default");
        System.out.println("  format    the audio format, \"44khz_16bit_mono\" by default");
        System.out.println();
        System.out.println(
                "Sample: java org.openhab.voice.voicerss.tool.CreateTTSCache --api-key 1234567890 cache en-US default @messages.txt");
        System.out.println();
    }

    private void generateCacheForFile(String apiKey, String cacheDir, String locale, String voice, String codec,
            String format, String inputFileName) throws IOException {
        File inputFile = new File(inputFileName);
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                generateCacheForMessage(apiKey, cacheDir, locale, voice, codec, format, line);
            }
        }
    }

    private void generateCacheForMessage(String apiKey, String cacheDir, String locale, String voice, String codec,
            String format, String msg) throws IOException {
        if (msg == null) {
            System.err.println("Ignore msg=null");
            return;
        }
        String trimmedMsg = msg.trim();
        if (trimmedMsg.length() == 0) {
            System.err.println("Ignore msg=''");
            return;
        }
        try {
            CachedVoiceRSSCloudImpl impl = new CachedVoiceRSSCloudImpl(cacheDir, false);
            File cachedFile = impl.getTextToSpeechAsFile(apiKey, trimmedMsg, locale, voice, codec, format);
            System.out.println("Created cached audio for locale='" + locale + "', voice='" + voice + "', msg='"
                    + trimmedMsg + "' to file=" + cachedFile);
        } catch (IllegalStateException | IOException ex) {
            System.err.println("Failed to create cached audio for locale='" + locale + "', voice='" + voice + "',msg='"
                    + trimmedMsg + "'");
        }
    }
}
