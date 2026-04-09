# XML Artisan — API Reference

> Complete reference for the library's public API.

**Package:** `net.fndanko.xml.artisan`

---

## Table of Contents

1. [XML](#xml)
2. [Sel](#sel)
3. [Node](#node)
4. [BoundSel\<T\>](#boundselt)
5. [JoinConfig\<T\>](#joinconfigt)
6. [JoinedSel\<T\>](#joinedselt)
7. [OutputOptions](#outputoptions)

---

## XML

Entry point of the library. Represents an in-memory XML document.

### Factory methods

#### `XML.from(Path path) → XML`

Loads and parses an XML document from a file.

```java
XML xml = XML.from(Path.of("./catalog.xml"));
```

**Exceptions:** `UncheckedIOException` if the file does not exist. `ParseException` if the XML is malformed.

#### `XML.parse(String xmlString) → XML`

Parses an XML document from a string.

```java
XML xml = XML.parse("<root><item id='1'/></root>");
```

**Exceptions:** `ParseException` if the XML is malformed.

#### `XML.create(String rootTagName) → XML`

Creates an empty document with the specified root element.

```java
XML xml = XML.create("catalog");
// Produces: <catalog/>
```

### Point read and write

#### `.get(String xpath) → String`

Evaluates the XPath expression from the document root and returns the value as a string.

```java
String id = xml.get("/catalog/book/@id");
String title = xml.get("/catalog/book[1]/title");
```

**Returns:** The value of the matched node. `""` (empty string) if the XPath matches nothing.

#### `.set(String xpath, String value) → void`

Sets the value of the node matched by the XPath expression.

```java
xml.set("/catalog/book[1]/@id", "new-id");
xml.set("/catalog/book[1]/title", "New Title");
```

**Behavior:** No-op if the XPath matches nothing.

### Selections

#### `.sel(String xpath) → Sel`

Creates a selection with all nodes matching the XPath expression, evaluated from the document root.

```java
Sel books = xml.sel("//book");
Sel italianBooks = xml.sel("//book[@lang='it']");
```

**Returns:** Empty `Sel` if no nodes match. Never `null`.

### Namespace

#### `.namespace(String prefix, String uri) → XML`

Registers a namespace prefix for use in XPath expressions.

```java
xml.namespace("dc", "http://purl.org/dc/elements/1.1/");
xml.sel("//dc:title").text("New Title");
```

**Returns:** The `XML` object itself (for chaining).

### Text normalization

#### `.normalizeText() → XML`

Applies `normalizeText()` recursively across the entire document (depth-first post-order: children are normalized before the node itself).

```java
xml.normalizeText();
```

**Returns:** The `XML` object itself (for chaining).

### Serialization

#### `.toString() → String`

Serializes the complete XML document, including the XML declaration.

#### `.toFragment() → String`

Serializes the XML document without the XML declaration.

#### `.writeTo(Path path) → void`

Writes the document to a file with default options.

#### `.writeTo(Path path, OutputOptions options) → void`

Writes the document to a file with custom options.

```java
xml.writeTo(Path.of("./output.xml"), OutputOptions.builder()
    .indent(true)
    .indentAmount(2)
    .omitDeclaration(false)
    .encoding("UTF-8")
    .build());
```

---

## Sel

Selection of zero or more nodes. All modification operations are immediate (they act on the DOM at call time) and return `Sel` for chaining. Implements `Iterable<Node>`.

### Reading

#### `.attr(String name) → String`

Returns the attribute value from the **first node** in the selection.

**Returns:** `""` if the selection is empty or the attribute does not exist.

#### `.text() → String`

Returns the direct text of the **first node** in the selection — concatenates the content of all direct TEXT and CDATA child nodes, without modifying the DOM.

**Returns:** `""` if the selection is empty or has no direct text.

```java
// <p>Hello <b>world</b> today</p>
xml.sel("//p").text();   // → "Hello  today" (direct text only, not recursive)
```

#### `.deepText() → String`

Returns the recursive text of all descendants of the **first node** (equivalent to `getTextContent()`).

**Returns:** `""` if the selection is empty.

```java
// <p>Hello <b>world</b> today</p>
xml.sel("//p").deepText();   // → "Hello world today"
```

#### `.size() → int`

Number of nodes in the selection.

#### `.empty() → boolean`

`true` if the selection contains no nodes.

### Modification

All modification operations return the **same `Sel`** for chaining. On an empty selection they are no-ops.

#### `.attr(String name, String value) → Sel`

Sets the attribute to a fixed value on **all nodes** in the selection.

```java
xml.sel("//p").attr("style", "text-align: center;");
```

#### `.attr(String name, Function<String, String> fn) → Sel`

Transforms the attribute on each node. The function receives the current value and returns the new value.

```java
xml.sel("//a").attr("href", v -> v.replace("http://", "https://"));
```

#### `.text(String value) → Sel`

Sets the direct text on all nodes. Internally calls `normalizeText()`, removes all direct TEXT/CDATA child nodes, and inserts a single TEXT_NODE as the first child. Preserves all child elements.

```java
xml.sel("//title").text("New Title");
// <p>Hello <b>world</b> today</p> + .text("New") → <p>New<b>world</b></p>
```

#### `.text(Function<String, String> fn) → Sel`

Transforms the direct text on each node. Reads with `text()` (no side-effect), applies the function, writes with `text(String)`.

```java
xml.sel("//code").text(v -> v.trim());
```

#### `.cdata(String value) → Sel`

Identical to `text(String)` but inserts a CDATA_SECTION_NODE instead of a TEXT_NODE. Internally calls `normalizeText()`, removes all direct TEXT/CDATA child nodes, and inserts a single CDATA_SECTION_NODE as the first child. Preserves all child elements.

```java
xml.sel("//script").cdata("var x = '<div>hello</div>';");
// Content with XML special characters is preserved in the CDATA section
```

**Note:** `text()` in read mode returns the content of both TEXT and CDATA nodes — the distinction only matters on write.

#### `.content(XML fragment) → Sel`

Replaces **all** children of each node in the selection with the root element of the XML fragment. Preserves the node itself (tag, attributes, position in the DOM).

```java
// <p class="x">old text</p>  →  <p class="x"><div><b>new</b> content</div></p>
xml.sel("//p").content(XML.parse("<div><b>new</b> content</div>"));
```

#### `.content(String xmlContent) → Sel`

String variant. Accepts an XML string that may contain mixed content without a single root. Internally wrapped in a synthetic tag, parsed, and the **children** of the wrapper are imported.

```java
// <p class="x">old text</p>  →  <p class="x"><b>new</b> content</p>
xml.sel("//p").content("<b>new</b> content");
```

**Key difference:**
- `content(XML)` → imports the fragment's root element (a single node)
- `content(String)` → parses with a wrapper, imports the wrapper's children (can be mixed content)

#### `.normalizeText() → Sel`

Merges fragmented direct TEXT/CDATA child nodes into a single node, positioned as the first child. If at least one was CDATA, the result is CDATA. Leaves child elements intact.

```java
// <p>Hello <b>world</b> today</p> → <p>Hello  today<b>world</b></p>
xml.sel("//p").normalizeText();
```

#### `.coalesceText() → Sel`

Destructive normalization: collects `getTextContent()`, removes **all** children, sets a single text node. Destroys child elements.

```java
// <p>Hello <b>world</b> today</p> → <p>Hello world today</p>
xml.sel("//p").coalesceText();
```

#### `.remove() → Sel`

Removes all nodes in the selection from the DOM.

**Returns:** The parent selection (from which this selection was created).

### Structural insertion

Side-effect operations that **return the original selection** (not the inserted nodes). Insertion is performed for each node in the selection.

#### `.append(String tagName) → Sel`

Appends an empty child with the specified tag to each node in the selection.

```java
xml.sel("//item").append("status");
// Appends <status/> as the last child of each <item>
// Returns the selection of <item> elements, not the <status> elements
```

#### `.append(XML fragment) → Sel`

Appends an XML fragment as a child to each node in the selection.

#### `.prepend(String tagName) → Sel`

Prepends an empty child (as the first child) to each node.

#### `.prepend(XML fragment) → Sel`

Prepends an XML fragment to each node.

#### `.before(String tagName) → Sel`

Inserts an empty sibling before each node in the selection.

```java
xml.sel("//section").before(XML.parse("<hr/>"));
// Inserts <hr/> before each <section>
// Returns the selection of <section> elements
```

#### `.before(XML fragment) → Sel`

Inserts an XML fragment as a sibling before each node.

#### `.after(String tagName) → Sel`

Inserts an empty sibling after each node in the selection.

#### `.after(XML fragment) → Sel`

Inserts an XML fragment as a sibling after each node.

#### `.replace(XML fragment) → Sel`

Replaces each node in the selection with the XML fragment.

**Important:** Unlike the other structural methods, `replace` returns a **`Sel` containing the new nodes** (the ones that replaced the originals), because the originals no longer exist in the DOM.

```java
Sel newNodes = xml.sel("//old-tag").replace(XML.parse("<new-tag/>"));
newNodes.attr("migrated", "true");
```

### Navigation

#### `.sel(String xpath) → Sel`

Creates a sub-selection: evaluates the XPath **relative to each node** in the current selection.

```java
xml.sel("//div").sel("//a");
// For each <div>, finds all descendant <a> elements
// Note: "//a" is treated as ".//a" in sub-selections
```

**Returns:** Empty `Sel` if no nodes match. The new `Sel` keeps a reference to this selection as its parent (for `.end()`).

#### `.end() → Sel`

Returns to the selection from which this one was created (via `.sel()`).

```java
xml.sel("//div")
    .sel("//p").attr("style", "margin: 0;")
    .end()  // back to //div
    .attr("class", "processed");
```

**Behavior:** If called on a root selection (created by `xml.sel(...)`) returns itself.

### Data binding

#### `.data(List<T> data) → BoundSel<T>`

Associates a data list with the selection using positional matching (first node ↔ first datum, etc.).

#### `.data(List<T> data, Function<T, K> dataKey, Function<Node, K> nodeKey) → BoundSel<T>`

Associates a data list using key-based matching.

```java
xml.sel("//item").data(items, Item::getId, node -> node.attr("id"));
```

### Conversion and iteration

#### `.stream() → Stream<Node>`

Returns a `Stream<Node>` over the selection's nodes. `Stream.empty()` if empty.

#### `.list() → List<Node>`

Returns a `List<Node>` with all nodes. Empty list if the selection is empty.

#### `.first() → Node`

Returns the first node in the selection as a `Node`. Empty `Node` if the selection is empty.

#### `.last() → Node`

Returns the last node in the selection as a `Node`. Empty `Node` if the selection is empty.

#### `.order() → Sel`

Reorders the nodes in the DOM so that their document order matches the selection order.

#### `Iterable<Node>`

`Sel` implements `Iterable<Node>`, enabling use in for-each loops:

```java
for (Node book : xml.sel("//book")) {
    System.out.println(book.attr("id"));
}
```

---

## Node

Extends `Sel`. Represents a single XML node. Supports all `Sel` operations plus DOM navigation methods.

An empty `Node` (null object) is returned by operations like `.first()` on an empty `Sel`. All operations on an empty `Node` are no-ops.

### DOM Navigation

#### `.children() → Sel`

Returns a `Sel` with the node's direct children.

```java
Node book = xml.sel("//book").first();
book.children().attr("visible", "true");    // modifies all children
List<Node> kids = book.children().list();   // list of children
```

**Returns:** Empty `Sel` if the node is a leaf or an empty `Node`.

#### `.parent() → Node`

Returns the parent node.

**Returns:** Empty `Node` if the node is the document root or an empty `Node`.

#### `.name() → String`

Returns the element's tag name.

**Returns:** `""` if empty `Node`.

### Structural insertion

Unlike `Sel`, structural methods on `Node` return the **newly created node**. This enables deep structure construction.

#### `.append(String tagName) → Node`

Creates an empty child and appends it at the end. Returns the **child**.

```java
node.append("chapter")         // → the new <chapter>
    .attr("num", "1")
    .append("title")           // → the new <title> inside <chapter>
        .text("Introduction");
```

#### `.append(XML fragment) → Node`

Appends an XML fragment as a child. Returns the **new node** (the inserted fragment's root).

#### `.prepend(String tagName) → Node`

Creates an empty child and prepends it (before all other children). Returns the **child**.

#### `.prepend(XML fragment) → Node`

Prepends an XML fragment. Returns the **new node**.

#### `.insert(String tagName, Node before) → Node`

Inserts an empty child before the specified `before` node. Returns the **child**.

#### `.before(String tagName) → Node`

Inserts an empty sibling before this node. Returns the **new node**.

#### `.before(XML fragment) → Node`

Inserts an XML fragment as a sibling before this node. Returns the **new node**.

#### `.after(String tagName) → Node`

Inserts an empty sibling after this node. Returns the **new node**.

#### `.after(XML fragment) → Node`

Inserts an XML fragment as a sibling after this node. Returns the **new node**.

#### `.replace(XML fragment) → Node`

Replaces this node with the XML fragment. Returns the **new node** (the replacement). The original node no longer exists in the DOM.

### DOM access

#### `.unwrap() → org.w3c.dom.Node`

Returns the underlying DOM node.

**Returns:** `null` if empty `Node`.

---

## BoundSel\<T\>

Selection with associated data, awaiting the join. Transitional type created by `Sel.data()`.

### `.join(String tagName) → JoinedSel<T>`

Shorthand form. Executes the join with opinionated defaults:

- **enter:** appends a new element with the specified tag.
- **update:** identity (node unchanged).
- **exit:** removes the node.

```java
xml.sel("//item").data(items).join("item")
    .attrWith("name", (current, item) -> item.getName());
```

**Returns:** `JoinedSel<T>` containing the merge of enter and update nodes.

### `.join(JoinConfig<T> config) → JoinedSel<T>`

Form with explicit configuration. See [JoinConfig\<T\>](#joinconfigt).

```java
xml.sel("//item").data(items).join(JoinConfig.<Item>builder()
    .defaults("item")
    .update((node, item) -> node.attr("name", item.getName()))
    .build());
```

---

## JoinConfig\<T\>

Join configuration via builder pattern. Provides granular control over enter, update, and exit.

### `JoinConfig.builder() → Builder<T>` (static)

Creates a new builder.

### Builder methods

#### `.defaults(String tagName) → Builder<T>`

Activates the shorthand behavior as a baseline:

- **enter:** appends an element with the specified tag.
- **update:** identity.
- **exit:** removes the node.

Handlers specified afterward override individual defaults.

#### `.enter(BiFunction<Node, T, Node> fn) → Builder<T>`

Specifies the handler for data items with no corresponding node. The function receives `(parent, datum)` and returns the created node.

Passing `null` means "skip, do not create nodes."

#### `.update(BiFunction<Node, T, Node> fn) → Builder<T>`

Specifies the handler for nodes with a corresponding datum. The function receives `(node, datum)` and returns the node (typically the same one, after modification).

Passing `null` means "identity, do not modify the node."

#### `.exit(Consumer<Node> fn) → Builder<T>`

Specifies the handler for nodes with no corresponding datum. The function receives the node to handle.

Passing `null` means "skip, leave the node in the DOM."

#### `.build() → JoinConfig<T>`

Builds the configuration.

### Value semantics

| Handler state | enter | update | exit |
|---------------|-------|--------|------|
| **From `.defaults()`** | Appends tag | Identity | Removes |
| **Custom handler** | Executes handler | Executes handler | Executes handler |
| **Explicit `null`** | Skipped | Identity | Skipped |
| **Unspecified (without `.defaults()`)** | Skipped | Identity | Skipped |

### Examples

```java
// Start from defaults, override only update
JoinConfig.<Item>builder()
    .defaults("item")
    .update((node, item) -> node.attr("name", item.getName()))
    .build();

// Enter only — don't touch existing nodes, don't remove anything
JoinConfig.<Item>builder()
    .enter((parent, item) -> parent.append("item").attr("id", item.getId()))
    .build();

// Defaults with custom exit
JoinConfig.<Item>builder()
    .defaults("item")
    .exit(node -> node.attr("deprecated", "true"))
    .build();

// Defaults but do NOT remove exit nodes (explicit null)
JoinConfig.<Item>builder()
    .defaults("item")
    .exit(null)
    .build();
```

---

## JoinedSel\<T\>

Extends `Sel`. Result of the join: contains the merged nodes (enter + update) with the associated datum accessible through `with*` methods.

### Methods inherited from Sel

All `Sel` methods are available and work normally (without datum access).

```java
.attr("processed", "true")    // sets on all nodes, without datum
.text("fixed")                 // same
```

### Operations with datum access

#### `.attrWith(String name, BiFunction<String, T, String> fn) → JoinedSel<T>`

Sets the attribute using a function that receives the current attribute value and the associated datum.

```java
.attrWith("name", (current, person) -> person.getFullName())
.attrWith("age", (current, person) -> String.valueOf(person.getAge()))
```

#### `.textWith(Function<T, String> fn) → JoinedSel<T>`

Sets the text content using a function that receives the associated datum.

```java
.textWith(person -> person.getBio())
```

#### `.cdataWith(Function<T, String> fn) → JoinedSel<T>`

Sets the content as a CDATA_SECTION_NODE using a function that receives the associated datum. Symmetric with `textWith` but produces a CDATA node.

```java
.cdataWith(script -> script.getSource())
```

#### `.eachWith(BiConsumer<Node, T> fn) → JoinedSel<T>`

Executes an operation for each node, with access to both the node and the datum.

```java
.eachWith((node, person) -> {
    node.append("address").text(person.getAddress());
    node.append("email").text(person.getEmail());
})
```

### Transitions

#### `.sel(String xpath) → Sel`

Sub-selection. Returns `Sel` (not `JoinedSel`): the data binding is lost because the datum belongs to the node, not to its descendants.

#### `.toSel() → Sel`

Explicitly drops the binding and returns a plain `Sel` with the same nodes.

### Ordering

#### `.order() → JoinedSel<T>`

Reorders the nodes in the DOM so that their order matches the data order.

---

## OutputOptions

Serialization options for `XML.writeTo()`.

### `OutputOptions.builder() → Builder` (static)

#### `.indent(boolean) → Builder`

Enables indentation. Default: `false`.

#### `.indentAmount(int) → Builder`

Number of spaces per indentation level. Default: `2`.

#### `.omitDeclaration(boolean) → Builder`

If `true`, omits the XML declaration. Default: `false`.

#### `.encoding(String) → Builder`

Document encoding. Default: `"UTF-8"`.

#### `.build() → OutputOptions`

Builds the options.
