/*-
 * =================================LICENSE_START==================================
 * bashsubstitution4j
 * ====================================SECTION=====================================
 * Copyright (C) 2024 Andy Boothe
 * ====================================SECTION=====================================
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
 * ==================================LICENSE_END===================================
 */
package com.sigpwned.bashsubstitution4j.util;

public final class Integers {
  private Integers() {}

  /**
   * Parses an integer from a {@link CharSequence} using radix 10.
   * 
   * <p>
   * Sadly, we cannot simply use {@link Integer#parseInt(CharSequence, int beginIndex, int endIndex,
   * int radix)} because that method was released in Java 9, and we are targeting Java 8.
   * 
   * @param s the text to parse
   * @return the parsed integer value
   * @throws NullPointerException if s is null
   * @throws NumberFormatException if the input is not a valid integer
   */
  public static int parseInt(CharSequence s) {
    if (s == null)
      throw new NullPointerException();
    if (s.length() == 0)
      throw new NumberFormatException("Input is null or empty");

    final int length = s.length();
    int si = 0;

    // Check for a sign
    final boolean isNegative;
    if (s.charAt(0) == '-') {
      if (length == 1)
        throw new NumberFormatException("Invalid input: " + s);
      isNegative = true;
      si = si + 1;
    } else if (s.charAt(0) == '+') {
      if (length == 1)
        throw new NumberFormatException("Invalid input: " + s);
      isNegative = false;
      si = si + 1;
    } else {
      isNegative = false;
    }

    int result = 0;
    while (si < length) {
      char c = s.charAt(si);
      if (c < '0' || c > '9') {
        throw new NumberFormatException("Invalid character '" + c + "' in input: " + s);
      }

      int digit = c - '0';

      // Check for overflow
      if (result > (Integer.MAX_VALUE - digit) / 10) {
        throw new NumberFormatException("Integer overflow for input: " + s);
      }

      result = result * 10 + digit;

      si = si + 1;
    }

    return isNegative ? -result : +result;
  }
}
