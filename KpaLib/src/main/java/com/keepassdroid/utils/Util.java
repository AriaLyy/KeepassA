/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.keepassdroid.database.exception.SamsungClipboardException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.ClipboardManager;
import android.widget.TextView;

public class Util {
	public static String getClipboard(Context context) {
		ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		CharSequence csText = clipboard.getText();
		if (csText == null) {
			return "";
		}
		
		return csText.toString();
	}
	
	public static void copyToClipboard(Context context, String text) throws SamsungClipboardException {
		ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		
		try {
			clipboard.setText(text);
		} catch (NullPointerException e) {
			throw new SamsungClipboardException(e);
		}
	}
	
	public static void gotoUrl(Context context, String url) throws ActivityNotFoundException {
		if ( url != null && url.length() > 0 ) {
			Uri uri = Uri.parse(url);
			context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
		}
	}
	
	public static void gotoUrl(Context context, int resId) throws ActivityNotFoundException {
		gotoUrl(context, context.getString(resId));
	}

	public static String getEditText(Activity act, int resId) {
		TextView te =  (TextView) act.findViewById(resId);
		
		if (te != null) {
			return te.getText().toString();
		} else {
			return "";
		}
	}
	
	public static void setEditText(Activity act, int resId, String str) {
		TextView te =  (TextView) act.findViewById(resId);
		
		if (te != null) {
			te.setText(str);
		}
	}

	private static final int MAX_BUF_SIZE = 1024;
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[MAX_BUF_SIZE];
		int read;
		while ((read = in.read(buf)) != -1) {
			out.write(buf, 0, read);
		}
	}

	public static int copyStream(InputStream in, OutputStream out, int maxBytes) throws IOException {
	    if (maxBytes <= 0) return 0;

		int bufSize = Math.min(maxBytes, MAX_BUF_SIZE);
		byte[] buf = new byte[bufSize];
		int origMax = maxBytes;

		int read;
		do {
			assert(maxBytes > 0);
			if (maxBytes >= buf.length) {
				read = in.read(buf);
			} else {
				read = in.read(buf, 0, maxBytes);
			}
			if (read == -1) { break; }

			out.write(buf, 0 , read);
			maxBytes -= read;

		} while (maxBytes > 0);

		// return total amonut read
		return origMax - maxBytes;
	}

	
	
}