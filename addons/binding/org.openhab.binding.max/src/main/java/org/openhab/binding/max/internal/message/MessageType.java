/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.max.internal.message;

/**
* This enumeration represents the different message types provided by the MAX!Cube protocol. 
* 
* @author Andreas Heil (info@aheil.de)
* @since 1.4.0
*/
public enum MessageType {
	H("H:"),
	M("M:"),
	C("C:"), 
	L("L:"),
	S("S:");
	
	private String messageIndicator;
	
	MessageType(String messageIndicator) {
		this.messageIndicator = messageIndicator;
	}
	
	public String getMessageIndicator() {
		return this.messageIndicator;
	}
}
