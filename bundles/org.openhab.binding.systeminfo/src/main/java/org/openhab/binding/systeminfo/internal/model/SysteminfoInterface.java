/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.systeminfo.internal.model;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;

/**
 * {@link SysteminfoInterface} defines the methods needed to provide this binding with the required system information.
 *
 * @author Svilen Valkanov - Initial contribution
 * @author Wouter Born - Add null annotations
 */
@NonNullByDefault
public interface SysteminfoInterface {

    // Operating system info
    /**
     * Get the Family of the operating system /e.g. Windows,Unix,.../
     */
    public StringType getOsFamily();

    /**
     * Get the manufacturer of the operating system
     */
    public StringType getOsManufacturer();

    /**
     * Get the version of the operating system
     *
     * @return
     */
    public StringType getOsVersion();

    // CPU info
    /**
     * Get the name of the CPU
     */
    public StringType getCpuName();

    /**
     * Get description about the CPU e.g (model, family, vendor, serial number, identifier, architecture(32bit or
     * 64bit))
     */
    public StringType getCpuDescription();

    /**
     * Get the number of logical CPUs/cores available for processing.
     */
    public DecimalType getCpuLogicalCores();

    /**
     * Get the number of physical CPUs/cores available for processing.
     */
    public DecimalType getCpuPhysicalCores();

    /**
     * Get the recent average CPU load for all logical processors
     *
     * @return the load as percentage value /0-100/
     */
    public DecimalType getCpuLoad();

    /**
     * Returns the system load average for the last minute.
     *
     * @return the load as a number of processes or null, if no information is available
     */
    public @Nullable DecimalType getCpuLoad1();

    /**
     * Returns the system load average for the last 5 minutes.
     *
     * @return the load as number of processes or null, if no information is available
     */
    public @Nullable DecimalType getCpuLoad5();

    /**
     * Returns the system load average for the last 15 minutes.
     *
     * @return the load as number of processes or null, if no information is available
     */
    public @Nullable DecimalType getCpuLoad15();

    /**
     * Get the number of threads currently running
     *
     * @return number of threads
     */
    public DecimalType getCpuThreads();

    /**
     * Get the System uptime (time since boot).
     *
     * @return time in minutes since boot
     */
    public DecimalType getCpuUptime();

    // Battery info
    /**
     * Get battery name
     *
     * @param index
     * @throws IllegalArgumentException
     */
    public StringType getBatteryName(int index) throws IllegalArgumentException;

    /**
     * Get estimated time remaining for the power source.
     *
     * @param index
     * @return minutes remaining charge or null, if the time is estimated as unlimited
     * @throws IllegalArgumentException
     */
    public @Nullable DecimalType getBatteryRemainingTime(int index) throws IllegalArgumentException;

    /**
     * Battery remaining capacity.
     *
     * @param index
     * @return percentage value /0-100/
     * @throws IllegalArgumentException
     */
    public @Nullable DecimalType getBatteryRemainingCapacity(int index) throws IllegalArgumentException;

    // Memory info
    /**
     * Returns total size of memory
     *
     * @return memory size in MB
     */
    public @Nullable DecimalType getMemoryTotal();

    /**
     * Returns available size of memory
     *
     * @return memory size in MB
     */
    public @Nullable DecimalType getMemoryAvailable();

    /**
     * Returns used size of memory
     *
     * @return memory size in MB
     */
    public @Nullable DecimalType getMemoryUsed();

    /**
     * Percents of available memory on the machine
     *
     * @return percent of available memory or null, if no information is available
     */
    public @Nullable DecimalType getMemoryAvailablePercent();

    /**
     * Percents of used memory on the machine
     *
     * @return percent of used memory or null, if no information is available
     */
    public @Nullable DecimalType getMemoryUsedPercent();

    // Swap memory info
    /**
     * Returns total size of swap memory
     *
     * @return memory size in MB or 0, if no there is no swap memory
     */
    public @Nullable DecimalType getSwapTotal();

    /**
     * Returns available size swap of memory
     *
     * @return memory size in MB or 0, if no there is no swap memory
     */
    public @Nullable DecimalType getSwapAvailable();

