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

public final class Globbing {
  /**
   * Any character that appears in a pattern, other than the special pattern characters described
   * below, matches itself. The NUL character may not occur in a pattern. A backslash escapes the
   * following character; the escaping backslash is discarded when matching. The special pattern
   * characters must be quoted if they are to be matched literally.
   * 
   * The special pattern characters have the following meanings:
   *
   * 
   * 
   * Matches any string, including the null string. When the globstar shell option is enabled, and
   * ‘*’ is used in a filename expansion context, two adjacent ‘*’s used as a single pattern will
   * match all files and zero or more directories and subdirectories. If followed by a ‘/’, two
   * adjacent ‘*’s will match only directories and subdirectories. ?
   * 
   * Matches any single character. […]
   * 
   * Matches any one of the enclosed characters. A pair of characters separated by a hyphen denotes
   * a range expression; any character that falls between those two characters, inclusive, using the
   * current locale’s collating sequence and character set, is matched. If the first character
   * following the ‘[’ is a ‘!’ or a ‘^’ then any character not enclosed is matched. A ‘-’ may be
   * matched by including it as the first or last character in the set. A ‘]’ may be matched by
   * including it as the first character in the set. The sorting order of characters in range
   * expressions, and the characters included in the range, are determined by the current locale and
   * the values of the LC_COLLATE and LC_ALL shell variables, if set.
   * 
   * For example, in the default C locale, ‘[a-dx-z]’ is equivalent to ‘[abcdxyz]’. Many locales
   * sort characters in dictionary order, and in these locales ‘[a-dx-z]’ is typically not
   * equivalent to ‘[abcdxyz]’; it might be equivalent to ‘[aBbCcDdxYyZz]’, for example. To obtain
   * the traditional interpretation of ranges in bracket expressions, you can force the use of the C
   * locale by setting the LC_COLLATE or LC_ALL environment variable to the value ‘C’, or enable the
   * globasciiranges shell option.
   * 
   * Within ‘[’ and ‘]’, character classes can be specified using the syntax [:class:], where class
   * is one of the following classes defined in the POSIX standard:
   * 
   * alnum alpha ascii blank cntrl digit graph lower print punct space upper word xdigit
   * 
   * A character class matches any character belonging to that class. The word character class
   * matches letters, digits, and the character ‘_’.
   * 
   * Within ‘[’ and ‘]’, an equivalence class can be specified using the syntax [=c=], which matches
   * all characters with the same collation weight (as defined by the current locale) as the
   * character c.
   * 
   * Within ‘[’ and ‘]’, the syntax [.symbol.] matches the collating symbol symbol.
   * 
   * @param pattern
   * @return
   */
  public static CharSequence glob(CharacterIterator iterator, boolean greedy) {
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
