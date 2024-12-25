package com.sigpwned.bashsubstitution4j;

import static java.util.Objects.requireNonNull;
import java.util.NoSuchElementException;

@SuppressWarnings("serial")
public class UnsetVariableInSubstitutionException extends NoSuchElementException {
  private final String name;

  public UnsetVariableInSubstitutionException(String name) {
    this(name, "unset variable: " + name);
  }

  public UnsetVariableInSubstitutionException(String name, String message) {
    super(message);
    this.name = requireNonNull(name);
  }

  public String getName() {
    return name;
  }
}
