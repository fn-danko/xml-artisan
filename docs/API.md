# XML Artisan — API Reference

> Reference completo dell'API pubblica della libreria.

**Package:** `net.fndanko.xml.artisan`

---

## Indice

1. [XML](#xml)
2. [Sel](#sel)
3. [Node](#node)
4. [BoundSel\<T\>](#boundselt)
5. [JoinConfig\<T\>](#joinconfigt)
6. [JoinedSel\<T\>](#joinedselt)
7. [OutputOptions](#outputoptions)

---

## XML

Entry point della libreria. Rappresenta un documento XML in memoria.

### Factory methods

#### `XML.from(Path path) → XML`

Carica e parsa un documento XML da file.

```java
XML xml = XML.from(Path.of("./catalog.xml"));
```

**Eccezioni:** `UncheckedIOException` se il file non esiste. Unchecked exception se l'XML è malformato.

#### `XML.parse(String xmlString) → XML`

Parsa un documento XML da stringa.

```java
XML xml = XML.parse("<root><item id='1'/></root>");
```

**Eccezioni:** Unchecked exception se l'XML è malformato.

#### `XML.create(String rootTagName) → XML`

Crea un documento vuoto con il root element specificato.

```java
XML xml = XML.create("catalog");
// Produce: <catalog/>
```

### Lettura e scrittura puntuale

#### `.get(String xpath) → String`

Valuta l'espressione XPath dalla radice del documento e restituisce il valore come stringa.

```java
String id = xml.get("/catalog/book/@id");
String title = xml.get("/catalog/book[1]/title");
```

**Ritorno:** Il valore del nodo trovato. `""` (stringa vuota) se l'XPath non trova nodi.

#### `.set(String xpath, String value) → void`

Imposta il valore del nodo trovato dall'XPath.

```java
xml.set("/catalog/book[1]/@id", "new-id");
xml.set("/catalog/book[1]/title", "Nuovo titolo");
```

**Comportamento:** No-op se l'XPath non trova nodi.

### Selezioni

#### `.sel(String xpath) → Sel`

Crea una selezione con tutti i nodi che matchano l'espressione XPath, valutata dalla radice del documento.

```java
Sel books = xml.sel("//book");
Sel italianBooks = xml.sel("//book[@lang='it']");
```

**Ritorno:** `Sel` vuoto se nessun nodo corrisponde. Mai `null`.

### Namespace

#### `.namespace(String prefix, String uri) → XML`

Registra un prefisso namespace per l'uso nelle espressioni XPath.

```java
xml.namespace("dc", "http://purl.org/dc/elements/1.1/");
xml.sel("//dc:title").text("Nuovo titolo");
```

**Ritorno:** L'oggetto `XML` stesso (per chaining).

### Text normalization

#### `.normalizeText() → XML`

Applica `normalizeText()` ricorsivamente a tutto il documento (depth-first post-order: prima normalizza i figli, poi il nodo stesso).

```java
xml.normalizeText();
```

**Ritorno:** L'oggetto `XML` stesso (per chaining).

### Serializzazione

#### `.toString() → String`

Serializza il documento XML completo, con dichiarazione XML.

#### `.toFragment() → String`

Serializza il documento XML senza dichiarazione XML.

#### `.writeTo(Path path) → void`

Scrive il documento su file con opzioni di default.

#### `.writeTo(Path path, OutputOptions options) → void`

Scrive il documento su file con opzioni personalizzate.

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

Selezione di zero o più nodi. Tutte le operazioni di modifica sono immediate (agiscono sul DOM nel momento della chiamata) e restituiscono `Sel` per il chaining. Implementa `Iterable<Node>`.

### Lettura

#### `.attr(String name) → String`

Restituisce il valore dell'attributo dal **primo nodo** della selezione.

**Ritorno:** `""` se la selezione è vuota o l'attributo non esiste.

#### `.text() → String`

Restituisce il testo diretto del **primo nodo** della selezione — concatena il contenuto di tutti i nodi TEXT e CDATA figli diretti, senza modificare il DOM.

**Ritorno:** `""` se la selezione è vuota o non ha testo diretto.

```java
// <p>Hello <b>world</b> today</p>
xml.sel("//p").text();   // → "Hello  today" (solo testo diretto, non ricorsivo)
```

#### `.deepText() → String`

Restituisce il testo ricorsivo di tutti i discendenti del **primo nodo** (equivalente a `getTextContent()`).

**Ritorno:** `""` se la selezione è vuota.

```java
// <p>Hello <b>world</b> today</p>
xml.sel("//p").deepText();   // → "Hello world today"
```

#### `.size() → int`

Numero di nodi nella selezione.

#### `.empty() → boolean`

`true` se la selezione non contiene nodi.

### Modifica

Tutte le operazioni di modifica restituiscono la **stessa `Sel`** per il chaining. Su selezione vuota sono no-op.

#### `.attr(String name, String value) → Sel`

Imposta l'attributo con valore fisso su **tutti i nodi** della selezione.

```java
xml.sel("//p").attr("style", "text-align: center;");
```

#### `.attr(String name, Function<String, String> fn) → Sel`

Trasforma l'attributo su ogni nodo. La funzione riceve il valore corrente e restituisce il nuovo valore.

```java
xml.sel("//a").attr("href", v -> v.replace("http://", "https://"));
```

#### `.text(String value) → Sel`

Imposta il testo diretto su tutti i nodi. Chiama `normalizeText()` internamente, rimuove tutti i nodi TEXT/CDATA figli diretti e inserisce un singolo TEXT_NODE come primo figlio. Preserva tutti gli elementi figli.

```java
xml.sel("//title").text("Nuovo titolo");
// <p>Hello <b>world</b> today</p> + .text("New") → <p>New<b>world</b></p>
```

#### `.text(Function<String, String> fn) → Sel`

Trasforma il testo diretto su ogni nodo. Legge con `text()` (senza side-effect), applica la funzione, scrive con `text(String)`.

```java
xml.sel("//code").text(v -> v.trim());
```

#### `.cdata(String value) → Sel`

Identico a `text(String)` ma inserisce un CDATA_SECTION_NODE invece di TEXT_NODE. Chiama `normalizeText()` internamente, rimuove tutti i nodi TEXT/CDATA figli diretti e inserisce un singolo CDATA_SECTION_NODE come primo figlio. Preserva tutti gli elementi figli.

```java
xml.sel("//script").cdata("var x = '<div>hello</div>';");
// Il contenuto con caratteri speciali XML è preservato nel CDATA
```

**Nota:** `text()` in lettura restituisce il contenuto sia di TEXT che di CDATA — la distinzione è solo in scrittura.

#### `.content(XML fragment) → Sel`

Sostituisce **tutti** i figli di ogni nodo nella selezione con il root element del frammento XML. Preserva il nodo stesso (tag, attributi, posizione nel DOM).

```java
// <p class="x">old text</p>  →  <p class="x"><div><b>new</b> content</div></p>
xml.sel("//p").content(XML.parse("<div><b>new</b> content</div>"));
```

#### `.content(String xmlContent) → Sel`

Variante stringa. Accetta una stringa XML che può contenere mixed content senza un singolo root. Internamente wrappata in un tag sintetico, parsata, e i **figli** del wrapper vengono importati.

```java
// <p class="x">old text</p>  →  <p class="x"><b>new</b> content</p>
xml.sel("//p").content("<b>new</b> content");
```

**Differenza chiave:**
- `content(XML)` → importa il root element del frammento (un singolo nodo)
- `content(String)` → parsa con wrapper, importa i figli del wrapper (può essere mixed content)

#### `.normalizeText() → Sel`

Unifica i nodi TEXT/CDATA figli diretti frammentati in un singolo nodo, posizionato come primo figlio. Se almeno uno era CDATA, il risultato è CDATA. Lascia intatti gli elementi figli.

```java
// <p>Hello <b>world</b> today</p> → <p>Hello  today<b>world</b></p>
xml.sel("//p").normalizeText();
```

#### `.coalesceText() → Sel`

Normalizzazione distruttiva: raccoglie `getTextContent()`, rimuove **tutti** i figli, setta un singolo nodo testo. Distrugge gli elementi figli.

```java
// <p>Hello <b>world</b> today</p> → <p>Hello world today</p>
xml.sel("//p").coalesceText();
```

#### `.remove() → Sel`

Rimuove tutti i nodi della selezione dal DOM.

**Ritorno:** La selezione padre (da cui questa selezione è stata creata).

### Inserimento strutturale

Operazioni di side-effect che **restituiscono la selezione originale** (non i nodi inseriti). L'inserimento avviene per ogni nodo della selezione.

#### `.append(String tagName) → Sel`

Aggiunge un figlio vuoto con il tag specificato a ogni nodo della selezione.

```java
xml.sel("//item").append("status");
// Aggiunge <status/> come ultimo figlio di ogni <item>
// Ritorna la selezione degli <item>, non dei <status>
```

#### `.append(XML fragment) → Sel`

Aggiunge un frammento XML come figlio a ogni nodo della selezione.

#### `.prepend(String tagName) → Sel`

Aggiunge un figlio vuoto in testa (come primo figlio) a ogni nodo.

#### `.prepend(XML fragment) → Sel`

Aggiunge un frammento XML in testa a ogni nodo.

#### `.before(String tagName) → Sel`

Inserisce un sibling vuoto prima di ogni nodo della selezione.

```java
xml.sel("//section").before(XML.parse("<hr/>"));
// Inserisce <hr/> prima di ogni <section>
// Ritorna la selezione delle <section>
```

#### `.before(XML fragment) → Sel`

Inserisce un frammento XML come sibling prima di ogni nodo.

#### `.after(String tagName) → Sel`

Inserisce un sibling vuoto dopo ogni nodo della selezione.

#### `.after(XML fragment) → Sel`

Inserisce un frammento XML come sibling dopo ogni nodo.

#### `.replace(XML fragment) → Sel`

Sostituisce ogni nodo della selezione con il frammento XML.

**Attenzione:** A differenza degli altri metodi strutturali, `replace` restituisce un **`Sel` con i nuovi nodi** (quelli che hanno sostituito gli originali), perché gli originali non esistono più nel DOM.

```java
Sel newNodes = xml.sel("//old-tag").replace(XML.parse("<new-tag/>"));
newNodes.attr("migrated", "true");
```

### Navigazione

#### `.sel(String xpath) → Sel`

Crea una sotto-selezione: valuta l'XPath **relativamente a ciascun nodo** della selezione corrente.

```java
xml.sel("//div").sel("//a");
// Per ogni <div>, cerca tutti i discendenti <a>
// Nota: "//a" è trattato come ".//a" nelle sotto-selezioni
```

**Ritorno:** `Sel` vuoto se nessun nodo corrisponde. Il nuovo `Sel` mantiene un riferimento a questa selezione come padre (per `.end()`).

#### `.end() → Sel`

Risale alla selezione da cui questa è stata generata (tramite `.sel()`).

```java
xml.sel("//div")
    .sel("//p").attr("style", "margin: 0;")
    .end()  // torna a //div
    .attr("class", "processed");
```

**Comportamento:** Se chiamato su una selezione radice (creata da `xml.sel(...)`) restituisce sé stessa.

### Data binding

#### `.data(List<T> data) → BoundSel<T>`

Associa una lista di dati alla selezione con matching posizionale (primo nodo ↔ primo dato, ecc.).

#### `.data(List<T> data, Function<T, K> dataKey, Function<Node, K> nodeKey) → BoundSel<T>`

Associa una lista di dati con matching per key function.

```java
xml.sel("//item").data(items, Item::getId, node -> node.attr("id"));
```

### Conversione e iterazione

#### `.stream() → Stream<Node>`

Restituisce un `Stream<Node>` sui nodi della selezione. `Stream.empty()` se vuota.

#### `.list() → List<Node>`

Restituisce una `List<Node>` con tutti i nodi. Lista vuota se selezione vuota.

#### `.first() → Node`

Restituisce il primo nodo della selezione come `Node`. `Node` vuoto se selezione vuota.

#### `.last() → Node`

Restituisce l'ultimo nodo della selezione come `Node`. `Node` vuoto se selezione vuota.

#### `.order() → Sel`

Riordina i nodi nel DOM in modo che il loro ordine nel documento corrisponda all'ordine nella selezione.

#### `Iterable<Node>`

`Sel` implementa `Iterable<Node>`, consentendo l'uso in for-each:

```java
for (Node book : xml.sel("//book")) {
    System.out.println(book.attr("id"));
}
```

---

## Node

Estende `Sel`. Rappresenta un singolo nodo XML. Supporta tutte le operazioni di `Sel` più metodi di navigazione DOM.

Un `Node` vuoto (null object) è restituito da operazioni come `.first()` su `Sel` vuoto. Tutte le operazioni su un `Node` vuoto sono no-op.

### Navigazione DOM

#### `.children() → Sel`

Restituisce una `Sel` con i figli diretti del nodo.

```java
Node book = xml.sel("//book").first();
book.children().attr("visible", "true");    // modifica tutti i figli
List<Node> kids = book.children().list();   // lista dei figli
```

**Ritorno:** `Sel` vuoto se nodo foglia o `Node` vuoto.

#### `.parent() → Node`

Restituisce il nodo padre.

**Ritorno:** `Node` vuoto se il nodo è la radice del documento o se è un `Node` vuoto.

#### `.name() → String`

Restituisce il nome del tag dell'elemento.

**Ritorno:** `""` se `Node` vuoto.

### Inserimento strutturale

A differenza di `Sel`, i metodi strutturali su `Node` restituiscono il **nuovo nodo creato**. Questo permette la costruzione di strutture in profondità.

#### `.append(String tagName) → Node`

Crea un figlio vuoto e lo aggiunge in coda. Restituisce il **figlio**.

```java
node.append("chapter")         // → il nuovo <chapter>
    .attr("num", "1")
    .append("title")           // → il nuovo <title> dentro <chapter>
        .text("Introduzione");
```

#### `.append(XML fragment) → Node`

Aggiunge un frammento XML come figlio. Restituisce il **nuovo nodo** (root del frammento inserito).

#### `.prepend(String tagName) → Node`

Crea un figlio vuoto e lo aggiunge in testa (prima di tutti gli altri figli). Restituisce il **figlio**.

#### `.prepend(XML fragment) → Node`

Aggiunge un frammento XML in testa. Restituisce il **nuovo nodo**.

#### `.insert(String tagName, Node before) → Node`

Inserisce un figlio vuoto prima del nodo `before` specificato. Restituisce il **figlio**.

#### `.before(String tagName) → Node`

Inserisce un sibling vuoto prima di questo nodo. Restituisce il **nuovo nodo**.

#### `.before(XML fragment) → Node`

Inserisce un frammento XML come sibling prima. Restituisce il **nuovo nodo**.

#### `.after(String tagName) → Node`

Inserisce un sibling vuoto dopo questo nodo. Restituisce il **nuovo nodo**.

#### `.after(XML fragment) → Node`

Inserisce un frammento XML come sibling dopo. Restituisce il **nuovo nodo**.

#### `.replace(XML fragment) → Node`

Sostituisce questo nodo con il frammento XML. Restituisce il **nuovo nodo** (il sostituto). Il nodo originale non esiste più nel DOM.

### Accesso DOM

#### `.unwrap() → org.w3c.dom.Node`

Restituisce il nodo DOM sottostante.

**Ritorno:** `null` se `Node` vuoto.

---

## BoundSel\<T\>

Selezione con dati associati, in attesa del join. Tipo transitorio creato da `Sel.data()`.

### `.join(String tagName) → JoinedSel<T>`

Forma abbreviata (shorthand). Esegue il join con default opinionati:

- **enter:** appende un nuovo elemento con il tag specificato.
- **update:** identity (nodo invariato).
- **exit:** rimuove il nodo.

```java
xml.sel("//item").data(items).join("item")
    .attrWith("name", (current, item) -> item.getName());
```

**Ritorno:** `JoinedSel<T>` contenente la merge di nodi enter e update.

### `.join(JoinConfig<T> config) → JoinedSel<T>`

Forma con configurazione esplicita. Vedi [JoinConfig\<T\>](#joinconfigt).

```java
xml.sel("//item").data(items).join(JoinConfig.<Item>builder()
    .defaults("item")
    .update((node, item) -> node.attr("name", item.getName()))
    .build());
```

---

## JoinConfig\<T\>

Configurazione del join tramite builder pattern. Permette controllo granulare su enter, update, exit.

### `JoinConfig.builder() → Builder<T>` (static)

Crea un nuovo builder.

### Builder methods

#### `.defaults(String tagName) → Builder<T>`

Attiva il comportamento della shorthand come base:

- **enter:** appende un elemento con il tag specificato.
- **update:** identity.
- **exit:** rimuove il nodo.

Gli handler specificati successivamente sovrascrivono i singoli default.

#### `.enter(BiFunction<Node, T, Node> fn) → Builder<T>`

Specifica l'handler per i dati senza nodo corrispondente. La funzione riceve `(parent, dato)` e restituisce il nodo creato.

Passare `null` significa "ignora, non creare nodi".

#### `.update(BiFunction<Node, T, Node> fn) → Builder<T>`

Specifica l'handler per i nodi con dato corrispondente. La funzione riceve `(nodo, dato)` e restituisce il nodo (tipicamente lo stesso, dopo averlo modificato).

Passare `null` significa "identity, non modificare il nodo".

#### `.exit(Consumer<Node> fn) → Builder<T>`

Specifica l'handler per i nodi senza dato corrispondente. La funzione riceve il nodo da gestire.

Passare `null` significa "ignora, lascia il nodo nel DOM".

#### `.build() → JoinConfig<T>`

Costruisce la configurazione.

### Semantica dei valori

| Stato handler | enter | update | exit |
|---------------|-------|--------|------|
| **Da `.defaults()`** | Appende tag | Identity | Rimuove |
| **Handler custom** | Esegue handler | Esegue handler | Esegue handler |
| **`null` esplicito** | Ignora | Identity | Ignora |
| **Non specificato (senza `.defaults()`)** | Ignora | Identity | Ignora |

### Esempi

```java
// Parto dai default, sovrascrivo solo update
JoinConfig.<Item>builder()
    .defaults("item")
    .update((node, item) -> node.attr("name", item.getName()))
    .build();

// Solo enter, non tocco esistenti, non rimuovo
JoinConfig.<Item>builder()
    .enter((parent, item) -> parent.append("item").attr("id", item.getId()))
    .build();

// Default con exit personalizzato
JoinConfig.<Item>builder()
    .defaults("item")
    .exit(node -> node.attr("deprecated", "true"))
    .build();

// Default ma NON rimuovere gli exit (null esplicito)
JoinConfig.<Item>builder()
    .defaults("item")
    .exit(null)
    .build();
```

---

## JoinedSel\<T\>

Estende `Sel`. Risultato del join: contiene i nodi merged (enter + update) con il dato associato accessibile tramite metodi `with*`.

### Metodi ereditati da Sel

Tutti i metodi di `Sel` sono disponibili e funzionano normalmente (senza accesso al dato).

```java
.attr("processed", "true")    // imposta su tutti i nodi, senza dato
.text("fixed")                 // idem
```

### Operazioni con accesso al dato

#### `.attrWith(String name, BiFunction<String, T, String> fn) → JoinedSel<T>`

Imposta l'attributo usando una funzione che riceve il valore corrente dell'attributo e il dato associato.

```java
.attrWith("name", (current, person) -> person.getFullName())
.attrWith("age", (current, person) -> String.valueOf(person.getAge()))
```

#### `.textWith(Function<T, String> fn) → JoinedSel<T>`

Imposta il contenuto testuale usando una funzione che riceve il dato associato.

```java
.textWith(person -> person.getBio())
```

#### `.cdataWith(Function<T, String> fn) → JoinedSel<T>`

Imposta il contenuto come CDATA_SECTION_NODE usando una funzione che riceve il dato associato. Simmetrico a `textWith` ma produce un nodo CDATA.

```java
.cdataWith(script -> script.getSource())
```

#### `.eachWith(BiConsumer<Node, T> fn) → JoinedSel<T>`

Esegue un'operazione per ogni nodo, con accesso al nodo e al dato.

```java
.eachWith((node, person) -> {
    node.append("address").text(person.getAddress());
    node.append("email").text(person.getEmail());
})
```

### Transizioni

#### `.sel(String xpath) → Sel`

Sotto-selezione. Restituisce `Sel` (non `JoinedSel`): il binding dati viene perso perché il dato è del nodo, non dei suoi discendenti.

#### `.toSel() → Sel`

Abbandona esplicitamente il binding e restituisce un `Sel` normale con gli stessi nodi.

### Ordinamento

#### `.order() → JoinedSel<T>`

Riordina i nodi nel DOM in modo che il loro ordine corrisponda all'ordine dei dati.

---

## OutputOptions

Opzioni di serializzazione per `XML.writeTo()`.

### `OutputOptions.builder() → Builder` (static)

#### `.indent(boolean) → Builder`

Abilita l'indentazione. Default: `false`.

#### `.indentAmount(int) → Builder`

Numero di spazi per livello di indentazione. Default: `2`.

#### `.omitDeclaration(boolean) → Builder`

Se `true`, omette la dichiarazione XML. Default: `false`.

#### `.encoding(String) → Builder`

Encoding del documento. Default: `"UTF-8"`.

#### `.build() → OutputOptions`

Costruisce le opzioni.
