package com.oracle.truffle.heap;

/**
 * This represents a set of data members that should be excluded from the
 * reachable objects query. This is useful to exclude observers from the
 * transitive closure of objects reachable from a given object, allowing
 * some kind of real determination of the "size" of that object.
 *
 * @author    A. Sundararajan
 */

public interface ReachableExcludes {
    /**
     * @return true if the given field is on the hitlist of excluded
     * 		fields.
     */
    public boolean isExcluded(String fieldName);
}
