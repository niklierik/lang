# 04: Expressions lower with explicit casts

**What to build:** Every operator the language has lowers into nested actions, with the numeric width
rules expressed as explicit casts rather than as text assembled during emission. After this ticket
the numeric semantics are visible in a structure that can be asserted, and the redundant double cast
the current emitter produces becomes visible rather than hidden.

**Blocked by:** 03, 01

**Status:** resolved

- [x] Binary and unary operators lower to nested actions; expressions are NOT flattened into
      temporaries (see ADR 0002 — this is a language guarantee, not a preference)
- [x] Result-typed evaluation is expressed as a cast action wrapped around each arithmetic action
- [x] The unsigned detour is expressed as cast actions around the operands, taken only by the
      operators that take it today, and never narrower than the 32-bit unsigned type
- [x] Division and remainder do not detour
- [x] Relational, equality and logical operators lower without a result cast, as they do today
- [x] The detour and result-cast decisions no longer live in any emitter
- [x] Every numeric case in the existing transpiler test data is ported to an action-tree assertion,
      covering each integer and float type, the most negative signed value, the widest unsigned
      value, nested arithmetic, and unary plus and minus
