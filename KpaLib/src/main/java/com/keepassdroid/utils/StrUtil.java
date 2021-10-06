/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StrUtil {
	public static List<String> splitSearchTerms(String search) {
		List<String> list = new ArrayList<String>();
		if (search == null) { return list; }
		
		StringBuilder sb = new StringBuilder();
		boolean quoted = false;
		
		for (int i = 0; i < search.length(); i++) {
			char ch = search.charAt(i);
			
			if ( ((ch == ' ') || (ch == '\t') || (ch == '\r') || (ch == '\n'))
					&& !quoted) {
				
				int len = sb.length();
				if (len > 0) {
					list.add(sb.toString());
					sb.delete(0, len);
				}
				else if (ch == '\"') { 
					quoted = !quoted;
				}
				else {
					sb.append(ch);
				}
			}
		}
		
		if (sb.length() > 0) {
			list.add(sb.toString());
		}
		
		return list;
	}
	
	public static int indexOfIgnoreCase(String text, String search, int start, Locale locale) {
		if (text == null || search == null) return -1;
		
		return text.toLowerCase(locale).indexOf(search.toLowerCase(locale), start);
	}
	
	public static int indexOfIgnoreCase(String text, String search, Locale locale) {
		return indexOfIgnoreCase(text, search, 0, locale);
	}
	
	public static String replaceAllIgnoresCase(String text, String find, String newText, Locale locale) {
		if (text == null || find == null || newText == null) { return text; }
		
		int pos = 0;
		while (pos < text.length()) {
			pos = indexOfIgnoreCase(text, find, pos, locale);
			if (pos < 0) { break; }
			
			String before = text.substring(0, pos);
			String after = text.substring(pos + find.length());
			
			text = before.concat(newText).concat(after);
			pos += newText.length();
		}
		
		return text;
	}
	
}