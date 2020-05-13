package com.oracle.truffle.heap.interop;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * Utility methods for working with interop types.
 *
 * tryAs... methods convert arbitrary interop objects to their local counterparts, returning null if the conversion
 * fails.
 */
public final class Types {
    private Types() {}

    public static boolean isPrimitiveValue(Object o) {
        return o instanceof Boolean || o instanceof Byte || o instanceof Short ||
                o instanceof Integer || o instanceof Long || o instanceof Float ||
                o instanceof Double || o instanceof Character || o instanceof String;
    }

    public static boolean isNull(Object obj) {
        return isNull(obj, InteropLibrary.getFactory().getUncached());
    }

    public static boolean isNull(Object obj, InteropLibrary interop) {
        if (obj == null) {
            return true;
        } else if (obj instanceof TruffleObject) {
            return interop.isNull(obj);
        }
        return false;
    }

    public static int compareValues(Object lhs, Object rhs) {
        Integer result = tryCompareValues(lhs, rhs);
        if (result != null) return result; else {
            throw new IllegalArgumentException("Cannot compare "+lhs+" to "+rhs+".");
        }
    }

    public static Integer tryCompareValues(Object lhs, Object rhs) {
        Integer result;
        result = tryCompareIntegralValues(lhs, rhs);
        if (result == null) result = tryCompareFloatingPointValues(lhs, rhs);
        if (result == null) result = tryCompareCharacterValues(lhs, rhs);
        if (result == null) result = tryCompareStringValues(lhs, rhs);
        return result;
    }

    public static Integer tryCompareStringValues(Object lhs, Object rhs) {
        String stringLHS = tryAsString(lhs);
        String stringRHS = tryAsString(rhs);
        if (stringLHS == null || stringRHS == null) return null; else {
            return stringLHS.compareTo(stringRHS);
        }
    }

    public static Integer tryCompareIntegralValues(Object lhs, Object rhs) {
        Long longLHS = tryAsIntegralNumber(lhs);
        Long longRHS = tryAsIntegralNumber(rhs);
        if (longLHS == null || longRHS == null) return null; else {
            return Long.compare(longLHS, longRHS);
        }
    }

    public static Integer tryCompareFloatingPointValues(Object lhs, Object rhs) {
        Double doubleLHS = tryAsFloatingPointNumber(lhs);
        Double doubleRHS = tryAsFloatingPointNumber(rhs);
        if (doubleLHS == null || doubleRHS == null) return null; else {
            return Double.compare(doubleLHS, doubleRHS);
        }
    }

    public static Integer tryCompareCharacterValues(Object lhs, Object rhs) {
        if (!(lhs instanceof Character && rhs instanceof Character)) return null; else {
            return Character.compare((Character) lhs, (Character) rhs);
        }
    }

