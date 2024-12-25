package com.sigpwned.bashsubstitution4j;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import java.text.CharacterIterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sigpwned.bashsubstitution4j.util.Globbing;
import com.sigpwned.bashsubstitution4j.util.Parsing;
import com.sigpwned.bashsubstitution4j.util.Patterns;

public class BashSubstitutor {
  private final Map<String, String> env;
  private boolean strict;

  public BashSubstitutor(Map<String, String> env) {
    this(env, false);
  }

  public BashSubstitutor(Map<String, String> env, boolean strict) {
    this.env = requireNonNull(env);
    this.strict = strict;
  }

  /**
   * Implements bash substitution rules:
   *
   * <ul>
   * <li><code>${NAME}</code></li>
   * <li><code>${NAME:-DEFAULT}</code></li>
   * <li><code>${NAME:+ERROR_MESSAGE}</code></li>
   * <li><code>${NAME#PREFIX}</code></li>
   * <li><code>${NAME##PREFIX}</code></li>
   * <li><code>${NAME%PREFIX}</code></li>
   * <li><code>${NAME%%PREFIX}</code></li>
   * <li><code>${NAME/PATTERN/REPLACEMENT}</code></li>
   * <li><code>${NAME//PATTERN/REPLACEMENT}</code></li>
   * <li><code>${NAME/#PATTERN/REPLACEMENT}</code></li>
   * <li><code>${NAME/%PATTERN/REPLACEMENT}</code></li>
   * <li><code>${NAME@X}</code></li>
   * </ul>
   *
   * @param text the text to expand. It may contain any number of the above expressions, including
   *        none.
   * @return the expanded text
   * 
   * @throws NullPointerException if text is null
   * @throws IllegalArgumentException if an unsupported pattern is encountered
   */
  public String substitute(String text) {
    if (text == null)
      throw new NullPointerException();

    if (text.isEmpty())
      return text;

    final ViewCharSequence view = new ViewCharSequence(text);
    final CharacterIterator iterator = new CharSequenceCharacterIterator(text);

    final StringBuilder result = new StringBuilder();

    while (iterator.current() != CharacterIterator.DONE) {
      if (Parsing.attempt(iterator, "${")) {
        final int start = iterator.getIndex();

        int braces = 1;
        while (iterator.current() != CharacterIterator.DONE && braces > 0) {
          char ch = iterator.current();
          if (ch == '{') {
            braces = braces + 1;
          } else if (ch == '}') {
            braces = braces - 1;
          }
          iterator.next();
        }

        if (braces > 0) {
          // Well, this is an awkward position. We found a "${" without a matching "}".
          throw new IllegalArgumentException("Unmatched ${");
        }

        // We subtract 1 because we're past the closing brace.
        final int end = iterator.getIndex() - 1;

        result.append(evaluateExpression(view.subSequence(start, end)));
      } else {
        result.append(iterator.current());
        iterator.next();
      }
    }

    return result.toString();
  }

