/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.exception;

public class InvalidPasswordException extends InvalidDBException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8729476180242058319L;

	public InvalidPasswordException(String str) {
		super(str);
	}
	
	public InvalidPasswordException() {
		super();
	}
}