# 01: Record the decisions

**What to build:** The reasoning behind the lowering stage is written down before the code exists, so
that an agent picking up any later ticket cold finds the decisions rather than re-deriving them — and
so that the one decision the code cannot show (why expressions are not flattened) is discoverable.

Docs only. No code changes.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] `docs/adr/` exists
- [x] ADR 0001 records why a stage exists between decorating and emitting: the compiler must be able
      to insert code the user never wrote, and to withdraw code it had already planned, neither of
      which a forward-only stream permits. The declaration/destructor pair is the motivating case
- [x] ADR 0002 records why expressions stay nested: `&&` and `||` short-circuit, C's operators
      provide that for free, and flattening expressions into temporaries would remove a language
      guarantee silently. It states explicitly that three-address form was considered and rejected
- [x] `CONTEXT.md` gains `Action`, `Action tree`, `Lower` and `Block`, each with the `_Avoid_` list
      the file's other entries carry
- [x] `CONTEXT.md`'s `Transpiler` entry no longer claims a transpiler reads a decorated AST node
- [x] `docs/language-reference.md` states that `&&` and `||` stop evaluating once the result is known
- [x] The module graph in `CLAUDE.md` is NOT touched here — it describes built state and belongs to 06
