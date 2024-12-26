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

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Patterns {
  private Patterns() {}

  /**
   * Replaces the first occurrence of a pattern in a string with the result of a function.
   * 
   * @param p the pattern to search for
   * @param s the string to search in
   * @param r the function to use to replace each match
   * @return the string with all occurrences of the pattern replaced
   * 
   * @throws NullPointerException if p is null
   * @throws NullPointerException if s is null
   * @throws NullPointerException if replacer is null
   */
  public static CharSequence replaceFirst(Pattern p, CharSequence s,
      Function<MatchResult, CharSequence> r) {
    if (p == null)
      throw new NullPointerException();
    if (s == null)
      throw new NullPointerException();
    if (r == null)
      throw new NullPointerException();

    final Matcher m = p.matcher(s);
    if (m.find()) {
      final StringBuilder result = new StringBuilder();
      result.append(s, 0, m.start());
      result.append(r.apply(m));
      result.append(s, m.end(), s.length());
      return result;
    } else {
      return s;
    }
  }

  /**
   * Replaces all occurrences of a pattern in a string with the result of a function.
   * 
   * @param p the pattern to search for
   * @param s the string to search in
   * @param r the function to use to replace each match
   * @return the string with all occurrences of the pattern replaced
   * 
   * @throws NullPointerException if p is null
   * @throws NullPointerException if s is null
   * @throws NullPointerException if replacer is null
   */
  public static CharSequence replaceAll(Pattern p, CharSequence s,
      Function<MatchResult, CharSequence> r) {
    if (p == null)
      throw new NullPointerException();
    if (s == null)
      throw new NullPointerException();
    if (r == null)
      throw new NullPointerException();

    final StringBuilder result = new StringBuilder();

    int start = 0;
    final Matcher m = p.matcher(s);
    while (m.find()) {
      result.append(s, start, m.start());
      result.append(r.apply(m));
      start = m.end();
    }
    result.append(s, start, s.length());

    return result;
  }
}
