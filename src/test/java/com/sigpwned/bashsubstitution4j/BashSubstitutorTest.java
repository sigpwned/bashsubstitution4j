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
  public void givenVariableName_whenSubstituted_thenReplacedWithValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("value", substitutor.substitute("${NAME}"));
  }

  @Test
  public void givenIndirectVariableName_whenSubstituted_thenReplacedWithValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "VAR", "VAR", "value"));
    assertEquals("value", substitutor.substitute("${!NAME}"));
  }

  @Test
  public void givenUnsetVariable_whenSubstituted_thenReturnsEmptyString() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf());
    assertEquals("", substitutor.substitute("${NAME}"));
  }

  @Test
  public void givenDefaultValueSyntax_whenVariableIsEmpty_thenDefaultValueUsed() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", ""));
    assertEquals("default", substitutor.substitute("${NAME:-default}"));
  }

  @Test
  public void givenDefaultValueSyntax_whenVariableIsUnset_thenDefaultValueUsed() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf());
    assertEquals("default", substitutor.substitute("${NAME:-default}"));
  }

  @Test
  public void givenDefaultValueSyntax_whenVariableIsUnsetStrict_thenDefaultValueUsed() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf(), true);
    assertEquals("default", substitutor.substitute("${NAME:-default}"));
  }

  @Test
  public void givenDefaultValueSyntax_whenVariableIsSet_thenVariableValueUsed() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("value", substitutor.substitute("${NAME:-default}"));
  }

  @Test
  public void givenAlternateValueSyntax_whenVariableIsSet_thenAlternateValueUsed() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("alternate", substitutor.substitute("${NAME:+alternate}"));
  }

  @Test
  public void givenAlternateValueSyntax_whenVariableIsUnset_thenReturnsEmptyString() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf());
    assertEquals("", substitutor.substitute("${NAME:+alternate}"));
  }

  @Test
  public void givenAlternateValueSyntax_whenVariableIsUnsetStrict_thenReturnsEmptyString() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf(), true);
    assertEquals("", substitutor.substitute("${NAME:+alternate}"));
  }

  @Test
  public void givenColonQuestionSyntax_whenSubstituteWithUnsetVariable_thenThrowsException() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf());
    assertThrows(UnsetVariableInSubstitutionException.class,
        () -> substitutor.substitute("${NAME:?}"));
  }

  @Test
  public void givenColonQuestionSyntax_whenSubstituteWithEmptyVariable_thenThrowsException() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", ""));
    assertThrows(UnsetVariableInSubstitutionException.class,
        () -> substitutor.substitute("${NAME:?}"));
  }

  @Test
  public void givenColonQuestionSyntax_whenSubstituteWithPopulatedVariable_thenReturnsValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "VALUE"));
    assertEquals("VALUE", substitutor.substitute("${NAME:?}"));
  }

  @Test
  public void givenVariableWithPrefix_whenPrefixRemoved_thenReturnsRemainingValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "prefixvalue"));
    assertEquals("value", substitutor.substitute("${NAME#prefix}"));
  }

  @Test
  public void givenVariableWithLongestPrefix_whenPrefixRemoved_thenReturnsRemainingValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "prefixprefixvalue"));
    assertEquals("value", substitutor.substitute("${NAME##pre*fix}"));
  }

  @Test
  public void givenVariableWithSuffix_whenSuffixRemoved_thenReturnsRemainingValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuesuffix"));
    assertEquals("value", substitutor.substitute("${NAME%suffix}"));
  }

  @Test
  public void givenVariableWithLongestSuffix_whenSuffixRemoved_thenReturnsRemainingValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuesuffixsuffix"));
    assertEquals("value", substitutor.substitute("${NAME%%suf*fix}"));
  }

  @Test
  public void givenPatternInVariable_whenReplacedOnce_thenReturnsModifiedValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuePATTERNvalue"));
    assertEquals("valueREPLACEMENTvalue", substitutor.substitute("${NAME/PATTERN/REPLACEMENT}"));
  }

  @Test
  public void givenPatternInVariable_whenReplacedGlobally_thenReturnsModifiedValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "PATTERNvaluePATTERNvalue"));
    assertEquals("REPLACEMENTvalueREPLACEMENTvalue",
        substitutor.substitute("${NAME//PATTERN/REPLACEMENT}"));
  }

  @Test
  public void givenPatternAtStart_whenReplaced_thenReturnsModifiedValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "PATTERNvalue"));
    assertEquals("REPLACEMENTvalue", substitutor.substitute("${NAME/#PATTERN/REPLACEMENT}"));
  }

  @Test
  public void givenPatternAtStart_whenNotReplaced_thenReturnsOriginalValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuePATTERN"));
    assertEquals("valuePATTERN", substitutor.substitute("${NAME/#PATTERN/REPLACEMENT}"));
  }

  @Test
  public void givenPatternAtEnd_whenReplaced_thenReturnsModifiedValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "valuePATTERN"));
    assertEquals("valueREPLACEMENT", substitutor.substitute("${NAME/%PATTERN/REPLACEMENT}"));
  }

  @Test
  public void givenPatternAtEnd_whenNotReplaced_thenReturnsOriginalValue() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "PATTERNvalue"));
    assertEquals("PATTERNvalue", substitutor.substitute("${NAME/%PATTERN/REPLACEMENT}"));
  }

  @Test
  public void givenCommaSubstitutionWithoutPattern_whenSubstituted_thenFirstCharacterLowercased() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "VALUE"));
    assertEquals("vALUE", substitutor.substitute("${NAME,}"));
  }

  @Test
  public void givenCommaSubstitutionWithMatchingPattern_whenSubstituted_thenFirstCharacterLowercased() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "VALUE"));
    assertEquals("vALUE", substitutor.substitute("${NAME,[V]}"));
  }

  @Test
  public void givenCommaSubstitutionWithNonMatchingPattern_whenSubstituted_thenNoChange() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "VALUE"));
    assertEquals("VALUE", substitutor.substitute("${NAME,[X]}"));
  }

  @Test
  public void givenCommaCommaSubstitutionWithoutPattern_whenSubstituted_thenFirstCharacterLowercased() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "VALUE"));
    assertEquals("value", substitutor.substitute("${NAME,,}"));
  }

  @Test
  public void givenCommaCommaSubstitutionWithMatchingPattern_whenSubstituted_thenFirstCharacterLowercased() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "VALUE"));
    assertEquals("valUE", substitutor.substitute("${NAME,,[VAL]}"));
  }

  @Test
  public void givenCommaCommaSubstitutionWithNonMatchingPattern_whenSubstituted_thenNoChange() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "VALUE"));
    assertEquals("VALUE", substitutor.substitute("${NAME,,[X]}"));
  }

  @Test
  public void givenCaretSubstitutionWithoutPattern_whenSubstituted_thenFirstCharacterLowercased() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("Value", substitutor.substitute("${NAME^}"));
  }

  @Test
  public void givenCaretSubstitutionWithMatchingPattern_whenSubstituted_thenFirstCharacterLowercased() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("Value", substitutor.substitute("${NAME^[v]}"));
  }

  @Test
  public void givenCaretSubstitutionWithNonMatchingPattern_whenSubstituted_thenNoChange() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("value", substitutor.substitute("${NAME^[x]}"));
  }

  @Test
  public void givenCaretCaretSubstitutionWithoutPattern_whenSubstituted_thenFirstCharacterLowercased() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("VALUE", substitutor.substitute("${NAME^^}"));
  }

  @Test
  public void givenCaretCaretSubstitutionWithMatchingPattern_whenSubstituted_thenFirstCharacterLowercased() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("VALue", substitutor.substitute("${NAME^^[val]}"));
  }

  @Test
  public void givenCaretCaretSubstitutionWithNonMatchingPattern_whenSubstituted_thenNoChange() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertEquals("value", substitutor.substitute("${NAME^^[x]}"));
  }

  @Test
  public void givenLowercaseTransformation_whenVariableSubstituted_thenConvertedToLowercase() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "HELLO"));
    assertEquals("hello", substitutor.substitute("${NAME@L}"));
  }

  @Test
  public void givenFirstUppercaseTransformation_whenVariableSubstituted_thenConvertedToUppercase() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "hello"));
    assertEquals("Hello", substitutor.substitute("${NAME@u}"));
  }

  @Test
  public void givenAllUppercaseTransformation_whenVariableSubstituted_thenConvertedToUppercase() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "hello"));
    assertEquals("HELLO", substitutor.substitute("${NAME@U}"));
  }

  @Test
  public void givenNullInput_whenSubstituted_thenThrowsNullPointerException() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertThrows(NullPointerException.class, () -> substitutor.substitute(null));
  }

  @Test
  public void givenStrictSubstitutor_whenSubstituteWithUnsetVariable_thenThrowsException() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf(), true);
    assertThrows(UnsetVariableInSubstitutionException.class,
        () -> substitutor.substitute("${NAME}"));
  }

  @Test
  public void givenStrictSubstitutor_whenSubstituteWithEmptyVariable_thenThrowsException() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", ""), true);
    assertEquals("", substitutor.substitute("${NAME}"));
  }

  @Test
  public void givenInvalidPattern_whenSubstituted_thenThrowsIllegalArgumentException() {
    BashSubstitutor substitutor = new BashSubstitutor(mapOf("NAME", "value"));
    assertThrows(IllegalArgumentException.class, () -> substitutor.substitute("${NAME!INVALID}"));
  }

  @Test
  public void givenComplexInput_whenSubstituted_thenAllSubstitutionsApplied() {
    BashSubstitutor substitutor =
        new BashSubstitutor(mapOf("PATH", "/usr/bin", "USER", "USERNAME", "NAME", "prefixvalue"));
    String input = "Path: ${PATH:-/default}, User: ${USER@L}, Prefix Removed: ${NAME#prefix}";
    String expected = "Path: /usr/bin, User: username, Prefix Removed: value";
    assertEquals(expected, substitutor.substitute(input));
  }
}
