# Integer arithmetic width

## Why this exists

`docs/roadmap.md` records the symptom: integer literals are emitted with C suffixes wider than their
own type, so an `I32` literal becomes `-32l`. The cause is broader than the suffix table. Rhenium
resolves an arithmetic node to a Rhenium type and then emits C that computes at whatever width C's
own rules pick, which is not the same width. The two disagree, and where they disagree the emitted
program is wrong.

Three further defects share that shape and are closed here, because each one lets analysis accept a
program whose emitted C is wrong or does not compile at all — a class the repo treats as a compiler
bug rather than bad input.

## Decisions

### Result-typed evaluation

An arithmetic node computes at the width of **its own type**, not at the width C would promote its
operands to. Every arithmetic node is emitted cast to its type's `cName`.

The alternative — letting C's integer promotions stand — cannot express `I8` and `I16` arithmetic at
all. C promotes anything narrower than `int` to `int` before operating, whatever suffix the operands
carry, so four of the ten numeric types would compute at a width the language never asked for and no
change to the suffix table could fix it.

### Signed overflow wraps, via the unsigned detour

Signed integer overflow is undefined behaviour in C, which means clang's optimizer is entitled to
assume it never happens. Rhenium promises two's-complement wraparound instead, bought by emitting
signed arithmetic through the corresponding unsigned type, where wraparound is defined by the C
standard:

```
(int32_t)((uint32_t)(a)+(uint32_t)(b))
```

The final unsigned-to-signed conversion is implementation-defined before C23 and defined from C23
on; clang implements it as two's-complement in both cases. The undefined behaviour — the part the
optimizer acts on — is gone.

`-fwrapv` on the `clang` line would buy the same guarantee for one flag, and was rejected: it makes
the guarantee a property of how the compiler is invoked rather than of the C that is emitted.

The detour applies at every signed width, including `I8` and `I16` where C's promotion to `int`
already makes overflow unreachable. The redundant casts fold away in clang, and the rule stays one
sentence instead of a width-conditional special case.

The detour applies to `+`, `-`, `*` and unary `-`. It does **not** apply to `/` and `%`: signed
division overflows only at `MIN / -1`, and routing that through unsigned produces a different
answer rather than a wrapped one.

### Literals carry a cast, not a suffix

Every numeric literal is emitted as `(cName)digits`, replacing the per-kind suffix table. Unsigned
types keep a `u` suffix on top of the cast.

The cast makes the C type of the constant follow from the Rhenium type by construction, with no
assumption about whether `int32_t` is `int` on the host. The `u` is not redundant with it:
`(uint64_t)18446744073709551615` produces the right value but warns
`-Wimplicitly-unsigned-literal`, because C's ladder for an unsuffixed decimal constant is
int → long → long long and never reaches unsigned. `u` moves the constant into the unsigned ladder.
It is applied to every unsigned type rather than to `U64` alone, so the rule needs no width table.

### Mixed-sign arithmetic is rejected

`arithmeticType` currently falls through its same-family checks into `if (left is SignedIntType)
return left`, so `I32 + U32` resolves to `I32` silently. There is no reading of that which is not a
trap; C's own answer — unsigned wins, quietly — is the canonical example of the footgun.

Mixed-sign arithmetic now raises a diagnostic. The escape hatch is an explicit cast, and `AS` is a
lexer token with no parser rule, so no cast syntax exists yet: until it does, mixed-sign arithmetic
is unwritable rather than silently wrong. Mixed int/float arithmetic stays implicit and resolves to
the float type, which is what the wider type means.

### `%` is integer-only

`arithmeticType` handles `PERCENT` exactly like `STAR`, so `F64 % F64` type-checks and emits
`(1.0%2.0)`, which clang rejects: `invalid operands to binary expression`. `%` on a float now raises
`IllegalBinaryOperation`.

Emitting `fmod` instead would link today, since `-lm` is already on the `clang` line. It was
rejected because it commits the language to float modulo semantics as a side effect of a bug fix,
and `docs/language-reference.md` has never claimed them.

### Non-finite float literals are rejected

`parsedAs` catches `NumberFormatException`, which is what integer overflow raises — so
`I32(99999999999)` correctly produces `InvalidValueOfLiteral`. Float parsing does not throw:
`Float.parseFloat` returns `Infinity` on overflow, so the literal is built with a non-finite value
and emission renders it as `Infinityf`, which clang rejects with `use of undeclared identifier`.
Float literals are now checked for finiteness and raise the same existing diagnostic.

The lexer's `FLOAT` rule has no exponent part, so triggering this takes around forty literal digits.
It is closed anyway: it is the same "analysis accepts, emission does not compile" hole as `%` on
floats, and leaving one of a pair open is the inconsistent outcome.

## Emitted C

A cast wraps an operand only where one is being applied, so the detour is visible and nothing else
gains parentheses it does not need.

| Source | Emitted |
| --- | --- |
| `I32(32)` | `(int32_t)32` |
| `U32(32)` | `(uint32_t)32u` |
| `U64(18446744073709551615)` | `(uint64_t)18446744073709551615u` |
| `F32(1.5)` | `(float32_t)1.5` |
| `I32(1) + I32(2)` | `(int32_t)((uint32_t)((int32_t)1)+(uint32_t)((int32_t)2))` |
| `-I32(5)` | `(int32_t)(-(uint32_t)((int32_t)5))` |
| `I32(6) / I32(2)` | `(int32_t)((int32_t)6/(int32_t)2)` |
| `U32(1) + U32(2)` | `(uint32_t)((uint32_t)1u+(uint32_t)2u)` |
| `F64(1.5) + F64(2.5)` | `(float64_t)((float64_t)1.5+(float64_t)2.5)` |
| `I32(1) < I32(2)` | `((int32_t)1<(int32_t)2)` |

Relational, equality and logical operators are untouched: they yield `Boolean` and perform no
arithmetic whose width could be wrong.

The detour needs a signed-to-unsigned counterpart for each width, which lands as a property on
`SignedIntType` in `semanticContext` beside `cName` and `cFormat`.

## The primitive type names

None of the above is testable first. `GlobalScope.insertPrimitives()` binds `I32`, `I16` and `I8` —
and a first, immediately overwritten `U64` — to `SignedIntType.I64`, so `let a: I32 = 0;` declares
an `int64_t` today. Every width test would pass or fail for a reason unrelated to what it asserts
until that is fixed, so it is the first ticket.

## What is not verified by the build

Per the decision recorded in the tickets, the tests assert emitted C text only. A string assertion
pins the shape of the emission but cannot show that `I32(2147483647) + I32(1)` evaluates to
`-2147483648` at runtime. That guarantee is verified by hand through `clang` once, the way the print
work was, and is not covered by `./gradlew build`.

## Known gaps left open

Division by zero and `MIN / -1` are both undefined in C and remain undefined in Rhenium. Diagnosing
them needs a way to fail at runtime — there is no `if`, no panic and no `Result`, and the standard
library's error handling is a December milestone. Recorded in `docs/roadmap.md`.
