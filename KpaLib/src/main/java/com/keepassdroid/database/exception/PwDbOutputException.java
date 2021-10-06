/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.exception;

public class PwDbOutputException extends Exception {
	public PwDbOutputException(String string) {
		super(string);
	}

	public PwDbOutputException(String string, Exception e) { super(string, e); }

	public PwDbOutputException(Exception e) {
		super(e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3321212743159473368L;
}