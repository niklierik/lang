# 02: The lowering module skeleton

**What to build:** A new module exists for the lowering stage and the build knows about it. Nothing
lowers yet — this is the prefactor that keeps Gradle and Dagger plumbing out of the same context
window as the lowering logic.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] A `lowering` module is on the shared convention plugin and included in the build
- [x] It depends on the AST and the semantic context, and on nothing else
- [x] It declares a Dagger module of its own, following the project's `@Binds` convention
- [x] A sealed `Action` interface exists, so that later tickets get compiler-checked exhaustiveness
- [x] The module has a test source set with its own Dagger test component, mirroring how the other
      modules' test components are built
- [x] `./gradlew build` is green and no existing behaviour changed
