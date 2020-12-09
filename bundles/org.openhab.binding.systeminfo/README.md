# Systeminfo Binding

System information Binding provides operating system and hardware information including:

-   Operating system name, version and manufacturer;
-   CPU recent and average load for last 1, 5, 15 minutes, name, description, number of physical and logical cores, running threads number, system uptime;
-   Free, total and available memory;
-   Free, total and available swap memory;
-   Hard drive name, model and serial number;
-   Free, total, available storage space and storage type (NTSFS, FAT32 ..);
-   Battery information - estimated remaining time, capacity, name;
-   Sensors information - CPU voltage and temperature, fan speeds;
-   Display information;
-   Network IP,name and adapter name, mac, data sent and received, packets sent and received;
-   Process information - size of RAM memory used, CPU load, process name, path, number of threads.

The binding uses [OSHI](https://github.com/oshi/oshi) API to access this information regardless of the underlying
platform and does not need any native parts.

## Bridge configuration

Bridge **computer** represents a system with one storage volume, one display device and one network adapter:

```
Bridge systeminfo:computer:<SystemId> [ interval_high=<number>, interval_medium=<number> ]
```

| Parameter       | Type    | Required   | Default   | Description                                                                    |
| --------------- | :-----: | :--------: | :-------: | ------------------------------------------------------------------------------ |
| interval_high   | Number  | No         | 1 second  | Refresh interval in seconds for channels with 'High' priority configuration.   |
| interval_medium | Number  | No         | 60 second | Refresh interval in seconds for channels with 'Medium' priority configuration. |

That means that by default configuration:

*   channels with priority set to 'High' are updated every second
*   channels with priority set to 'Medium' are updated every minute
*   channels with priority set to 'Low' are updated only at initialization or at Refresh command.

For more info see [channel configuration](#channel-configuration).
The bridge has the following properties:

-   `cpu_logicalCores` - Number of CPU logical cores
-   `cpu_physicalCores` - Number of CPU physical cores
-   `os_manufacturer` - The manufacturer of the operating system
-   `os_version` - The version of the operating system
-   `os_family` - The family of the operating system

If multiple storage or display devices support is needed, new thing type has to be defined.
This is workaround until [this issue](https://github.com/eclipse/smarthome/issues/588) is resolved and
it is possible to add dynamically channels to DSL defined thing.

## Thing configuration

The binding supports one thing type - **process**.

```
Thing process <ProcessName> [ pid=<number> ]
```

| Parameter | Type    | Required   | Default   | Description         |
| --------- | :-----: | :--------: | :-------: | ------------------- |
| pid       | Number  | Yes        | 0         | Process identifier. |

## Discovery

The discovery service implementation tries to resolve the computer name.
If the resolving process fails, the computer name is set to "Unknown".
In both cases it creates a Discovery Result with bridge type  **computer**.

When [this issue](https://github.com/eclipse/smarthome/issues/1118) is resolved it will be possible to implement creation of dynamic channels (e.g. the binding will scan how much storage devices are present and create channel groups for them).
At the moment this is not supported.

## Binding configuration

No binding configuration required.

## Channels

The binding support several channel group. Each channel group, contains one or more channels.
In the list below, you can find, how are channel group and channels id`s related.

**bridge** `computer`

*   **group** `memory`
  * **channel** `available, total, used, availablePercent, usedPercent`
*   **group** `swap`
  * **channel** `available, total, used, availablePercent, usedPercent`
*   **group** `storage` (deviceIndex)
  * **channel** `available, total, used, availablePercent, usedPercent, name, description, type`
*   **group** `drive` (deviceIndex)
  * **channel** `name, model, serial`
*   **group** `display` (deviceIndex)
  * **channel** `information`
*   **group** `battery` (deviceIndex)
  * **channel** `name, remainingCapacity, remainingTime`
*   **group** `cpu`
  * **channel** `name, description, load, load1, load5, load15, uptime`
*   **group** `sensors`
  * **channel** `cpuTemp, cpuVoltage, fanSpeed`
*   **group** `network` (deviceIndex)
  * **channel** `ip, mac, networkDisplayName, networkName, packetsSent, packetsReceived, dataSent, dataReceived`

The groups marked with "(deviceIndex)" may have device index attached to the Channel Group.

-   channel ::= channel_group & (deviceIndex)
-   deviceIndex ::= number > 0
-   (e.g. *storage1#available*)

The binding uses this index to get information about a specific device from a list of devices (e.g on a single computer
several local disks could be installed with names C:\, D:\, E:\ - the first will have deviceIndex=0, the second
deviceIndex=1 etc). If device with this index is not existing, the binding will display an error message on the console.

Unfortunately this feature can't be used at the moment without manually adding these new channel groups to the thing
description (located in OH-INF/thing/computer.xml).

The table shows more detailed information about each channel type:

| Channel ID         | Channel Description                                              | Item type                | Default priority | Advanced |
| ------------------ | :--------------------------------------------------------------: | :----------------------: | :--------------: | -------- |
| load               | Recent load in percents                                          | Number:Dimensionless     | High             | False    |
| load1              | Load for the last 1 minute in percents                           | Number:Dimensionless     | Medium           | True     |
| load5              | Load for the last 5 minutes in percents                          | Number:Dimensionless     | Medium           | True     |
| load15             | Load for the last 15 minutes in percents                         | Number:Dimensionless     | Medium           | True     |
| threads            | Number of threads currently running                              | Number                   | Medium           | True     |
| uptime             | System uptime (time after start) in minutes                      | Number                   | Medium           | True     |
| name               | Name of the device                                               | String                   | Low              | False    |
| available          | Available memory size                                            | Number:DataAmount        | High             | False    |
| used               | Used memory size                                                 | Number:DataAmount        | High             | False    |
| total              | Total memory size                                                | Number:DataAmount        | Low              | False    |
| availablePercent   | Available size in %                                              | Number:Dimensionless     | High             | False    |
| usedPercent        | Used size in %                                                   | Number:Dimensionless     | High             | False    |
| model              | The model of the device                                          | String                   | Low              | True     |
| serial             | The serial number of the device                                  | String                   | Low              | True     |
| description        | Description of the device                                        | String                   | Low              | True     |
| type               | Storage type                                                     | String                   | Low              | True     |
| temperature        | CPU Temperature in degrees Celsius                               | Number:Temperature       | High             | True     |
| voltage            | CPU Voltage in V                                                 | Number:ElectricPotential | Medium           | True     |
| speed              | Fan speed in rpm                                                 | Number                   | Medium           | True     |
| remainingTime      | Remaining time                                                   | Number:Time              | Medium           | False    |
| remainingCapacity  | Remaining capacity in %                                          | Number:Dimensionless     | Medium           | False    |
| information        | Product, manufacturer, SN, width and height of the display in cm | String                   | Low              | True     |
| ip                 | Host IP address of the network                                   | String                   | Low              | False    |
| mac                | MAC address                                                      | String                   | Low              | True     |
| networkName        | The name of the network                                          | String                   | Low              | False    |
| networkDisplayName | The display name of the network                                  | String                   | Low              | False    |
| packetsSent        | Number of packets sent                                           | Number                   | Medium           | True     |
| packetsReceived    | Number of packets received                                       | Number                   | Medium           | True     |
| dataSent           | Data sent                                                        | Number:DataAmount        | Medium           | True     |
| dataReceived       | Data received                                                    | Number:DataAmount        | Medium           | True     |

**thing** `process`

| Channel ID         | Channel Description                                              | Item type         | Default priority | Advanced |
| ------------------ | :--------------------------------------------------------------: | :---------------: | :--------------: | -------- |
| name               | Name of the process                                              | String            | Low              | False    |
| resident           | Resident Set Size (RSS)                                          | Number:DataAmount | High             | False    |
| virtual            | Virtual Memory Size (VSZ)                                        | Number:DataAmount | High             | False    |
| user               | Name of the user this proccess runsString                        | String            | Low              | True     |
| path               | Full path of the process                                         | String            | Low              | True     |
| threads            | Number of threads in this process                                | Number            | Medium           | True     |

VSZ includes all memory that the process can access, including memory that is swapped out and memory that is from
shared libraries. RSS shows how much memory is allocated to that process and is in RAM. It does not include memory
that is swapped out. It does include memory from shared libraries as long as the pages from those libraries are
actually in memory. It does include all stack and heap memory.

## Channel configuration

All channels can change its configuration parameters at runtime.
The binding will trigger the necessary changes (reduce or increase the refresh time, change channel priority or the process that is being tracked).

Each of the channels has a default configuration parameter - priority.
It has the following options:

-   **High**
-   **Medium**
-   **Low**

## Reporting issues

As already mentioned this binding depends heavily on the [OSHI](https://github.com/oshi/oshi) API to provide the operating system and hardware information.

Take a look at the console for an ERROR log message.

If you find an issue with a support for a specific hardware or software architecture please take a look at the [OSHI issues](https://github.com/oshi/oshi/issues).
Your problem might have be already reported and solved!
Feel free to open a new issue there with the log message and the and information about your software or hardware configuration.

After the issue is resolved the binding has to be [updated](#updating-this-binding).

For a general problem with the binding report the issue directly to openHAB.

## Updating this binding

OSHI project has a good support and regularly updates the library with fixes to issues and new features.

In order to update the version used in the binding, follow these easy steps:

-   Go to the [OSHI GitHub repo](https://github.com/oshi/oshi) and download the newest version available of the module oshi-core or download the jar from the [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Coshi-). Check if the versions of the OSHI dependencies as well (jna and jna-platform) are changed;
-   Replace the jars in lib folder;
-   Modify the .classpath file with the new versions of the jars;
-   Modify the header Bundle-ClassPath in the META-INF/MANIFEST.MF.

## Example

Things:

```
Bridge systeminfo:computer:server "Server" @ "Server" [interval_high=1, interval_medium=60]
{
  Thing process CoolService "Cool service" @ "Server" [ pid=20 ]
}
```

Items:

```
/* Network information*/
String Network_AdapterName        "Adapter name"        <network>        { channel="systeminfo:computer:server:network#networkDisplayName" }
String Network_Name               "Name"                <network>        { channel="systeminfo:computer:server:network#networkName" }
String Network_IP                 "IP address"          <network>        { channel="systeminfo:computer:server:network#ip" }
String Network_Mac                "Mac address"         <network>        { channel="systeminfo:computer:server:network#mac" }
Number Network_DataSent           "Data sent"           <flowpipe>       { channel="systeminfo:computer:server:network#dataSent" }
Number Network_DataReceived       "Data received"       <returnpipe>     { channel="systeminfo:computer:server:network#dataReceived" }
Number Network_PacketsSent        "Packets sent"        <flowpipe>       { channel="systeminfo:computer:server:network#packetsSent" }
Number Network_PacketsReceived    "Packets received"    <returnpipe>     { channel="systeminfo:computer:server:network#packetsReceived" }

/* CPU information*/
String CPU_Name                   "Name"                <none>           { channel="systeminfo:computer:server:cpu#name" }
String CPU_Description            "Description"         <none>           { channel="systeminfo:computer:server:cpu#description" }
Number CPU_Load                   "Load"                <none>           { channel="systeminfo:computer:server:cpu#load"}
Number CPU_Load1                  "Load (1 min)"        <none>           { channel="systeminfo:computer:server:cpu#load1" }
Number CPU_Load5                  "Load (5 min)"        <none>           { channel="systeminfo:computer:server:cpu#load5" }
Number CPU_Load15                 "Load (15 min)"       <none>           { channel="systeminfo:computer:server:cpu#load15" }
Number CPU_Threads                "Threads"             <none>           { channel="systeminfo:computer:server:cpu#threads" }
Number CPU_Uptime                 "Uptime"              <time>           { channel="systeminfo:computer:server:cpu#uptime" }

/* Drive information*/
String Drive_Name                 "Name"                <none>           { channel="systeminfo:computer:server:drive#name" }
String Drive_Model                "Model"               <none>           { channel="systeminfo:computer:server:drive#model" }
String Drive_Serial               "Serial"              <none>           { channel="systeminfo:computer:server:drive#serial" }

/* Storage information*/
String Storage_Name               "Name"                <none>           { channel="systeminfo:computer:server:storage#name" }
String Storage_Type               "Type"                <none>           { channel="systeminfo:computer:server:storage#type" }
String Storage_Description        "Description"         <none>           { channel="systeminfo:computer:server:storage#description" }
Number Storage_Available          "Available"           <none>           { channel="systeminfo:computer:server:storage#available" }
Number Storage_Used               "Used"                <none>           { channel="systeminfo:computer:server:storage#used" }
Number Storage_Total              "Total"               <none>           { channel="systeminfo:computer:server:storage#total" }
Number Storage_Available_Percent  "Available (%)"       <none>           { channel="systeminfo:computer:server:storage#availablePercent" }
Number Storage_Used_Percent       "Used (%)"            <none>           { channel="systeminfo:computer:server:storage#usedPercent" }

/* Memory information*/
Number Memory_Available           "Available"           <none>           { channel="systeminfo:computer:server:memory#available" }
Number Memory_Used                "Used"                <none>           { channel="systeminfo:computer:server:memory#used" }
Number Memory_Total               "Total"               <none>           { channel="systeminfo:computer:server:memory#total" }
Number Memory_Available_Percent   "Available (%)"       <none>           { channel="systeminfo:computer:server:memory#availablePercent" }
Number Memory_Used_Percent        "Used (%)"            <none>           { channel="systeminfo:computer:server:memory#usedPercent" }

/* Swap memory information*/
Number Swap_Available             "Available"           <none>           { channel="systeminfo:computer:server:swap#available" }
Number Swap_Used                  "Used"                <none>           { channel="systeminfo:computer:server:swap#used" }
Number Swap_Total                 "Total"               <none>           { channel="systeminfo:computer:server:swap#total" }
Number Swap_Available_Percent     "Available (%)"       <none>           { channel="systeminfo:computer:server:swap#availablePercent" }
Number Swap_Used_Percent          "Used (%)"            <none>           { channel="systeminfo:computer:server:swap#usedPercent" }

/* Battery information*/
String Battery_Name               "Name"                <batterylevel>   { channel="systeminfo:computer:server:battery#name" }
Number Battery_RemainingCapacity  "Remaining Capacity"  <batterylevel>   { channel="systeminfo:computer:server:battery#remainingCapacity" }
Number Battery_RemainingTime      "Remaining Time"      <batterylevel>   { channel="systeminfo:computer:server:battery#remainingTime" }

/* Display information*/
String Display_Description        "Display description" <screen>         { channel="systeminfo:computer:server:display#information" }

/* Sensors information*/
Number Sensor_FanSpeed            "Fan speed"           <fan>            { channel="systeminfo:computer:server:fans#fanSpeed" }

/* Process information*/
Number Process_load               "Load [%.1f%%]"         <none> { channel="systeminfo:process:server:CoolService:load" }
Number Process_real_memory        "Real memory [%.1f]"    <none> { channel="systeminfo:process:server:CoolService:resident" }
Number Process_virtual_memory     "Virtual memory [%.1f]" <none> { channel="systeminfo:process:server:CoolService:virtual" }
String Process_name               "Name [%s]"             <none> { channel="systeminfo:process:server:CoolService:name" }
String Process_user               "User [%s]"             <none> { channel="systeminfo:process:server:CoolService:user" }
String Process_path               "Path"                  <none> { channel="systeminfo:process:server:CoolService:path" }
Number Process_threads            "Threads"               <none> { channel="systeminfo:process:server:CoolService:threads" }
```

Sitemap:

```
Text label="Systeminfo" {
    Frame label="Network Information" {
        Default item=Network_AdapterName
        Default item=Network_Name
        Default item=Network_IP
        Default item=Network_Mac
        Default item=Network_DataSent
        Default item=Network_DataReceived
        Default item=Network_PacketsSent
        Default item=Network_PacketsReceived
    }
    Frame label="CPU Information" {
        Default item=CPU_Name
        Default item=CPU_Description
        Default item=CPU_Load
        Default item=CPU_Load1
        Default item=CPU_Load5
        Default item=CPU_Load15
        Default item=CPU_Threads
        Default item=CPU_Uptime
    }
    Frame label="Drive Information" {
        Default item=Drive_Name
        Default item=Drive_Model
        Default item=Drive_Serial
    }
    Frame label="Storage Information" {
        Default item=Storage_Name
        Default item=Storage_Type
        Default item=Storage_Description
        Default item=Storage_Available
        Default item=Storage_Used
        Default item=Storage_Total
        Default item=Storage_Available_Percent
        Default item=Storage_Used_Percent
    }
    Frame label="Memory Information" {
        Default item=Memory_Available
        Default item=Memory_Used
        Default item=Memory_Total
        Default item=Memory_Available_Percent
        Default item=Memory_Used_Percent
    }
    Frame label="Swap Memory Information" {
        Default item=Swap_Available
        Default item=Swap_Used
        Default item=Swap_Total
        Default item=Swap_Available_Percent
        Default item=Swap_Used_Percent
    }
    Frame label="Battery, Display and Sensor Information" {
        Default item=Battery_Name
        Default item=Battery_RemainingCapacity
        Default item=Battery_RemainingTime
        Default item=Display_Description
        Default item=Sensor_CPUTemp
        Default item=Sensor_CPUVoltage
        Default item=Sensor_FanSpeed
    }
    Frame label="Process Information" {
        Default item=Process_load
        Default item=Process_used
        Default item=Process_name
        Default item=Process_threads
        Default item=Process_path
    }
}
```
