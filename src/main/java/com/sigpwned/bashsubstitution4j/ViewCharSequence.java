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
