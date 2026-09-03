# Reject `%` on floats

Status: ready-for-agent

`arithmeticType` handles `PERCENT` exactly like `STAR`, so `F64(1.0) % F64(2.0)` type-checks and
emits `(1.0%2.0)`. C has no `%` for floating point, so clang rejects it: `invalid operands to binary
expression ('double' and 'double')`. Analysis accepting a program whose emitted C does not compile is
a compiler bug by this repo's doctrine, not bad input.

`fmod` would link today, since `-lm` is already on the `clang` line. [The spec](../spec.md) rejects
it instead rather than commit the language to float modulo semantics as a side effect of a bug fix.

## Tasks

- `%` with a float on either side raises `IllegalBinaryOperation` — no new diagnostic type
- Cases for `F32`, `F64` and a mixed int/float pair; one pinning that integer `%` still resolves

## Done when

`F64(1.0) % F64(2.0)` produces a diagnostic, and `I32(1) % I32(2)` still resolves to `I32`.
