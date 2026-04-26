# XML Artisan

> Fluent XML document manipulation in Java, with selections inspired by D3.js.

**Package:** `net.fndanko.xml.artisan`

---

## What is XML Artisan

XML Artisan is a Java library that wraps the standard JVM APIs (DOM, XPath) behind a fluent, concise interface. It lets you read, modify, and create XML documents through readable operation chains, with zero external dependencies.

Its standout feature is a **selection-based data binding** system, inspired by the join pattern from [D3.js](https://d3js.org/d3-selection/joining): you can synchronize Java data lists with XML nodes through enter, update, and exit groups.

### Installation

Available on Maven Central.

#### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("net.fn-danko:xml-artisan:1.7.0")
}
```

#### Maven

```xml
<dependency>
  <groupId>net.fn-danko</groupId>
  <artifactId>xml-artisan</artifactId>
  <version>1.7.0</version>
</dependency>
```

#### Java Module System (JPMS)

The library ships as a named module. To use it from a modular project, add the following to your `module-info.java`:

```java
requires net.fndanko.xml.artisan;
```

The module re-exports `java.xml` transitively, so DOM types (`Document`, `Node`) returned by `XML.wrap` and `Node.unwrap` are visible without an extra `requires java.xml`.

---

## Quick Start

### Loading a document

```java
// From file
XML xml = XML.from(Path.of("./catalog.xml"));

// From string
XML xml = XML.parse("<catalog><book id='1'><title>Java</title></book></catalog>");

// Empty document
XML xml = XML.create("catalog");
```

### Reading values

```java
String id = xml.get("/catalog/book/@id");          // "1"
String title = xml.get("/catalog/book[1]/title");   // "Java"
```

### Modifying with selections

```java
// Batch modification on all matching nodes
xml.sel("//book")
    .attr("available", "true");

// Transformation with a function
xml.sel("//a").attr("href", v -> v.replace("http://", "https://"));

// Sub-selections with navigation
xml.sel("//div")
    .attr("class", "container")
    .sel("//p")
        .attr("style", "margin: 0;")
        .sel("//a")
            .attr("target", "_blank")
        .end()      // back to //p
    .end()          // back to //div
    .attr("data-processed", "true");
```

### Iterating

```java
// For-each
for (Node node : xml.sel("//book")) {
    System.out.println(node.attr("id") + ": " + node.text());
}

// Java Stream
List<String> titles = xml.sel("//book/title")
    .stream()
    .map(Node::text)
    .collect(Collectors.toList());
```

### Building structures

```java
Node catalog = xml.sel("//catalog").first();
catalog.append("book")
    .attr("id", "3")
    .attr("lang", "en")
    .append("title")
        .text("New Book");
```

### Synchronizing data with join

```java
List<Item> items = List.of(
    new Item("1", "First"),
    new Item("2", "Second"),
    new Item("3", "Third")
);

// Shorthand form: creates missing nodes, removes extras
xml.sel("//item")
    .data(items)
    .join("item")
    .attrWith("name", (current, item) -> item.getName());

// Full-control form
xml.sel("//item")
    .data(items, Item::getId, node -> node.attr("id"))
    .join(JoinConfig.<Item>builder()
        .defaults("item")
        .update((node, item) -> node.attr("name", item.getName()))
        .exit(node -> node.attr("deprecated", "true"))
        .build());
```

### Saving

```java
xml.writeTo(Path.of("./output.xml"));

// With options
xml.writeTo(Path.of("./output.xml"), OutputOptions.builder()
    .indent(true)
    .indentAmount(2)
    .encoding("UTF-8")
    .build());
```

---

## Key Principles

- **Fluent chaining.** Every operation returns a valid object to continue the chain.
- **No exceptions during chaining.** Empty selections, XPath queries with no results, operations on empty sets — everything is handled silently as a no-op.
- **Everything is a selection.** `Node` extends `Sel`: a single node is a selection of cardinality 1 with additional DOM navigation methods.
- **Two clear modes.** Without `.data()`, operations are immediate. With `.data()`, you enter a declarative (lazy) mode that resolves at `.join()`.
- **Zero dependencies.** Only standard JVM APIs.

---

## Use Cases

### Batch transformation

```java
XML xml = XML.from(Path.of("./report.xml"));
xml.sel("//field[@type='date']")
    .attr("format", "ISO-8601")
    .text(v -> v.replace("/", "-"));
xml.writeTo(Path.of("./report-fixed.xml"));
```

### Content insertion

```java
// Insert an element before every section
xml.sel("//section")
    .before(XML.parse("<hr/>"))
    .attr("class", "separated");

// Replace obsolete elements
xml.sel("//old-tag")
    .replace(XML.parse("<new-tag/>"))
    .attr("migrated", "true");
```

### Combining XML fragments

```java
XML main = XML.from(Path.of("./main.xml"));
XML extra = XML.parse("<note priority='high'>Urgent</note>");
main.sel("//task").first().append(extra);
```

### Generating from data

```java
XML xml = XML.create("people");
List<Person> people = fetchPeople();

xml.sel("//person")
    .data(people)
    .join("person")
    .eachWith((node, person) -> {
        node.attr("id", person.getId());
        node.append("name").text(person.getFullName());
        node.append("email").text(person.getEmail());
    });
```

---

## Documentation

- **[DESIGN.md](./docs/DESIGN.md)** — Vision, principles, and design decisions with their rationale.
- **[ARCHITECTURE.md](./docs/ARCHITECTURE.md)** — Internal structure, implementation patterns, and technical details.
- **[API.md](./docs/API.md)** — Complete public API reference.
- **[TESTING.md](./docs/TESTING.md)** — Testing strategy and test structure.
