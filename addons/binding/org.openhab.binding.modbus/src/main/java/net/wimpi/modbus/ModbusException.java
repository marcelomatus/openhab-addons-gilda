/***
 * Copyright 2002-2010 jamod development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

package net.wimpi.modbus;

/**
 * Superclass of all specialised exceptions in
 * this package.
 *
 * @author Dieter Wimberger
 * @version @version@ (@date@)
 */
public class ModbusException
    extends Exception {

  /**
   * Constructs a new <tt>ModbusException</tt>
   * instance.
   */
  public ModbusException() {
    super();
  }//constructor

  /**
   * Constructs a new <tt>ModbusException</tt>
   * instance with the given message.
   * <p>
   * @param message the message describing this
   *        <tt>ModbusException</tt>.
   */
  public ModbusException(String message) {
    super(message);
  }//constructor

}//ModbusException
