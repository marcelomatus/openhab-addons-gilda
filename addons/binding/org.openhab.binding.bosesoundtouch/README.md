# openHAB2 binding for Bose SoundTouch

This is the openHAB2 binding for the Bose SoundTouch multiroom system.
Here you can find a precompiled [Binding](https://github.com/marvkis/org.openhab.binding.bosesoundtouch-dist)

After installing the binding into the your openHAB2 distribution, you can start discovering your devices through the PaperUI GUI (btw: see discovery hints on the bottom of this document).

After discovering and configuring the device through the web GUI you may use it. To access them, the 'classical openHAB1 way', trough the items / sitemaps / rules way. Here a few samples for the configuration:

From the **items/bose.items** file:
```
Switch  Bose1_Power                      "Power: [%s]"          <switch>      { channel="bosesoundtouch:device:BOSEMACADDR:power" }
Dimmer  Bose1_Volume                     "Volume: [%d %%]"      <volume>      { channel="bosesoundtouch:device:BOSEMACADDR:volume" }
Number  Bose1_Bass                       "Bass: [%d %%]"        <volume>      { channel="bosesoundtouch:device:BOSEMACADDR:bass" }
Switch  Bose1_Mute                       "Mute: [%s]"           <volume_mute> { channel="bosesoundtouch:device:BOSEMACADDR:mute" }
String  Bose1_OperationMode              "OperationMode: [%s]"  <text>        { channel="bosesoundtouch:device:BOSEMACADDR:operationMode" }
String  Bose1_PlayerControl              "Player Control: [%s]" <text>        { channel="bosesoundtouch:device:BOSEMACADDR:playerControl" }
String  Bose1_ZoneAdd                    "Zone add: [%s]"       <text>        { channel="bosesoundtouch:device:BOSEMACADDR:zoneAdd" }
String  Bose1_ZoneRemove                 "Zone remove: [%s]"    <text>        { channel="bosesoundtouch:device:BOSEMACADDR:zoneRemove" }
Number  Bose1_Preset                     "Preset: [%d]"         <text>        { channel="bosesoundtouch:device:BOSEMACADDR:preset" }
String  Bose1_PresetControl              "Preset Control: [%s]" <text>        { channel="bosesoundtouch:device:BOSEMACADDR:presetControl" }
Number  Bose1_SaveAsPreset               "Save as Preset: [%d]" <text>        { channel="bosesoundtouch:device:BOSEMACADDR:saveAsPreset" }
String  Bose1_KeyCode                    "Key Code: [%s]"       <text>        { channel="bosesoundtouch:device:BOSEMACADDR:keyCode" }
String  Bose1_ZoneInfo                   "Zone Info: [%s]"      <text>        { channel="bosesoundtouch:device:BOSEMACADDR:zoneInfo", autoupdate="false" }
Switch  Bose1_RateEnabled                "Rate: [%s]"           <switch>      { channel="bosesoundtouch:device:BOSEMACADDR:rateEnabled" }
Switch  Bose1_SkipEnabled                "Skip: [%s]"           <switch>      { channel="bosesoundtouch:device:BOSEMACADDR:skipEnabled" }
Switch  Bose1_SkipPreviousEnabled        "SkipPrevious: [%s]"   <switch>      { channel="bosesoundtouch:device:BOSEMACADDR:skipPreviousEnabled" }
String  Bose1_nowPlayingAlbum            "Album: [%s]"          <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingAlbum" }
String  Bose1_nowPlayingArtist           "Artist: [%s]"         <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingArtist" }
String  Bose1_nowPlayingArtwork          "Art: [%s]"            <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingArtwork" }
String  Bose1_nowPlayingDescription      "Description: [%s]"    <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingDescription" }
String  Bose1_nowPlayingGenre            "Genre: [%s]"          <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingGenre" }
String  Bose1_nowPlayingItemName         "Playing: [%s]"        <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingItemName" }
String  Bose1_nowPlayingStationLocation  "Radio Location: [%s]" <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingStationLocation" }
String  Bose1_nowPlayingStationName      "Radio Name: [%s]"     <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingStationName" }
String  Bose1_nowPlayingTrack            "Track: [%s]"          <text>        { channel="bosesoundtouch:device:BOSEMACADDR:nowPlayingTrack" }
```

A simple sitemap **sitemaps/bose.sitemap**:

```
sitemap demo label="Bose Test Items"
{
	Frame label="Bose 1" {
        Switch item=Bose1_Power
		Slider item=Bose1_Volume
		Number item=Bose1_Bass
		Switch item=Bose1_Mute
		Text item=Bose1_OperationMode
		Text item=Bose1_PlayerControl
		Text item=Bose1_ZoneAdd
		Text item=Bose1_ZoneRemove
		Number item=Bose1_Preset
		Text item=Bose1_PresetControl
		Number item=Bose1_SaveAsPreset
		Text item=Bose1_KeyCode
		Text item=Bose1_ZoneInfo
		Text item=Bose1_nowPlayingAlbum
		Text item=Bose1_nowPlayingArtist
		Text item=Bose1_nowPlayingArtwork
		Text item=Bose1_nowPlayingDescription
		Text item=Bose1_nowPlayingGenre
		Text item=Bose1_nowPlayingItemName
		Text item=Bose1_nowPlayingStationLocation
		Text item=Bose1_nowPlayingTrack
	}
}
```

A few samples for the channels (for the CLI):
```
smarthome send Bose1_Volume "10"

smarthome send Bose1_KeyCode "PRESET_1"
smarthome send Bose1_ZoneAdd "<devicename>” e.g.
smarthome send Bose1_ZoneAdd "livingroom"
smarthome send Bose1_ZoneAdd "<device-mac-address>” e.g.
```
this also can be done through rules:

Bose.rule:
```
rule "Bose: Combine Kitchen with living room"
when
		Item Bose1_power changed
then
		if (Bose1_power.state == ON) {
			sendCommand(Bose1_ZoneAdd, "kitchen")
		}
end

rule "wake up"
when
		Time cron "0 30 6 ? * MON,TUE,WED,THU,FRI”
then
		sendCommand(Bose1_Power, ON)
		sendCommand(Bose1_Volume, 10)
		sendCommand(Bose1_KeyCode, “PRESET_1")
end
```

I hope this gives you some idea's how to use this plugin.

If you need support or have new idea's please use the [openHAB cummunity](https://community.openhab.org/t/bose-soundtouch-binding/5678) to post the requests.

#### Add additional PRESETS

When selected a presetable ContentItem you are able to save this preset with the saveAsPreset Channel
Use numbers greater than six to save the selected source as Preset.
Once the source is saved, you can use it like the other PRESETS with the channel preset and the channel presetControl
Note that these PRESETS are only available when using openHAB

#### Known issues and Workarounds

##### Limmited support

Basic things work:

 * Turning On/Off
 * Setting / Reading Volume
 * Muting / Play / Pause
 * Grouping / Ungrouping devices
 * Start playing things from 'Presets'

What's missing is direct control of sources (Servers / Radio Stations / Spotify ...)  trough the interfaces. I currently also have no real concept how this can be adopted with the openHAB2 interface. If you have some good idea's and use cases, drop me a message in the forum or open a ticket ;)

##### Discovery
I had some troubles to get discovery on linux working. It worked like a charm on my OSX devel box, but on my production server was not able to discover the bose devices.

After tracing it down: It seems that the Bonjour/MDNS/UPnP-Stuff on linux binds to ipv6 only, and currently the bose devices only support IPv4. The trick was to set
```shell
export _JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true”
```
before starting openHAB. When this is set the discovery worked on the linux box.

##### State inconsistency after openHAB2 startup
The next known issue is during openHAB2 startup:
The SoundTouch speakers are contacted in a quite early phase during openHAB2 startup and all status information is fetched. But in this stage it seems that the binding files are not parsed, and so you see invalid states when you open the sitemap. Just turn the speakers on/off to refresh all states.

##### Detailed states missing
On my devel box I see all the detailed states. But on my production box I see that the states seem to be updated accordingly (on the console logs) but the values are not visible within the sitemap. Currently I don’t have an idea why. Maybe you can drop me a short note if it’s working on your box or not.
