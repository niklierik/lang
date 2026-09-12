# Expressions stay nested in the action tree

Rhenium guarantees that `&&` and `||` stop evaluating once the result is known. Actions nest, so a
binary action is emitted as C's own `&&` or `||` and the C standard provides that guarantee for
free — which is why the action tree has no three-address form, no temporaries, and no flattening of
expressions into a linear sequence.

This is the decision most worth recording, because the code cannot show it. A nested action tree
looks like an arbitrary choice, and flattening it into temporaries looks like a tidy-up; it would in
fact evaluate both operands of every `&&` and silently delete a language guarantee. Recovering
short-circuiting from a flattened form requires branch and label actions, which the language has no
use for until `if` exists.

## Considered options

Three-address form was considered seriously and rejected. Its real attraction was that a temporary's
declared type *is* result-typed evaluation, which would have made the numeric width rules structural
instead of textual. That gain is not worth breaking short-circuiting for, and most of it is had
anyway from making the casts explicit actions.

## Consequences

Result-typed evaluation and the unsigned detour remain casts — explicit, nested `CastAction`s
produced during lowering rather than text assembled by an emitter. They are visible and assertable,
but they are still casts, so the redundant double cast the old emitter produced survives this change.
It is now visible in the action tree, which is what makes removing it possible later.

The guarantee is not observable yet: `&&` and `||` have no type rule in the semantic analyzer, so
every use of them is currently rejected (see the known gaps in
[the roadmap](../roadmap.md)). This decision is recorded now anyway, because lowering is where the
guarantee would be lost and lowering is being built now — by the time the type rule lands, the shape
that preserves short-circuiting is already in place.

Anyone reaching for temporaries should read this file first.
