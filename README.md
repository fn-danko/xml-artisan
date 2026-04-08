# XML Artisan

> Manipolazione fluent di documenti XML in Java, con selezioni ispirate a D3.js.

**Package:** `net.fndanko.xml.artisan`

---

## Cos'è XML Artisan

XML Artisan è una libreria Java che avvolge le API standard della JVM (DOM, XPath) in un'interfaccia fluent e concisa. Permette di leggere, modificare e creare documenti XML con catene di operazioni leggibili, senza dipendenze esterne.

La caratteristica distintiva è il sistema di **selezioni con data binding**, ispirato al join pattern di [D3.js](https://d3js.org/d3-selection/joining): si possono sincronizzare liste di dati Java con nodi XML tramite i gruppi enter, update e exit.

### Installazione

Il pacchetto è distribuito tramite [GitHub Packages](https://github.com/fn-danko/xml-artisan/packages). È necessario configurare il registry e autenticarsi con un token GitHub con scope `read:packages`.

#### Gradle (Kotlin DSL)

In `settings.gradle.kts` o `build.gradle.kts`, aggiungere il repository:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/fn-danko/xml-artisan")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

Poi aggiungere la dipendenza:

```kotlin
dependencies {
    implementation("net.fndanko:xml-artisan:1.4.2")
}
```

Le credenziali possono essere definite in `~/.gradle/gradle.properties`:

```properties
gpr.user=IL_TUO_USERNAME_GITHUB
gpr.key=IL_TUO_TOKEN_GITHUB
```

#### Maven

Aggiungere il repository in `~/.m2/settings.xml`:

```xml
<servers>
  <server>
    <id>github</id>
    <username>IL_TUO_USERNAME_GITHUB</username>
    <password>IL_TUO_TOKEN_GITHUB</password>
  </server>
</servers>
```

Poi nel `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/fn-danko/xml-artisan</url>
  </repository>
</repositories>

<dependency>
  <groupId>net.fndanko</groupId>
  <artifactId>xml-artisan</artifactId>
  <version>1.4.2</version>
</dependency>
```

---

## Quick Start

### Caricare un documento

```java
// Da file
XML xml = XML.from(Path.of("./catalog.xml"));

// Da stringa
XML xml = XML.parse("<catalog><book id='1'><title>Java</title></book></catalog>");

// Documento vuoto
XML xml = XML.create("catalog");
```

### Leggere valori

```java
String id = xml.get("/catalog/book/@id");          // "1"
String title = xml.get("/catalog/book[1]/title");   // "Java"
```

### Modificare con selezioni

```java
// Modifica batch su tutti i nodi che matchano
xml.sel("//book")
    .attr("available", "true");

// Trasformazione con funzione
xml.sel("//a").attr("href", v -> v.replace("http://", "https://"));

// Sotto-selezioni con navigazione
xml.sel("//div")
    .attr("class", "container")
    .sel("//p")
        .attr("style", "margin: 0;")
        .sel("//a")
            .attr("target", "_blank")
        .end()      // risale a //p
    .end()          // risale a //div
    .attr("data-processed", "true");
```

### Iterare

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

### Costruire strutture

```java
Node catalog = xml.sel("//catalog").first();
catalog.append("book")
    .attr("id", "3")
    .attr("lang", "it")
    .append("title")
        .text("Nuovo Libro");
```

### Sincronizzare dati con il join

```java
List<Item> items = List.of(
    new Item("1", "Primo"),
    new Item("2", "Secondo"),
    new Item("3", "Terzo")
);

// Forma abbreviata: crea i mancanti, rimuove gli eccessi
xml.sel("//item")
    .data(items)
    .join("item")
    .attrWith("name", (current, item) -> item.getName());

// Forma con controllo completo
xml.sel("//item")
    .data(items, Item::getId, node -> node.attr("id"))
    .join(JoinConfig.<Item>builder()
        .defaults("item")
        .update((node, item) -> node.attr("name", item.getName()))
        .exit(node -> node.attr("deprecated", "true"))
        .build());
```

### Salvare

```java
xml.writeTo(Path.of("./output.xml"));

// Con opzioni
xml.writeTo(Path.of("./output.xml"), OutputOptions.builder()
    .indent(true)
    .indentAmount(2)
    .encoding("UTF-8")
    .build());
```

---

## Principi chiave

- **Fluent chaining.** Ogni operazione restituisce un oggetto valido per continuare la catena.
- **Mai eccezioni durante il chaining.** Selezioni vuote, XPath senza risultati, operazioni su insiemi vuoti — tutto è gestito silenziosamente come no-op.
- **Tutto è una selezione.** `Node` estende `Sel`: un singolo nodo è una selezione di cardinalità 1 con metodi di navigazione DOM aggiuntivi.
- **Due modalità chiare.** Senza `.data()` le operazioni sono immediate. Con `.data()` si entra in modalità dichiarativa (lazy) che si applica al `.join()`.
- **Zero dipendenze.** Solo API standard della JVM.

---

## Casi d'uso

### Trasformazione batch

```java
XML xml = XML.from(Path.of("./report.xml"));
xml.sel("//field[@type='date']")
    .attr("format", "ISO-8601")
    .text(v -> v.replace("/", "-"));
xml.writeTo(Path.of("./report-fixed.xml"));
```

### Inserimento di contenuto

```java
// Aggiunge un elemento prima di ogni sezione
xml.sel("//section")
    .before(XML.parse("<hr/>"))
    .attr("class", "separated");

// Sostituisce elementi obsoleti
xml.sel("//old-tag")
    .replace(XML.parse("<new-tag/>"))
    .attr("migrated", "true");
```

### Combinazione di frammenti XML

```java
XML main = XML.from(Path.of("./main.xml"));
XML extra = XML.parse("<note priority='high'>Urgente</note>");
main.sel("//task").first().append(extra);
```

### Generazione da dati

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

## Documentazione

- **[DESIGN.md](./docs/DESIGN.md)** — Visione, principi e decisioni progettuali con le loro motivazioni.
- **[ARCHITECTURE.md](./docs/ARCHITECTURE.md)** — Struttura interna, pattern implementativi, dettagli tecnici.
- **[API.md](./docs/API.md)** — Reference completo dell'API pubblica.
- **[TESTING.md](./docs/TESTING.md)** — Strategia di testing e struttura dei test.

