# Lowering: an action tree between the AST and the emitted C

Status: ready-for-agent

## Problem Statement

Every emitter in `transpiler` writes C text straight into an `OutputStream` as it walks a decorated
AST node. Emission is therefore forward-only and write-once: once bytes are in the stream, nothing
can go back and change them, and nothing can be inserted between two statements that were already
written.

That is not a style complaint, it is a capability the compiler is about to need. November's work on
the memory model requires the compiler to emit code the user never wrote — a resource destroyed at
the end of the scope that owns it — and to withdraw such code when a later statement takes ownership
away. Neither operation is expressible against a stream. There is also nowhere to put a rewrite that
spans two statements, and nowhere to inspect what is about to be emitted before it becomes text.

A second, smaller problem follows from the same cause: because emission and byte-writing are the same
act, the only way to test a decision like the unsigned detour is to assert on exact C text. Those
assertions change whenever the spelling changes, even when behaviour does not.

## Solution

A new pipeline stage, **lowering**, sits between decorating and emitting. It walks the decorated AST
and builds an **action tree**: a tree of small data classes, each one standing for a construct in the
C program that is about to be written. The `transpiler` then does nothing but concatenate that tree
into text.

The governing invariant, in the developer's own words:

> AST nodes represent Rhenium source. Actions represent the upcoming C code.

Everything that gets emitted comes from an action. Because a `Block` action owns a mutable, ordered
list of its children, the compiler can append to a scope it is still building, and — once ownership
analysis exists — remove an action it had already planned. Because actions carry plain data and hold
no reference back to the AST, an action can be constructed for code that has no Rhenium source behind
it at all, which is the case the memory model needs.

## User Stories

1. As a compiler developer, I want a representation of the C program that exists before any text is
   written, so that I can inspect and change what is about to be emitted.
2. As a compiler developer, I want a `Block` action that owns a mutable list of child actions, so
   that I can append a statement to a scope that is still open.
3. As a compiler developer, I want to be able to remove an action I have already produced, so that a
   later statement can withdraw code an earlier one planned.
4. As a compiler developer, I want actions to carry plain data rather than AST references, so that I
   can construct an action for C code that has no Rhenium source behind it.
5. As a compiler developer, I want the `transpiler` module to be unable to see AST types at all, so
   that the compiler proves the seam is real instead of my having to trust it.
6. As a compiler developer, I want every semantic decision to live in `lowering` and none in the
   printer, so that there is one place to look when the emitted C is wrong.
7. As a compiler developer, I want the printer to perform lookups but never decisions, so that the
   rule for where a change belongs is unambiguous.
8. As a compiler developer, I want the result-typed evaluation cast to appear in the action tree as
   an explicit cast action, so that the rule is visible in a structure rather than buried in
   string concatenation.
9. As a compiler developer, I want the unsigned detour to appear as nested cast actions, so that a
   future reader can see the detour without reading emitter code.
10. As a compiler developer, I want the redundant double cast the current emitter produces to become
    visible in the action tree, so that a later pass can remove it.
11. As a compiler developer, I want a literal's finished C spelling — the unsigned suffix, the
    `Long.MIN_VALUE` workaround — decided during lowering, so that the printer holds no knowledge of
    C literal syntax.
12. As a compiler developer, I want the `printf` conversion for a printed value resolved during
    lowering, so that the printer does not need to know that types have formats.
13. As a compiler developer, I want the boolean-to-`"true"`/`"false"` rendering expressed as a
    ternary action, so that the printer does not need to know booleans are special.
14. As a compiler developer, I want `main` represented as a function action containing a block, so
    that October's real functions lower into a type that already exists.
15. As a compiler developer, I want an explicit return action, so that the stray trailing semicolon
    the current root emitter produces disappears.
16. As a compiler developer, I want one lowerer class per AST node kind, wired with Dagger, so that
    lowering looks like the decorator walk it parallels and adding syntax stays one ritual.
