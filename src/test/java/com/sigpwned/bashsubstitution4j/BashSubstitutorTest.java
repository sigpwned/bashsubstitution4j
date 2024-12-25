package com.sigpwned.bashsubstitution4j;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BashSubstitutorTest {
  public static <K, V> Map<K, V> mapOf() {
    return emptyMap();
  }

  public static <K, V> Map<K, V> mapOf(K k1, V v1) {
    return singletonMap(k1, v1);
  }

  public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
    final HashMap<K, V> map = new HashMap<>(2);
    map.put(k1, v1);
    map.put(k2, v2);
    return unmodifiableMap(map);
  }

  public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
    final HashMap<K, V> map = new HashMap<>(3);
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    return unmodifiableMap(map);
  }

  @Test
  void givenVariableName_whenSubstituted_thenReplacedWithValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("value", substitutor.substitute("${NAME}"));
  }

  @Test
  void givenUnsetVariable_whenSubstituted_thenReturnsEmptyString() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf());
    assertEquals("", substitutor.substitute("${NAME}"));
  }

  @Test
  void givenDefaultValueSyntax_whenVariableIsUnset_thenDefaultValueUsed() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf());
    assertEquals("default", substitutor.substitute("${NAME:-default}"));
  }

  @Test
  void givenDefaultValueSyntax_whenVariableIsSet_thenVariableValueUsed() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("value", substitutor.substitute("${NAME:-default}"));
  }

  @Test
  void givenAlternateValueSyntax_whenVariableIsSet_thenAlternateValueUsed() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("alternate", substitutor.substitute("${NAME:+alternate}"));
  }

  @Test
  void givenAlternateValueSyntax_whenVariableIsUnset_thenReturnsEmptyString() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf());
    assertEquals("", substitutor.substitute("${NAME:+alternate}"));
  }

  @Test
  void givenVariableWithPrefix_whenPrefixRemoved_thenReturnsRemainingValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "prefixvalue"));
    assertEquals("value", substitutor.substitute("${NAME#prefix}"));
  }

  @Test
  void givenVariableWithLongestPrefix_whenPrefixRemoved_thenReturnsRemainingValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "prefixprefixvalue"));
    assertEquals("value", substitutor.substitute("${NAME##pre*fix}"));
  }

  @Test
  void givenVariableWithSuffix_whenSuffixRemoved_thenReturnsRemainingValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuesuffix"));
    assertEquals("value", substitutor.substitute("${NAME%suffix}"));
  }

  @Test
  void givenVariableWithLongestSuffix_whenSuffixRemoved_thenReturnsRemainingValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuesuffixsuffix"));
    assertEquals("value", substitutor.substitute("${NAME%%suf*fix}"));
  }

  @Test
  void givenPatternInVariable_whenReplacedOnce_thenReturnsModifiedValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuePATTERNvalue"));
    assertEquals("valueREPLACEMENTvalue", substitutor.substitute("${NAME/PATTERN/REPLACEMENT}"));
  }

  @Test
  void givenPatternInVariable_whenReplacedGlobally_thenReturnsModifiedValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "PATTERNvaluePATTERNvalue"));
    assertEquals("REPLACEMENTvalueREPLACEMENTvalue",
        substitutor.substitute("${NAME//PATTERN/REPLACEMENT}"));
  }

  @Test
  void givenPatternAtStart_whenReplaced_thenReturnsModifiedValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "PATTERNvalue"));
    assertEquals("REPLACEMENTvalue", substitutor.substitute("${NAME/#PATTERN/REPLACEMENT}"));
  }

  @Test
  void givenPatternAtEnd_whenReplaced_thenReturnsModifiedValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuePATTERN"));
    assertEquals("valueREPLACEMENT", substitutor.substitute("${NAME/%PATTERN/REPLACEMENT}"));
  }

  @Test
  void givenLowercaseTransformation_whenVariableSubstituted_thenConvertedToLowercase() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "HELLO"));
    assertEquals("hello", substitutor.substitute("${NAME@L}"));
  }

  @Test
  void givenUppercaseTransformation_whenVariableSubstituted_thenConvertedToUppercase() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "hello"));
    assertEquals("HELLO", substitutor.substitute("${NAME@U}"));
  }

  @Test
  void givenNullInput_whenSubstituted_thenThrowsNullPointerException() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertThrows(NullPointerException.class, () -> substitutor.substitute(null));
  }

  @Test
  void givenInvalidPattern_whenSubstituted_thenThrowsIllegalArgumentException() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertThrows(IllegalArgumentException.class, () -> substitutor.substitute("${NAME!INVALID}"));
  }

  @Test
  void givenComplexInput_whenSubstituted_thenAllSubstitutionsApplied() {
    BashSubstitutor substitutor =
        new BashSubstitutor(mapOf("PATH", "/usr/bin", "USER", "USERNAME", "NAME", "prefixvalue"));
    String input = "Path: ${PATH:-/default}, User: ${USER@L}, Prefix Removed: ${NAME#prefix}";
    String expected = "Path: /usr/bin, User: username, Prefix Removed: value";
    assertEquals(expected, substitutor.substitute(input));
  }
}