    /**
     * Returns used size of swap memory
     *
     * @return memory size in MB or 0, if no there is no swap memory
     */
    public @Nullable DecimalType getSwapUsed();

    /**
     * Percents of available swap memory on the machine
     *
     * @return percent of available memory or null, if no there is no swap memory
     */
    public @Nullable DecimalType getSwapAvailablePercent();

    /**
     * Percents of used swap memory on the machine
     *
     * @return percent of used memory or null, if no there is no swap memory
     */
    public @Nullable DecimalType getSwapUsedPercent();

    // Storage info
    /**
     * Returns the total space of the logical storage volume.
     *
     * @param deviceIndex - the index of the logical volume
     * @return storage size in MB
     * @throws IllegalArgumentException
     */
    public @Nullable DecimalType getStorageTotal(int deviceIndex) throws IllegalArgumentException;

    /**
     * Returns the available storage space on the logical storage volume
     *
     * @param deviceIndex - the index of the logical volume
     * @return storage size in MB
     * @throws IllegalArgumentException
     */
    public @Nullable DecimalType getStorageAvailable(int deviceIndex) throws IllegalArgumentException;

    /**
     * Gets the used storage space on the logical storage volume
     *
     * @param deviceIndex - the index of the logical volume
     * @return storage size in MB
     * @throws IllegalArgumentException
     */
    public @Nullable DecimalType getStorageUsed(int deviceIndex) throws IllegalArgumentException;

    /**
     * Gets the percent of available storage on the logical volume
     *
     * @param deviceIndex - the index of the logical volume
     * @return percent of available storage or null
     * @throws IllegalArgumentException
     */
    public @Nullable DecimalType getStorageAvailablePercent(int deviceIndex) throws IllegalArgumentException;

    /**
     * Gets the percent of used storage on the logical volume
     *
     * @param deviceIndex - the index of the logical volume
     * @return percent of used storage or null
     * @throws IllegalArgumentException
     */
    public @Nullable DecimalType getStorageUsedPercent(int deviceIndex) throws IllegalArgumentException;

    /**
     * Gets the name of the logical storage volume
     *
     * @throws IllegalArgumentException
     */
    public StringType getStorageName(int deviceIndex) throws IllegalArgumentException;

    /**
     * Gets the type of the logical storage volume (e.g. NTFS, FAT32)
     *
     * @throws IllegalArgumentException
     */
    public StringType getStorageType(int deviceIndex) throws IllegalArgumentException;

    /**
     * Gets the description of the logical storage volume
     *
     * @throws IllegalArgumentException
     */
    public StringType getStorageDescription(int deviceIndex) throws IllegalArgumentException;

    // Hardware drive info
    /**
     * Gets the name of the physical storage drive
     *
     * @param deviceIndex - index of the storage drive
     * @throws IllegalArgumentException
     */
    public StringType getDriveName(int deviceIndex) throws IllegalArgumentException;

    /**
     * Gets the model of the physical storage drive
     *
     * @param deviceIndex - index of the storage drive
     * @throws IllegalArgumentException
     */
    public StringType getDriveModel(int deviceIndex) throws IllegalArgumentException;

    /**
     * Gets the serial number of the physical storage drive
     *
     * @param deviceIndex - index of the storage drive
     * @throws IllegalArgumentException
     */
    public StringType getDriveSerialNumber(int deviceIndex) throws IllegalArgumentException;

    // Network info
    /**
     * Get the Host IP address of the network.
     *
     * @param index - the index of the network
     * @return 32-bit IPv4 address
     * @throws IllegalArgumentException
     */
    public StringType getNetworkIp(int index) throws IllegalArgumentException;

    /**
     * Get the name of this network.
     *
     * @param index - the index of the network
     * @throws IllegalArgumentException
     */
    public StringType getNetworkName(int index) throws IllegalArgumentException;

    /**
     * The description of the network. On some platforms, this is identical to the name.
     *
     * @param index - the index of the network
     * @throws IllegalArgumentException
     */
    public StringType getNetworkDisplayName(int index) throws IllegalArgumentException;

    /**
     * Gets the MAC Address of the network.
     *
     * @param index - the index of the network
     * @throws IllegalArgumentException
     */
    public StringType getNetworkMac(int index) throws IllegalArgumentException;