17. As a compiler developer, I want the action set to be a sealed hierarchy dispatched by a single
    `when` in the printer, so that adding an action kind is a compile error until it is printed
    rather than a forgotten Dagger binding.
18. As a compiler developer, I want lowering to return actions directly rather than a diagnosed
    result, so that the stage's signature states that everything the user can get wrong was already
    caught.
19. As a compiler developer, I want an unhandled node kind in lowering to throw, so that a missing
    case is reported as the compiler bug it is.
20. As a compiler developer, I want to assert on the action tree instead of on emitted C, so that a
    test fails when behaviour changes and not when spelling does.
21. As a compiler developer, I want the existing transpiler test cases carried over to the new seam,
    so that no numeric-semantics coverage is lost in the move.
22. As a compiler developer, I want mangled variable names normalised in test output, so that
    assertions do not depend on a generated UUID.
23. As a compiler developer, I want the numeric width rules asserted as cast actions around
    arithmetic, so that result-typed evaluation is pinned structurally.
24. As a Rhenium programmer, I want `&&` and `||` to stop evaluating once the result is known, so
    that I can guard an expression with a condition that makes it safe.
25. As a Rhenium programmer, I want that short-circuit behaviour written down as a language
    guarantee, so that I can rely on it rather than on what the current compiler happens to do.
26. As a compiler developer, I want the reason expressions stay nested recorded as a decision, so
    that nobody flattens them into temporaries and silently deletes the short-circuit guarantee.
27. As a compiler developer, I want the reason the lowering stage exists recorded, so that a future
    reader does not collapse it back into direct emission as an apparent simplification.
28. As a compiler developer, I want the project glossary to name the new stage and its parts, so that
    the vocabulary is fixed before more code uses it.
29. As a compiler developer, I want the glossary's existing definition of the transpiler corrected,
    so that it does not describe behaviour the code no longer has.
30. As a compiler developer, I want the module graph documentation updated, so that the new stage and
    the dependency that disappeared are both discoverable.
31. As a compiler developer, I want no unused action kinds shipped, so that the module contains only
    what is reachable today.

## Implementation Decisions

### A new module

`lowering` is a new Gradle module holding both the action types and the walk that builds them; the
walk is the only thing that constructs actions, so splitting those apart buys nothing. The resulting
graph:

```
lowering    →  ast, semanticContext
transpiler  →  lowering, semanticContext
app         →  ... + lowering
```

`transpiler` **loses its dependency on `ast`**. That removed edge is the load-bearing part of the
choice: it makes it a compile error for a printer to read an AST node, so the separation is enforced
rather than observed. If the printer still needs `ast`, the lowering is incomplete and Gradle says
so.

`app` gains the stage between decorating and emitting. The existing `either { }` chain in the
compiler is unaffected: lowering cannot fail, so it does not participate in diagnostic accumulation.

### Actions carry data, not nodes

An action holds plain data only — no reference to any AST node. This is forced rather than
stylistic: the cast that implements result-typed evaluation has no AST node behind it, because
nobody wrote it, and neither will a resource destructor. An action set that could only wrap existing
nodes could not represent either.

Types are the one exception to "plain data", and deliberately so: an action carries an
`ExpressionType` from `semanticContext` where it needs a type, and the printer reads that type's C
name. `semanticContext` already owns the mapping from a Rhenium type to its C spelling, and
duplicating it into strings or into a second enum would create a thing to keep in sync. Operators and
literals carry finished C text, because there is no type there to look up.

### The dividing line: decisions in lowering, lookups in the printer

The printer concatenates. It may follow a type to its C name, because that is a lookup with no
judgement in it. It may not decide anything:

- the `printf` conversion for a value is resolved during lowering and carried as text
- the boolean `"true"`/`"false"` rendering becomes a ternary action, so the printer never learns that
  booleans print differently
- a literal's C spelling — the unsigned suffix, the `Long.MIN_VALUE` workaround — is decided during
  lowering and carried as finished text
