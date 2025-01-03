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
package com.sigpwned.bashsubstitution4j;

import static java.util.Objects.requireNonNull;
import java.text.CharacterIterator;

/**
 * An implementation of CharacterIterator for CharSequence.
 */
public class CharSequenceCharacterIterator implements CharacterIterator {
  private final CharSequence text;
  private int index;

  /**
   * Constructs a {@link CharacterIterator} for the given {@link CharSequence}.
   *
   * @param sequence the CharSequence to iterate over
   */
  public CharSequenceCharacterIterator(CharSequence sequence) {
    this.text = requireNonNull(sequence);
    this.index = 0;
  }

  @Override
  public char first() {
    index = 0;
    return current();
  }

  @Override
  public char last() {
    if (text.length() == 0) {
      return DONE;
    }
    index = text.length() - 1;
    return current();
  }

  @Override
  public char current() {
    if (index < 0 || index >= text.length()) {
      return DONE;
    }
    return text.charAt(index);
  }

  @Override
  public char next() {
    if (index < text.length() - 1) {
      index = index + 1;
      return current();
    }
    index = text.length(); // Move to DONE
    return DONE;
  }

  @Override
  public char previous() {
    if (index > 0) {
      index = index - 1;
      return current();
    }
    index = -1; // Move to DONE
    return DONE;
  }

  @Override
  public char setIndex(int position) {
    if (position < 0 || position > text.length()) {
      throw new IllegalArgumentException("Invalid index: " + position);
    }
    index = position;
    return current();
  }

  @Override
  public int getBeginIndex() {
    return 0;
  }

  @Override
  public int getEndIndex() {
    return text.length();
  }

  @Override
  public int getIndex() {
    return index;
  }

  @Override
  public Object clone() {
    CharSequenceCharacterIterator clone = new CharSequenceCharacterIterator(this.text);
    clone.index = this.index;
    return clone;
  }

  public CharSequence getCharSequence() {
    return text;
  }
}
