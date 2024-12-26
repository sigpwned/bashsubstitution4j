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

import java.util.Map;
import com.sigpwned.bashsubstitution4j.BashSubstitutor;

public final class BashSubstitution {
  private BashSubstitution() {}

  /**
   * Substitutes environment variables in a string.
   * 
   * @param env the environment variables to use
   * @param text the string to substitute environment
   * @return the string with all environment variables replaced
   * 
   * @see BashSubstitutor#substitute(String)
   */
  public static String substitute(Map<String, String> env, String text) {
    return substitute(env, text, false);
  }


  /**
   * Substitutes environment variables in a string with the given strictness.
   * 
   * @param env the environment variables to use
   * @param text the string to substitute environment
   * @return the string with all environment variables replaced
   * 
   * @see BashSubstitutor#substitute(String)
   */
  public static String substitute(Map<String, String> env, String text, boolean strict) {
    return new BashSubstitutor(env, strict).substitute(text);
  }
}
