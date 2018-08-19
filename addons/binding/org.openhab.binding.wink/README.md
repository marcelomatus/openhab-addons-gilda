# Wink Hub Binding

This binding supports devices connected to a Wink Hub via the Wink cloud API.  At time of writing this binding supports Dimmable Lights, Binary light switches, Remotes and Connected Lock devices.

## Overview

The Wink binding represents a "Hub" as a bridge thing and "Devices" as things connected to the bridge.  Since the binding works only with the Wink API, in order to use it you must obtain a wink application ID and secret and then obtain an initial refresh token using a standard oauth flow.  You can obtain a wink application ID by visiting https://developer.wink.com.

### Hub

The Hub supports not channels itself and basically just serves as a proxy for the connected devices.  It must be configured though as all devices delegate commands to the bridge.

### Devices

When devices are connected to the wink hub they become discoverable by the binding through the wink api.  Different devices will support different channels depending on the capabilities of the device.

## Supported Things

<table>
<tr><td><b>Thing</b></td><td><b>Thing Type</b></td><td><b>Description</b></td></tr>
<tr><td>wink_hub_2</td><td>Bridge</td><td>The Hub itself</td></tr>
<tr><td>light_bulb</td><td>Thing</td><td>Dimmable Light Bulb</td></tr>
<tr><td>binary_switch</td><td>Thing</td><td>Simple On/Off Switch</td></tr>
<tr><td>remote</td><td>Thing</td><td>Remote for hub</td></tr>
<tr><td>lock</td><td>Thing</td><td>Connected Lock</td></tr>
<tr><td>door_bell</td><td>Thing</td><td>Door Bell</td></tr>
<tr><td>thermostat</td><td>Thing</td><td>Thermostat</td></tr>
</table>

## Discovery

You can search for wink Devices from the Inbox and the binding will add all devices connected to your hub to the inbox.

## Binding Configuration

In order for the binding to work, you must add a wink.cfg file (must be named wink.cfg) to the services directoy.  The file must contain three parameters: client_id, client_secret and refresh_token.

```
client_id=client_id_from_wink.com_alkjflkjwev8esflkjad
client_secret=secret_from_wink.com_lakjdf9enadnaoariar
refresh_token=refresh_token_from_oauth_flow_adfajeinwaefnav83
```

This will allow the binding to obtain a new access token whenever it needs one.

You can optionally configure the binding to use a delegated authentication manager by logging into a simple service on heroku (https://openhab-authservice.herokuapp.com) and create an account using your github account by clicking the 'login with github' link.  Once you log in, you can click the 'connect to wink' button and go through the oauth dance.  The service will store tokens for you and provide tokens to your binding if you add the following config to your wink.cfg file:

```
auth_service=delegated
auth_service_token=token_displayed_on_authservice_screen_after_login
```

This can also be configured from the Paper UI binding config screen.

## Thing Configuration

If you want to configure things yourself, you should define the Bridge and all things like so:

```
Bridge wink:wink_hub_2:hub "Wink Hub" {
	Thing	light_bulb   MyLight	"My Light"  [uuid="2398fnakdn-akdsf-asdflakjdf-wein3"]
}
```

You must specify the uuid for each thing in order for the device to initialize.  Discovery will automatically do this for you, but if you want to do it manually, you'll have to use the wink api to discover the uuid's of each device

## Channels

Depending on the device being configured, there are different channels available:

<table>
<tr><td><b>Thing</b></td><td><b>Channel</b></td><td><b>Description</b></td></tr>
<tr><td rowspan="2">light_bulb</td><td>lightstate</td><td>The powered stated of the light (On/Off)</td></tr>
<tr><td>lightlevel</td><td>The dimmer percentage</td></tr>
<tr><td>lock</td><td>lockstate</td><td>The position of the lock (ON == Locked)</td></tr>
<tr><td rowspan="15">thermostat</td><td>thermostat_currentsetpoint</td><td>Thermostat Current Desired Temperature</td></tr>
<tr><td>thermostat_currentmode</td><td>The current set mode: Cool, Heat, or Auto</td></tr>
<tr><td>thermostat_hasfan</td><td>Whether or not the thermostat unit has a fan, Readonly</td></tr>
<tr><td>thermostat_online</td><td>Whether or not the device is reachable remotely, Readonly</td></tr>
<tr><td>thermostat_ecomode</td><td>Whether or not the thermostat is running in an energy efficient mode, Readonly</td></tr>
<tr><td>thermostat_humidity</td><td>Thermostat Current Humidity, Readonly</td></tr>
<tr><td>thermostat_occupied</td><td>Whether or not the thermostat has detected occupancy in the last 30 minutes, Readonly</td></tr>
<tr><td>thermostat_techname</td><td>Contractor contact data, Readonly</td></tr>
<tr><td>thermostat_techphone</td><td>Contractor contact data, Readonly</td></tr>
<tr><td>thermostat_lasterror</td><td>The current alert/warning on the thermostat, Readonly</td></tr>
<tr><td>thermostat_fanactive</td><td>Whether or not the fan is actively running, Readonly</td></tr>
<tr><td>thermostat_runningmode</td><td>The current running mode: Aux, Cool, Heat, or Idle, Readonly</td></tr>
<tr><td>thermostat_smarttemperature</td><td>Ecobee only, mean temp of all remote sensors and thermostat, Readonly</td></tr>
<tr><td>thermostat_currenttemperature</td><td>Thermostat Current Temperature, maps to room temperature last read from device itself, Readonly</td></tr>
<tr><td>thermostat_externaltemperature</td><td>The outdoor temperature/weather, Readonly</td></tr>
</table>

Your items file will look like:

```
Item Switch MyLightSwitch "My Light Switch"  {channel="wink:light_bulb:MyLight:lightstate"}
Item Dimmer MyLightDimmer "My Light Dimmer"  {channel="wink:light_bulb:MyLight:lightlevel"}
```

If you add your thermostat Thing through Paper UI, then you do not need anything in your things file.  You can copy the channel info from Paper UI for the channel you are interested in.  In this case, your items file will look like:

```
Number ThermostatSetpoint "Temperature [%.1f °F]" {channel="wink:thermostat:8316ea86-cabb-447b-94f4-fakeUUIDabab:thermostat_currentsetpoint"}
String ThermostatMode "Thermostat Mode [%s]" {channel="wink:thermostat:8316ea86-cabb-447b-94f4-fakeUUIDabab:thermostat_currentmode"}
```

Your sitemap file will look like:

```
Setpoint item=ThermostatSetpoint minValue=60 maxValue=77 step=1.0
Selection item=ThermostatMode label="Thermostat Mode" mappings=["Heat"=heat, "Cool"=cool, "Auto"=auto]
```