- the result-typed cast and the unsigned detour become explicit, nested cast actions, and the logic
  that chooses them moves wholesale out of the binary and unary emitters

The reason to draw the line here is that it makes one sentence true and testable: every decision the
compiler makes about the emitted C is in `lowering`, and `lowering` is the only stage with
assertions on it.

### Shape: a nested tree, not a flat list

Actions nest. Expressions stay trees; there is no three-address form, no temporaries, and no
flattening. A `Block` owns an ordered, mutable list of child actions.

This is not merely the smaller change. Rhenium guarantees that `&&` and `||` short-circuit, and
nested actions print as C's own `&&` and `||`, which provide that guarantee for free. Flattening
expressions into temporaries would evaluate both operands and break a language rule silently, and
recovering it would require branch and label actions before `if` exists. See ADR 0002.

### The action set

Statement-shaped: `Block`, `FunctionAction`, `ReturnAction`, `VarDeclarationAction`,
`AssignmentAction`, `ExpressionStatementAction`, `PrintAction`.

Expression-shaped: `BinaryAction`, `UnaryAction`, `CastAction`, `TernaryAction`, `ConstantAction`,
`VarRefAction`.

All of them implement one sealed interface. `main` is a `FunctionAction` whose body is a `Block`
ending in a `ReturnAction`, which is also what removes the stray semicolon the current root emitter
writes before `return 0`. The C prologue — the includes and typedefs — stays in the printer: it is a
fixed string derived from no Rhenium source, so making it an action would gain a type and buy
nothing. That is a decision to revisit when the language stops being this small.

### Class layout

One lowerer per AST node kind, each an `ILowerer`-style interface with a `@Singleton` implementation
and a `@Binds` entry, mirroring the decorator walk it parallels — so adding syntax stays the same
ritual the project already documents, with lowering inserted into it. Mutually recursive lowerers use
the established `dagger.Lazy` field injection idiom.

Printing goes the other way: a single printer with one exhaustive `when` over the sealed action set.
The action set is closed and owned by this project, so exhaustiveness is checked by the compiler,
which is strictly stronger than a Dagger binding that can be forgotten.

### Lowering cannot fail

Lowering returns actions directly, not a diagnosed result. Everything the user can get wrong has
already been caught and reported by the semantic analyzer, so a node lowering cannot handle is a
compiler bug and throws, exactly as an unhandled node kind does elsewhere in the project.

### Short-circuit becomes a documented rule

`&&` and `||` short-circuit. This is currently true only by accident — inherited from C because the
emitter writes the operator through — and is written nowhere. It becomes a stated language guarantee
in the language reference, and ADR 0002 records that expressions staying nested is what satisfies it.

### Documents that are part of the work

- **ADR 0001** — why a stage exists between decorating and emitting: revisiting and inserting
  statements, with the declaration/destructor pair as the motivating case.
- **ADR 0002** — why expressions stay nested: short-circuit is a language guarantee, C's operators
  provide it, flattening to temporaries would remove it silently. This is the one a future reader
  most needs, because the code cannot show it.
- **Glossary** — `Action`, `Lower`, `Block` and `Action tree` added; the existing `Transpiler` entry
  rewritten, since it currently defines a transpiler as reading a decorated AST node and writing C to
  a stream, which stops being true.
- **Architecture notes** — the module graph and the four-file ritual for adding syntax both gain the
  lowering stage.

`docs/adr/` does not exist yet and is created here.

## Testing Decisions

A good test here asserts what the compiler decided, not how the decision was spelled. The current
transpiler tests fail the standard: they compare exact C text, so a change in whitespace or cast
spelling breaks assertions about integer width that have not changed. Moving the assertions onto the
action tree fixes that, because the tree records the decision — a cast action of a given type — and
not its rendering.

### The seam

One new seam, one retired, no net growth. Lowering is tested at its top: decorated AST in, action
tree out. The test drives the real pipeline — parse, decorate, lower — and compares a rendered
action tree against an expected string.

### Prior art