  protected CharSequence evaluateExpression(CharSequence expression) {
    if (expression.length() == 0)
      throw new IllegalArgumentException("Empty expression");

    final CharSequenceCharacterIterator iterator = new CharSequenceCharacterIterator(expression);
    if (!isParameterStartChar(iterator.current()))
      throw new IllegalArgumentException("Invalid parameter name");

    final StringBuilder parameter = new StringBuilder();
    parameter.append(iterator.current());
    iterator.next();
    while (iterator.current() != CharacterIterator.DONE && isParameterChar(iterator.current())) {
      parameter.append(iterator.current());
      iterator.next();
    }

    if (iterator.current() == CharacterIterator.DONE) {
      return handleExpr(parameter.toString());
    } else if (Parsing.attempt(iterator, ":-")) {
      final CharSequence defaultValue = Parsing.slurp(iterator);
      return handleColonDashExpr(parameter.toString(), defaultValue);
    } else if (Parsing.attempt(iterator, ":+")) {
      final CharSequence errorMessage = Parsing.slurp(iterator);
      return handleColonPlusExpr(parameter.toString(), errorMessage);
    } else if (Parsing.attempt(iterator, ":?")) {
      final CharSequence errorMessage = Parsing.slurp(iterator);
      return handleColonQuestionExpr(parameter.toString(), errorMessage);
    } else if (Parsing.attempt(iterator, ":")) {
      final int position = Parsing.parseInt(iterator);
      if (Parsing.attempt(iterator, ":")) {
        int length = Parsing.parseInt(iterator);
        if (iterator.current() != CharacterIterator.DONE)
          throw new IllegalArgumentException("Unsupported pattern: " + expression);
        return handleColonColonExpr(parameter.toString(), position, length);
      } else {
        if (iterator.current() != CharacterIterator.DONE)
          throw new IllegalArgumentException("Unsupported pattern: " + expression);
        return handleColonExpr(parameter.toString(), position);
      }
    } else if (Parsing.attempt(iterator, "##")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleHashHashExpr(parameter.toString(), pattern);
    } else if (Parsing.attempt(iterator, "#")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleHashExpr(parameter.toString(), pattern);
    } else if (Parsing.attempt(iterator, "%%")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handlePercentPercentExpr(parameter.toString(), pattern);
    } else if (Parsing.attempt(iterator, "%")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handlePercentExpr(parameter.toString(), pattern);
    } else if (Parsing.attempt(iterator, "//")) {
      final CharSequence pattern = Parsing.until(iterator, '/');
      Parsing.expect(iterator, "/");
      final CharSequence replacement = Parsing.slurp(iterator);
      return handleSlashSlashExpr(parameter.toString(), pattern, replacement);
    } else if (Parsing.attempt(iterator, "/#")) {
      final CharSequence pattern = Parsing.until(iterator, '/');
      Parsing.expect(iterator, "/");
      final CharSequence replacement = Parsing.slurp(iterator);
      return handleSlashHashExpr(parameter.toString(), pattern, replacement);
    } else if (Parsing.attempt(iterator, "/%")) {
      final CharSequence pattern = Parsing.until(iterator, '/');
      Parsing.expect(iterator, "/");
      final CharSequence replacement = Parsing.slurp(iterator);
      return handleSlashPercentExpr(parameter.toString(), pattern, replacement);
    } else if (Parsing.attempt(iterator, "/")) {
      final CharSequence pattern = Parsing.until(iterator, '/');
      Parsing.expect(iterator, "/");
      final CharSequence replacement = Parsing.slurp(iterator);
      return handleSlashExpr(parameter.toString(), pattern, replacement);
    } else if (Parsing.attempt(iterator, "^^")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleCaretCaretExpr(parameter.toString(), pattern);
    } else if (Parsing.attempt(iterator, "^")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleCaretExpr(parameter.toString(), pattern);
    } else if (Parsing.attempt(iterator, ",,")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleCommaCommaExpr(parameter.toString(), pattern);
    } else if (Parsing.attempt(iterator, ",")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleCommaExpr(parameter.toString(), pattern);
    } else if (Parsing.attempt(iterator, "@")) {
      final char operator = iterator.current();
      if (operator == CharacterIterator.DONE)
        throw new IllegalArgumentException("Unsupported pattern: " + expression);

      if (iterator.next() != CharacterIterator.DONE)
        throw new IllegalArgumentException("Unsupported pattern: " + expression);

      return handleAtExpr(parameter.toString(), operator);
    }

    throw new IllegalArgumentException("Unsupported pattern: " + expression);
  }

  private static boolean isParameterStartChar(int ch) {
    return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
  }

  private static boolean isParameterChar(int ch) {
    return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')
        || ch == '_';
  }

  /**
   * Returns the value of the specified environment variable, or an empty string if not found.
   * 
   * @param name the name of the environment variable
   * @return the value of the environment variable, or an empty string if not found
   */
  private CharSequence handleExpr(String name) {
    if (name == null)
      throw new NullPointerException();
    return findEnvironmentVariable(name).orElse("");
  }

