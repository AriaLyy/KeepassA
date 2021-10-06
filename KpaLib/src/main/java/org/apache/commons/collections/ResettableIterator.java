/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package org.apache.commons.collections;

import java.util.Iterator;

/** 
 * Defines an iterator that can be reset back to an initial state.
 * <p>
 * This interface allows an iterator to be repeatedly reused.
 *
 * @since Commons Collections 3.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author Stephen Colebourne
 */
public interface ResettableIterator extends Iterator {

    /**
     * Resets the iterator back to the position at which the iterator
     * was created.
     */
    public void reset();

}