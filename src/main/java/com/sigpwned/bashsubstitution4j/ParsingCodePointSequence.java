package com.sigpwned.bashsubstitution4j;

import java.util.NoSuchElementException;

/**
 * A logical unidirectional code point iterator that also provides operations helpful for parsing
 * text. Also implements the {@link CharSequence} interface to support passing the text around "in
 * place" without additional allocations or copies.
 */
public class ParsingCodePointSequence implements CharSequence {
  private final CharSequence text;
  private int position;

  public ParsingCodePointSequence(CharSequence text) {
    this.text = text;
    this.position = 0;
  }

  /**
   * Returns true if there are more characters to read.
   * 
   * @return {@code true} if there are more characters to read, otherwise {@code false}
   */
  public boolean hasNext() {
    return position < text.length();
  }

  /**
   * Returns the next code point in the text without advancing the position
   * 
   * @return the next code point in the text
   * @throws NoSuchElementException if there are no more characters to read
   */
  public int peek() {
    if (position == text.length())
      throw new NoSuchElementException();
    return Character.codePointAt(text, position);
  }

  /**
   * Returns the next code point in the text and advances the position
   * 
   * @return the next code point in the text
   * @throws NoSuchElementException if there are no more characters to read
   */
  public int next() {
    int result = peek();
    position += position + Character.charCount(result);
    return result;
  }

  /**
   * Returns the current position in the given text
   * 
   * @return the current position in the text
   */
  public int position() {
    return position;
  }

  /**
   * Attempts to match the given character sequence at the current position.
   * 
   * <p>
   * If the given character sequence matches the text at the current position, then the position is
   * advanced by the length of the given character sequence and the method returns true.
   * 
   * <p>
   * Otherwise, the position is not advanced and the method returns false.
   * 
   * @param s the character sequence to match
   * @return true if the character sequence matches the text at the current position, otherwise
   *         false
   */
  public boolean attempt(CharSequence s) {
    if (position + s.length() > text.length())
      return false;

    int si = 0;
    while (si < s.length()) {
      int tcp = Character.codePointAt(text, position + si);
      int scp = Character.codePointAt(s, si);
      if (tcp != scp)
        return false;
      si = si + Character.charCount(tcp);
    }

    position = position + si;

    return true;
  }

  /**
   * Expects the given character sequence at the current position. If the character sequence does
   * not match the text at the current position, then an exception is thrown.
   * 
   * @param s the character sequence to match
   * @throws IllegalArgumentException if the character sequence does not match the text at the
   *         current position
   */
  public void expect(CharSequence s) {
    if (!attempt(s))
      throw new IllegalArgumentException("Expected " + s);
  }

  @Override
  public char charAt(int index) {
    return text.charAt(position + index);
  }

  @Override
  public int length() {
    return text.length() - position;
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return text.subSequence(position + start, position + end);
  }

  /**
   * Returns a string representation of the text starting from the current position.
   */
  @Override
  public String toString() {
    return text.subSequence(position, text.length()).toString();
  }
}
