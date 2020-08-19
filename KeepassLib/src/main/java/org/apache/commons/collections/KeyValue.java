/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package org.apache.commons.collections;

/**
 * Defines a simple key value pair.
 * <p>
 * A Map Entry has considerable additional semantics over and above a simple
 * key-value pair. This interface defines the minimum key value, with just the
 * two get methods.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public interface KeyValue {

    /**
     * Gets the key from the pair.
     *
     * @return the key 
     */
    Object getKey();

    /**
     * Gets the value from the pair.
     *
     * @return the value
     */
    Object getValue();

}