Both halves already exist in this repo and should be followed rather than reinvented:

- the existing transpiler tests already build the parse → decorate → assert pipeline through a
  test-only Dagger component, and failing to parse or decorate already fails the test with rendered
  diagnostics
- the AST builder tests already render a tree to an s-expression and compare strings, precisely
  because the nodes cannot be compared by equality; the same renderer pattern, living in test
  sources with an exhaustive `when` that errors on an unknown kind, applies to actions

### What gets tested

`lowering` is the only module gaining tests. Every case in the existing transpiler test data source
is carried over and re-expressed as an expected action tree, so the numeric coverage — per-type
literal spelling, result-typed evaluation, the unsigned detour, the mixed-signedness rules, the
boolean rendering, print versus println — survives the move. Mangled variable names must be
normalised in the rendered output, as the current tests already do, because they contain a generated
UUID and would otherwise make assertions non-deterministic.

### What does not get tested

The printer ships with no tests. This follows from the decision to keep no C-text assertions and no
compile-and-run tests. It is a knowing trade: the action tree can be asserted correct while the C is
broken by a typo in a format string or a missing separator, and that failure surfaces only when a
program is compiled by hand. See Further Notes.

## Out of Scope

- **`FreeAction` and anything else in the memory model.** The declaration/destructor pair is the
  motivation for this work, not part of it. Nothing about `using`, `take` or `gives` is built here,
  and no action kind is added that today has no caller.
- **Who resolves ownership** — the semantic analyzer or lowering — is deliberately undecided. It is
  the question that determines whether lowering ever needs to revise an action it has already
  produced, and nothing in this spec depends on the answer.
- **An index from source node to actions.** Not built. It is additive whenever a case for it appears,
  and it changes no action type or printer when it does.
- **Rewriting passes of any kind**, including the cast peephole that would remove the redundant
  double cast. The redundancy becomes visible in the action tree here; removing it is separate work.
- **Three-address form and temporaries.** Explicitly rejected, for the short-circuit reason in ADR
  0002.
- **Control flow actions** — branches, labels, loops. October's work. Short-circuit needs none of it,
  because nested actions print as C's operators.
- **Making the C prologue an action.** Stays printer-side while the language is this small.
- **Formatting or readability of the emitted C.** The output stays a single line; nothing here makes
  it prettier or uglier on purpose.
- **A second backend.** The action tree is shaped for C and names its fields after C. Nothing here is
  an attempt at target independence.
- **Compiling and running `.re` programs as a test strategy.**

## Further Notes

### One recorded disagreement

The decision to leave the printer entirely untested was taken knowingly and against advice. The
argument for a handful of golden C assertions is that they are the only thing standing between a
printer typo and a program that fails when a human runs it, and that they would be stable once the
action-to-text mapping settles. The argument against — that assertions on generated text are exactly
what this work exists to get away from — was the developer's, and it wins. Recorded so that the next
person to be bitten by an unprinted separator finds the reasoning rather than an oversight.

### On the shape of the eventual destructor work

Two notes from the design discussion, kept because they will save time in November.

The motivating scenario is a resource declared in a block, destroyed at that block's end, where a
later statement transfers ownership away and the planned destruction must be withdrawn. The
declaration and the transferring statement are in the same block, and the lowerer holds that block
while it walks it — so what that scenario needs is a per-block scratch map from symbol to planned
action, discarded when the block closes. It does not need a global index, which is why one is not
built.

There is also a forward-only variant worth weighing when the time comes: hold a per-block set of live
resources and append destructors when the block **closes**, so a transfer removes an entry from the
set and no action is ever created and then deleted. It produces the same C without any retroactive
edit. Both remain open; the choice belongs with the ownership question above.

### Sequencing

The stage is built now, while the language has five statement kinds and six expression kinds, rather
than after October adds scopes, control flow, functions and structs. Porting roughly twenty-five node
kinds off a stream later is the same work at two and a half times the size, and blocks — which
October introduces — are exactly where the stream model hurts most.
