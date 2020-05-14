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
        return obj == null || ((obj instanceof TruffleObject) && interop.isNull(obj));
    }

    public static int compareValues(Object lhs, Object rhs) {
        Integer result = tryCompareValues(lhs, rhs);
        return result != null ? result : Errors.illegalArgument("Cannot compare "+lhs+" to "+rhs+".");
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
        return (stringLHS == null || stringRHS == null) ? null : stringLHS.compareTo(stringRHS);
    }

    public static Integer tryCompareIntegralValues(Object lhs, Object rhs) {
        Long longLHS = tryAsIntegralNumber(lhs);
        Long longRHS = tryAsIntegralNumber(rhs);
        return (longLHS == null || longRHS == null) ? null : Long.compare(longLHS, longRHS);
    }

    public static Integer tryCompareFloatingPointValues(Object lhs, Object rhs) {
        Double doubleLHS = tryAsFloatingPointNumber(lhs);
        Double doubleRHS = tryAsFloatingPointNumber(rhs);
        return (doubleLHS == null || doubleRHS == null) ? null : Double.compare(doubleLHS, doubleRHS);
    }

    public static Integer tryCompareCharacterValues(Object lhs, Object rhs) {
        return (lhs instanceof Character && rhs instanceof Character) ? Character.compare((Character) lhs, (Character) rhs) : null;
    }

    public static <T extends TruffleObject> T tryAsInstance(Object value, Class<T> clazz) {
        return clazz.isInstance(value) ? clazz.cast(value) : null;
    }

    public static boolean asBoolean(Object value) {
        return asBoolean(value, InteropLibrary.getFactory().getUncached());
    }

    public static boolean asBoolean(Object value, InteropLibrary interop) {
        Boolean bool = tryAsBoolean(value, interop);
        return bool != null ? bool : Errors.classCast(value, Boolean.class);
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
                    Errors.rethrow(RuntimeException.class, e);
                }
            }
        }
        return null;
    }

    public static long asIntegralNumber(Object number) {
        Long value = tryAsIntegralNumber(number);
        return value != null ? value : Errors.classCast(value, Long.class);
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
                    Errors.rethrow(RuntimeException.class, e);
                }
            }
        }
        return null;
    }

    public static double asFloatingPointNumber(Object number) {
        Double value = tryAsFloatingPointNumber(number);
        return value != null ? value : Errors.classCast(number, Double.class);
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
                    Errors.rethrow(RuntimeException.class, e);
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
        return string != null ? string : Errors.classCast(value, String.class);
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
                    Errors.rethrow(RuntimeException.class, e);
                }
            }
        }
        return null;
    }

    public static TruffleObject tryAsExecutable(Object value) {
        return tryAsExecutable(value, InteropLibrary.getFactory().getUncached());
    }

    public static TruffleObject tryAsExecutable(Object value, InteropLibrary interop) {
        return (value instanceof TruffleObject && interop.isExecutable(value)) ? (TruffleObject) value : null;
    }

    public static TruffleObject tryAsArray(Object value) {
        return tryAsArray(value, InteropLibrary.getFactory().getUncached());
    }

    public static TruffleObject tryAsArray(Object value, InteropLibrary interop) {
        return (value instanceof TruffleObject && interop.hasArrayElements(value)) ? (TruffleObject) value : null;
    }

    /**
     * @return Member descriptor of the given object, or null if there are no members.
     */
    public static TruffleObject tryReadMemberDescriptor(Object value, InteropLibrary interop) {
        if (value instanceof TruffleObject && interop.hasMembers(value)) {
            try {
                Object descriptor = interop.getMembers(value, false);
                TruffleObject members = Types.tryAsArray(descriptor, interop);
                return members != null ? members : Errors.illegalState("Members descriptor must be a truffle array. Found: "+descriptor+".");
            } catch (UnsupportedMessageException e) {
                Errors.rethrow(RuntimeException.class, e);
            }
        }
        return null;
    }

    public static TruffleObject tryReadMemberDescriptor(Object value) {
        return tryReadMemberDescriptor(value, InteropLibrary.getFactory().getUncached());
    }



}
