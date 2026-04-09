# XML Artisan — Testing Strategy

> Approach, structure, and test coverage.

---

## Table of Contents

1. [General Approach](#general-approach)
2. [Implementation Phases](#implementation-phases)
3. [Test Structure](#test-structure)
4. [Test Suites](#test-suites)
5. [Conventions](#conventions)
6. [Test XML Documents](#test-xml-documents)

---

## General Approach

### Test-first on the public API

The testing approach is:

1. **Implement all interfaces and types** needed to satisfy the compiler first (empty stubs).
2. **Write all tests** focusing exclusively on the library's public API.
3. **Implement** until the tests pass.

### Focus on functional tests

Tests work with the external API just as a library user would. We do not test internal classes directly — if an internal behavior is critical, it must be observable through the public API.

**Exception:** Internal unit tests are acceptable for particularly complex and isolated logic that would be difficult to verify indirectly (e.g., the key-function matching algorithm, the XPath `//` → `.//` rewrite).

### Each test is self-contained

Every test creates its own XML document (from a string or empty) and verifies the result. No dependencies between tests, no shared state.

---

## Implementation Phases

### Phase 1: Types and compilation (v1.0)

Create the public interfaces/classes with stub methods:

```
net.fndanko.xml.artisan.XML
net.fndanko.xml.artisan.Sel
net.fndanko.xml.artisan.Node
net.fndanko.xml.artisan.OutputOptions
```

Goal: the project compiles, tests can be written, tests fail (red).

### Phase 2: Core tests (v1.0)

Write all tests for core functionality: entry point, direct selections, navigation, structural insertion, serialization, resilience.

### Phase 3: Core implementation (v1.0)

Implement until all Phase 2 tests pass.

### Phase 4: Data binding types (v1.1)

Add the stub types:

```
net.fndanko.xml.artisan.BoundSel
net.fndanko.xml.artisan.JoinedSel
net.fndanko.xml.artisan.JoinConfig
```

### Phase 5: Data binding tests (v1.1)

Write all tests for the join pattern.

### Phase 6: Data binding implementation (v1.1)

Implement until all Phase 5 tests pass.

---

## Test Structure

Tests are organized by functional area, mirroring the API sections:

```
src/test/java/net/fndanko/xml/artisan/
 ├── XMLEntryPointTest.java           // factory methods, get/set, namespace
 ├── SelReadTest.java                 // .attr(), .text(), .size(), .empty()
 ├── SelModifyTest.java               // .attr(name,value), .text(value), .remove()
 ├── SelTransformTest.java            // .attr(name,fn), .text(fn)
 ├── SelNavigationTest.java           // .sel(), .end(), sub-selections, contextual XPath
 ├── SelStructuralTest.java           // .append(), .prepend(), .before(), .after(), .replace()
 ├── SelIterationTest.java            // .stream(), .list(), .first(), .last(), Iterable, .order()
 ├── NodeNavigationTest.java          // .children(), .parent(), .name()
 ├── NodeStructuralTest.java          // append/prepend/before/after/replace on Node (different return)
 ├── NodeAsSelTest.java               // Node used as Sel: .sel(), data binding from Node
 ├── SerializationTest.java           // .toString(), .toFragment(), .writeTo(), OutputOptions
 ├── XPathTest.java                   // contextual XPath, rewrite //, namespace
 ├── ResilienceTest.java              // empty Sel, empty Node, safe chaining, no-op
 ├── TextNodeTest.java               // normalizeText, text (direct semantics), deepText, coalesceText
 ├── DataBindingTest.java             // .data(), positional and key-based matching
 ├── JoinShorthandTest.java           // .join("tag"), default enter/update/exit
 ├── JoinConfigTest.java              // JoinConfig builder, .defaults(), explicit null, override
 ├── JoinedSelTest.java               // .attrWith(), .textWith(), .eachWith(), .toSel(), .order()
 └── JoinAdvancedTest.java            // multiple parents, insertion order, edge cases
```

---

## Test Suites

### 1. `XMLEntryPointTest` — Entry point and basic operations

```
Load XML from string with XML.parse()
Load XML from file with XML.from()
Create empty document with XML.create()
Read attribute with .get()
Read text with .get()
Write attribute with .set()
Write text with .set()
.get() on XPath with no results → empty string
.set() on XPath with no results → no-op
Register namespace and use it in queries
Register multiple namespaces
```

### 2. `SelReadTest` — Reading from selections

```
.attr(name) returns value from first node
.attr(name) on non-existent attribute → empty string
.attr(name) on empty selection → empty string
.text() returns direct text from first node (not recursive)
.text() on node with no direct text → empty string
.text() on empty selection → empty string
.size() returns correct number of nodes
.size() on empty selection → 0
.empty() on selection with nodes → false
.empty() on empty selection → true
```

### 3. `SelModifyTest` — Modification with fixed values

```
.attr(name, value) sets on all nodes
.attr(name, value) on empty selection → no-op, chaining continues
.text(value) sets direct text on all nodes, preserves child elements
.text(value) on empty selection → no-op, chaining continues
.remove() removes nodes from the DOM
.remove() on empty selection → no-op
.remove() returns the parent selection
Multiple chaining: .attr().text().attr() works
```

### 4. `SelTransformTest` — Modification with functions

```
.attr(name, fn) transforms the value on each node
.attr(name, fn) with non-existent attribute → fn receives empty string
.text(fn) transforms direct text on each node, preserves child elements
.text(fn) with empty text → fn receives empty string
Transform functions executed on every node (not just the first)
```

### 5. `SelNavigationTest` — Selections and navigation

```
.sel() creates contextual sub-selection
.sel() on empty selection → empty Sel
.sel() nested: sel().sel().sel()
.end() returns to parent selection
.end() on root selection → returns itself
.end() after multiple sub-selections: end().end()
Sub-selection searches only within parent selection nodes (not global)
Full chaining: sel().attr().end().attr()
```

### 6. `SelStructuralTest` — Structural insertion on Sel

```
.append(tag) adds a child to every node in the selection
.append(tag) returns the original selection (not the new nodes)
.append(XML) appends a fragment to every node
.prepend(tag) adds a child at the start of every node
.prepend(XML) prepends a fragment
.before(tag) inserts a sibling before every node
.before(tag) returns the original selection
.before(XML) inserts a fragment as a sibling before
.after(tag) inserts a sibling after every node
.after(XML) inserts a fragment as a sibling after
.replace(XML) replaces every node
.replace(XML) returns Sel with the new nodes (not the originals)
All methods on empty selection → no-op, chaining continues
```

### 7. `SelIterationTest` — Iteration and conversion

```
For-each on Sel iterates all nodes
For-each on empty Sel → no iteration
.stream() returns stream with all nodes
.stream() on empty Sel → empty stream
.list() returns list with all nodes
.list() on empty Sel → empty list
.first() returns the first node
.first() on empty Sel → empty Node
.last() returns the last node
.last() on empty Sel → empty Node
.order() reorders nodes in the DOM to match the selection order
```

### 8. `NodeNavigationTest` — DOM navigation

```
.children() returns Sel with children
.children() on leaf node → empty Sel
.children() chainable: node.children().attr(...)
.children().list() converts to list
.parent() returns the parent node
.parent() on root node → empty Node
.name() returns the tag name
.name() on empty Node → empty string
```

### 9. `NodeStructuralTest` — Insertion on Node

```
.append(tag) on Node returns the new node (not the original)
.append(tag) enables deep construction: node.append("a").append("b")
.append(XML) returns the root node of the inserted fragment
.prepend(tag) returns the new node
.insert(tag, before) inserts at the correct position
.before(tag) returns the new sibling
.before(XML) returns the new node
.after(tag) returns the new sibling
.after(XML) returns the new node
.replace(XML) returns the new replacement node
.replace(XML) the original node is no longer in the DOM
```

### 10. `NodeAsSelTest` — Node used as Sel

```
Node has all Sel methods
node.sel(xpath) creates a sub-selection from the node
node.attr(name, value) works as a Sel of cardinality 1
node.data(list) enters binding mode
Node obtained from iteration is usable as Sel
```

### 11. `SerializationTest` — Output

```
.toString() produces XML with declaration
.toFragment() produces XML without declaration
.writeTo(path) writes to file
.writeTo(path, options) with indentation
.writeTo(path, options) with specific encoding
.writeTo(path, options) with declaration omitted
Modified document serializes with the modifications
```

### 12. `XPathTest` — Contextual XPath

```
Absolute XPath from xml.sel() searches from root
Relative XPath in sub-selection searches from context
"//" in sub-selection treated as ".//" (relative)
Explicit ".//" works in sub-selection
XPath with predicates works
XPath for attributes (@attr) works
XPath with namespaces works
Malformed XPath → XPathException
```

### 13. `ResilienceTest` — Resilience and null object

```
Empty selection: all operations are no-ops
Empty selection: long chaining does not throw exceptions
Empty Node from .first() on empty Sel: all operations are no-ops
Empty Node: .attr(name) → ""
Empty Node: .text() → ""
Empty Node: .name() → ""
Empty Node: .children() → empty Sel
Empty Node: .parent() → empty Node
Empty Node: .sel(xpath) → empty Sel
Empty Node: .append(tag) → empty Node
Empty Node: .prepend(tag) → empty Node
Empty Node: .before(tag) → empty Node
Empty Node: .after(tag) → empty Node
Empty Node: .replace(xml) → empty Node
Empty Node: .remove() → no-op
Empty Node: .unwrap() → null
Mixed chaining with empty selections and nodes: xml.sel("//nothing").first().children().sel("//x").attr("a","b") → no-op without exceptions
.end() on root selection → itself (no exception)
Empty Sel: .deepText() → ""
Empty Sel: .normalizeText() → no-op
Empty Sel: .coalesceText() → no-op
Empty Node: .deepText() → ""
Empty Node: .normalizeText() → no-op
Empty Node: .coalesceText() → no-op
Empty Sel: .cdata() → no-op
Empty Node: .cdata() → no-op
Empty Sel: .content(XML) → no-op
Empty Sel: .content(String) → no-op
Empty Node: .content(XML) → no-op
Empty Node: .content(String) → no-op
.data() with empty list → all nodes go to exit
.data() on empty Sel → all data go to enter
```

### 14. `TextNodeTest` — Text node handling

```
normalizeText: adjacent text nodes merged into one
normalizeText: text around elements merged as first child
normalizeText: mix of TEXT + CDATA produces CDATA
normalizeText: CDATA only stays CDATA
normalizeText: no text nodes → no-op
normalizeText: empty element → no-op
normalizeText: preserves child elements
normalizeText: preserves attributes
normalizeText: applied to all nodes in the selection
normalizeText: chainable (returns self)
text() read: simple direct text
text() read: mixed content → direct text only
text() read: no direct text → empty string
text() read: does not modify the DOM
text() read: fragmented text nodes concatenated
text() read: empty selection → empty string
text() write: replaces direct text, preserves children
text() write: empty string removes direct text
text() write: adds text to a node with no text
text() write: applied to all nodes
text() write: chainable
text() transform: applied to each node
text() transform: preserves children on mixed content
text() transform: empty text → fn receives empty string
deepText: simple element → full text
deepText: mixed content → all descendant text
deepText: nested → concatenates everything
deepText: empty selection → empty string
deepText: no side-effects
coalesceText: mixed content → single text, children removed
coalesceText: nested elements → flattens everything
coalesceText: no mixed content → unchanged
coalesceText: preserves attributes
coalesceText: applied to all nodes
coalesceText: chainable
coalesceText then text → reads single string
XML.normalizeText: recursive across all levels
XML.normalizeText: preserves structure
XML.normalizeText: chainable (returns XML)
cdata: simple value creates CDATA_SECTION_NODE
cdata: preserves child elements
cdata: replaces existing TEXT_NODE with CDATA
cdata: replaces existing CDATA with new CDATA
cdata: applied to all nodes
cdata: chainable
cdata: empty selection → no-op
cdata: text() reads CDATA content
cdata: special characters preserved
content(XML): replaces all children
content(XML): preserves node attributes
content(XML): preserves node position
content(XML): imports root element of fragment
content(XML): applied to all nodes
content(XML): chainable
content(XML): empty selection → no-op
content(String): parses and replaces
content(String): mixed content supported
content(String): single element
content(String): empty string removes all children
```

### 15. `DataBindingTest` — Basic data binding

```
.data() with positional matching: correct correspondence
.data() with more data than nodes: excess go to enter
.data() with more nodes than data: excess go to exit
.data() with same cardinality: all go to update
.data() with key function: key-based matching
.data() with key function and different order: correct matching
.data() with duplicate keys in nodes: duplicates go to exit
.data() with duplicate keys in data: duplicates go to enter
```

### 16. `JoinShorthandTest` — Join shorthand form

```
.join("tag") creates missing nodes with the specified tag
.join("tag") does not modify update nodes (identity)
.join("tag") removes exit nodes
.join("tag") returns merged enter + update
.join("tag") on empty Sel with data: all created
.join("tag") with empty list: all removed
Post-join operations with .attrWith() work
```

### 17. `JoinConfigTest` — JoinConfig builder

```
.defaults("tag") activates shorthand behavior
.defaults("tag") with .update() override: only update changed
.defaults("tag") with .exit(null): exit disabled
.defaults("tag") with .enter(handler) override: only enter changed
Without .defaults(): unspecified enter → skipped
Without .defaults(): unspecified exit → skipped
.enter(null) explicit → skipped even with .defaults()
.update(null) explicit → identity
.exit(null) explicit → skipped
Custom enter/update/exit handlers execute correctly
```

### 18. `JoinedSelTest` — Post-join operations

```
.attrWith() accesses the associated datum
.attrWith() receives the current attribute value as first parameter
.textWith() sets text from the datum
.eachWith() executes operation with node and datum
.toSel() returns Sel without binding
.sel() on JoinedSel returns Sel (loses binding)
.attr() (inherited from Sel) works without datum
.order() reorders nodes according to data order
Chaining: .attrWith().textWith().attr().toSel().sel()
```

### 19. `JoinAdvancedTest` — Advanced scenarios

```
Join with multiple parents: enter executed for each parent
Insertion order: enter nodes at the correct position (not appended)
Insertion order with key function: position respected
.order() after join with reordered data: DOM reflects new order
Join on a selection with a single node
Repeated join on the same document
Join with different datum types (String, POJO, record)
Full join: enter + update + exit all present
```

---

## Conventions

### Test naming

Pattern: `<what>_<condition>_<expectedResult>`

```java
@Test void attr_onEmptySel_returnsEmptyString() { ... }
@Test void join_withMoreDataThanNodes_createsEnterNodes() { ... }
@Test void end_onRootSelection_returnsSelf() { ... }
```

### Test structure

```java
@Test
void attr_withFunction_transformsEachNode() {
    // Arrange
    XML xml = XML.parse("<root><a href='http://example.com'/><a href='http://test.com'/></root>");

    // Act
    xml.sel("//a").attr("href", v -> v.replace("http://", "https://"));

    // Assert
    assertEquals("https://example.com", xml.get("//a[1]/@href"));
    assertEquals("https://test.com", xml.get("//a[2]/@href"));
}
```

### Assertions

- Verify the resulting XML state, not internals.
- Use `xml.get()` to verify point values.
- Use `xml.sel().size()` to verify cardinality.
- Use `xml.toString()` or `xml.toFragment()` to verify overall structure only when necessary.

---

## Test XML Documents

Tests use inline XML (strings) when dealing with very simple, self-contained documents, or when testing string-based parsing:

```java
XML xml = XML.parse("""
    <catalog>
        <book id="1" lang="it">
            <title>Java in Action</title>
            <author>Mario Rossi</author>
        </book>
        <book id="2" lang="en">
            <title>Design Patterns</title>
            <author>GoF</author>
        </book>
    </catalog>
    """);
```

More complex documents are placed in `src/test/resources` and read from there.

For `XML.from()` tests (reading from file), temporary files are created with JUnit's `@TempDir`.

For `XML.writeTo()` tests, output is written to `@TempDir` and read back for verification.
