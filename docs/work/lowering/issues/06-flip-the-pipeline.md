# 06: Flip the pipeline and delete the stream emitters

**What to build:** Compiling a Rhenium program runs decorate → lower → print, and the old
forward-only emitters are gone. This is the contract half of the expand–contract sequence: until now
both paths existed, and after this only one does.

**Blocked by:** 04, 05

**Status:** resolved

- [x] Compiling a program routes through the lowering stage and produces a working binary
- [x] The per-node stream emitters, the node-emitter interface and the golden C-text tests are deleted
- [x] The transpiler module no longer depends on the AST module, and the build enforces it
- [x] No module retains a dependency it no longer uses
- [x] `CLAUDE.md` documents the lowering stage in the module graph, the pipeline description and the
      list of places adding new syntax must touch
- [x] The full test suite is green
- [x] A sample program compiles with clang and runs, confirming the emitted C is still valid — this
      is a manual check, not a test, per the decision to keep no C-text or compile-and-run tests
