/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.database.exception;

import android.net.Uri;

import com.keepassdroid.utils.EmptyUtils;

import java.io.FileNotFoundException;

/**
 * Created by bpellin on 3/14/16.
 */
public class ContentFileNotFoundException extends FileNotFoundException {
    public static FileNotFoundException getInstance(Uri uri) {
        if (uri == null) { return new FileNotFoundException(); }

        String scheme = uri.getScheme();

        if (!EmptyUtils.isNullOrEmpty(scheme) && scheme.equalsIgnoreCase("content")) {
            return new ContentFileNotFoundException();
        }

        return new FileNotFoundException();
    }

    public  ContentFileNotFoundException() {
        super();
    }
}