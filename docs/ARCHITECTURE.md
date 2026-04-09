# XML Artisan — Architecture

> Internal structure, implementation patterns, and technical details for contributors.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Type Hierarchy](#type-hierarchy)
3. [Public Type Details](#public-type-details)
4. [Implementation Patterns](#implementation-patterns)
5. [XPath Management](#xpath-management)
6. [Join Mechanics](#join-mechanics)
7. [Internal Resilience](#internal-resilience)
8. [Thread Safety](#thread-safety)
9. [JVM Dependencies](#jvm-dependencies)

---

## Architecture Overview

XML Artisan is a thin layer on top of the standard JVM DOM and XPath APIs. It does not reimplement parsing or DOM management — it uses `javax.xml.parsers.DocumentBuilder` for parsing and `org.w3c.dom` as the underlying data structure.

The architecture is composed of:

- **DOM wrapping layer** — `XML`, `Sel`, `Node` wrap the standard DOM behind a fluent interface.
- **XPath layer** — Centralized XPath expression handling with support for relative contexts and namespaces.
- **Data binding layer** — `BoundSel<T>`, `JoinedSel<T>`, `JoinConfig<T>` implement the join pattern on top of selections.

```
┌─────────────────────────────────────────────┐
│              Public API                      │
│   XML, Sel, Node, BoundSel, JoinedSel       │
├─────────────────────────────────────────────┤
│           Data Binding Layer                 │
│   JoinConfig, data join, merge              │
├─────────────────────────────────────────────┤
│           XPath Layer                        │
│   Context node, rewrite //, namespace        │
├─────────────────────────────────────────────┤
│           DOM Wrapping Layer                 │
│   Wrapper around org.w3c.dom                 │
├─────────────────────────────────────────────┤
│           Standard JVM APIs                  │
│   javax.xml.parsers, org.w3c.dom,            │
│   javax.xml.xpath, javax.xml.transform       │
└─────────────────────────────────────────────┘
```

---

## Type Hierarchy

### Public types

```
XML                                             // entry point, wraps a Document
 │
 ├── Sel                                        // selection of N nodes, immediate operations
 │    └── Node extends Sel                      // selection of 1 node + DOM navigation
 │
 ├── BoundSel<T>                                // selection with associated data, lazy
 │
 ├── JoinedSel<T> extends Sel                   // post-join: merged enter+update, datum accessible
 │
 ├── JoinConfig<T>                              // join configuration via builder
 │    └── JoinConfig.Builder<T>                 // builder for JoinConfig
 │
 └── OutputOptions                              // serialization options
      └── OutputOptions.Builder                 // builder for OutputOptions
```

### Relationships between types (transition flow)

```
XML ──.sel(xpath)──→ Sel ──.data(list)──→ BoundSel<T> ──.join(...)──→ JoinedSel<T>
                      │                                                    │
                      │←──────────.toSel()─────────────────────────────────┘
                      │←──────────.sel(xpath)───────────────────────────────┘
                      │
                      ├──.first()/.last()──→ Node (extends Sel)
                      ├──.end()──→ Sel (parent)
                      └──.stream()──→ Stream<Node>
```

---

## Public Type Details

### `XML`

Entry point of the library. Wraps an `org.w3c.dom.Document` and provides factory methods, point read/write, selection creation, and serialization.

```
XML
 ├── // Factory methods (static)
 ├── XML.from(Path) → XML                          // parse from file
 ├── XML.parse(String) → XML                       // parse from string
 ├── XML.create(String rootTag) → XML               // empty document with root
 ├── XML.wrap(Document) → XML                       // wrap existing DOM Document
 │
 ├── // Point read and write
 ├── .get(xpath) → String                           // "" if not found
 ├── .set(xpath, value) → void                      // no-op if not found
 │
 ├── // Selections
 ├── .sel(xpath) → Sel                              // empty Sel if no match
 │
 ├── // Text normalization
 ├── .normalizeText() → XML                          // recursive across entire document
 │
 ├── // Namespace
 ├── .namespace(prefix, uri) → XML                  // register namespace for XPath
 │
 ├── // Serialization
 ├── .toString() → String                           // with XML declaration
 ├── .toFragment() → String                         // without declaration
 └── .writeTo(Path, OutputOptions?) → void          // write to file
```

**Internally** holds a reference to `org.w3c.dom.Document` and a shared `javax.xml.xpath.XPath` instance with the registered namespaces.

### `Sel`

Selection of zero or more nodes. All modification operations are immediate (side-effects on the DOM) and return `Sel` for chaining.

```
Sel
 ├── // Reading (from first node)
 ├── .attr(name) → String                          // "" if empty or attribute absent
 ├── .text() → String                              // direct text (concatenates child TEXT/CDATA, no side-effect)
 ├── .deepText() → String                          // recursive text (getTextContent)
 ├── .size() → int
 ├── .empty() → boolean
 │
 ├── // Modification (immediate, returns the same Sel)
 ├── .attr(name, value) → Sel
 ├── .attr(name, Function<String,String>) → Sel     // fn(currentValue) → new
 ├── .text(value) → Sel                             // normalizeText + direct text, preserves children
 ├── .text(Function<String,String>) → Sel            // text() + fn + text(String)
 ├── .cdata(value) → Sel                             // like text(value) but CDATA_SECTION_NODE
 ├── .content(XML) → Sel                             // replaces all children with fragment root
 ├── .content(String) → Sel                          // replaces all children with parsed mixed content
 ├── .normalizeText() → Sel                          // merges direct TEXT/CDATA into one, first child
 ├── .coalesceText() → Sel                           // destructive: getTextContent → remove all → single text
 ├── .remove() → Sel                                // returns parent selection
 │
 ├── // Structural insertion (side-effect, returns the same Sel)
 ├── .append(tagName) → Sel
 ├── .append(XML) → Sel
 ├── .prepend(tagName) → Sel
 ├── .prepend(XML) → Sel
 ├── .before(tagName) → Sel
 ├── .before(XML) → Sel
 ├── .after(tagName) → Sel
 ├── .after(XML) → Sel
 ├── .replace(XML) → Sel                            // EXCEPTION: returns Sel with the new nodes
 │
 ├── // Navigation
 ├── .sel(xpath) → Sel                              // contextual sub-selection
 ├── .end() → Sel                                   // parent selection (self if root)
 │
 ├── // Data binding
 ├── .data(List<T>) → BoundSel<T>                   // positional
 ├── .data(List<T>, Function<T,K>, Function<Node,K>) → BoundSel<T>  // with key function
 │
 ├── // Conversion and iteration
 ├── .stream() → Stream<Node>
 ├── .list() → List<Node>
 ├── .first() → Node                                // empty Node if selection is empty
 ├── .last() → Node                                 // empty Node if selection is empty
 ├── .order() → Sel                                  // reorders nodes in the DOM
 └── implements Iterable<Node>
```

**Internally** holds a `List<org.w3c.dom.Node>`, a reference to the parent `Sel` (for `.end()`), and a reference to the owning `XML` (for access to XPath and Document).

**Note on structural methods:** `append`/`prepend`/`before`/`after` on `Sel` return the original selection (the insertion is a side-effect). The chain's focus stays on the selected elements. `replace` is the exception: it returns a `Sel` with the new nodes because the originals no longer exist in the DOM.

### `Node extends Sel`

Specialization for a single node. Inherits everything from `Sel` and adds DOM navigation. Structural methods have **different semantics** from `Sel`: they return the newly created node.

```
Node extends Sel
 ├── (inherits everything from Sel)
 │
 ├── // DOM navigation
 ├── .children() → Sel                              // children as a selection
 ├── .parent() → Node                               // empty Node if root
 ├── .name() → String                               // tag name, "" if empty
 │
 ├── // Insertion (returns the NEW node, unlike Sel)
 ├── .append(tagName) → Node
 ├── .append(XML) → Node
 ├── .prepend(tagName) → Node
 ├── .prepend(XML) → Node
 ├── .insert(tagName, beforeNode) → Node
 ├── .before(tagName) → Node
 ├── .before(XML) → Node
 ├── .after(tagName) → Node
 ├── .after(XML) → Node
 ├── .replace(XML) → Node                           // returns the new node
 │
 ├── // Underlying DOM access
 └── .unwrap() → org.w3c.dom.Node                   // null if empty Node
```

**Architectural decision: different return types on Node vs Sel for structural methods.**

The Java compiler resolves to the most specific type: if the object is a `Node`, methods like `append()` return `Node`. If the object is a `Sel`, they return `Sel`. This is implemented via overrides with covariant return types.

On `Node`, returning the new node enables deep construction:

```java
node.append("chapter").attr("num", "1").append("title").text("Intro");
```

On `Sel`, returning the original selection enables batch modifications:

```java
xml.sel("//item").append("status").attr("active", "true");
// .attr() applies to each <item>, not to the new <status> elements
```

### `BoundSel<T>`

Selection with associated data, awaiting the join. Transitional type.

```
BoundSel<T>
 ├── .join(String tagName) → JoinedSel<T>               // shorthand
 └── .join(JoinConfig<T>) → JoinedSel<T>                // explicit configuration
```

**Internally** holds the data list, the key function (optional), the original selection, and the reference to the parent for the enter phase.

### `JoinConfig<T>`

Join configuration via builder. Defines the behavior for enter, update, and exit.

```
JoinConfig<T>
 └── JoinConfig.builder() → Builder<T>
      ├── .defaults(String tagName) → Builder<T>
      ├── .enter(BiFunction<Node, T, Node>) → Builder<T>     // (parent, datum) → created node
      ├── .update(BiFunction<Node, T, Node>) → Builder<T>    // (node, datum) → node
      ├── .exit(Consumer<Node>) → Builder<T>                  // (node) → void
      └── .build() → JoinConfig<T>
```

**Internal value semantics in the builder:**

The builder tracks three internal states for each handler: *default* (set by `.defaults()`), *custom* (set by `.enter()`/`.update()`/`.exit()` with a non-null value), *explicit null* (set with `null`).

| State | enter | update | exit |
|-------|-------|--------|------|
| **Shorthand** `.join("tag")` | Appends a node with that tag | Identity | Removes the node |
| **`.defaults("tag")`** | Appends a node with that tag | Identity | Removes the node |
| **Custom handler** | Executes the handler | Executes the handler | Executes the handler |
| **Explicit `null`** | Skipped (no node created) | Identity | Skipped (node remains) |
| **Unspecified (without `.defaults()`)** | Skipped (no node created) | Identity | Skipped (node remains) |

The key insight: `.defaults("tag")` sets all three handlers to the shorthand values. A subsequent `.update(handler)` overrides only update, leaving enter and exit at their defaults. A subsequent `.exit(null)` explicitly disables exit (the handler is null, not "unspecified").

### `JoinedSel<T> extends Sel`

Result of the join: contains the merged nodes (enter + update) with the associated datum.

```
JoinedSel<T> extends Sel
 ├── (inherits everything from Sel)
 │
 ├── // Operations with datum access
 ├── .attrWith(String name, BiFunction<String, T, String>) → JoinedSel<T>
 ├── .textWith(Function<T, String>) → JoinedSel<T>
 ├── .cdataWith(Function<T, String>) → JoinedSel<T>
 ├── .eachWith(BiConsumer<Node, T>) → JoinedSel<T>
 │
 ├── // Transitions (lose the binding)
 ├── .sel(xpath) → Sel                              // sub-selection without datum
 ├── .toSel() → Sel                                 // explicit binding drop
 │
 ├── // Ordering
 └── .order() → JoinedSel<T>
```

**Internally** holds a node → datum map for the `with*` methods. The map is built during join execution.

---

## Implementation Patterns

### Null Object Pattern

The library never uses `null` as a return value during chaining. Two "empty objects" exist:

- **Empty `Sel`** — a selection with an empty node list. All operations are no-ops.
- **Empty `Node`** — extends empty `Sel`. All navigation methods return empty values.

The empty `Node` should be a singleton. The empty `Sel` should not, because it carries a reference to its parent `Sel` for `.end()`.

### Sel parent and `.end()`

Every `Sel` created through a sub-selection (`.sel()`) holds a reference to the `Sel` that spawned it. `.end()` returns that reference. A root selection (created by `xml.sel(...)`) has itself as its parent — `.end()` returns itself, without exceptions.

### DOM node wrapping

`org.w3c.dom.Node` instances are wrapped in XML Artisan `Node` objects on demand. There is no global wrapping cache — the same DOM node may be wrapped in multiple `Node` objects at different times. Identity is based on the underlying DOM node (accessible via `.unwrap()`), not the wrapper object.

### Selection immutability vs node mutability

The selections themselves are immutable: the list of nodes in a `Sel` does not change after creation. What changes is the content of the nodes in the DOM (attributes, text, children). This is consistent with D3 where "selections are immutable; only the elements are mutable."

---

## XPath Management

### Internal architecture

The `XML` object holds an instance of `javax.xml.xpath.XPath` configured with the registered namespaces. XPath expressions are compiled and evaluated through this instance.

### Evaluation context

| Call | Context node for `xpath.evaluate()` |
|------|--------------------------------------|
| `xml.get(xpath)` | The `Document` |
| `xml.sel(xpath)` | The `Document` |
| `sel.sel(xpath)` | Each node in the parent selection |
| `node.sel(xpath)` | The underlying node |

### Automatic `//` → `.//` rewrite

In sub-selections, expressions starting with `//` are rewritten as `.//` before evaluation. This happens in the wrapping layer, not in the XPath layer.

**Rule:** If the expression starts with `//` AND the context is not the Document, `.` is prepended to the expression.

The Java XPath API natively supports relative contexts via `xpath.evaluate(expression, contextNode, returnType)` where `contextNode` is any `org.w3c.dom.Node`. Relative expressions (e.g., `.//a`, `child::a`, `@id`) work correctly with this mechanism.

### Namespaces

Namespaces are registered on the `XML` object and propagated to the XPath instance through a custom `NamespaceContext`. All selections created from that `XML` inherit the same namespace resolution.

```java
xml.namespace("dc", "http://purl.org/dc/elements/1.1/");
// From this point on, "dc:" is resolved in all XPath expressions
```

---

## Join Mechanics

### Execution flow

1. **`.data(list, keyFn?, nodeKeyFn?)`** — creates a `BoundSel<T>` that stores the data list, the key function, and the original selection.

2. **`.join(...)`** — executes the data join:
   - a. Computes the matching between nodes and data (positional or by key).
   - b. Classifies each node/datum into enter, update, or exit.
   - c. Groups by parent (if there are multiple parents).
   - d. Executes the handlers for each group in order: exit, update, enter.
   - e. For enter, inserts new nodes at the position corresponding to the data order (before the next update sibling).
   - f. Merges the enter and update nodes into the resulting `JoinedSel<T>`.
   - g. Builds the node → datum map for the `with*` methods.

### Positional matching

Without a key function, matching is by index: the node at index 0 corresponds to the datum at index 0, and so on. Nodes beyond the data length go into exit; data beyond the node length go into enter.

### Key-based matching

With a key function, matching is based on logical identity. The key function is evaluated on each datum (`keyFn.apply(item)`) and on each node (`nodeKeyFn.apply(node)`). Nodes and data with the same key are paired (update). Data without a corresponding node go into enter. Nodes without a corresponding datum go into exit.

If multiple nodes have the same key, the duplicates go into exit. If multiple data items have the same key, the duplicates go into enter.

### Multiple parents

If the selection contains nodes with different parents, the join operates on each group of nodes that share the same parent, following D3 semantics. The data is distributed across groups.

### Insertion order

Nodes created during enter are inserted at the position corresponding to the data order, not appended at the end of the parent. The mechanism works as follows: the enter node is inserted before the next sibling that belongs to the update group. This maintains consistency between the data order and the node order in the DOM.

The `.order()` method on `JoinedSel` reorders all nodes (enter and update) in the DOM according to the selection order (which reflects the data order). Useful when update nodes have shifted position relative to the data.

---

## Internal Resilience

### Complete behavior table

#### `Sel` and general operations

| Situation | Behavior |
|-----------|----------|
| `xml.get(xpath)` finds no nodes | `""` |
| `xml.set(xpath, value)` finds no nodes | No-op |
| `xml.sel(xpath)` finds no nodes | Empty `Sel` |
| Operation on empty `Sel` | No-op, returns the same empty `Sel` |
| `.end()` on root selection | Returns itself |
| `.data()` with empty list | All nodes go to exit |
| `.data()` on empty `Sel` | All data go to enter |
| `.stream()` on empty `Sel` | `Stream.empty()` |
| `.list()` on empty `Sel` | Empty list |
| `.size()` on empty `Sel` | `0` |
| `.attr(name)` on first node, attribute absent | `""` |
| `.text()` on first node, no direct text | `""` |
| `.deepText()` on first node, no text content | `""` |
| `.normalizeText()` on empty `Sel` | No-op |
| `.coalesceText()` on empty `Sel` | No-op |
| `.cdata(value)` on empty `Sel` | No-op |
| `.content(XML)` on empty `Sel` | No-op |
| `.content(String)` on empty `Sel` | No-op |

#### Empty `Node` (null object)

| Operation | Behavior |
|-----------|----------|
| `.attr(name)` | `""` |
| `.attr(name, value)` | No-op, returns the same empty `Node` |
| `.text()` | `""` |
| `.deepText()` | `""` |
| `.text(value)` | No-op, returns the same empty `Node` |
| `.normalizeText()` | No-op |
| `.coalesceText()` | No-op |
| `.cdata(value)` | No-op |
| `.content(XML)` | No-op |
| `.content(String)` | No-op |
| `.name()` | `""` |
| `.children()` | Empty `Sel` |
| `.parent()` | Empty `Node` |
| `.sel(xpath)` | Empty `Sel` |
| `.append(tag/xml)` | Empty `Node` |
| `.prepend(tag/xml)` | Empty `Node` |
| `.before(tag/xml)` | Empty `Node` |
| `.after(tag/xml)` | Empty `Node` |
| `.replace(xml)` | Empty `Node` |
| `.remove()` | No-op |
| `.unwrap()` | `null` |

### Allowed exceptions

Hierarchy: `XmlArtisanException extends RuntimeException` with specific subclasses. The only exceptions are for non-recoverable programming errors:

- **Malformed XML** — `XML.parse()` / `XML.from()` with invalid input → `ParseException`.
- **Malformed XPath** — syntax error in the expression → `XPathException` (the message includes the expression).
- **Invalid name** — tags or attributes with disallowed characters → `InvalidNameException` (the message includes the name).
- **File not found** — `XML.from()` with a non-existent path → `UncheckedIOException`.
- **Unregistered namespace prefix** — `IllegalArgumentException`.
- **Serialization error** — `XmlArtisanException` (base type).

All extend `RuntimeException`, so catching `XmlArtisanException` intercepts all library errors (except I/O and namespace errors).

---

## Thread Safety

No type in the library (`XML`, `Sel`, `Node`, `BoundSel`, `JoinedSel`) is thread-safe. The user is responsible for synchronization if the same objects are accessed from multiple threads.

This choice is consistent with:
- The underlying `org.w3c.dom.Document`, which is not thread-safe.
- The `javax.xml.xpath.XPath` instance, which is not thread-safe.
- The typical usage pattern: a transformation pipeline on a single thread.

---

## JVM Dependencies

XML Artisan uses exclusively standard JVM APIs:

| API | Usage |
|-----|-------|
| `javax.xml.parsers.DocumentBuilder` | XML parsing |
| `org.w3c.dom.*` | DOM data structure |
| `javax.xml.xpath.*` | XPath expression evaluation |
| `javax.xml.transform.*` | Serialization (DOM → string/file) |

No external dependencies. Compatible with any Java version that includes these APIs (Java 8+).
