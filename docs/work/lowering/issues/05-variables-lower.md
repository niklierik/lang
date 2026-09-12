# 05: Variables lower

**What to build:** Declaring a variable, assigning to one, and discarding an expression's value all
go through the action tree. Together with 04 this covers every statement kind the language has.

**Blocked by:** 03

**Status:** resolved

- [x] `let` and `const` declarations lower to a declaration action carrying the declared type and the
      variable's mangled C name
- [x] Assignment lowers to an assignment action whose target is a variable reference action
- [x] An expression statement lowers to an action that discards its value
- [x] Reading a variable lowers to a variable reference action carrying only its mangled name
- [x] A declared boolean still emits the boolean typedef
- [x] The remaining cases in the existing transpiler test data — declaration, assignment, printing a
      variable, discarding a value — are ported to action-tree assertions
