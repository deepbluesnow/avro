/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.avro.logicalTypes;

import com.google.common.collect.ImmutableMap;
import java.nio.ByteBuffer;
import java.util.Set;
import org.apache.avro.AbstractLogicalType;
import org.apache.avro.Schema;
import org.codehaus.jackson.JsonNode;

  /** Decimal represents arbitrary-precision fixed-scale decimal numbers  */
public final class BigInteger extends AbstractLogicalType {

    private static final Set<String> RESERVED = AbstractLogicalType.reservedSet("precision");

    private final int precision;

    public BigInteger(int precision,  Schema.Type type) {
      super(type, RESERVED, "bigint", ImmutableMap.of("precision", (Object) precision));
      if (precision <= 0) {
        throw new IllegalArgumentException("Invalid " + this.logicalTypeName + " precision: " +
            precision + " (must be positive)");
      }
      this.precision = precision;
    }

    public BigInteger(JsonNode node, Schema.Type type) {
        this(node.get("precision").asInt(), type);
    }

    @Override
    public void validate(Schema schema) {
      // validate the type
      if (schema.getType() != Schema.Type.FIXED &&
          schema.getType() != Schema.Type.BYTES &&
          schema.getType() != Schema.Type.STRING) {
        throw new IllegalArgumentException(this.logicalTypeName + " must be backed by fixed or bytes");
      }
      if (precision > maxPrecision(schema)) {
        throw new IllegalArgumentException(
            "fixed(" + schema.getFixedSize() + ") cannot store " +
                precision + " digits (max " + maxPrecision(schema) + ")");
      }
   }

    @Override
    public Set<String> reserved() {
      return RESERVED;
    }

    private long maxPrecision(Schema schema) {
      if (schema.getType() == Schema.Type.BYTES
              || schema.getType() == Schema.Type.STRING) {
        // not bounded
        return Integer.MAX_VALUE;
      } else if (schema.getType() == Schema.Type.FIXED) {
        int size = schema.getFixedSize();
        return Math.round(          // convert double to long
            Math.floor(Math.log10(  // number of base-10 digits
                Math.pow(2, 8 * size - 1) - 1)  // max value stored
            ));
      } else {
        // not valid for any other type
        return 0;
      }
    }

    @Override
    public Class<?> getLogicalJavaType() {
        return java.math.BigInteger.class;
    }

    @Override
    public Object deserialize(Object object) {
      switch (type) {
        case STRING:
          java.math.BigInteger result = new java.math.BigInteger(object.toString());
          return result;
        case BYTES:
          //ByteBuffer buf = ByteBuffer.wrap((byte []) object);
          ByteBuffer buf = (ByteBuffer) object;
          buf.rewind();
          byte[] unscaled = new byte[buf.remaining()];
          buf.get(unscaled);
          return new java.math.BigInteger(unscaled);
        default:
          throw new UnsupportedOperationException("Unsupported type " + type + " for " + this);
      }

    }

    @Override
    public Object serialize(Object object) {
      java.math.BigInteger decimal = (java.math.BigInteger) object;
      switch (type) {
        case STRING:
            return decimal.toString();
        case BYTES:
          byte[] unscaledValue = decimal.toByteArray();
          ByteBuffer buf = ByteBuffer.allocate(unscaledValue.length);
          buf.put(unscaledValue);
          buf.rewind();
          return buf;
        default:
          throw new UnsupportedOperationException("Unsupported type " + type + " for " + this);
      }
    }
  }
