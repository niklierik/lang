# Reject float literals that overflow to infinity

Status: resolved

`parsedAs` in `LiteralVisitor` turns a `NumberFormatException` into `InvalidValueOfLiteral`, which is
why integer overflow is caught: `Integer.parseInt` throws. Float parsing does not — `Float.parseFloat`
returns `Infinity` — so the literal is built holding a non-finite value, and emission renders
`literal.value`, producing `Infinityf`. clang: `use of undeclared identifier 'Infinity'`.

The lexer's `FLOAT` rule has no exponent part, so reaching it takes around forty literal digits.
[The spec](../spec.md) closes it anyway, as the same hole as `%` on floats.

## Tasks

- Check finiteness after parsing a float literal, raising the existing `InvalidValueOfLiteral`
- Cases for `F32` and `F64` overflow, and one pinning that a normal float literal still parses

## Done when

A float literal too large for its type produces a diagnostic instead of emitting `Infinity`.
