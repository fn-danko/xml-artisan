# CLAUDE.md — XML Artisan

## Build & Test Commands

```bash
./gradlew build          # Compile + test
./gradlew test           # Run tests only
./gradlew test --tests "net.fndanko.xml.artisan.SelReadTest"  # Single test class
./gradlew test --tests "*.SelReadTest.attr_*"                 # Test name pattern
```

- Java 17 (toolchain), Gradle build system
- JUnit 5 (Jupiter) for testing
- Zero external runtime dependencies — only standard JVM APIs (DOM, XPath, Transformer)

## Architecture

```
XML              → Entry point, wraps Document + XPath engine + namespace context
Sel              → Selection of 0..N DOM nodes, fluent batch operations
Node extends Sel → Single-node selection, DOM navigation, structural ops return new Node
BoundSel<T>      → Sel + data list, produced by Sel.data(); performs enter/update/exit join
JoinConfig<T>    → Immutable join configuration (enter/update/exit handlers, builder pattern)
JoinedSel<T>     → Sel + datum map, produced by BoundSel.join(); data-aware post-join ops
OutputOptions    → Immutable serialization config (builder pattern)
```

**Layering:** Public API → Data binding (join lifecycle) → XPath layer (context eval, `//` rewrite, namespaces) → DOM wrapper → `javax.xml.*`

### Key internal state

- `XML`: holds `Document`, `XPath` instance, `Map<String,String>` namespace prefixes
- `Sel`: holds `List<org.w3c.dom.Node>`, `Sel parent`, `XML owner`
- `Node`: wraps single `org.w3c.dom.Node` (or null for `EMPTY` singleton)
- `BoundSel<T>`: holds `List<T> data`, optional `Function<T,String> dataKey`, optional `Function<Node,String> nodeKey`
- `JoinConfig<T>`: holds enter/update/exit handlers + explicit-set flags; `defaults(tag)` sets all three
- `JoinedSel<T>`: holds `Map<org.w3c.dom.Node, T> datumMap` for data-aware operations

## Design Decisions

- **Fluent chaining** — all methods return `this` (Sel) or new node (Node structural ops); never returns null
- **Null Object Pattern** — `Node.EMPTY` singleton and empty `Sel` are silent no-ops; no exceptions during chaining
- **`Node extends Sel`** — a single node is a selection of cardinality 1; LSP compliant
- **Sel structural ops** (`append`/`prepend`/`before`/`after`) return original `Sel` for batch side-effects; **Node structural ops** return new `Node` for fluent construction
- **`replace()`** is the exception: always returns the new nodes (both on Sel and Node)
- **XPath `//` rewrite** — in sub-selections (`sel.sel()`, `node.sel()`), leading `//` is rewritten to `.//` for contextual search
- **`remove()` returns parent** — uniquely among Sel methods, for backtracking after deletion
- **`end()` on root** returns self (no exception)
- **Only permitted exceptions:** malformed XPath → RuntimeException, bad XML → unchecked wrapping SAXException, missing file → UncheckedIOException

## Test Conventions

- **Test-first approach**: stubs → tests → implementation
- **AAA pattern**: Arrange / Act / Assert in every test
- **Naming**: `<action>_<condition>_<expectedResult>()` (e.g., `attr_onEmptySel_returnsEmptyString`)
- **Verify via public API**: use `.get()`, `.sel().size()`, `.attr()`, `.text()` — not implementation internals
- **13 test suites (v1.0)** ordered by dependency:
  1. `XMLEntryPointTest` — factory methods, get/set, namespace
  2. `SelReadTest` — attr, text, size, empty
  3. `SelModifyTest` — attr/text set, remove
  4. `SelTransformTest` — attr/text with Function
  5. `SelNavigationTest` — sel, end, sub-selections
  6. `SelStructuralTest` — append/prepend/before/after/replace on Sel
  7. `SelIterationTest` — stream, list, first, last, Iterable, order
  8. `NodeNavigationTest` — children, parent, name
  9. `NodeStructuralTest` — structural ops returning Node
  10. `NodeAsSelTest` — Node used as Sel (LSP)
  11. `SerializationTest` — toString, toFragment, writeTo, OutputOptions
  12. `XPathTest` — contextual XPath, rewrite, namespaces
  13. `ResilienceTest` — empty Sel/Node, chaining safety
- **5 test suites (v1.1 data binding):**
  14. `DataBindingTest` — positional and key-based matching, enter/update/exit classification
  15. `JoinShorthandTest` — `.join("tag")` shorthand, defaults behavior
  16. `JoinConfigTest` — builder, defaults, explicit null overrides, handler order, custom handlers
  17. `JoinedSelTest` — `attrWith`, `textWith`, `eachWith`, `toSel`, `sel`, `order`, chaining
  18. `JoinAdvancedTest` — multi-parent grouping, insertion order, repeated joins, mixed types, full lifecycle

## Data Binding (v1.1)

D3.js-style enter/update/exit join lifecycle via `Sel.data()` → `BoundSel.join()` → `JoinedSel`.

- **Type flow:** `Sel` → `.data(list)` → `BoundSel<T>` → `.join(config)` → `JoinedSel<T>` → `.toSel()` → `Sel`
- **Matching:** positional (by index) or key-based (via `dataKey`/`nodeKey` functions)
- **Per-parent grouping:** nodes with different parents form separate groups; each group independently joins against the full data array (D3 semantics)
- **Handler order:** exit → update → enter
- **Handler resolution:** `JoinConfig.defaults("tag")` sets enter=append, update=identity, exit=remove; individual `.enter()`/`.update()`/`.exit()` override selectively; explicit `null` means "do nothing" (distinct from "not set")
- **Merge ordering:** index-based `slots[]` array ensures merged list follows data order; `order()` reorders DOM to match
- **Post-join ops:** `attrWith`, `textWith`, `eachWith` apply data-aware transforms to merged (enter+update) nodes

## Package

`net.fndanko.xml.artisan` — all public types in a single flat package.
