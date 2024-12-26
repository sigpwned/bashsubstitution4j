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
import com.sigpwned.bashsubstitution4j.CharSequenceCharacterIterator;

public final class Parsing {
  private Parsing() {}

  /**
   * Attempts to match the given character sequence at the current position of the given iterator.
   * If the character sequence does not match the text at the current position of the iterator, then
   * the iterator's state is unchanged, and the method returns {@code false}. If the character
   * sequence does match the text at the current position of the iterator, then the iterator's
   * position is advanced past the matched text, and the method returns {@code true}.
   * 
   * @param iterator the iterator to use
   * @param s the character sequence to match
   * 
   * @return {@code true} if the character sequence was matched and the iterator's position was
   *         advanced, or {@code false} otherwise.
   * 
   * @throws NullPointerException if iterator is null
   * @throws NullPointerException if s is null
   * 
   */
  public static boolean attempt(CharacterIterator iterator, CharSequence s) {
    if (iterator == null)
      throw new NullPointerException();
    if (s == null)
      throw new NullPointerException();

    final int initialIndex = iterator.getIndex();

    if (initialIndex + s.length() > iterator.getEndIndex())
      return false;

    int si = 0;
    while (si < s.length()) {
      char tci = iterator.current();
      char sci = s.charAt(si);
      if (tci != sci) {
        iterator.setIndex(initialIndex);
        return false;
      }
      iterator.next();
      si = si + 1;
    }

    return true;
  }

  /**
   * Expects to find the given character sequence at the current position of the given iterator. If
   * the character sequence does not match the text at the current position of the iterator, then an
   * {@link IllegalArgumentException} is thrown. If the character sequence does match the text at
   * the current position of the iterator, then the iterator's position is advanced past the matched
   * text.
   * 
   * <p>
   * This method is particularly useful when consuming text that has already been validated.
   * 
   * @param iterator the iterator to use
   * @param s the character sequence to match
   * 
   * @throws IllegalArgumentException if the character sequence does not match the text at the
   *         current position of the iterator
   * @throws NullPointerException if iterator is null
   * @throws NullPointerException if s is null
   */
  public static void expect(CharacterIterator iterator, CharSequence s) {
    boolean succeeded = attempt(iterator, s);
    if (succeeded == false)
      throw new IllegalArgumentException("Expected " + s);
  }

  public static CharSequence until(CharacterIterator iterator, char c) {
    final StringBuilder result = new StringBuilder();
    while (iterator.current() != CharacterIterator.DONE && iterator.current() != c) {
      result.append(iterator.current());
      iterator.next();
    }
    return result;
  }

  /**
   * Returns the integer value of the text at the current position of the given iterator. The
   * iterator's position is advanced past the text that was parsed as an integer. If the text at the
   * current position of the iterator is not a valid integer, then a {@link NumberFormatException}
   * is thrown.
   * 
   * @param iterator the iterator to use
   * @return the integer value of the text at the current position of the iterator
   * @throws NumberFormatException if the text at the current position of the iterator is not a
   *         valid integer
   */
  public static int parseInt(CharacterIterator iterator) {
    final StringBuilder result = new StringBuilder();
    if (iterator.current() == '-') {
      result.append('-');
      iterator.next();
    } else if (iterator.current() == '+') {
      result.append('+');
      iterator.next();
    }
    while (iterator.current() != CharacterIterator.DONE && Character.isDigit(iterator.current())) {
      result.append(iterator.current());
      iterator.next();
    }
    return Integers.parseInt(result);
  }

  /**
   * Consumes the remaining characters from the given iterator and returns them as a new
   * {@link CharSequence}. The iterator's position is at the end of the text after this method is
   * called.
   * 
   * @param iterator the iterator to use
   * @param length the number of characters to consume
   * @return the consumed characters
   */
  public static CharSequence slurp(CharSequenceCharacterIterator iterator) {
    final CharSequence result =
        iterator.getCharSequence().subSequence(iterator.getIndex(), iterator.getEndIndex());
    iterator.setIndex(iterator.getEndIndex());
    return result;
  }
}
