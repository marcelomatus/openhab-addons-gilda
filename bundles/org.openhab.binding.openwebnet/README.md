# OpenWebNet (BTicino/Legrand) Binding

This binding integrates BTicino / Legrand MyHOME&reg; BUS and ZigBee wireless (MyHOME_Play&reg;) devices using the [OpenWebNet](https://en.wikipedia.org/wiki/OpenWebNet) protocol.

The binding supports:

- both wired BUS/SCS (MyHOME) and wireless setups (MyHOME ZigBee). The two networks can be configured simultaneously
- discovery of BUS/SCS IP gateways and ZigBee USB gateways and devices
- commands from openHAB and feedback (events) from BUS/SCS and wireless network
- numeric (`12345`) and alpha-numeric (`abcde` - HMAC authentication) gateway passwords

![F454 Gateway](doc/F454_gateway.png)
![USB ZigBee Gateway](doc/USB_gateway.jpg)

## Supported Things

In order for this biding to work, a **BTicino/Legrand OpenWebNet gateway** is needed in your home system to talk to devices.

These gateways have been tested with the binding:

- **IP gateways** or scenario programmers, such as BTicino 
[F454](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?productId=006), 
[MyHOMEServer1](http://www.bticino.com/products-catalogue/myhome_up-simple-home-automation-system/), 
[MyHOME_Screen10](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?lang=EN&productId=001), 
[MH201](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?productId=053),
[MH202](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?productId=059), 
[F455](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?productId=051),
[MH200N](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?productId=016), 
[F453](http://www.homesystems-legrandgroup.com/BtHomeSystems/productDetail.action?productId=027),  etc.

- **ZigBee USB Gateways**, such as [BTicino 3578](https://catalogo.bticino.it/BTI-3578-IT) and [Legrand 088328](https://www.legrand.com/ecatalogue/088328-openweb-net-zigbee-gateway-radio-interface.html)

**NOTE** The new BTicino Living Now&reg; wireless system is not supported as it does not use the OpenWebNet protocol.

The following Things and OpenWebNet `WHOs` are supported:

### For BUS/SCS

| Category             | WHO          | Thing Type IDs                      | Description                                                 | Status           |
| -------------------- | :----------: | :---------------------------------: | ----------------------------------------------------------- | ---------------- |
| Gateway Management   | `13`         | `bus_gateway`                       | Any IP gateway supporting OpenWebNet protocol should work (e.g. F454 / MyHOMEServer1 / MH202 / F455 / MH200N, ...) | Successfully tested: F454, MyHOMEServer1, MyHOME_Screen10, F455, F452, F453AV, MH201, MH202, MH200N. Some connection stability issues/gateway resets reported with MH202  |
| Lightning            | `1`          | `bus_on_off_switch`, `bus_dimmer`   | BUS switches and dimmers. Green switches.                   | Successfully tested: F411/2, F411/4, F411U2, F422, F429. AM5658 Green Switch. Some discovery issues reported with F429 (DALI Dimmers)  |
| Automation           | `2`          | `bus_automation`                    | BUS roller shutters, with position feedback and auto-calibration | Successfully tested: LN4672M2  |

### For ZigBee (Radio)

| Category   | WHO   | Thing Type IDs                    | Description                                                           | Status                               |
| ---------- | :---: | :-------------------------------: | :-------------------------------------------------------------------: | ------------------------------------ |
| Gateway    | `13`  | `zb_gateway`                      | Wireless ZigBee USB Gateway (BTicino/Legrand models: BTI-3578/088328) | Tested: BTI-3578 and LG 088328       |
| Lighting   | `1`   | `zb_dimmer`, `zb_on_off_switch`, `zb_on_off_switch2u` | ZigBee dimmers, switches and 2-unit switches      | Tested: BTI-4591, BTI-3584, BTI-4585 |
| Automation | `2`   | `zb_automation`                   | ZigBee roller shutters                                                | *To be tested*                       |

## Discovery

Gateway and Things discovery is supported using PaperUI by pressing the discovery ("+") button form Inbox.

### BUS/SCS Discovery

- BUS Gateway automatic discovery will work only for newer gateways supporting UPnP: F454, MyHOMEServer1, MH201, MH202, MH200N, MyHOME_Screen 10.
For other gateways you can add them manually, see [Thing Configuration](#thing-configuration) below.
- After gateway is discovered and added a connection with default password (`12345`) is tested first: if it does not work the gateway will go offline and an error status will be set. A correct password must then be set in the gateway Thing configuration otherwise the gateway will not become online.
- Once the gateway is online, a second Scan request from Inbox will discover BUS devices
- BUS/SCS Dimmers must be ON and dimmed (30%-100%) during a Scan, otherwise they will be discovered as simple On/Off switches
    - *KNOWN ISSUE*: In some cases dimmers connected to a F429 Dali-interface are not automatically discovered

#### Discovery by Activation

Devices can also be discovered if activated while a Inbox Scan is active: start a new Scan, wait 15-20 seconds and then _while the Scan is still active_ (spinning arrow in Inbox), activate the physical device (for example dim the dimmer) to have it discovered by the binding.

If a device cannot be discovered automatically it's always possible to add them manually, see [Configuring Devices](#configuring-devices).

### ZigBee Discovery

- The ZigBee USB Gateway must be inserted in one of the USB ports of the openHAB computer before discovery is started
- ***IMPORTANT NOTE:*** As for other OH2 bindings using the USB/serial ports, on Linux the `openhab` user must be member of the `dialout` group, to be able to use USB/serial port: set the group with the following command:

    ```
    $ sudo usermod -a -G dialout openhab
    ```

    The user will need to logout and login to see the new group added. If you added your user to this group and still cannot get permission, reboot Linux to ensure the new group permission is attached to the `openhab` user.
- Once the gateway is discovered and added, a second discovery request from Inbox will discover devices. Because of the ZigBee radio network, discovery will take ~40-60 sec. Be patient!
- Wireless devices must be part of the same ZigBee network of the ZigBee USB Gateway to discover them. Please refer to [this video by BTicino](https://www.youtube.com/watch?v=CoIgg_Xqhbo) to setup a ZigBee wireless network which includes the ZigBee USB Gateway 
- Only powered wireless devices part of the same ZigBee network and within radio coverage of the ZigBee USB Gateway will be discovered. Unreachable or not powered devices will be discovered as *GENERIC* devices and cannot be controlled. Control units cannot be discovered by the ZigBee USB Gateway and therefore are not supported

## Thing Configuration

### Configuring BUS/SCS Gateway

To add a gateway manually using PaperUI: go to *Inbox > "+" > OpenWebNet > click `ADD MANUALLY`* and then select `OpenWebNet BUS Gateway` device.

Configuration parameters are:

- `host` : IP address / hostname of the BUS/SCS gateway (`String`, *mandatory*)
   - Example: `192.168.1.35`
- `port` : port (`int`, *optional*, default: `20000`)
- `passwd` : gateway password (`String`, *required* for gateways that have a password. Default: `12345`)
   - Example: `abcde` or `12345`
   - if the BUS/SCS gateway is configured to accept connections from the openHAB computer IP address, no password should be required
   - in all other cases, a password must be configured. This includes gateways that have been discovered and added from Inbox: without a password configured they will remain OFFLINE
- `discoveryByActivation`: discover BUS devices when they are activated also when a device scan is not currently active (`boolean`, *optional*, default: `false`). See [Discovery by Activation](#discovery-by-activation).

Alternatively the BUS/SCS Gateway thing can be configured using the `.things` file, see `openwebnet.things` example [below](#full-example).

### Configuring Wireless ZigBee USB Gateway 

The wireless ZigBee USB Gateway is discovered automatically by activating a new Scan from the Inbox.

Manual configuration *is not supported* at the moment.

### Configuring Devices

Devices can be discovered automatically from Inbox after a gateway has been configured and connected.

Devices can be also added manually from PaperUI. For each device it must be configured:

- the associated gateway (`Bridge Selection` menu)
- the `WHERE` config parameter (`OpenWebNet Device Address`):
  - example for BUS/SCS: Point to Point `A=2 PL=4` --> `WHERE="24"`
  - example for BUS/SCS: Point to Point `A=6 PL=4` on local bus --> `WHERE="64#4#01"`
  - example for ZigBee devices: use decimal format address without the UNIT part and network: ZigBee `WHERE=414122201#9` --> `WHERE="4141222"`


## Channels

Devices support some of the following channels:

| Channel Type ID (channel ID)        | Item Type     | Description                                                             | Read/Write |
|--------------------------|---------------|-------------------------------------------------------------------------|:----------:|
| `switch`                 | Switch        | To switch the device `ON` and `OFF`                                     |    R/W     |
| `brightness`             | Dimmer        | To adjust the brightness value (Percent, `ON`, `OFF`)                   |    R/W     |
| `shutter`                | Rollershutter | To activate roller shutters (`UP`, `DOWN`, `STOP`, Percent - [see Shutter position](#shutter-position)) |    R/W     |

### Notes on channels

#### `shutter` position

For Percent commands and position feedback to work correctly, the `shutterRun` Thing config parameter must be configured equal to the time taken (in milliseconds) by the roller shutter to go from full UP, to full DOWN.
It's possible to enter a value manually or set `shutterRun=AUTO` (default) to calibrate `shutterRun` parameter automatically. With `shutterRun=AUTO` the first time a Percent command is sent from openHAB to the roller shutter a *UP >> DOWN >> Position%* cycle will be performed automatically and the `shutterRun` config parameter will then be auto-calibrated to the correct value.

- if `shutterRun` is not set, or is set to AUTO but calibration has not been performed yet, then position estimation will remain `UNDEFINED`
- if `shutterRun` is set manually but too higher than the actual roller shutter time, then position estimation will remain `UNDEFINED`: try to reduce `shutterRun` until you find the right value
- before adding/configuring roller shutter Things (or installing a binding update) it is suggested to have all roller shutters `UP`, otherwise the Percent command won’t work until the roller shutter is fully rolled up
- if the gateway gets disconnected then the binding cannot know anymore the roller shutter position: if `shutterRun` is defined (and correct), then just roll the shutter all UP / DOWN and its position will be estimated again
- the roller shutter position is estimated based on UP/DOWN timing and therefore an error of ±2% is normal


## Integration with assistants

To be visible to assistants like Google Assistant/Amazon Alexa/Apple HomeKit (Siri) an item must have the correct tag.
Items created automatically with PaperUI (Simple Mode item linking: `Configuration > System > Item Linking > Simple mode > SAVE`) will get automatically the default tag from the binding: in particular items associated with these channels will have the following tags:

- `switch` / `brightness` channels will have the `Lighting` tag
- `shutter` channel will have the `Blinds` tag

After configuration, you can double-check which tags are set looking at the `tags` attribute in the REST API: http://openhabianpi.local:8080/rest/items.

**NOTE** For items created automatically with PaperUI tags are added automatically by the OpenWebNet binding, but you have to check which tags are actually supported by each openHAB add-on (Google Assistant/Alexa/HomeKit).

After items and their correct tags are set, it will be enough to link openHAB with [myopenhab](https://www.openhab.org/addons/integrations/openhabcloud/) and with the Google Assistant/Alexa/HomeKit add-on, and you will be able to discover/control BTicino items from assistants.

The device name imported in the assistant will be label given to the item, and not the item name; usually you can rename devices in the assistants.
(item labels are not mandatory in openHAB, but for the Google Assistant Action they are absolutely necessary)

Note that the most flexible configuration is obtained using `.items` file: see the examples below.

See these official docs and other threads in the OH community for more information about Google Assistant/Alexa/HomeKit integration and configuration:

- Google Assistant (Google Home): <https://www.openhab.org/docs/ecosystem/google-assistant/>
- Amazon Alexa: <https://www.openhab.org/docs/ecosystem/alexa/>
- Apple HomeKit (Siri): <https://www.openhab.org/addons/integrations/homekit/>

**NOTE** You will need to add tags manually for items created using PaperUI when Simple Mode item linking is de-activated, or for items created using `.items` file.

## Full Example

### openwebnet.things:

```xtend
Bridge openwebnet:bus_gateway:mybridge "MyHOMEServer1" [ host="192.168.1.35", passwd="abcde", port=20000, discoveryByActivation=false ] {
      bus_on_off_switch        LR_switch        "Living Room Light"       [ where="51" ]
      bus_dimmer               LR_dimmer        "Living Room Dimmer"      [ where="25#4#01" ]
      bus_dimmer               LR_dalidimmer    "Living Room Dali-Dimmer" [ where="0311#4#01" ]
      bus_automation           LR_shutter       "Living Room Shutter"     [ where="93", shutterRun="10050"]
}
``` 


```xtend
// ZigBee USB Gateway configuration: only needed for radio devices
Bridge openwebnet:zb_gateway:myZBgateway  [serialPort="kkkkkkk"] {
    zb_dimmer          myzigbeedimmer [ where="xxxxx"]
    zb_on_off_switch   myzigbeeswitch [ where="yyyyy"]
}
```

### openwebnet.items:

Items (Light, Dimmer, etc.) will be discovered by Google Assistant/Alexa/HomeKit if their tags are configured like in the example.

```xtend
Switch			iLR_switch			"Light"								<light>          (gLivingRoom)                [ "Lighting" ]  { channel="openwebnet:bus_on_off_switch:mybridge:LR_switch:switch" }
Dimmer			iLR_dimmer			"Dimmer [%.0f %%]"					<DimmableLight>  (gLivingRoom)                [ "Lighting" ]  { channel="openwebnet:bus_dimmer:mybridge:LR_dimmer:brightness" }
Dimmer			iLR_dalidimmer		"Dali-Dimmer [%.0f %%]"				<DimmableLight>  (gLivingRoom)                [ "Lighting" ]  { channel="openwebnet:bus_dimmer:mybridge:LR_dalidimmer:brightness" }
/* For Dimmers, use category DimmableLight to have Off/On switch in addition to the Percent slider in PaperUI */
Rollershutter	iLR_shutter			"Shutter [%.0f %%]"					<rollershutter>  (gShutters, gLivingRoom)     [ "Blinds"   ]  { channel="openwebnet:bus_automation:mybridge:LR_shutter:shutter" }
```

### openwebnet.sitemap

```xtend
sitemap openwebnet label="OpenWebNet Binding Example Sitemap"
{
    Frame label="Living Room"
    {
          Default item=iLR_switch           icon="light"    
          Default item=iLR_dimmer           icon="light" 
          Default item=iLR_dalidimmer       icon="light"
          Default item=iLR_shutter
    }
}
```

## Disclaimer

- This binding is not associated by any means with BTicino or Legrand companies
- Contributors of this binding have no liability for any direct, indirect, incidental, special, exemplary, or consequential damage to things or people caused by using the binding connected to a real BTicino/Legrand (OpenWebNet) plant/system and its physical devices. The final user is the only responsible for using this binding in a real environment. See Articles 5. and 6. of [Eclipse Public Licence 2.0](https://www.eclipse.org/legal/epl-2.0/) under which this binding software is distributed
- The OpenWebNet protocol is maintained and Copyright by BTicino/Legrand. The documentation of the protocol if freely accessible for developers on the [MyOpen Community website - https://www.myopen-legrandgroup.com/developers](https://www.myopen-legrandgroup.com/developers/)
- OpenWebNet, MyHOME, MyHOME_Play and Living Now are registered trademarks by BTicino/Legrand
- This binding uses `openwebnet-lib 0.9.x`, an OpenWebNet Java lib partly based on [openwebnet/rx-openwebnet](https://github.com/openwebnet/rx-openwebnet) client library by @niqdev, modified to support:
    - gateways and OWN frames for ZigBee
    - frame parsing
    - monitoring events from BUS
  
  The lib also uses few modified classes from the old openHAB 1.x BTicino binding for socket handling and priority queues.

## Special thanks

Special thanks for helping on testing this binding go to:
[@m4rk](https://community.openhab.org/u/m4rk/),
[@bastler](https://community.openhab.org/u/bastler),
[@gozilla01](https://community.openhab.org/u/gozilla01),
[@enrico.mcc](https://community.openhab.org/u/enrico.mcc),
[@k0nti](https://community.openhab.org/u/k0nti/),
[@gilberto.cocchi](https://community.openhab.org/u/gilberto.cocchi/),
[@llegovich](https://community.openhab.org/u/llegovich),
[@gabriele.daltoe](https://community.openhab.org/u/gabriele.daltoe)
and many others at the fantastic openHAB community!
