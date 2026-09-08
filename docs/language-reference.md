# Language reference

## Entry points and projects

```
entry Main {
    Console.WriteLine(f"{a} + {b} = {sum}");
}
```

Several `entry` blocks may live in one file. `Project.json` picks one with `"DefaultEntry"`, and a
`#Default` pragma marks one in-file. `Project.json` also carries `Name`, `Version`, `Src`, `Res`,
`Dependencies` and `AllowC`. Files declare a namespace with `namespace Rhenium.Examples.Game;`.

## Functions

Kotlin-shaped, and they need no enclosing class:

```
fun Sum(a: I32, b: I32): I32 {
    return a + b;
}
```

Omitting `: ReturnType`, or writing `Void`, means the function returns no value.

## Variables

TypeScript-shaped, `let` mutable and `const` immutable, with the type optional when inferable:

```
let mutableVar: I32 = 0;
const inferred = 0;
```

`const` immutability is **shallow**: the binding cannot be reassigned, but an object it holds can
still be mutated. A `const` binding is not a valid l-value.

## Types

`I8` / `I16` / `I32` / `I64`, `U8` / `U16` / `U32` / `U64`, `F32` / `F64`, `Boolean`, `Character`
(`'a'`), plus `String` (ASCII only), `Ref<T>`, `Ptr<T>`, `Optional<T>` and `Array<T>`.

Strings interpolate with `f"...{expr}..."`. `nameof(x)` and `address(x)` are built in.

## Numeric semantics

Arithmetic is evaluated at the width of its own result type, not at whatever width the underlying C
would promote its operands to. `I8 + I8` is an `I8` operation and wraps at eight bits.

Signed integer overflow wraps, two's complement, as unsigned overflow always has. It is not
undefined.

`%` is integer-only; applying it to a float is a diagnostic. Division by zero and `-2147483648 / -1`
are undefined.

The operands of a binary operator must agree in signedness — `I32 + U32` and `I32 < U32` are both
diagnostics rather than silent conversions — and the explicit cast that would let a program opt into
one does not exist yet. Comparison is included because C converts the signed operand to unsigned
there, which makes `-1 < 1` come out false. Mixing an integer with a float is allowed and yields the
float type.

A literal's width follows the type it is written as, so `I32(-32)` is an `I32`. A bare integer
literal is an `I32` and a bare decimal literal an `F64`. A literal whose value does not fit its type
is a diagnostic, including a float literal large enough to overflow to infinity.

## Operator precedence

Highest to lowest, left-associative within a row, following C#:

| Operators | Category |
| --- | --- |
| symbol, literals, `object.property`, `array[index]`, `objectOrNull?.property`, `function(arg)` | Primaries |
| `+x`, `-x`, `!x` | Unary |
| `x ^ y` | Pow |
| `x * y`, `x / y`, `x % y` | Multiplicative |
| `x + y`, `x - y` | Additive |
| `x < y`, `x > y`, `x <= y`, `x >= y` | Relational |
| `x as Type`, `x is Type` | Type-based |
| `x == y`, `x != y` | Equality |
| `x && y`, `x \|\| y` | Conditional |
| `nullable ?? default` | Null-coalescing |
| `if (condition) ifTrue else ifFalse` | Conditional expression |

Parentheses override precedence as usual.

## Assignment

Assignment is **not** an expression. It may appear once and yields no value, so `a = b = c` is
rejected. The forms are `=`, `+=`, `-=`, `*=`, `/=`, `%=` and `??=`, plus `x++` and `x--`.

All of them require an l-value: a primary other than a function call or an optional access
(`object?.field`).

## Control flow

`if` / `else if` / `else`, `while`, C-style `for (init; condition; increment)` and
`foreach (const item of collection)`, with `break` and `continue`.

**Braces are mandatory** — a braceless `if (condition) x = y;` is an error. There is no
`do`-`while`; `loop` and `repeat` are listed as planned.

## Objects

Structs live on the stack, are copied when passed, and have no owner to manage. Classes live on the
heap and their ownership must be managed — see the [memory model](memory-model.md).

**There is no inheritance**, by the composition-over-inheritance principle; interfaces provide
polymorphism. Classes take constructor-style parameters and may declare fields, methods,
`property` / `readonly property`, an `init { }` block and a `delete { }` block. `mutable` marks a
mutable field, `mutate fun` a method that mutates the receiver, and `group` groups statics.

Generics are planned as bare type parameters with no constraints. Union types and pattern matching
are explicitly optional, only if time allows.
