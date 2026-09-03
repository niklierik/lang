# Reject mixed-sign arithmetic

Status: resolved

`arithmeticType` falls through its same-family checks into `if (left is SignedIntType) return left`,
so `I32(1) + U32(1)` resolves to `I32` with no diagnostic. C's own rule here — unsigned wins,
silently — is the canonical footgun, and [the spec](../spec.md) rejects the operation instead.

`AS` is a lexer token with no parser rule, so there is no cast to point the user at yet. Mixed-sign
arithmetic is unwritable until there is, which is the honest state.

Mixed int/float arithmetic is unaffected and still resolves to the float type.

## Tasks

- A diagnostic under `semanticAnalyzer/diagnostics/`, one public type in its own file
- `arithmeticType` raises it when one side is signed and the other unsigned
- Cases for both operand orders, and one pinning that int/float mixing still resolves to the float

## Done when

`I32(1) + U32(1)` produces one diagnostic naming both types, and `I32(1) + F64(1.0)` still resolves
to `F64`.
