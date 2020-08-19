/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.keepassdroid.utils;

import java.util.Calendar;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;

import java.util.Date;

public class DateUtil {
    private static final DateTime dotNetEpoch = new DateTime(1, 1, 1, 0, 0, 0, DateTimeZone.UTC);
    private static final DateTime javaEpoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeZone.UTC);

    private static final long epochOffset;
    private static Calendar calendar;

    static {
        Date dotNet = dotNetEpoch.toDate();
        Date java = javaEpoch.toDate();

        epochOffset = (javaEpoch.getMillis() - dotNetEpoch.getMillis()) / 1000L;
    }

    public static Date convertKDBX4Time(long seconds) {

        DateTime dt = dotNetEpoch.plus(seconds * 1000L);

        // Switch corrupted dates to a more recent date that won't cause issues on the client
        if (dt.isBefore(javaEpoch)) {
            return javaEpoch.toDate();
        }

        return dt.toDate();
    }

    public static long convertDateToKDBX4Time(DateTime dt) {
        try {
            Seconds secs = Seconds.secondsBetween(javaEpoch, dt);
            return secs.getSeconds() + epochOffset;
        } catch (ArithmeticException e) {
            // secondsBetween overflowed an int
            Date javaDt = dt.toDate();
            long seconds = javaDt.getTime() / 1000L;
            return seconds + epochOffset;
        }
    }
    public static Calendar getCalendar() {
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }

        return calendar;
    }
}