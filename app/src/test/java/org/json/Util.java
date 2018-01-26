package org.json;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains the helper methods not included in the standard android API, but which is used
 * by the implementations here.
 */

class Util {

    /**
     * Tests if the value should be tried as a decimal. It makes no test if there are actual digits.
     *
     * @param val value to test
     * @return true if the string is "-0" or if it contains '.', 'e', or 'E', false otherwise.
     */
    static boolean isDecimalNotation(final String val) {
        return val.indexOf('.') > -1 || val.indexOf('e') > -1
                || val.indexOf('E') > -1 || "-0".equals(val);
    }


    /**
     * Make a JSON text of an Object value. If the object has an
     * value.toJSONString() method, then that method will be used to produce the
     * JSON text. The method is required to produce a strictly conforming text.
     * If the object does not contain a toJSONString method (which is the most
     * common case), then a text will be produced by other means. If the value
     * is an array or Collection, then a JSONArray will be made from it and its
     * toJSONString method will be called. If the value is a MAP, then a
     * JSONObject will be made from it and its toJSONString method will be
     * called. Otherwise, the value's toString method will be called, and the
     * result will be quoted.
     *
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @param value
     *            The value to be serialized.
     * @return a printable, displayable, transmittable representation of the
     *         object, beginning with <code>{</code>&nbsp;<small>(left
     *         brace)</small> and ending with <code>}</code>&nbsp;<small>(right
     *         brace)</small>.
     * @throws JSONException
     *             If the value is or contains an invalid number.
     */
    static String valueToString(Object value) throws JSONException {
        if (value == null || value.equals(null)) {
            return "null";
        }
        if (value instanceof String) {
            return JSONObject.quote((String) value);
        }
        if (value instanceof Number) {
            // not all Numbers may match actual JSON Numbers. i.e. Fractions or Complex
            final String numberAsString = JSONObject.numberToString((Number) value);
            try {
                // Use the BigDecimal constructor for it's parser to validate the format.
                @SuppressWarnings("unused")
                BigDecimal unused = new BigDecimal(numberAsString);
                // Close enough to a JSON number that we will return it unquoted
                return numberAsString;
            } catch (NumberFormatException ex){
                // The Number value is not a valid JSON number.
                // Instead we will quote it as a string
                return JSONObject.quote(numberAsString);
            }
        }
        if (value instanceof Boolean || value instanceof JSONObject
                || value instanceof JSONArray) {
            return value.toString();
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            return new JSONObject(map).toString();
        }
        if (value instanceof Collection) {
            Collection<?> coll = (Collection<?>) value;
            return new JSONArray(coll).toString();
        }
        if (value.getClass().isArray()) {
            return new JSONArray(value).toString();
        }
        if(value instanceof Enum<?>){
            return JSONObject.quote(((Enum<?>)value).name());
        }
        return JSONObject.quote(value.toString());
    }

