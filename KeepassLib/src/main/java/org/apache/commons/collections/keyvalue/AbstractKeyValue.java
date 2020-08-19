/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package org.apache.commons.collections.keyvalue;

import org.apache.commons.collections.KeyValue;

/**
 * Abstract pair class to assist with creating <code>KeyValue</code>
 * and {@link java.util.Map.Entry Map.Entry} implementations.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author James Strachan
 * @author Michael A. Smith
 * @author Neil O'Toole
 * @author Stephen Colebourne
 */
public abstract class AbstractKeyValue implements KeyValue {

    /** The key */
    protected Object key;
    /** The value */
    protected Object value;

    /**
     * Constructs a new pair with the specified key and given value.
     *
     * @param key  the key for the entry, may be null
     * @param value  the value for the entry, may be null
     */
    protected AbstractKeyValue(Object key, Object value) {
        super();
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key from the pair.
     *
     * @return the key 
     */
    public Object getKey() {
        return key;
    }

    /**
     * Gets the value from the pair.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Gets a debugging String view of the pair.
     * 
     * @return a String view of the entry
     */
    public String toString() {
        return new StringBuffer()
            .append(getKey())
            .append('=')
            .append(getValue())
            .toString();
    }

}