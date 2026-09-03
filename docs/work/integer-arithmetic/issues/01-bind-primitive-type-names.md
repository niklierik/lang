# Bind each primitive type name to its own type

Status: resolved

`GlobalScope.insertPrimitives()` binds `I32`, `I16` and `I8` to `SignedIntType.I64`, so a declared
`let a: I32 = 0;` produces an `int64_t`. `U64` is inserted twice — once to `SignedIntType.I64`, then
again to `UnsignedIntType.U64`, which is what makes the defect invisible for that one name.

Nothing else in [the spec](../spec.md) can be tested until a declared type is the type that was
declared.

## Tasks

- Bind `I32`, `I16`, `I8` to their own `SignedIntType` entries; delete the duplicate `U64` insert
- A test that every primitive name resolves to the type it spells

## Done when

`let a: I32 = 0;` emits `int32_t`, and a test would fail if any name were rebound.
