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
