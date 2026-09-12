# Lower the AST to an action tree before emitting C

The transpiler used to walk the decorated AST and write C text straight into an output stream, which
made emission forward-only and write-once: nothing could be inserted between two statements already
written, and nothing already written could be withdrawn. The memory model needs both — a resource is
destroyed at the end of the scope that owns it, which is code the user never wrote, and a later
`take` withdraws a destruction the compiler had already planned. So a **lowering** stage now sits
between decorating and emitting, building an **action tree** whose `Block` actions own mutable lists
of children, and the transpiler was reduced to concatenating that tree into text.

## Considered options

Keeping the stream and buffering statements separately was rejected: it is the same thing with a
worse name, and it leaves the emitters deciding what the C looks like.

Representing an action as a wrapper around an AST node was rejected because the constructs that
motivate the stage have no AST node behind them. The cast that implements result-typed evaluation was
invented by the emitter, and a destructor call is invented by the compiler; neither can be
represented by wrapping something the user wrote. Actions therefore carry plain data — with
`ExpressionType` as the single exception, since it already owns the mapping to a C spelling and
duplicating that into strings would create something to keep in sync.

## Consequences

`transpiler` no longer depends on `ast`, and that missing edge is deliberate: it makes reading an AST
node from a printer a compile error rather than a convention. Every decision about the emitted C is
now made in `lowering`, and the printer performs lookups only — which matters because `lowering` is
the only one of the two with tests on it.

The emitted C carries more redundant parentheses and casts than before, because the printer
parenthesises every composite action unconditionally instead of tracking what its parent already
wrapped. That is the cost of a printer that makes no decisions. The redundancy is now visible in the
action tree, so a peephole pass can remove it; none exists yet.

Ownership resolution is deliberately not placed yet — it may belong to the semantic analyzer, where
diagnostics live and where leak detection must report, or to lowering. That choice decides whether
lowering ever revises an action it has already produced, and nothing built so far depends on it.
