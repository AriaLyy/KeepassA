package com.arialyy.frame.util

import java.io.Serializable

/**
 * Represents a triad of values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Triple exhibits value semantics, i.e. two triples are equal if all three components are equal.
 * An example of decomposing it into values:
 * @sample samples.misc.Tuples.tripleDestructuring
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the third value.
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 * @property fourth Third value.
 */
public data class FourTimes<out A, out B, out C, out D>(
        public val first: A,
        public val second: B,
        public val third: C,
        public val fourth: D
) : Serializable {

    /**
     * Returns string representation of the [Triple] including its [first], [second]  ，[third] and[fourth]values.
     */
    public override fun toString(): String = "($first, $second, $third, $fourth)"
}

public data class FifthTimes<out A, out B, out C, out D, out E>(
        public val first: A,
        public val second: B,
        public val third: C,
        public val fourth: D,
        public val fifth: E
) : Serializable {

    /**
     * Returns string representation of the [Triple] including its [first], [second]  ，[third] and[fourth] and[fifth]values.
     */
    public override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}

public data class SixthTimes<out A, out B, out C, out D, out E, out F>(
        public val first: A,
        public val second: B,
        public val third: C,
        public val fourth: D,
        public val fifth: E,
        public val sixth: F
) : Serializable {

    /**
     * Returns string representation of the [Triple] including its [first], [second]  ，[third] and[fourth] and[fifth]values.
     */
    public override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth)"
}