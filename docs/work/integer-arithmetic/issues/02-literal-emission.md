# Emit numeric literals as a cast, not a suffix

Status: ready-for-agent
Blocked by: 01

`CLiteralExpressionTranspiler` emits a fixed per-kind suffix. Two rows are wrong: `I32` emits `l` and
`U32` emits `lu`, both 64-bit on LP64, so an `I32` literal enters a C expression as a `long`. The
`I64` and `U64` rows are merely redundant.

[The spec](../spec.md) replaces the table with `(cName)digits`, plus a `u` on every unsigned type.
The `u` is not redundant with the cast: without it `(uint64_t)18446744073709551615` warns
`-Wimplicitly-unsigned-literal`, because C's unsuffixed decimal ladder never reaches unsigned.

## Tasks

- Rewrite the ten numeric branches as `(cName)digits`, with `u` appended for unsigned types
- Update the existing `CTranspilerTests` cases, whose expected strings all carry the old suffixes
- A case for `U64` at its maximum, which is the literal the `u` exists for

## Done when

Every numeric literal emits its own `cName` as a cast, and no case in the suite still expects `l`,
`ll`, `lu` or `llu`.