    static final void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    /**
     * Throw an exception if the object is a NaN or infinite number.
     *
     * @param o
     *            The object to test.
     * @throws JSONException
     *             If o is a non-finite number.
     */
    static void testValidity(Object o) throws JSONException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
                    throw new JSONException(
                            "JSON does not allow non-finite numbers.");
                }
            } else if (o instanceof Float) {
                if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
                    throw new JSONException(
                            "JSON does not allow non-finite numbers.");
                }
            }
        }
    }

    static final Writer writeValue(Writer writer, Object value,
            int indentFactor, int indent) throws JSONException, IOException {
        if (value == null || value.equals(null)) {
            writer.write("null");
        } else if (value instanceof String) {
            writer.write(value.toString());
        } else if (value instanceof Number) {
            // not all Numbers may match actual JSON Numbers. i.e. fractions or Imaginary
            final String numberAsString = JSONObject.numberToString((Number) value);
            try {
                // Use the BigDecimal constructor for its parser to validate the format.
                @SuppressWarnings("unused")
                BigDecimal testNum = new BigDecimal(numberAsString);
                // Close enough to a JSON number that we will use it unquoted
                writer.write(numberAsString);
            } catch (NumberFormatException ex){
                // The Number value is not a valid JSON number.
                // Instead we will quote it as a string
                quote(numberAsString, writer);
            }
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else if (value instanceof Enum<?>) {
            writer.write(JSONObject.quote(((Enum<?>)value).name()));
        } else if (value instanceof JSONObject) {
            writeJSONObject((JSONObject) value, writer, indentFactor, indent);
        } else if (value instanceof JSONArray) {
            writeJSONArray((JSONArray) value, writer, indentFactor, indent);
        } else {
            quote(value.toString(), writer);
        }
        return writer;
    }


    public static Writer quote(String string, Writer w) throws IOException {
        if (string == null || string.length() == 0) {
            w.write("\"\"");
            return w;
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    w.write('\\');
                    w.write(c);
                    break;
                case '/':
                    if (b == '<') {
                        w.write('\\');
                    }
                    w.write(c);
                    break;
                case '\b':
                    w.write("\\b");
                    break;
                case '\t':
                    w.write("\\t");
                    break;
                case '\n':
                    w.write("\\n");
                    break;
                case '\f':
                    w.write("\\f");
                    break;
                case '\r':
                    w.write("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                            || (c >= '\u2000' && c < '\u2100')) {
                        w.write("\\u");
                        hhhh = Integer.toHexString(c);
                        w.write("0000", 0, 4 - hhhh.length());
                        w.write(hhhh);
                    } else {
                        w.write(c);
                    }
            }
        }
        w.write('"');
        return w;
    }


    /**
     * Write the contents of the JSONObject as JSON text to a writer.
     *
     * <p>If <code>indentFactor > 0</code> and the {@link JSONObject}
     * has only one key, then the object will be output on a single line:
     * <pre>{@code {"key": 1}}</pre>
     *
     * <p>If an object has 2 or more keys, then it will be output across
     * multiple lines: <code><pre>{
     *  "key1": 1,
     *  "key2": "value 2",
     *  "key3": 3
     * }</pre></code>
     * <p><b>
     * Warning: This method assumes that the data structure is acyclical.
     * </b>
     *
     * @param writer
     *            Writes the serialized JSON
     * @param indentFactor
     *            The number of spaces to add to each level of indentation.
     * @param indent
     *            The indentation of the top level.
     * @return The writer.
     * @throws JSONException
     */
    static Writer writeJSONObject(JSONObject obj, Writer writer, int indentFactor, int indent)
            throws JSONException {
        try {
            boolean commanate = false;
            final int length = obj.length();
            writer.write('{');

            if (length == 1) {
                final Map.Entry<String,?> entry = toMap(obj).entrySet().iterator().next();
                final String key = entry.getKey();
                writer.write(JSONObject.quote(key));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                try{
                    Util.writeValue(writer, entry.getValue(), indentFactor, indent);
                } catch (Exception e) {
                    JSONException ex = new JSONException("Unable to write JSONObject value for key: " + key);
                    ex.initCause(e);
                    throw ex;
                }
            } else if (length != 0) {
                final int newindent = indent + indentFactor;
                for (final Map.Entry<String,?> entry : toMap(obj).entrySet()) {
                    if (commanate) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    Util.indent(writer, newindent);
                    final String key = entry.getKey();
                    writer.write(JSONObject.quote(key));
                    writer.write(':');
                    if (indentFactor > 0) {
                        writer.write(' ');
                    }
                    try {
                        Util.writeValue(writer, entry.getValue(), indentFactor, newindent);
                    } catch (Exception e) {
                        JSONException ex = new JSONException("Unable to write JSONObject value for key: " + key);
                        ex.initCause(e);
                        throw ex;
                    }
                    commanate = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                Util.indent(writer, indent);
            }
            writer.write('}');
            return writer;
        } catch (IOException exception) {
            JSONException ex = new JSONException(exception.getMessage());
            ex.initCause(exception);
            throw ex;
        }
    }

    private static Map<String,?> toMap(JSONObject obj)
            throws JSONException {
        Map<String, Object> ret = new HashMap<>();
        for (Iterator<String> itr = obj.keys(); itr.hasNext();) {
            String key = itr.next();
            ret.put(key, obj.get(key));
        }
        return ret;
    }

    /**
     * Write the contents of the JSONArray as JSON text to a writer.
     *
     * <p>If <code>indentFactor > 0</code> and the {@link JSONArray} has only
     * one element, then the array will be output on a single line:
     * <pre>{@code [1]}</pre>
     *
     * <p>If an array has 2 or more elements, then it will be output across
     * multiple lines: <pre>{@code
     * [
     * 1,
     * "value 2",
     * 3
     * ]
     * }</pre>
     * <p><b>
     * Warning: This method assumes that the data structure is acyclical.
     * </b>
     *
     * @param writer
     *            Writes the serialized JSON
     * @param indentFactor
     *            The number of spaces to add to each level of indentation.
     * @param indent
     *            The indentation of the top level.
     * @return The writer.
     * @throws JSONException
     */
    static Writer writeJSONArray(JSONArray obj, Writer writer, int indentFactor, int indent)
            throws JSONException {
        try {
            boolean commanate = false;
            int length = obj.length();
            writer.write('[');

            if (length == 1) {
                try {
                    Util.writeValue(writer, obj.get(0),
                            indentFactor, indent);
                } catch (Exception e) {
                    JSONException ex = new JSONException("Unable to write JSONArray value at index: 0");
                    ex.initCause(e);
                    throw ex;
                }
            } else if (length != 0) {
                final int newindent = indent + indentFactor;

                for (int i = 0; i < length; i += 1) {
                    if (commanate) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    Util.indent(writer, newindent);
                    try {
                        Util.writeValue(writer, obj.get(i),
                                indentFactor, newindent);
                    } catch (Exception e) {
                        JSONException ex = new JSONException("Unable to write JSONArray value at index: " + i);
                        ex.initCause(e);
                        throw ex;
                    }
                    commanate = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                Util.indent(writer, indent);
            }
            writer.write(']');
            return writer;
        } catch (IOException e) {
            JSONException ex = new JSONException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }


    static Object stringToValue(String string) {
        if (string.equals("")) {
            return string;
        }
        if (string.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (string.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (string.equalsIgnoreCase("null")) {
            return JSONObject.NULL;
        }

        /*
         * If it might be a number, try converting it. If a number cannot be
         * produced, then the value will just be a string.
         */

        char initial = string.charAt(0);
        if ((initial >= '0' && initial <= '9') || initial == '-') {
            try {
                // if we want full Big Number support this block can be replaced with:
                // return stringToNumber(string);
                if (isDecimalNotation(string)) {
                    Double d = Double.valueOf(string);
                    if (!d.isInfinite() && !d.isNaN()) {
                        return d;
                    }
                } else {
                    Long myLong = Long.valueOf(string);
                    if (string.equals(myLong.toString())) {
                        if (myLong.longValue() == myLong.intValue()) {
                            return Integer.valueOf(myLong.intValue());
                        }
                        return myLong;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return string;
    }

}
