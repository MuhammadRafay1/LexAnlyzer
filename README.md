Reserved Keywords:
Integer = int
Decimal = dec
Boolean = bln
Character = char
Global = mrWorld
Local = mrArea


Data types:
Integer
Decimal
Boolean
Character

Constraints: 
Only lowercase alphabets [a_z]

Comments:
SingleLine Comment: %%
Multiline: %*.....*%

Operators:
+, -, *, /, %, and exponentiation (e.g., ^)


Regular Expressions:

Reserved Keywords
(int|dec|bln|char|mrWorld|mrArea)

Identifiers
[a-z][a-z0-9_]*

Data Types
\d+                          // Integer
\d+\.\d{1,5}                 // Decimal
(true|false)                 // Boolean
'[a-z]'                      // Character

Comments
%%.*                         // Single-line
%\*(\*(?!%)|[^*])*\*%        // Multi-line


Operators
[+\-*/%^]                    // Arithmetic

Whitespace
[ \t\n]+                     // Spaces, tabs, newlines

