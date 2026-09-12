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

**Lower**:
To walk the decorated AST and build the action tree from it. Lowering never fails — everything the
user can get wrong was already reported by then — so it returns actions rather than a `Diagnosed`.
_Avoid_: Compile, emit, generate, translate, desugar

**Lowerer**:
A walker that lowers one kind of AST node. One per node kind, as the decorators are.
_Avoid_: Visitor, emitter, builder

**Action**:
One construct of the C program that is about to be written. An action carries plain data and holds no
reference to the AST, so an action exists for C code that has no Rhenium source behind it — the cast
that implements result-typed evaluation, and eventually the destruction of a resource. Where an
action needs a type it carries an expression type, because that already knows its own C name.
_Avoid_: Instruction, statement, emission, node, operation — `instruction` implies a flat
three-address form, which this deliberately is not

**Action tree**:
Every action for a program, nested. AST nodes represent Rhenium source; actions represent the
upcoming C code, and everything emitted comes from an action. Expressions nest rather than flatten,
which is what makes `&&` and `||` short-circuit — see
[ADR 0002](./docs/adr/0002-expressions-stay-nested-in-the-action-tree.md).
_Avoid_: IR, action list, action map, intermediate representation, bytecode

**Block**:
The action that owns an ordered, mutable list of child actions. Its mutability is the point of the
whole stage: it is what lets the compiler append a statement to a scope it is still building, and
withdraw one it had already planned.
_Avoid_: Scope, body, sequence, compound statement — `scope` is the semantic-context term and the two
are different things

**Transpiler**:
The printer that concatenates an action tree into C text. It performs lookups — following an
expression type to its C name — but makes no decisions; every decision about the emitted C is made
while lowering.
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
Emitting signed `+`, `-`, `*` and unary `-` through an unsigned type, where C defines wraparound, so
that signed overflow is defined rather than undefined behaviour. The type detoured through is never
narrower than `uint32_t`, because C promotes anything narrower back to signed `int` and the detour
would buy nothing. Division and remainder do not take it, because `MIN / -1` through unsigned changes
the answer rather than wrapping it.
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
