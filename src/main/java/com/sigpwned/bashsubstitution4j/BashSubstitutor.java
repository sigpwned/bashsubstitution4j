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

/**
 * A class for performing {@link Bash https://en.wikipedia.org/wiki/Bash_(Unix_shell)}-like
 * substitution on strings.
 * 
 * <h2>Supported Syntax</h2>
 * 
 * <p>
 * This implementation supports the following syntax with the same semantics as defined by
 * <a href="https://www.gnu.org/software/bash/manual/html_node/Shell-Parameter-Expansion.html">the
 * Bash manual</a>.
 * 
 * <ul>
 * <li><code>${NAME}</code></li>
 * <li><code>${NAME:-DEFAULT}</code></li>
 * <li><code>${NAME:+ERROR_MESSAGE}</code></li>
 * <li><code>${NAME#PREFIX}</code></li>
 * <li><code>${NAME##PREFIX}</code></li>
 * <li><code>${NAME%SUFFIX}</code></li>
 * <li><code>${NAME%%SUFFIX}</code></li>
 * <li><code>${NAME/PATTERN/REPLACEMENT}</code></li>
 * <li><code>${NAME//PATTERN/REPLACEMENT}</code></li>
 * <li><code>${NAME/#PATTERN/REPLACEMENT}</code></li>
 * <li><code>${NAME/%PATTERN/REPLACEMENT}</code></li>
 * <li><code>${NAME@X}</code></li>
 * </ul>
 * 
 * <p>
 * Additionally, indirect reference syntax <code>${!NAME}</code> is supported and applies to all
 * listed substitution rules. For example, if <code>NAME</code> is set to "VAR",
 * <code>${!NAME}</code> will reference the value of the variable "VAR" and apply the specified
 * substitution rule (e.g., <code>${!NAME:-DEFAULT}</code>).
 * 
 * <p>
 * Only regular string variables are supported. Array variables and other complex data types are not
 * supported by this implementation at this time.
 * 
 * <h2>Strict Mode</h2>
 * 
 * <p>
 * Instances of this class can be created with strict mode enabled.
 * 
 * <p>
 * If strict mode is disabled, then any references to an unset variable will be treated as if the
 * variable were set with the value of <code>""</code>.
 * 
 * <p>
 * If strict mode is enabled, then evaluating a substitution that references an unset variable will
 * result in an {@link UnsetVariableInSubstitutionException} being thrown. This additional error
 * checking applies to all syntax without a built-in default-handling mechanism, which is to say the
 * following substitution patterns:
 * 
 * <ul>
 * <li><code>${NAME}</code></li>
 * <li><code>${NAME#PREFIX}</code></li>
 * <li><code>${NAME##PREFIX}</code></li>
 * <li><code>${NAME%SUFFIX}</code></li>
 * <li><code>${NAME%%SUFFIX}</code></li>
 * <li><code>${NAME/PATTERN/REPLACEMENT}</code></li>
 * <li><code>${NAME//PATTERN/REPLACEMENT}</code></li>
 * <li><code>${NAME/#PATTERN/REPLACEMENT}</code></li>
 * <li><code>${NAME/%PATTERN/REPLACEMENT}</code></li>
 * <li><code>${NAME@X}</code></li>
 * </ul>
 * 
 * <p>
 * Additionally, strict mode will cause an exception to be thrown when evaluating an indirect
 * reference if the given variable does not exist, but will simply return an empty string if the
 * referenced variable does not exist. For example, evaluating <code>${!NAME}</code> when
 * <code>NAME</code> is unset will result in an exception; however, evaluating <code>${!NAME}</code>
 * when <code>NAME</code> is set to <code>"MISSING"</code> and <code>MISSING</code> is unset, will
 * substitute the empty string.
 * 
 * <h2>Unsupported Syntax</h2>
 * 
 * <p>
 * This implementation performs variable substitution only. It does not support the following
 * additional expansion mechanisms or syntax:
 * 
 * <ul>
 * <li>Arithmetic expansion</li>
 * <li>Command substitution</li>
 * <li>Process substitution</li>
 * <li>Word splitting</li>
 * <li>Filename expansion (globbing)</li>
 * <li>Tilde expansion</li>
 * <li>Brace expansion</li>
 * <li>Quoting</li>
 * <li>Escape characters</li>
 * <li>Array variables</li>
 * </ul>
 */
public class BashSubstitutor {
  private final Map<String, String> env;
  private boolean strict;

  /**
   * Equivalent to <code>new BashSubstitutor(System.getenv(), false)</code>.
   *
   * @param env the required environment variables to use for substitution
   * 
   * @see #BashSubstitutor(Map, boolean)
   */
  public BashSubstitutor(Map<String, String> env) {
    this(env, false);
  }

  /**
   * @param env the required environment variables to use for substitution
   * @param strict whether to enable strict
   * 
   * @throws NullPointerException if env is null
   */
  public BashSubstitutor(Map<String, String> env, boolean strict) {
    this.env = requireNonNull(env);
    this.strict = strict;
  }

