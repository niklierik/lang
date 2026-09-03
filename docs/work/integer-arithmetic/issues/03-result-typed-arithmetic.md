# Compute arithmetic at the result type's width

Status: resolved
Blocked by: 02

Rhenium resolves `I32 + I32` to `I32`, then emits `(a+b)` and lets C pick the width. [The
spec](../spec.md) makes every arithmetic node emit cast to its own `cName`, and routes signed
`+ - *` and unary `-` through the unsigned counterpart so overflow wraps instead of being undefined.

This is the ticket the roadmap gap names.

## Tasks

- A signed-to-unsigned counterpart property on `SignedIntType`
- `CBinaryOpTranspiler`: cast the result to the node's `cName`; for signed `+ - *`, cast both
  operands to the unsigned counterpart first. Leave `/` and `%` without the detour — routing
  `MIN / -1` through unsigned changes the answer rather than wrapping it
- `CUnaryOpTranspiler`: the same for unary `-` on a signed type. `!` and unary `+` are unaffected
- Cases per family: signed `+ - * / %`, unsigned, float, unary `-`, and a nested expression showing
  the casts compose

## Done when

`./gradlew :transpiler:test` pins the emitted shape for every family, and the gap entry in
`docs/roadmap.md` is replaced by the division-by-zero entry.

## Not covered

A string assertion cannot show that `I32(2147483647) + I32(1)` evaluates to `-2147483648`. Verify
that once by hand through `clang` and record it; the build does not prove it.
