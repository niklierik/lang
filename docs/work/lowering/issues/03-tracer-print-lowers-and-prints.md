# 03: Tracer — a print statement lowers and prints

**What to build:** The first complete path through the new stage. Printing a value goes decorated AST
→ action tree → C text, and the C it produces is equivalent to what the stream emitter produces
today. The stream path is left in place and still drives compilation, so nothing regresses.

**Blocked by:** 02

**Status:** resolved

- [x] Printing a literal, printing with and without a line break, and a bare line break all lower to
      an action tree
- [x] A printed boolean lowers to a ternary action, so that the printer never learns booleans are
      special
- [x] The `printf` conversion is resolved during lowering and carried as text
- [x] A literal's finished C spelling is decided during lowering, including the unsigned suffix and
      the most-negative-integer workaround; the cast around it is a separate, visible cast action
- [x] `main` is a function action whose body is a block ending in a return action
- [x] A single printer turns the action tree into C via one exhaustive `when`, and makes no decisions
- [x] Lowering returns actions directly rather than a diagnosed result, and throws on a node kind it
      does not handle
- [x] Tests assert the rendered action tree as a string, with mangled variable names normalised
- [x] The existing stream emitters and their tests still pass untouched
