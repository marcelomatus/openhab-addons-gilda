# Compatibility of openHAB 1.x add-ons with openHAB 2

Here you can find the compatibility matrices in order to find out, which version of the openHAB 2.0 (alpha or SNAPSHOT) runtime is compatible with which version of the openHAB 1.x addons (either 1.6.0 release or 1.7.0-SNAPSHOT).

This page is a community effort - please help filling the gaps and analysing potential problems!

## Bindings

| Binding | 2.0 alpha + 1.6.0 | 2.0 alpha + 1.7.0-SNAPSHOT | 2.0-SNAPSHOT + 1.6.0 | 2.0-SNAPSHOT + 1.7.0-SNAPSHOT | Remarks |
|-------|:------------------:|:---------------------------:|:----------------------:|:-------------------------------:|---|
| AlarmDecoder |  |  |  |  |  |
| Anel |  |  |  |  |  |
| Asterisk |  |  |  |  |  |
| Astro |  |  |  |  |  |
| Bluetooth |  |  |  |  |  |
| Comfo Air |  |  |  |  |  |
| Config Admin |  |  |  |  |  |
| CUL |  |  |  |  |  |
| CUPS |  |  |  |  |  |
| Davis |  |  |  |  |  |
| digitalSTROM |  |  |  |  |  |
| DSC Alarm |  |  |  |  |  |
| DMX512 |  |  |  |  |  |
| EDS OWSever |  |  |  |  |  |
| Energenie |  |  |  |  |  |
| EnOcean |  |  |  |  |  |
| Epson Projector |  |  |  |  |  |
| Exec |  |  |  |  |  |
| Freebox |  |  |  |  |  |
| Freeswitch |  |  |  |  |  |
| Fritz!Box |  |  |  |  |  |
| Fritz AHA |  |  |  |  |  |
| FS20 |  |  |  |  |  |
| Global Cache IR |  |  |  |  |  |
| GPIO |  |  |  |  |  |
| HAI/Leviton OmniLink |  |  |  |  |  |
| HDAnywhere |  |  |  |  | 2.0 version available |
| Heatmiser |  |  |  |  |  |
| Homematic / Homegear | X | X | X | X |  |
| HTTP | X | X | X | X |  |
| IEC 62056-21 |  |  |  |  |  |
| IHC / ELKO |  |  |  |  |  |
| Insteon Hub |  |  |  |  |  |
| Insteon PLM |  |  |  |  |  |
| IRtrans |  |  |  |  | 2.0 version available |
| jointSPACE-Binding
| KNX | X |  |  |  |Only tested the first combination. Works quite well. 2.0 version available |
| Koubachi |  |  |  |  |  |
| Leviton/HAI Omnilink |  |  |  |  |  |
| MAX!Cube |  |  |  |  |  |
| MAX! CUL |  |  |  |  |  |
| MiLight |  |  |  |  |  |
| Modbus TCP |  |  |  |  |  |
| Westaflex Modbus
| MPD |  |  |  |  |  |
| MQTT |  |  |  |  |  |
| MQTTitude |  |  |  |  |  |
| Neohub |  |  |  |  |  |
| Netatmo |  |  |  |  |  |
| Network Health | X | X | X | X |  |
| Nibe Heatpump |  |  |  |  |  |
| Nikobus |  |  |  |  |  |
| Novelan/Luxtronic Heatpump |  |  |  |  |  |
| NTP | X | X | X | X |  |
| Oceanic |  |  |  |  | 2.0 version available |
| One-Wire |  |  |  |  |  |
| Onkyo AV Receiver |  |  |  |  |  |
| Open Energy Monitor |  |  |  |  |  |
| OpenPaths presence detection |  |  |  |  |  |
| OpenSprinkler |  |  |  |  |  |
| OSGi Configuration Admin |  |  |  |  |  |
| Philips Hue |  |  |  |  | 2.0 version available |
| Piface |  |  |  |  |  |
| Pioneer-AVR-Binding
| Plugwise |  |  |  |  |  |
| PLCBus |  |  |  |  |  |
| Pulseaudio |  |  |  |  |  |
| RFXCOM |  |  |  |  |  |
| RME |  |  |  |  | 2.0 version available |
| Samsung TV |  |  |  |  |  |
| Serial |  |  |  |  |  |
| Snmp |  |  |  |  |  |
| Squeezebox |  |  |  |  |  |
| System Info |  |  |  |  |  |
| Somfy URTSI II |  |  |  |  |  |
| Sonos |  |  |  |  | 2.0 version available |
| Swegon ventilation |  |  |  |  |  |
| TCP/UDP |  |  |  |  |  |
| Tellstick |  |  |  |  |  |
| TinkerForge |  |  |  |  |  |
| VDR |  |  |  |  |  |
| Velleman-K8055 |  |  |  |  |  |
| Wake-on-LAN |  |  |  |  |  |
| Waterkotte EcoTouch Heatpump |  |  |  |  |  |
| Wemo |  |  |  |  | 2.0 version available |
| Withings |  |  |  |  |  |
| XBMC |  |  |  |  |  |
| xPL |  |  |  |  |  |
| Z-Wave |  |  |  |  |  |

## Persistence Services

| Persistence Service | 2.0 alpha + 1.6.0 | 2.0 alpha + 1.7.0-SNAPSHOT | 2.0-SNAPSHOT + 1.6.0 | 2.0-SNAPSHOT + 1.7.0-SNAPSHOT | Remarks |
|-------|:------------------:|:---------------------------:|:----------------------:|:-------------------------------:|---|
| db4o |  |  |  |  |  |
| rrd4j | X | X | X | X | only persistence, no rrd4j charts supported |
| MySQL |  |  |  |  |  |
| MongoDB |  |  |  |  |  |
| Sen.Se |  |  |  |  |  |
| Cosm |  |  |  |  |  |
| Logging | X | X | X | X |  |
| Exec |  |  |  |  |  |
| MQTT |  |  |  |  |  |
| InfluxDB |  |  |  |  |  |

## Actions

| Action | 2.0 alpha + 1.6.0 | 2.0 alpha + 1.7.0-SNAPSHOT | 2.0-SNAPSHOT + 1.6.0 | 2.0-SNAPSHOT + 1.7.0-SNAPSHOT | Remarks |
|-------|:------------------:|:---------------------------:|:----------------------:|:-------------------------------:|---|
| Mail | X | X | X | X |  |
| XMPP |  |  |  |  |  |
| Prowl |  |  |  |  |  |
| Twitter |  |  |  |  |  |
| Cosm |  |  |  |  |  |
| XBMC | X | X | X | X |  |
| NotifyMyAndroid |  |  |  |  |  |
| Squeezebox |  |  |  |  |  |
| Pushover |  |  |  |  |  |
| OpenWebIf |  |  |  |  |  |