  /**
   * Implements bash substitution rules, including indirect reference (${!NAME}) support:
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
   * Indirect reference syntax <code>${!NAME}</code> is supported and applies to all listed
   * substitution rules. For example, if <code>NAME</code> is set to "VAR", <code>${!NAME}</code>
   * will reference the value of the variable "VAR" and apply the specified substitution rule (e.g.,
   * <code>${!NAME:-DEFAULT}</code>).
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

    final StringBuilder name = new StringBuilder();

    if (iterator.current() == '!') {
      name.append(iterator.current());
      iterator.next();
    }

    if (!isParameterStartChar(iterator.current()))
      throw new IllegalArgumentException("Invalid parameter name");

    name.append(iterator.current());
    iterator.next();
    while (iterator.current() != CharacterIterator.DONE && isParameterChar(iterator.current())) {
      name.append(iterator.current());
      iterator.next();
    }

    if (iterator.current() == CharacterIterator.DONE) {
      return handleExpr(name);
    } else if (Parsing.attempt(iterator, ":-")) {
      final CharSequence defaultValue = Parsing.slurp(iterator);
      return handleColonDashExpr(name, defaultValue);
    } else if (Parsing.attempt(iterator, ":+")) {
      final CharSequence errorMessage = Parsing.slurp(iterator);
      return handleColonPlusExpr(name, errorMessage);
    } else if (Parsing.attempt(iterator, ":?")) {
      final CharSequence errorMessage = Parsing.slurp(iterator);
      return handleColonQuestionExpr(name, errorMessage);
    } else if (Parsing.attempt(iterator, ":")) {
      final int position = Parsing.parseInt(iterator);
      if (Parsing.attempt(iterator, ":")) {
        int length = Parsing.parseInt(iterator);
        if (iterator.current() != CharacterIterator.DONE)
          throw new IllegalArgumentException("Unsupported pattern: " + expression);
        return handleColonColonExpr(name, position, length);
      } else {
        if (iterator.current() != CharacterIterator.DONE)
          throw new IllegalArgumentException("Unsupported pattern: " + expression);
        return handleColonExpr(name, position);
      }
    } else if (Parsing.attempt(iterator, "##")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleHashHashExpr(name, pattern);
    } else if (Parsing.attempt(iterator, "#")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleHashExpr(name, pattern);
    } else if (Parsing.attempt(iterator, "%%")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handlePercentPercentExpr(name, pattern);
    } else if (Parsing.attempt(iterator, "%")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handlePercentExpr(name, pattern);
    } else if (Parsing.attempt(iterator, "//")) {
      final CharSequence pattern = Parsing.until(iterator, '/');
      Parsing.expect(iterator, "/");
      final CharSequence replacement = Parsing.slurp(iterator);
      return handleSlashSlashExpr(name, pattern, replacement);
    } else if (Parsing.attempt(iterator, "/#")) {
      final CharSequence pattern = Parsing.until(iterator, '/');
      Parsing.expect(iterator, "/");
      final CharSequence replacement = Parsing.slurp(iterator);
      return handleSlashHashExpr(name, pattern, replacement);
    } else if (Parsing.attempt(iterator, "/%")) {
      final CharSequence pattern = Parsing.until(iterator, '/');
      Parsing.expect(iterator, "/");
      final CharSequence replacement = Parsing.slurp(iterator);
      return handleSlashPercentExpr(name, pattern, replacement);
    } else if (Parsing.attempt(iterator, "/")) {
      final CharSequence pattern = Parsing.until(iterator, '/');
      Parsing.expect(iterator, "/");
      final CharSequence replacement = Parsing.slurp(iterator);
      return handleSlashExpr(name, pattern, replacement);
    } else if (Parsing.attempt(iterator, "^^")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleCaretCaretExpr(name, pattern);
    } else if (Parsing.attempt(iterator, "^")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleCaretExpr(name, pattern);
    } else if (Parsing.attempt(iterator, ",,")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleCommaCommaExpr(name, pattern);
    } else if (Parsing.attempt(iterator, ",")) {
      final CharSequence pattern = Parsing.slurp(iterator);
      return handleCommaExpr(name, pattern);
    } else if (Parsing.attempt(iterator, "@")) {
      final char operator = iterator.current();
      if (operator == CharacterIterator.DONE)
        throw new IllegalArgumentException("Unsupported pattern: " + expression);

      if (iterator.next() != CharacterIterator.DONE)
        throw new IllegalArgumentException("Unsupported pattern: " + expression);

      return handleAtExpr(name, operator);
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
  private CharSequence handleExpr(CharSequence name) {
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
  private CharSequence handleColonDashExpr(CharSequence name, CharSequence defaultValue) {
    if (name == null)
      throw new NullPointerException();
    if (defaultValue == null)
      throw new NullPointerException();
    try {
      return findEnvironmentVariable(name).orElse(defaultValue);
    } catch (UnsetVariableInSubstitutionException e) {
      // If the variable is unset, we return the default value, even in strict mode.
      return defaultValue;
    }
  }

  /**
   * Returns the empty string if the given name is unset, otherwise returns the given message.
   * 
   * @param name the name of the environment variable
   * @param message the message to return if the environment variable is set
   * @return the empty string if the environment variable is unset, otherwise the given message
   * 
   */
  private CharSequence handleColonPlusExpr(CharSequence name, CharSequence message) {
    if (name == null)
      throw new NullPointerException();
    if (message == null)
      throw new NullPointerException();
    try {
      if (findEnvironmentVariable(name).isPresent())
        return message;
      return "";
    } catch (UnsetVariableInSubstitutionException e) {
      // If the variable is unset, we return the empty string, even in strict mode.
      return "";
    }
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
  private CharSequence handleColonQuestionExpr(CharSequence name, CharSequence message) {
    if (name == null)
      throw new NullPointerException();
    if (message == null)
      throw new NullPointerException();

    Optional<CharSequence> maybeValue;
    try {
      maybeValue = findEnvironmentVariable(name);
    } catch (UnsetVariableInSubstitutionException e) {
      // In strict mode, just treat this as an empty value. We still throw an exception, just later.
      maybeValue = Optional.empty();
    }
    if (maybeValue.isPresent())
      return maybeValue.get();

    if (message.length() == 0)
      throw new UnsetVariableInSubstitutionException(name.toString());
    else
      throw new UnsetVariableInSubstitutionException(name.toString(), message.toString());
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
  private CharSequence handleColonExpr(CharSequence name, int position) {
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
  private CharSequence handleColonColonExpr(CharSequence name, int position, int length) {
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

  private CharSequence handleHashExpr(CharSequence name, CharSequence pattern) {
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

  private CharSequence handleHashHashExpr(CharSequence name, CharSequence pattern) {
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

  private CharSequence handlePercentExpr(CharSequence name, CharSequence pattern) {
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

  private CharSequence handlePercentPercentExpr(CharSequence name, CharSequence pattern) {
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

  private CharSequence handleSlashSlashExpr(CharSequence name, CharSequence pattern,
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

  private CharSequence handleSlashHashExpr(CharSequence name, CharSequence pattern,
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

  private CharSequence handleSlashPercentExpr(CharSequence name, CharSequence pattern,
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

  private CharSequence handleSlashExpr(CharSequence name, CharSequence pattern,
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

  private CharSequence handleCaretCaretExpr(CharSequence name, CharSequence pattern) {
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

  private CharSequence handleCaretExpr(CharSequence name, CharSequence pattern) {
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

  private CharSequence handleCommaCommaExpr(CharSequence name, CharSequence pattern) {
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

  private CharSequence handleCommaExpr(CharSequence name, CharSequence pattern) {
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

  private CharSequence handleAtExpr(CharSequence name, char operator) {
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
   * @param reference the name of the environment variable
   * @return the value of the environment variable
   * 
   * @throws UnsetVariableInSubstitutionException if the environment variable is unset and this
   *         instance is in strict mode
   * @throws NullPointerException if reference is null
   * @throws IllegalArgumentException if reference is empty
   */
  protected Optional<CharSequence> findEnvironmentVariable(CharSequence reference) {
    if (reference == null)
      throw new NullPointerException();
    if (reference.length() == 0)
      throw new IllegalArgumentException("name must not be empty");

    final String name;
    final boolean dereferenced;
    if (reference.charAt(0) == '!') {
      final String namePointer = reference.subSequence(1, reference.length()).toString();
      final Optional<CharSequence> maybeDereferenced = findEnvironmentVariable(namePointer);
      if (!maybeDereferenced.isPresent())
        throw new UnsetVariableInSubstitutionException(namePointer);
      name = maybeDereferenced.get().toString();
      dereferenced = true;
    } else {
      name = reference.toString();
      dereferenced = false;
    }

    CharSequence result = getEnv().get(name);
    if (result == null) {
      if (getEnv().containsKey(name))
        return Optional.empty();
      if (dereferenced)
        return Optional.empty();
      if (isStrict())
        throw new UnsetVariableInSubstitutionException(name);
      return Optional.empty();
    }
    if (result.length() == 0)
      return Optional.empty();
    return Optional.of(result);
  }

  /**
   * Returns the environment variables used for substitution. The returned map is unmodifiable.
   * 
   * @return the environment variables used for substitution
   */
  public Map<String, String> getEnv() {
    return unmodifiableMap(env);
  }

  /**
   * Returns whether this instance is in strict mode.
   * 
   * @return {@code true} if this instance is in strict mode, {@code false} otherwise
   */
  public boolean isStrict() {
    return strict;
  }

  /**
   * Sets whether this instance is in strict mode.
   * 
   * @param strict {@code true} if this instance should be in strict mode, {@code false} otherwise
   */
  public void setStrict(boolean strict) {
    this.strict = strict;
  }
}
