# Smappee Binding

This binding integrates the [Smappee energy monitor](http://www.smappee.com/be_en/home).

## Introduction

Thanks to the Smappee energy monitor you always know how much power your appliances consume, wherever you are. For more comfort, a better insight and sustainable energy savings up to 30%.

Both the Energy monitor and the Solar energy monitor can be used. These can be bought online in the [smappee webshop] (http://www.smappee.com/be_en/eshop/monitors.html).

The smappee supports clamps for 3 phase and 1 phase systems and there is a separate circuit clamp for your solar system. installation manuals can be found on [their support page](https://www.smappee.com/be_en/support)

## Supported Things

This binding provides 1 bridge thing type : 'smappee'. There should be only 1 smappee device installed in your home.
This thing act like a bridge and represents your smappee monitor device and should be configured first. Once configured, this will autodetect all other smappee things.
Detected things :
- smappee-appliance : A detected appliance, the appliances that are not categorized yet (type "Find Me"), are skipped. 
- smappee-actuator : A paired smappee switch.
- smappee-sensor : A paired water or gas sensor.

## Discovery

The smappee thing must be configured manually. Appliances, Actuators and the sensors are auto-detected. 

## Smappee thing configuration

As described on their [support page] (https://support.smappee.com/hc/en-us/articles/202153935-Where-can-I-find-the-API-documentation-), you need to email [support@smappee.com] (mailto:support@smappee.com) to get a client ID and a client secret to access the api. In that mail you need to provide them :
- Full name: your full name
- Monitor serial number: this can be found at the back of your smappee
- Account username: the username you use to log in in Smappee, same as thing setting 'Username'

### Configuration in PaperUi

Following settings must be configured in order to make your smappee binding work :

#### Smappee Bridge

| Setting              |                                                                        |
|----------------------|------------------------------------------------------------------------|
|Client Id             | The Smappee Api Oauth client id (obtain by mail from smappee support) |
|Client Secret         | The Smappee Api Oauth client secret (obtain by mail from smappee support)|
|Username              | The username of your Smappee|
|Password              | The password of your Smappee|
|Service location name | The name of your Smappee installation|
|Polling time          | How often (in minutes) does the smappee needs to be checked ?|

#### Appliance Thing

| Setting              |                                                                                                              |
|----------------------|--------------------------------------------------------------------------------------------------------------|
|Id                    | The unique identifier of the detected appliance, This is the number after "Find Me" for unlabeled appliances |
|Type                  | This is the smappee category ("Air Conditioner", "Blinds", "Microwave", ...                                  |

#### Actuator Thing

| Setting              |                                                                                                              |
|----------------------|--------------------------------------------------------------------------------------------------------------|
|Id                    | The unique identifier of the linked plug, This is a unique number starting from 1 for each installed plug    |

#### Sensor Thing

| Setting              |                                                                                                              |
|----------------------|--------------------------------------------------------------------------------------------------------------|
|Id                    | The unique identifier of the sensor, is a combined identifier : format : [SensorId-ChannelId], eg 1-1    |

### Configuration with config files

A manual setup through a `things/smappee.things` file could look like this:

```
Bridge smappee:smappee:mySmappee "Smappee" @ "Living Room" [client_id="xxx", client_secret="xxx", username="xxx", password="xxx", servicelocationname="xxx", polltime=5]
{
    Thing smappee:smappee-appliance:myAppliance [ id="xxx", type="Blinds" ]
    Thing smappee:smappee-actuator:myPlug [ id="xxx" ]
    Thing smappee:smappee-sensor:mySensor [ id="xxx" ]
}
```

## Channels

The following channels are supported :

#### Smappee Bridge

| Channel Type ID                 | Item Type    | Description                                                                |
|---------------------------------|--------------|----------------------------------------------------------------------------|
| smappee-consumption-channel     | Number       | The amount of energy that is being consumed now                            |
| smappee-solar-channel           | Number       | The amount of energy that is being generated by your solar power panels now|
| smappee-alwayson-channel        | Number       | The amount of energy that is always consumed                               |
 
#### Appliance Thing

| Channel Type ID            | Item Type    | Description                                                    |
|----------------------------|--------------|----------------------------------------------------------------|
| smappee-appliance-power    | Number       | The amount of energy that is being consumed now                |
|                            |              | Note : sometimes the api doesn't return any reading,           |
|                            |              | this can be validated in the appliance settings, show events   |

#### Actuator Thing

| Channel Type ID            | Item Type    | Description                                       |
|----------------------------|--------------|---------------------------------------------------|
| smappee-actuator-switch    | Switch       | Turn on the switch                                |

#### Sensor Thing 

| Channel Type ID            | Item Type    | Description                                       |
|----------------------------|--------------|---------------------------------------------------|
| smappee-sensor-value       | String       | The measured value of this sensor                 | 
 
## Item configuration

### Configuration with config files

A manual configuration through a `demo.items` file could look like this:

```
Number mySmappee_Consumption     "Consumption [%s]"      { channel="smappee:smappee:mySmappee:smappee-consumption-channel" }
Number mySmappee_Solar           "Solar [%s]"            { channel="smappee:smappee:mySmappee:smappee-solar-channel" } 
Number mySmappee_AlwaysOn        "Always On [%s]"        { channel="smappee:smappee:mySmappee:smappee-alwayson-channel" }

Number mySmappee_AppliancePower  "Appliance Power [%s]"  { channel="smappee:smappee:myAppliance:smappee-appliance-power" }
```
