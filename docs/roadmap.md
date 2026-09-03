# Roadmap and status

## Schedule

From `plan.md` on the `plans` branch. MSc thesis project, defense planned for spring 2027.

| When | Work |
| --- | --- |
| September 2026 | expressions, variables with types, temporary printing, type analysis, C generation infrastructure |
| October 2026 | scopes, `if` / `else if` / `else`, `for`, `while`, functions, structs |
| November 2026 | interfaces, classes, `take` / `gives`, `using` |
| December 2026 | generics, C interop, standard library |
| January 2027 | example program (Battleship), fixing what it uncovers |
| February–May 2027 | writing the thesis |

## What the compiler implements today

Literals, unary and binary arithmetic, relational, equality and logical operators, grouping,
`let` / `const` declarations with an optional declared type, assignment, expression statements, the
type rules over those, and a transpiler that emits every statement into a single C `main()`.

`print` and `println` write a value to standard output. They are a placeholder for the `Console` of
[the standard library](standard-library.md), reserved keywords rather than calls because the language
has neither functions nor strings yet, and they are removed when `Console` lands. See
[docs/work/print/spec.md](work/print/spec.md).

Everything else in these documents is unbuilt.

## Known gaps worth knowing before you start

- `^` (Pow) exists as a lexer token and has type rules in `BinaryOpNodeDecorator`, but **no parser
  rule** — it cannot be written in a program yet.
- There is no `entry`, no `namespace` and no `Project.json` handling. The compiler takes a single
  `.re` file path on the command line and compiles that.
- Relational and equality operators still accept mixed-sign operands, so `I32(-1) < U32(1)` is
  `false` — C converts the signed operand to unsigned. Arithmetic rejects the same pairing; the
  comparison case was left open deliberately and is not designed yet.
- Division by zero and `MIN / -1` are undefined. Diagnosing them needs a way to fail at runtime, and
  there is no `if`, no panic and no `Result` yet. See
  [docs/work/integer-arithmetic/spec.md](work/integer-arithmetic/spec.md).
