# Rhenium

A compiler for a small statically-typed language that transpiles to C. This glossary fixes the
vocabulary the compiler and its documents use; the architecture it is built from lives in
[CLAUDE.md](./CLAUDE.md) and the language being designed lives in [docs/](./docs/README.md).

## Language

### The pipeline

**Decorate**:
To walk the AST and fill in each node's semantic context — resolving types, binding symbols, setting
the relevant scope. The AST is mutated in place; nothing is rebuilt.
_Avoid_: Annotate, analyze, type-check, resolve

**Decorator**:
A walker that decorates one kind of AST node.
_Avoid_: Visitor, analyzer, checker — `visitor` is reserved for the parse-tree walk that builds the
AST, and the two are different trees

**Transpiler**:
An emitter that reads one kind of decorated AST node and writes C to an output stream.
_Avoid_: Generator, codegen, backend, compiler

### Errors

**Diagnostic**:
A message about something the user got wrong, carrying a line, a column and text. Diagnostics are
values that are returned and accumulated, never thrown; a thrown exception is a compiler bug rather
than bad input.
_Avoid_: Error, warning, exception, failure

**Poison type**:
`InvalidType` — the type a failed expression takes. It absorbs every operation and assignment so that
one mistake produces one diagnostic instead of a cascade of consequences.
_Avoid_: Error type, unknown type, any, bottom type

**Poisoned**:
Of a declaration or symbol: still present and still bound, but carrying the poison type because the
thing that would have given it a real type failed. A poisoned declaration is what lets the rest of
the program keep being analyzed.
_Avoid_: Failed, invalid, broken, unresolved

### Types and values

**Expression type**:
The type of a value, as resolved by the semantic analyzer. Every one of them knows the C spelling it
is emitted as, which implicit conversions it permits, and which casts it permits.
_Avoid_: Type annotation, data type, kind

**C name**:
The text an expression type is emitted as in C.
_Avoid_: Native name, mapped type, C type

**Result-typed evaluation**:
The rule that an arithmetic node computes at the width of its own type rather than at the width C's
integer promotions would pick. It is what the cast wrapped around every emitted arithmetic expression
buys, and it is why `I8` arithmetic wraps at eight bits instead of at thirty-two.
_Avoid_: Promotion, widening, coercion

**Unsigned detour**:
Emitting signed `+`, `-`, `*` and unary `-` through the corresponding unsigned type, where C defines
wraparound, so that signed overflow is defined rather than undefined behaviour. Division and
remainder do not take it, because `MIN / -1` through unsigned changes the answer rather than
wrapping it.
_Avoid_: Unsigned trick, wraparound cast, overflow workaround

**Printable type**:
An expression type that a value can be printed as, which is every type except the poison type.
_Avoid_: Displayable, formattable, showable

**Mangled name**:
The unique C identifier a variable is emitted as, derived from its source name so that C never sees
two declarations collide across scopes it does not have.
_Avoid_: Unique name, generated name, symbol name

### Scopes and symbols

**Symbol**:
A named thing a scope can resolve — a variable or a type.
_Avoid_: Binding, entry, definition

**Left value**:
A symbol that can appear on the left of an assignment. Types are symbols but are not left values.
_Avoid_: LHS, assignable, target, variable reference

**Global scope**:
The outermost scope, seeded with the primitive types before any source is analyzed.
_Avoid_: Root scope, builtin scope, prelude
