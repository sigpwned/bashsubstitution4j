package com.sigpwned.bashsubstitution4j;

import java.util.Optional;

public interface Environment {
  public Optional<String> getValue(String name);
}
