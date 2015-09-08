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

package net.wimpi.modbus.procimg;

import net.wimpi.modbus.util.Observable;


/**
 * Class implementing an observable register.
 *
 * @author Dieter Wimberger
 * @version @version@ (@date@)
 */
public class ObservableRegister
    extends Observable
    implements Register {

  /**
   * The word (<tt>byte[2]</tt> holding the content of this register.
   */
  protected byte[] m_Register = new byte[2];

  public int getValue() {
    return ((m_Register[0] & 0xff) << 8 | (m_Register[1] & 0xff));
  }//getValue

  public final int toUnsignedShort() {
    return ((m_Register[0] & 0xff) << 8 | (m_Register[1] & 0xff));
  }//toUnsignedShort

  public final synchronized void setValue(int v) {
    m_Register[0] = (byte) (0xff & (v >> 8));
    m_Register[1] = (byte) (0xff & v);
    notifyObservers("value");
  }//setValue

  public final short toShort() {
    return (short) ((m_Register[0] << 8) | (m_Register[1] & 0xff));
  }//toShort

  public final synchronized void setValue(short s) {
    m_Register[0] = (byte) (0xff & (s >> 8));
    m_Register[1] = (byte) (0xff & s);
    notifyObservers("value");
  }//setValue

  public final synchronized void setValue(byte[] bytes) {
    if (bytes.length < 2) {
      throw new IllegalArgumentException();
    } else {
      m_Register[0] = bytes[0];
      m_Register[1] = bytes[1];
      notifyObservers("value");
    }
  }//setValue

  public byte[] toBytes() {
    return m_Register;
  }//toBytes
  
}//class ObservableRegister
