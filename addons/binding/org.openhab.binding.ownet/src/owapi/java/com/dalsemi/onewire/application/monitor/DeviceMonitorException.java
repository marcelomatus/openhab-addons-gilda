/*---------------------------------------------------------------------------
 * Copyright (C) 2002 Dallas Semiconductor Corporation, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL DALLAS SEMICONDUCTOR BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Dallas Semiconductor
 * shall not be used except as stated in the Dallas Semiconductor
 * Branding Policy.
 *---------------------------------------------------------------------------
 */

package com.dalsemi.onewire.application.monitor;

import com.dalsemi.onewire.adapter.DSPortAdapter;

/**
 * Represents an encapsulated exception from a particular Device Monitor.
 * The state of this exception includes the device monitor object from
 * which the exception was thrown, the DSPortAdapter object that was being
 * used by the monitor, and the exact exception that was thrown.
 *
 * @author SH
 * @version 1.00
 */
public class DeviceMonitorException
   extends Exception
{
   /** The monitor which generated the event */
   protected AbstractDeviceMonitor deviceMonitor = null;
   /** The DSPortAdapter the monitor was using at the time of event */
   protected DSPortAdapter adapter = null;
   /** The exception that was generated by the search */
   protected Exception exception;

   /**
    * Creates a new DeviceMonitor event with the specified characteristics.
    *
    * @param source The monitor which generated the exception
    * @param adapter The DSPortAdapter the monitor was using
    * @param exception The actual exception which was thrown
    */
   DeviceMonitorException(AbstractDeviceMonitor deviceMonitor,
                          DSPortAdapter adapter,
                          Exception exception)
   {
      super("Device Monitor Exception");
      this.deviceMonitor = deviceMonitor;
      this.adapter = adapter;
      this.exception = exception;
   }

   /**
    * Returns the monitor which generated this event
    *
    * @return the monitor which generated this event
    */
   public AbstractDeviceMonitor getMonitor()
   {
      return this.deviceMonitor;
   }

   /**
    * Returns DSPortAdapter the monitor was using when the event was generated
    *
    * @return DSPortAdapter the monitor was using
    */
   public DSPortAdapter getAdapter()
   {
      return this.adapter;
   }

   /**
    * Returns the wrapped exception that was generated during a 1-Wire search.
    *
    * @return the wrapped exception that was generated during a 1-Wire search.
    */
   public Exception getException()
   {
      return exception;
   }

   /**
    * Throws the wrapped exception to the calling object.
    *
    */
   public void throwException()
      throws Exception
   {
      throw exception;
   }

   /**
    * Converts this object to a String.
    *
    * @return a string representation of this object
    */
   public String toString()
   {
      return "Device Monitor Exception: " + exception.toString();
   }
}