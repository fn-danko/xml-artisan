# XML Artisan — Design Document

> Vision, principles, and design decisions behind the library.

**Version:** 0.4

---

## Table of Contents

1. [Goals and Motivations](#goals-and-motivations)
2. [Inspirations](#inspirations)
3. [Design Principles](#design-principles)
4. [Key Decisions and Rationale](#key-decisions-and-rationale)
5. [Comparison with Existing Solutions](#comparison-with-existing-solutions)

---

## Goals and Motivations

The standard Java API for XML manipulation (`javax.xml.parsers`, `org.w3c.dom`, `javax.xml.xpath`) is powerful but extremely verbose. Common operations like selecting nodes, modifying attributes, or synchronizing data with an XML structure require dozens of lines of boilerplate.

XML Artisan sets out to:

- Provide a fluent, concise API for reading, modifying, and creating XML documents.
- Introduce the concept of **selections** inspired by D3.js, with support for data binding through the **join** pattern (enter/update/exit).
- Rely entirely on standard JVM APIs (DOM, XPath) with no external dependencies.
- Draw a clear line between **direct manipulation** (immediate, mutable) and **data synchronization** (lazy, declarative).
- Never break the flow of chaining: empty selections, missing results, and operations on empty sets are handled silently as no-ops.

---

## Inspirations

### D3.js — The selection system

At the heart of XML Artisan lies the [D3.js selection system](https://d3js.org/d3-selection), and specifically the [join pattern](https://d3js.org/d3-selection/joining). D3 demonstrated that the selection + data join model is remarkably powerful for synchronizing data with a node structure. XML Artisan brings this paradigm to the Java/XML world.

Key lessons taken from D3:

- **The unified join pattern** (`.join()`) as an evolution of the earlier explicit enter/update/exit split. D3 itself evolved toward `.join()` as the primary API because it is simpler and less error-prone.
- **Merging enter and update** as the result of the join, allowing common operations to be applied to both groups.
- **Positional insertion order** during enter: new nodes are inserted at the position corresponding to their data, not appended at the end of the parent.
- **Per-parent grouping**: if the selection contains nodes with different parents, the join operates on each parent group independently.

### jQuery — Fluent chaining

The fluent chaining API and the `.end()` method for backtracking to previous selections are inspired by jQuery. The philosophy of operating on sets of nodes with batch operations (`.attr()`, `.text()` applied across the entire selection) also comes from this model.

---

## Design Principles

### 1. Two modes, one entry point

The `XML` object is the starting point. Whether `.data()` is called or not determines whether you are in direct mode (immediate) or binding mode (lazy/declarative). The user does not have to choose a "mode" up front — their intent guides them naturally.

### 2. Direct = immediate

Operations on selections without data binding act on the underlying DOM immediately. `.attr("class", "active")` modifies the DOM the moment it is called. This is the intuitive behavior for anyone doing point modifications.

### 3. Binding = lazy

After `.data()`, you enter a declarative world. Operations are accumulated and applied only when `.join()` is called. This is necessary because the data join needs the full picture (enter, update, exit) before it can touch the DOM.

### 4. Everything is a selection

`Node` is a specialization of `Sel` (a selection of cardinality 1). This means a single node supports all selection operations, and the library works internally with selections throughout. The practical consequence is that you move fluidly between single-node operations and batch operations.

### 5. Different types for different semantics

`Sel`, `BoundSel<T>`, `JoinedSel<T>`, and `JoinConfig<T>` are distinct types. The compiler guides the user: you cannot call `.join()` without first calling `.data()`, and you cannot call `.attrWith()` on a `Sel` that did not come from a join. Errors surface at compile time, not at runtime.

### 6. Contextual XPath

Sub-selections (`.sel()` called on a `Sel`) evaluate the expression relative to the nodes of the parent selection. Expressions starting with `//` are automatically treated as `.//` in sub-selections, avoiding the counter-intuitive behavior of standard XPath.

### 7. Zero external dependencies

Only standard JVM APIs (`javax.xml.parsers`, `org.w3c.dom`, `javax.xml.xpath`, `javax.xml.transform`). No transitive dependencies.

### 8. Never break the chain

No operation throws exceptions during chaining. Empty selections, XPath queries with no results, operations on empty sets — everything is handled as a no-op with sensible defaults (empty strings, empty selections, empty Nodes). The only exceptions are for non-recoverable programming errors, with specific types: `ParseException` (malformed XML), `XPathException` (malformed XPath), `InvalidNameException` (invalid names) — all subclasses of `XmlArtisanException extends RuntimeException`.

### 9. Short, intuitive names

The API favors brief, readable names: `sel`, `attr`, `text`, `before`, `after`, `end`, `first`, `last`. No verbose overhead like `setAttribute` or `getTextContent`.

---

## Key Decisions and Rationale

### `Node extends Sel`

**Decision:** `Node` is a subclass of `Sel`, not a separate type.

**Rationale:** If "everything is a selection," then a single node is simply a selection of cardinality 1. This eliminates the need for duplicate APIs and enables fluid transitions: from a `Node` you can start a new selection with `.sel()`, from a selection you can extract a `Node` with `.first()`. Under the hood, the library always works with selections.

**Rejected alternative:** Completely separate `Node` and `Sel` types with explicit conversion methods. Rejected because it adds complexity with no real benefit.

### `.children()` returns `Sel`, not `List<Node>`

**Decision:** `node.children()` returns a `Sel` containing the node's children.

**Rationale:** Consistent with the "everything is a selection" principle. Enables immediate chaining: `node.children().attr("visible", "true")`. If a list is needed, `.list()` is available on any `Sel` (just like `.stream()`).

### `with*` methods for post-join data access

**Decision:** `JoinedSel<T>` adds `.attrWith()`, `.textWith()`, `.eachWith()` with the `With` suffix rather than overloading `.attr()` and `.text()`.

**Rationale:** Eliminates Java compiler ambiguity. When `T = String`, the compiler could confuse `Function<String, String>` with `BiFunction<String, String, String>` in `.attr()` overloads. The `With` suffix also makes it immediately visible in code when you are accessing the bound datum and when you are not.

**Rejected alternative:** Overloads based on the number of lambda parameters (option C). Works in most cases but fails with ambiguous method references. Too fragile.

### `.sel()` on `JoinedSel` returns `Sel` (loses the binding)

**Decision:** A sub-selection after a join loses the data binding and returns a plain `Sel`.

**Rationale:** The datum is associated with the join's nodes, not their descendants. It would make no sense to propagate a `Person` datum to the `<address>` or `<email>` children of a `<person>` node. Semantically, `.sel()` on `JoinedSel` is equivalent to `.toSel().sel(...)`.

### `append`/`before`/`after` have different semantics on `Node` vs `Sel`

**Decision:** On `Node`, they return the **new node** (for building structures). On `Sel`, they return the **original selection** (batch side-effect). `replace()` is the exception on `Sel`: it returns the new nodes because the originals no longer exist.

**Rationale:** Different usage patterns call for different return values. `Node` is used for deep construction (`node.append("a").append("b").text("...")`). `Sel` is used for batch modifications where the focus stays on the current selection. Consistency is maintained by the fact that `Node extends Sel` and the compiler resolves to the most specific type.

### JoinConfig with builder and `.defaults()`

**Decision:** The full form of the join uses `JoinConfig<T>` with a builder pattern, which supports `.defaults("tag")` to activate the shorthand behavior as a baseline.

**Rationale:** This solves three problems. First, it makes the difference between "not specified" (do nothing) and `.defaults()` (standard behavior: enter=append, update=identity, exit=remove) explicit. Second, it lets you start from the defaults and override only what you need. Third, passing an explicit `null` for a handler has a different meaning from not specifying it: `null` = "do nothing for this group"; unspecified without `.defaults()` = "do nothing"; unspecified with `.defaults()` = "use the default."

**Rejected alternative:** Three separate lambdas as `.join()` parameters. Less expressive, does not support the defaults-with-override pattern, and cannot distinguish between null and unspecified.

### Resilience: Null Object Pattern everywhere

**Decision:** Empty `Node`, empty `Sel`, empty strings — never `null`, never exceptions during chaining.

**Rationale:** The chain must never break. A user who writes `xml.sel("//maybe-exists").first().children().attr("x", "y")` should not have to worry about NullPointerException at any level. Every operation on an empty object is a no-op that returns another empty object.

**Rejected alternative:** `Optional<Node>` for `.first()` and `.last()`. More idiomatic in modern Java but breaks fluent chaining and forces the user to handle the empty case explicitly.

### XPath: automatic `//` → `.//` rewrite in sub-selections

**Decision:** In sub-selections, expressions starting with `//` are automatically treated as `.//`.

**Rationale:** In standard XPath, `//a` searches from the document root regardless of the context node. This is counter-intuitive when a user writes `xml.sel("//div").sel("//a")` expecting to find the `<a>` elements inside the `<div>` elements. The Java XPath API (`xpath.evaluate(expression, contextNode)`) natively supports context nodes, but the expression must be relative (`.//a`). The automatic rewrite avoids this pitfall.

### `text()` operates on direct text, not recursive

**Decision:** `text()` reads/writes only the direct TEXT/CDATA child nodes. `deepText()` provides recursive reading (the former behavior of `text()`).

**Rationale:** Production XML often contains mixed content: `<p>Hello <b>world</b> today</p>`. The most common use case is reading/writing a node's direct text, not its recursive text. With the old semantics (`getTextContent()`), `text()` would return `"Hello world today"` but `text("New")` would destroy the `<b>` element. The new semantics make `text()` consistent between reads and writes: both operate on direct text, preserving child elements. `normalizeText()` merges fragmented text nodes. `coalesceText()` is the destructive variant for when you actually need to flatten everything.

**Rejected alternative:** Keep `text()` recursive and add `directText()`. Rejected because the direct use case is more common and the recursive semantics are surprising in write mode (they destroy children).

### `content(XML)` imports the root, `content(String)` imports the wrapper's children

**Decision:** `content(XML)` imports the root element of the fragment as a single child. `content(String)` wraps the string in a synthetic `<_>...</_>` element, parses it, and imports the wrapper's children (supporting mixed content without a single root).

**Rationale:** The two variants cover different use cases. `content(XML)` is for when you already have a parsed `XML` and want to insert its root element. `content(String)` is for replacing content with arbitrary mixed content (e.g., `<b>bold</b> text`) that has no single root. The synthetic wrapper is transparent to the user.

### `cdata(String)` — write only

**Decision:** `cdata(String)` creates a CDATA_SECTION_NODE. There is no `cdata()` for reading — `text()` already reads both TEXT and CDATA.

**Rationale:** The distinction between TEXT and CDATA is only relevant in serialization (the content is identical). A `cdata()` reader would be redundant with `text()`. The user chooses CDATA for writing when the content contains XML special characters (HTML, scripts, etc.) that should be preserved without entity encoding.

### Thread safety: no guarantees

**Decision:** No type in the library is thread-safe.

**Rationale:** The underlying DOM API is not thread-safe. Adding synchronization would introduce significant overhead for a minority use case. The typical usage pattern is a transformation pipeline on a single thread.

---

## Comparison with Existing Solutions

| Library | Status | Approach | Data binding | D3 selections |
|---------|--------|----------|--------------|---------------|
| DOM API (JVM) | Active | Verbose, low-level | No | No |
| jOOX | Inactive (~2 years) | Fluent, jQuery-style | No | No |
| dom4j | Active | Custom API, not fluent | No | No |
| **XML Artisan** | — | Fluent + D3 selections | **Yes** | **Yes** |

The main differentiator against all alternatives is the **data binding with join pattern**. jOOX offers a similar fluent API but does not support data-to-node synchronization. dom4j has its own navigation API but is not fluent. No existing Java library brings the D3 paradigm to the XML world.
