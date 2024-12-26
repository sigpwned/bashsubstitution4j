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

/**
 * A {@link CharSequence} implementation that is a view into another {@link CharSequence}.
 */
public class ViewCharSequence implements CharSequence {
  private final CharSequence parent;
  private final int start;
  private final int end;

  public ViewCharSequence(CharSequence parent) {
    this(parent, 0, parent.length());
  }

  public ViewCharSequence(CharSequence parent, int start, int end) {
    this.parent = requireNonNull(parent);
    if (start < 0 || start > end || end > parent.length())
      throw new IndexOutOfBoundsException();
    this.start = start;
    this.end = end;
  }

  @Override
  public char charAt(int index) {
    if (index < 0 || index >= length())
      throw new IndexOutOfBoundsException();
    return parent.charAt(start + index);
  }

  @Override
  public int length() {
    return end - start;
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    if (start < 0 || start > end || end > length())
      throw new IndexOutOfBoundsException();
    return new ViewCharSequence(parent, this.start + start, this.start + end);
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder(length());
    for (int i = 0; i < length(); i++)
      result.append(charAt(i));
    return result.toString();
  }
}