  /**
   * Returns the value of the specified environment variable, or the default value if not found.
   * 
   * @param name the name of the environment variable
   * @param defaultValue the default value to return if the environment variable is not found
   * @return the value of the environment variable, or the default value if not found
   */
  private CharSequence handleColonDashExpr(String name, CharSequence defaultValue) {
    if (name == null)
      throw new NullPointerException();
    if (defaultValue == null)
      throw new NullPointerException();
    return findEnvironmentVariable(name).orElse(defaultValue);
  }

  /**
   * Returns the empty string if the given name is unset, otherwise returns the given message.
   * 
   * @param name the name of the environment variable
   * @param message the message to return if the environment variable is set
   * @return the empty string if the environment variable is unset, otherwise the given message
   * 
   */
  private CharSequence handleColonPlusExpr(String name, CharSequence message) {
    if (name == null)
      throw new NullPointerException();
    if (message == null)
      throw new NullPointerException();
    if (findEnvironmentVariable(name).isPresent())
      return message;
    return "";
  }

  /**
   * Returns the value of the given name if set and not empty, otherwise throws an exception with
   * the given message, or a reasonable default if the message is empty.
   * 
   * @param name the name of the environment variable
   * @param message the message to include in the exception if the environment variable is unset or
   *        empty
   * @return the empty string if the environment variable is unset
   * 
   * @throws UnsetVariableInSubstitutionException if the environment variable is unset or empty
   */
  private CharSequence handleColonQuestionExpr(String name, CharSequence message) {
    if (name == null)
      throw new NullPointerException();
    if (message == null)
      throw new NullPointerException();

    Optional<CharSequence> maybeValue = findEnvironmentVariable(name);
    if (maybeValue.isPresent())
      return maybeValue.get();

    if (message.length() == 0)
      throw new UnsetVariableInSubstitutionException(name);
    else
      throw new UnsetVariableInSubstitutionException(name, message.toString());
  }

  /**
   * Returns the substring of the value of the specified environment variable, starting at the
   * specified position.
   * 
   * @param name the name of the environment variable
   * @param position the position at which to start the substring
   * @return the substring of the value of the environment variable, starting at the specified
   *         position
   */
  private CharSequence handleColonExpr(String name, int position) {
    if (name == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");

    final int start;
    if (position >= 0)
      start = Math.min(value.length(), position);
    else
      start = Math.max(0, value.length() + position);

    return value.subSequence(start, value.length());
  }

  /**
   * Returns the substring of the value of the specified environment variable, starting at the
   * specified position and of the specified length.
   * 
   * @param name the name of the environment variable
   * @param position the position at which to start the substring
   * @param length the length of the substring
   * @return the substring of the value of the environment variable, starting at the specified
   *         position and of the specified length
   */
  private CharSequence handleColonColonExpr(String name, int position, int length) {
    if (name == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");

    final int start;
    if (position >= 0)
      start = Math.min(value.length(), position);
    else
      start = Math.max(0, value.length() + position);

    final int end;
    if (length >= 0)
      end = Math.min(value.length(), start + length);
    else
      end = Math.max(0, value.length() + length);

    if (start >= value.length())
      return "";
    if (end <= start)
      return "";

    return value.subSequence(start, end);
  }

  private CharSequence handleHashExpr(String name, CharSequence pattern) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    final Pattern p =
        Pattern.compile("^" + Globbing.glob(new CharSequenceCharacterIterator(pattern), false));
    final Matcher m = p.matcher(value);
    if (m.find()) {
      return value.subSequence(m.end(), value.length());
    }

    return value;
  }

  private CharSequence handleHashHashExpr(String name, CharSequence pattern) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    final Pattern p =
        Pattern.compile("^" + Globbing.glob(new CharSequenceCharacterIterator(pattern), true));
    final Matcher m = p.matcher(value);
    if (m.find()) {
      return value.subSequence(m.end(), value.length());
    }

    return value;
  }

