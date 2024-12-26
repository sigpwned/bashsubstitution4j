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

import java.text.CharacterIterator;
import java.util.regex.Pattern;

public final class StringGlobbing {
  /**
   * Converts a glob-style pattern, represented by a {@link CharacterIterator}, into a
   * Java-compatible regular expression pattern. This method supports common globbing syntax, such
   * as wildcards and character classes, and allows for greedy or non-greedy matching.
   *
   * <p>
   * The following globbing syntax is supported:
   * <ul>
   * <li><code>*</code>: Matches zero or more characters. Converted to <code>.*</code> for greedy
   * matching or <code>.*?</code> for non-greedy matching based on the {@code greedy}
   * parameter.</li>
   * <li><code>?</code>: Matches exactly one character. Converted to <code>.</code>.</li>
   * <li><code>[...]</code>: Matches any one of the enclosed characters or ranges. For example,
   * <code>[a-z]</code> matches any lowercase letter, and <code>[!abc]</code> matches any character
   * except 'a', 'b', or 'c'. Converted directly to the equivalent Java character class.</li>
   * <li>Literal characters: Characters that do not have special meaning in the globbing syntax are
   * quoted to ensure they are treated as literals in the resulting Java regular expression.</li>
   * </ul>
   *
   * @param iterator a {@link CharacterIterator} representing the glob-style pattern. This iterator
   *        is advanced as the method processes each character in the pattern.
   * @param greedy if {@code true}, <code>*</code> patterns are converted to <code>.*</code>; if
   *        {@code false}, they are converted to <code>.*?</code> for non-greedy matching.
   * @return a {@link CharSequence} containing the equivalent Java regular expression pattern.
   * @throws NullPointerException if {@code iterator} is {@code null}.
   */
  public static CharSequence toJavaPattern(CharacterIterator iterator, boolean greedy) {
    StringBuilder sb = new StringBuilder();
    while (iterator.current() != CharacterIterator.DONE) {
      if (Parsing.attempt(iterator, "*")) {
        if (greedy) {
          sb.append(".*");
        } else {
          sb.append(".*?");
        }
      } else if (Parsing.attempt(iterator, "?")) {
        sb.append(".");
      } else if (Parsing.attempt(iterator, "[")) {
        sb.append('[');
        if (Parsing.attempt(iterator, "!")) {
          sb.append('^');
        }

        while (iterator.current() != CharacterIterator.DONE && iterator.current() != ']') {
          char c1 = iterator.current();
          if (Parsing.attempt(iterator, "-")) {
            char c2 = iterator.current();
            sb.append(c1).append("-").append(c2);
          } else {
            sb.append(c1);
          }
          iterator.next();
        }
        sb.append(']');
        iterator.next();
      } else {
        sb.append(Pattern.quote(String.valueOf(iterator.current())));
        iterator.next();
      }
    }
    return sb;
  }
}
