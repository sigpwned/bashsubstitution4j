# bashsubstitution4j

A Bash ([Bourne Again SHell](https://www.gnu.org/software/bash/)) substitution library for Java 8+.

This library is intended as a *very* simple templating library with familiar syntax. It is explicitly *not* intended as a full Bash implementation, or to perform anything more than a basic templating library. Performing a substitution only performs basic string manipulation and not shell features. More specifically, it does not (and will not ever) create new processes, access the file system, print to stdout/stderr, etc.

## Features

The library supports the following Bash substitution features:

* Parameter expansion
* String globbing

The library does not currently support the following Bash substitution features, but may in the future:

* Arithmetic expansion
* Quoting
* Arrays
* Namerefs
* Indirection (e.g., `${!parameter}`)
* Extglob

The library does not currently support the following Bash substitution features, and never will:

* Tilde expansion, since it accesses system environment facts
* Command substitution, since it creates new processes
* File globbing, since it access the file system

## Supported substitutions

* `${parameter}`
* `${parameter:-word}`
* `${parameter:+word}`
* `${parameter:offset}`
* `${parameter:offset:length}`
* `${parameter#word}`
* `${parameter##word}`
* `${parameter%word}`
* `${parameter%%word}`
* `${parameter/pattern/string}`
* `${parameter//pattern/string}`
* `${parameter/#pattern/string}`
* `${parameter/%pattern/string}`
* `${parameter^pattern}`
* `${parameter^^pattern}`
* `${parameter,pattern}`
* `${parameter,,pattern}`
* `${parameter@operator}`, for operators `UuLQ`

## Quick start

To perform substitution using environment variables, use:

    BashSubstitution.substitute("This is my ${ADJECTIVE} template.", System.getenv());

To perform substitution using system properties, use:

    BashSubstitution.substitute("This is my ${ADJECTIVE} template.", System.getProperties());

To perform substitution using custom variables, use:

    Map<String, String> customVariables=new HashMap<>();
    customVariables.put("ADJECTIVE", "fancy");
    BashSubstitution.substitute("This is my ${ADJECTIVE} template.", customVariables);

## Advanced usage

Users can create an instance to perform many substitutions with a given set of variables and use it like this:

    BashSubstitutor substitutor=new BashSubstitutor(System.getenv());
    String substitution = substitutor.substitute("This is my ${ADJECTIVE} template.");

Users can translate a Bash (string) globbing expression using:

    Pattern p = BashGlobbing.toJavaPattern(globExpression);

## Customization


### Disabling syntax

By default, the `BashSubstitutor` class supports all the enumerated substitutions. If users want not to support some specific syntax, then they can simply create a new subclass overriding a specific expression type. For example:

    // Don't support the ${parameter#word} syntax
    BashSubstitutor substitutor = new BashSubstitutor(variables) {
        protected String evaluateShortestPrefixSubstitution(String parameter, String prefixWord) {
            throw new UnsupportedOperationException("syntax not supported");
        }
    };