  private CharSequence handlePercentExpr(String name, CharSequence pattern) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    final Pattern p =
        Pattern.compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), false) + "$");
    final Matcher m = p.matcher(value);
    if (m.find()) {
      return value.subSequence(0, m.start());
    }

    return value;
  }

  private CharSequence handlePercentPercentExpr(String name, CharSequence pattern) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    final Pattern p =
        Pattern.compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), true) + "$");
    final Matcher m = p.matcher(value);
    if (m.find()) {
      return value.subSequence(0, m.start());
    }

    return value;
  }

  private CharSequence handleSlashSlashExpr(String name, CharSequence pattern,
      CharSequence replacement) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();
    if (replacement == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    final Pattern p =
        Pattern.compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), true).toString());

    return Patterns.replaceAll(p, value, m -> replacement);
  }

  private CharSequence handleSlashHashExpr(String name, CharSequence pattern,
      CharSequence replacement) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();
    if (replacement == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    final Pattern p = Pattern
        .compile("^" + Globbing.glob(new CharSequenceCharacterIterator(pattern), true).toString());

    return Patterns.replaceAll(p, value, m -> replacement);
  }

  private CharSequence handleSlashPercentExpr(String name, CharSequence pattern,
      CharSequence replacement) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();
    if (replacement == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    final Pattern p = Pattern
        .compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), true).toString() + "$");

    return Patterns.replaceAll(p, value, m -> replacement);
  }

  private CharSequence handleSlashExpr(String name, CharSequence pattern,
      CharSequence replacement) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();
    if (replacement == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    final Pattern p =
        Pattern.compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), true).toString());

    return Patterns.replaceFirst(p, value, m -> replacement);
  }

  private CharSequence handleCaretCaretExpr(String name, CharSequence pattern) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");
    
    if (pattern.length() == 0)
      pattern = "?";

    final Pattern p =
        Pattern.compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), true).toString());

    return Patterns.replaceAll(p, value, m -> m.group().toUpperCase());
  }

  private CharSequence handleCaretExpr(String name, CharSequence pattern) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");

    if (pattern.length() == 0)
      pattern = "?";

    final Pattern p =
        Pattern.compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), true).toString());

    return Patterns.replaceFirst(p, value, m -> m.group().toUpperCase());
  }

  private CharSequence handleCommaCommaExpr(String name, CharSequence pattern) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");

    if (pattern.length() == 0)
      pattern = "?";

    final Pattern p =
        Pattern.compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), true).toString());

    return Patterns.replaceAll(p, value, m -> m.group().toLowerCase());
  }

  private CharSequence handleCommaExpr(String name, CharSequence pattern) {
    if (name == null)
      throw new NullPointerException();
    if (pattern == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");

    if (pattern.length() == 0)
      pattern = "?";

    final Pattern p =
        Pattern.compile(Globbing.glob(new CharSequenceCharacterIterator(pattern), true).toString());

    return Patterns.replaceFirst(p, value, m -> m.group().toLowerCase());
  }

  private CharSequence handleAtExpr(String name, char operator) {
    if (name == null)
      throw new NullPointerException();

    final CharSequence value = findEnvironmentVariable(name).orElse("");

    switch (operator) {
      case 'U':
        return value.toString().toUpperCase();
      case 'u':
        return new StringBuilder().append(Character.toUpperCase(value.charAt(0)))
            .append(value, 1, value.length()).toString();
      case 'L':
        return value.toString().toLowerCase();
      default:
        throw new IllegalArgumentException("Unknown modifier: @" + operator);
    }
  }

  /**
   * Retrieves the value of the specified environment variable, throwing an exception if not found.
   *
   * @param name the name of the environment variable
   * @return the value of the environment variable
   */
  protected Optional<CharSequence> findEnvironmentVariable(String name) {
    CharSequence result = getEnv().get(name);
    if (result == null) {
      if (getEnv().containsKey(name))
        return Optional.empty();
      if (isStrict())
        throw new UnsetVariableInSubstitutionException(name);
      return Optional.empty();
    }
    if (result.length() == 0)
      return Optional.empty();
    return Optional.of(result);
  }

  public Map<String, String> getEnv() {
    return unmodifiableMap(env);
  }

  public boolean isStrict() {
    return strict;
  }

  public void setStrict(boolean strict) {
    this.strict = strict;
  }
}