    /**
     * Get number of packets received
     *
     * @param index - the index of the network
     * @throws IllegalArgumentException
     */
    public DecimalType getNetworkPacketsReceived(int index) throws IllegalArgumentException;

    /**
     * Get number of packets sent
     *
     * @param index - the index of the network
     * @throws IllegalArgumentException
     */
    public DecimalType getNetworkPacketsSent(int index) throws IllegalArgumentException;

    /**
     * Get data sent in MB for this network
     *
     * @param index - the index of the network
     * @throws IllegalArgumentException
     */
    public DecimalType getNetworkDataSent(int index) throws IllegalArgumentException;

    /**
     * Get data received in MB for this network
     *
     * @param index - the index of the network
     * @throws IllegalArgumentException
     */
    public DecimalType getNetworkDataReceived(int index) throws IllegalArgumentException;

    // Display info
    /**
     * Get information about the display device as product number, manufacturer, serial number, width and height in cm";
     *
     * @param deviceIndex - the index of the display device
     * @throws IllegalArgumentException
     */
    public StringType getDisplayInformation(int deviceIndex) throws IllegalArgumentException;

    // Sensors info
    /**
     * Get the information from the CPU temperature sensors.
     *
     * @return Temperature in degrees Celsius if available, null otherwise.
     */
    public @Nullable DecimalType getSensorsCpuTemperature();

    /**
     * Get the information for the CPU voltage.
     *
     * @return Voltage in Volts if available, null otherwise.
     */
    public @Nullable DecimalType getSensorsCpuVoltage();

    /**
     * Get fan speed
     *
     * @param index
     * @return Speed in rpm or null if unable to measure fan speed
     * @throws IllegalArgumentException
     */
    public @Nullable DecimalType getSensorsFanSpeed(int index) throws IllegalArgumentException;

    /**
     * Returns the name of the process
     *
     * @param pid - the PID of the process
     * @throws IllegalArgumentException - thrown if process with this PID can not be found
     */
    public @Nullable StringType getProcessName(int pid) throws IllegalArgumentException;

    /**
     * Returns the CPU usage of the process
     *
     * @param pid - the PID of the process
     * @return - percentage value /0-100/
     * @throws IllegalArgumentException - thrown if process with this PID can not be found
     */
    public @Nullable DecimalType getProcessCpuUsage(int pid) throws IllegalArgumentException;

    /**
     * Returns the size of RAM memory only usage of the process.
     * It does include all stack and heap memory.
     *
     * @param pid - the PID of the process
     * @return memory size in MB
     * @throws IllegalArgumentException- thrown if process with this PID can not be found
     */
    public @Nullable DecimalType getProcessResidentMemory(int pid) throws IllegalArgumentException;

    /**
     * Returns Virtual Memory Size (VSZ). It includes all memory that the process can access,
     * including memory that is swapped out and memory that is from shared libraries.
     *
     * @param pid - the PID of the process
     * @return memory size in MB
     * @throws IllegalArgumentException- thrown if process with this PID can not be found
     */
    public @Nullable DecimalType getProcessVirtualMemory(int pid) throws IllegalArgumentException;

    /**
     * Returns the full path of the executing process.
     *
     * @param pid - the PID of the process
     * @throws IllegalArgumentException - thrown if process with this PID can not be found
     */
    public @Nullable StringType getProcessPath(int pid) throws IllegalArgumentException;

    /**
     * Returns the number of threads in this process.
     *
     * @param pid - the PID of the process
     * @throws IllegalArgumentException - thrown if process with this PID can not be found
     */
    public @Nullable DecimalType getProcessThreads(int pid) throws IllegalArgumentException;

    /**
     * Returns the number of seconds since the process started..
     *
     * @param pid - the PID of the process
     * @throws IllegalArgumentException - thrown if process with this PID can not be found
     */
    public @Nullable DecimalType getProcessUpTime(int pid) throws IllegalArgumentException;

    /**
     * Returns the user this process is run under.
     *
     * @param pid - the PID of the process
     * @throws IllegalArgumentException - thrown if process with this PID can not be found
     */
    public @Nullable StringType getProcessUser(int pid) throws IllegalArgumentException;
}