    public static <T extends TruffleObject> T tryAsInstance(Object value, Class<T> clazz) {
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        } else {
            return null;
        }
    }

    public static boolean asBoolean(Object value) {
        return asBoolean(value, InteropLibrary.getFactory().getUncached());
    }

    public static boolean asBoolean(Object value, InteropLibrary interop) {
        Boolean bool = tryAsBoolean(value, interop);
        if (bool != null) return bool; else {
            throw new ClassCastException("Cannot cast "+value+" to boolean.");
        }
    }

    public static Boolean tryAsBoolean(Object value) {
        return tryAsBoolean(value, InteropLibrary.getFactory().getUncached());
    }

    public static Boolean tryAsBoolean(Object value, InteropLibrary interop) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof TruffleObject) {
            if (interop.isBoolean(value)) {
                try {
                    return interop.asBoolean(value);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Argument is boolean but does not implement `asBoolean`.", e);
                }
            }
        }
        return null;
    }

    public static long asIntegralNumber(Object number) {
        Long value = tryAsIntegralNumber(number);
        if (value != null) return value; else {
            throw new ClassCastException("Cannot cast "+value+" to integral number.");
        }
    }

    public static Long tryAsIntegralNumber(Object number) {
        return tryAsIntegralNumber(number, InteropLibrary.getFactory().getUncached());
    }

    public static Long tryAsIntegralNumber(Object number, InteropLibrary interop) {
        if (number instanceof Byte) {
            return (long) (Byte) number;
        } else if (number instanceof Short) {
            return (long) (Short) number;
        } else if (number instanceof Integer) {
            return (long) (Integer) number;
        } else if (number instanceof Long) {
            return (Long) number;
        } else if (number instanceof TruffleObject) {
            if (interop.isNumber(number) && interop.fitsInLong(number)) {
                try {
                    return interop.asLong(number);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Fits into long, but does not implement `asLong`.", e);
                }
            }
        }
        return null;
    }

    public static double asFloatingPointNumber(Object number) {
        Double value = tryAsFloatingPointNumber(number);
        if (value != null) return value; else {
            throw new ClassCastException("Cannot cast "+number+" to number.");
        }
    }

    public static Double tryAsFloatingPointNumber(Object number) {
        return tryAsFloatingPointNumber(number, InteropLibrary.getFactory().getUncached());
    }

    public static Double tryAsFloatingPointNumber(Object number, InteropLibrary interop) {
        if (number instanceof Float) {
            return (double) (Float) number;
        } else if (number instanceof Double) {
            return (Double) number;
        } else if (number instanceof TruffleObject) {
            if (interop.isNumber(number) && interop.fitsInDouble(number)) {
                try {
                    return interop.asDouble(number);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Fits into double, but does not implement `asDouble`.", e);
                }
            }
        }
        // Fallback to integral numbers if not floating point.
        Long integral = tryAsIntegralNumber(number);
        return integral != null ? (double) integral : null;
    }


    public static String asString(Object value) {
        return asString(value, InteropLibrary.getFactory().getUncached());
    }

    public static String asString(Object value, InteropLibrary interop) {
        String string = tryAsString(value, interop);
        if (string != null) return string; else {
            throw new ClassCastException("Cannot cast "+value+" to String.");
        }
    }

    public static String tryAsString(Object value) {
        return tryAsString(value, InteropLibrary.getFactory().getUncached());
    }

    public static String tryAsString(Object value, InteropLibrary interop) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof TruffleObject) {
            if (interop.isString(value)) {
                try {
                    return interop.asString(value);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Argument is string but does not implement `asString`.", e);
                }
            }
        }
        return null;
    }

    public static TruffleObject tryAsExecutable(Object value) {
        return tryAsExecutable(value, InteropLibrary.getFactory().getUncached());
    }

    public static TruffleObject tryAsExecutable(Object value, InteropLibrary interop) {
        if (value instanceof TruffleObject && interop.isExecutable(value)) {
            return (TruffleObject) value;
        } else {
            return null;
        }
    }

    public static TruffleObject tryAsArray(Object value) {
        return tryAsArray(value, InteropLibrary.getFactory().getUncached());
    }

    public static TruffleObject tryAsArray(Object value, InteropLibrary interop) {
        if (value instanceof TruffleObject && interop.hasArrayElements(value)) {
            return (TruffleObject) value;
        } else {
            return null;
        }
    }

    /**
     * @return Member descriptor of the given object, or null if there are no members.
     */
    public static TruffleObject tryReadMemberDescriptor(Object value, InteropLibrary interop) {
        if (value instanceof TruffleObject && interop.hasMembers(value)) {
            try {
                Object descriptor = interop.getMembers(value, false);
                TruffleObject members = Types.tryAsArray(descriptor, interop);
                if (members == null) {
                    throw new IllegalStateException("Members descriptor must be a truffle array. Found: "+descriptor+".");
                }
                return members;
            } catch (UnsupportedMessageException e) {
                throw new IllegalStateException("Argument has members but does not implement `getMembers`.", e);
            }
        } else {
            return null;
        }
    }

    public static TruffleObject tryReadMemberDescriptor(Object value) {
        return tryReadMemberDescriptor(value, InteropLibrary.getFactory().getUncached());
    }



}
