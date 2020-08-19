/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.service.multidatasetservice.model

import android.util.Log
import java.util.Locale

object W3cHints {
  const val TAG = "W3cHints"
  const val HONORIFIC_PREFIX = "honorific-prefix";
  const val NAME = "name";
  const val GIVEN_NAME = "given-name";
  const val ADDITIONAL_NAME = "additional-name";
  const val FAMILY_NAME = "family-name";
  const val HONORIFIC_SUFFIX = "honorific-suffix";
  const val USERNAME = "username";
  const val NEW_PASSWORD = "new-password";
  const val CURRENT_PASSWORD = "current-password";
  const val ORGANIZATION_TITLE = "organization-title";
  const val ORGANIZATION = "organization";
  const val STREET_ADDRESS = "street-address";
  const val ADDRESS_LINE1 = "address-line1";
  const val ADDRESS_LINE2 = "address-line2";
  const val ADDRESS_LINE3 = "address-line3";
  const val ADDRESS_LEVEL4 = "address-level4";
  const val ADDRESS_LEVEL3 = "address-level3";
  const val ADDRESS_LEVEL2 = "address-level2";
  const val ADDRESS_LEVEL1 = "address-level1";
  const val COUNTRY = "country";
  const val COUNTRY_NAME = "country-name";
  const val POSTAL_CODE = "postal-code";
  const val CC_NAME = "cc-name";
  const val CC_GIVEN_NAME = "cc-given-name";
  const val CC_ADDITIONAL_NAME = "cc-additional-name";
  const val CC_FAMILY_NAME = "cc-family-name";
  const val CC_NUMBER = "cc-number";
  const val CC_EXPIRATION = "cc-exp";
  const val CC_EXPIRATION_MONTH = "cc-exp-month";
  const val CC_EXPIRATION_YEAR = "cc-exp-year";
  const val CC_CSC = "cc-csc";
  const val CC_TYPE = "cc-type";
  const val TRANSACTION_CURRENCY = "transaction-currency";
  const val TRANSACTION_AMOUNT = "transaction-amount";
  const val LANGUAGE = "language";
  const val BDAY = "bday";
  const val BDAY_DAY = "bday-day";
  const val BDAY_MONTH = "bday-month";
  const val BDAY_YEAR = "bday-year";
  const val SEX = "sex";
  const val URL = "url";
  const val PHOTO = "photo";

  // Optional W3C prefixes
  const val PREFIX_SECTION = "section-";
  const val SHIPPING = "shipping";
  const val BILLING = "billing";

  // W3C prefixes below...
  const val PREFIX_HOME = "home";
  const val PREFIX_WORK = "work";
  const val PREFIX_FAX = "fax";
  const val PREFIX_PAGER = "pager";

  // ... require those suffix
  const val TEL = "tel";
  const val TEL_COUNTRY_CODE = "tel-country-code";
  const val TEL_NATIONAL = "tel-national";
  const val TEL_AREA_CODE = "tel-area-code";
  const val TEL_LOCAL = "tel-local";
  const val TEL_LOCAL_PREFIX = "tel-local-prefix";
  const val TEL_LOCAL_SUFFIX = "tel-local-suffix";
  const val TEL_EXTENSION = "tel_extension";
  const val EMAIL = "email";
  const val IMPP = "impp";

  fun isW3cSectionPrefix(hint: String): Boolean {
    return hint.toUpperCase(Locale.ROOT)
        .startsWith(PREFIX_SECTION);
  }

  fun isW3cAddressType(hint: String): Boolean {
    when (hint.toUpperCase(Locale.ROOT)) {
      SHIPPING, BILLING ->
        return true
    }
    return false
  }

  fun isW3cTypePrefix(hint: String): Boolean {
    when (hint.toUpperCase(Locale.ROOT)) {
      PREFIX_WORK, PREFIX_FAX, PREFIX_HOME, PREFIX_PAGER ->
        return true
    }
    return false
  }

  fun isW3cTypeHint(hint: String): Boolean {
    when (hint.toUpperCase(Locale.ROOT)) {
      TEL, TEL_COUNTRY_CODE, TEL_NATIONAL, TEL_AREA_CODE, TEL_LOCAL,
      TEL_LOCAL_PREFIX, TEL_LOCAL_SUFFIX, TEL_EXTENSION, EMAIL, IMPP ->
        return true;
    }
    Log.w(TAG, "Inid W3C type hint: $hint");
    return false;
  }
}