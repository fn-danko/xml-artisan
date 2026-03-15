# XML Artisan — Architettura

> Struttura interna, pattern implementativi e dettagli tecnici per chi contribuisce al codice.

---

## Indice

1. [Panoramica dell'architettura](#panoramica-dellarchitettura)
2. [Gerarchia dei tipi](#gerarchia-dei-tipi)
3. [Dettaglio dei tipi pubblici](#dettaglio-dei-tipi-pubblici)
4. [Pattern implementativi](#pattern-implementativi)
5. [Gestione XPath](#gestione-xpath)
6. [Meccanica del join](#meccanica-del-join)
7. [Resilienza interna](#resilienza-interna)
8. [Thread safety](#thread-safety)
9. [Dipendenze JVM](#dipendenze-jvm)

---

## Panoramica dell'architettura

XML Artisan è un layer sottile sopra le API standard DOM e XPath della JVM. Non reimplementa il parsing né la gestione del DOM — usa `javax.xml.parsers.DocumentBuilder` per il parsing e `org.w3c.dom` come struttura dati sottostante.

L'architettura è composta da:

- **Layer di wrapping DOM** — `XML`, `Sel`, `Node` avvolgono il DOM standard offrendo un'interfaccia fluent.
- **Layer XPath** — Gestione centralizzata delle espressioni XPath con supporto per contesti relativi e namespace.
- **Layer di data binding** — `BoundSel<T>`, `JoinedSel<T>`, `JoinConfig<T>` implementano il pattern join sopra le selezioni.

```
┌─────────────────────────────────────────────┐
│              API Pubblica                    │
│   XML, Sel, Node, BoundSel, JoinedSel       │
├─────────────────────────────────────────────┤
│           Layer di Data Binding              │
│   JoinConfig, data join, merge              │
├─────────────────────────────────────────────┤
│           Layer XPath                        │
│   Context node, rewrite //, namespace        │
├─────────────────────────────────────────────┤
│           Layer di Wrapping DOM              │
│   Wrapper attorno a org.w3c.dom              │
├─────────────────────────────────────────────┤
│           API Standard JVM                   │
│   javax.xml.parsers, org.w3c.dom,            │
│   javax.xml.xpath, javax.xml.transform       │
└─────────────────────────────────────────────┘
```

---

## Gerarchia dei tipi

### Tipi pubblici

```
XML                                             // entry point, wrappa un Document
 │
 ├── Sel                                        // selezione di N nodi, operazioni immediate
 │    └── Node extends Sel                      // selezione di 1 nodo + navigazione DOM
 │
 ├── BoundSel<T>                                // selezione con dati associati, lazy
 │
 ├── JoinedSel<T> extends Sel                   // post-join: merged enter+update, dato accessibile
 │
 ├── JoinConfig<T>                              // configurazione join via builder
 │    └── JoinConfig.Builder<T>                 // builder per JoinConfig
 │
 └── OutputOptions                              // opzioni di serializzazione
      └── OutputOptions.Builder                 // builder per OutputOptions
```

### Relazioni tra tipi (flusso di transizione)

```
XML ──.sel(xpath)──→ Sel ──.data(list)──→ BoundSel<T> ──.join(...)──→ JoinedSel<T>
                      │                                                    │
                      │←──────────.toSel()─────────────────────────────────┘
                      │←──────────.sel(xpath)───────────────────────────────┘
                      │
                      ├──.first()/.last()──→ Node (extends Sel)
                      ├──.end()──→ Sel (padre)
                      └──.stream()──→ Stream<Node>
```

---

## Dettaglio dei tipi pubblici

### `XML`

Entry point della libreria. Wrappa un `org.w3c.dom.Document` e fornisce factory methods, lettura/scrittura puntuale, creazione di selezioni e serializzazione.

```
XML
 ├── // Factory methods (static)
 ├── XML.from(Path) → XML                          // parsing da file
 ├── XML.parse(String) → XML                       // parsing da stringa
 ├── XML.create(String rootTag) → XML               // documento vuoto con root
 │
 ├── // Lettura e scrittura puntuale
 ├── .get(xpath) → String                           // "" se non trovato
 ├── .set(xpath, value) → void                      // no-op se non trovato
 │
 ├── // Selezioni
 ├── .sel(xpath) → Sel                              // Sel vuoto se nessun match
 │
 ├── // Namespace
 ├── .namespace(prefix, uri) → XML                  // registra namespace per XPath
 │
 ├── // Serializzazione
 ├── .toString() → String                           // con dichiarazione XML
 ├── .toFragment() → String                         // senza dichiarazione
 └── .writeTo(Path, OutputOptions?) → void          // scrittura su file
```

**Internamente** mantiene un riferimento a `org.w3c.dom.Document` e un'istanza condivisa di `javax.xml.xpath.XPath` con i namespace registrati.

### `Sel`

Selezione di zero o più nodi. Tutte le operazioni di modifica sono immediate (side-effect sul DOM) e restituiscono `Sel` per il chaining.

```
Sel
 ├── // Lettura (dal primo nodo)
 ├── .attr(name) → String                          // "" se vuoto o attributo assente
 ├── .text() → String                              // "" se vuoto o senza text content
 ├── .size() → int
 ├── .empty() → boolean
 │
 ├── // Modifica (immediata, restituisce la stessa Sel)
 ├── .attr(name, value) → Sel
 ├── .attr(name, Function<String,String>) → Sel     // fn(valoreCorrente) → nuovo
 ├── .text(value) → Sel
 ├── .text(Function<String,String>) → Sel
 ├── .remove() → Sel                                // ritorna selezione padre
 │
 ├── // Inserimento strutturale (side-effect, restituisce la stessa Sel)
 ├── .append(tagName) → Sel
 ├── .append(XML) → Sel
 ├── .prepend(tagName) → Sel
 ├── .prepend(XML) → Sel
 ├── .before(tagName) → Sel
 ├── .before(XML) → Sel
 ├── .after(tagName) → Sel
 ├── .after(XML) → Sel
 ├── .replace(XML) → Sel                            // ECCEZIONE: ritorna Sel con i nuovi nodi
 │
 ├── // Navigazione
 ├── .sel(xpath) → Sel                              // sotto-selezione contestuale
 ├── .end() → Sel                                   // selezione padre (sé stessa se radice)
 │
 ├── // Data binding
 ├── .data(List<T>) → BoundSel<T>                   // posizionale
 ├── .data(List<T>, Function<T,K>, Function<Node,K>) → BoundSel<T>  // con key function
 │
 ├── // Conversione e iterazione
 ├── .stream() → Stream<Node>
 ├── .list() → List<Node>
 ├── .first() → Node                                // Node vuoto se selezione vuota
 ├── .last() → Node                                 // Node vuoto se selezione vuota
 ├── .order() → Sel                                  // riordina nel DOM
 └── implements Iterable<Node>
```

**Internamente** mantiene una `List<org.w3c.dom.Node>`, un riferimento al `Sel` padre (per `.end()`) e un riferimento all'`XML` di appartenenza (per accesso a XPath e Document).

**Nota semantica sui metodi strutturali:** `append`/`prepend`/`before`/`after` su `Sel` restituiscono la selezione originale (l'inserimento è un side-effect). Il focus della catena resta sugli elementi selezionati. `replace` è l'eccezione: restituisce un `Sel` con i nuovi nodi perché gli originali non esistono più nel DOM.

### `Node extends Sel`

Specializzazione per un singolo nodo. Eredita tutto da `Sel` e aggiunge navigazione DOM. I metodi strutturali hanno **semantica diversa** da `Sel`: restituiscono il nuovo nodo creato.

```
Node extends Sel
 ├── (eredita tutto da Sel)
 │
 ├── // Navigazione DOM
 ├── .children() → Sel                              // figli come selezione
 ├── .parent() → Node                               // Node vuoto se radice
 ├── .name() → String                               // nome tag, "" se vuoto
 │
 ├── // Inserimento (ritorna il NUOVO nodo, diverso da Sel)
 ├── .append(tagName) → Node
 ├── .append(XML) → Node
 ├── .prepend(tagName) → Node
 ├── .prepend(XML) → Node
 ├── .insert(tagName, beforeNode) → Node
 ├── .before(tagName) → Node
 ├── .before(XML) → Node
 ├── .after(tagName) → Node
 ├── .after(XML) → Node
 ├── .replace(XML) → Node                           // ritorna il nuovo nodo
 │
 ├── // Accesso DOM sottostante
 └── .unwrap() → org.w3c.dom.Node                   // null se Node vuoto
```

**Decisione architetturale: ritorno diverso su Node vs Sel per metodi strutturali.**

Il compilatore Java risolve al tipo più specifico: se l'oggetto è un `Node`, i metodi come `append()` restituiscono `Node`. Se l'oggetto è un `Sel`, restituiscono `Sel`. Questo è implementato tramite override con tipo di ritorno covariante.

Su `Node` il ritorno del nuovo nodo permette costruzione in profondità:

```java
node.append("chapter").attr("num", "1").append("title").text("Intro");
```

Su `Sel` il ritorno della selezione originale permette modifiche batch:

```java
xml.sel("//item").append("status").attr("active", "true");
// .attr() si applica a ogni <item>, non ai nuovi <status>
```

### `BoundSel<T>`

Selezione con dati associati, in attesa del join. Tipo transitorio.

```
BoundSel<T>
 ├── .join(String tagName) → JoinedSel<T>               // shorthand
 └── .join(JoinConfig<T>) → JoinedSel<T>                // configurazione esplicita
```

**Internamente** mantiene la lista di dati, la key function (opzionale), la selezione originale e il riferimento al parent per l'enter.

### `JoinConfig<T>`

Configurazione del join tramite builder. Definisce il comportamento per enter, update e exit.

```
JoinConfig<T>
 └── JoinConfig.builder() → Builder<T>
      ├── .defaults(String tagName) → Builder<T>
      ├── .enter(BiFunction<Node, T, Node>) → Builder<T>     // (parent, dato) → nodo creato
      ├── .update(BiFunction<Node, T, Node>) → Builder<T>    // (nodo, dato) → nodo
      ├── .exit(Consumer<Node>) → Builder<T>                  // (nodo) → void
      └── .build() → JoinConfig<T>
```

**Semantica interna dei valori nel builder:**

Il builder traccia per ciascun handler tre stati interni: *default* (impostato da `.defaults()`), *custom* (impostato da `.enter()`/`.update()`/`.exit()` con valore non-null), *null esplicito* (impostato con `null`).

| Stato | enter | update | exit |
|-------|-------|--------|------|
| **Shorthand** `.join("tag")` | Appende nodo con quel tag | Identity | Rimuove il nodo |
| **`.defaults("tag")`** | Appende nodo con quel tag | Identity | Rimuove il nodo |
| **Handler custom** | Esegue l'handler | Esegue l'handler | Esegue l'handler |
| **`null` esplicito** | Ignora (nessun nodo creato) | Identity | Ignora (nodo resta) |
| **Non specificato (senza `.defaults()`)** | Ignora (nessun nodo creato) | Identity | Ignora (nodo resta) |

La chiave: `.defaults("tag")` imposta i tre handler ai valori della shorthand. Un successivo `.update(handler)` sovrascrive solo update, lasciando enter e exit ai default. Un successivo `.exit(null)` disattiva exit esplicitamente (l'handler è null, non "non specificato").

### `JoinedSel<T> extends Sel`

Risultato del join: contiene i nodi merged (enter + update) con il dato associato.

```
JoinedSel<T> extends Sel
 ├── (eredita tutto da Sel)
 │
 ├── // Operazioni con accesso al dato
 ├── .attrWith(String name, BiFunction<String, T, String>) → JoinedSel<T>
 ├── .textWith(Function<T, String>) → JoinedSel<T>
 ├── .eachWith(BiConsumer<Node, T>) → JoinedSel<T>
 │
 ├── // Transizioni (perdono il binding)
 ├── .sel(xpath) → Sel                              // sotto-selezione senza dato
 ├── .toSel() → Sel                                 // abbandono esplicito del binding
 │
 ├── // Ordinamento
 └── .order() → JoinedSel<T>
```

**Internamente** mantiene una mappa nodo → dato per i metodi `with*`. La mappa viene costruita durante l'esecuzione del join.

---

## Pattern implementativi

### Null Object Pattern

La libreria non usa mai `null` come valore di ritorno durante il chaining. Esistono due "oggetti vuoti":

- **`Sel` vuoto** — selezione con lista nodi vuota. Tutte le operazioni sono no-op.
- **`Node` vuoto** — estende `Sel` vuoto. Tutti i metodi di navigazione restituiscono valori vuoti.

Il `Node` vuoto dovrebbe essere un singleton. Il `Sel` vuoto no, perché porta il riferimento al `Sel` padre per `.end()`.

### Sel padre e `.end()`

Ogni `Sel` creato tramite sotto-selezione (`.sel()`) mantiene un riferimento al `Sel` da cui è stato generato. `.end()` restituisce quel riferimento. Una selezione radice (creata da `xml.sel(...)`) ha come padre sé stessa — `.end()` restituisce sé stessa, senza eccezioni.

### Wrapping dei nodi DOM

I nodi `org.w3c.dom.Node` vengono avvolti in oggetti `Node` di XML Artisan on-demand. Non esiste una cache globale di wrapping — lo stesso nodo DOM può essere avvolto in più oggetti `Node` in momenti diversi. L'identità è basata sul nodo DOM sottostante (accessibile via `.unwrap()`), non sull'oggetto wrapper.

### Immutabilità delle selezioni vs mutabilità dei nodi

Le selezioni stesse sono immutabili: la lista di nodi in un `Sel` non cambia dopo la creazione. Ciò che cambia è il contenuto dei nodi nel DOM (attributi, testo, figli). Questo è coerente con D3 dove "selections are immutable; only the elements are mutable."

---

## Gestione XPath

### Architettura interna

L'oggetto `XML` mantiene un'istanza di `javax.xml.xpath.XPath` configurata con i namespace registrati. Le espressioni XPath vengono compilate ed eseguite tramite questa istanza.

### Contesto di valutazione

| Chiamata | Context node per `xpath.evaluate()` |
|----------|--------------------------------------|
| `xml.get(xpath)` | Il `Document` |
| `xml.sel(xpath)` | Il `Document` |
| `sel.sel(xpath)` | Ciascun nodo nella selezione padre |
| `node.sel(xpath)` | Il nodo sottostante |

### Rewrite automatico `//` → `.//`

Nelle sotto-selezioni, le espressioni che iniziano con `//` vengono riscritte come `.//` prima della valutazione. Questo avviene nel layer di wrapping, non nel layer XPath.

**Regola:** Se l'espressione inizia con `//` E il contesto non è il Document, viene preposto `.` all'espressione.

L'API Java XPath supporta nativamente i contesti relativi tramite `xpath.evaluate(expression, contextNode, returnType)` dove `contextNode` è un qualsiasi `org.w3c.dom.Node`. Le espressioni relative (es. `.//a`, `child::a`, `@id`) funzionano correttamente con questo meccanismo.

### Namespace

I namespace vengono registrati sull'oggetto `XML` e propagati all'istanza XPath tramite un `NamespaceContext` custom. Tutte le selezioni create da quell'`XML` ereditano la stessa risoluzione namespace.

```java
xml.namespace("dc", "http://purl.org/dc/elements/1.1/");
// Da questo punto, "dc:" è risolto in tutte le espressioni XPath
```

---

## Meccanica del join

### Flusso di esecuzione

1. **`.data(list, keyFn?, nodeKeyFn?)`** — crea un `BoundSel<T>` che memorizza la lista dati, la key function e la selezione originale.

2. **`.join(...)`** — esegue il data join:
   - a. Calcola il matching tra nodi e dati (posizionale o per chiave).
   - b. Classifica ogni nodo/dato in enter, update o exit.
   - c. Raggruppa per parent (se ci sono parent multipli).
   - d. Esegue gli handler per ciascun gruppo nell'ordine: exit, update, enter.
   - e. Per l'enter, inserisce i nuovi nodi nella posizione corrispondente all'ordine dei dati (prima del prossimo sibling update).
   - f. Merge dei nodi enter e update nella `JoinedSel<T>` risultante.
   - g. Costruisce la mappa nodo → dato per i metodi `with*`.

### Matching posizionale

Senza key function, il matching è per indice: il nodo all'indice 0 corrisponde al dato all'indice 0, e così via. I nodi oltre la lunghezza dei dati vanno in exit, i dati oltre la lunghezza dei nodi vanno in enter.

### Matching per key function

Con key function, il matching è basato sull'identità logica. La key function viene valutata su ciascun dato (`keyFn.apply(item)`) e su ciascun nodo (`nodeKeyFn.apply(node)`). Nodi e dati con la stessa chiave vengono associati (update). Dati senza nodo corrispondente vanno in enter. Nodi senza dato corrispondente vanno in exit.

Se più nodi hanno la stessa chiave, i duplicati vanno in exit. Se più dati hanno la stessa chiave, i duplicati vanno in enter.

### Parent multipli

Se la selezione contiene nodi con parent diversi, il join opera per ciascun gruppo di nodi che condividono lo stesso parent, come D3. I dati vengono distribuiti tra i gruppi.

### Ordine di inserimento

I nodi creati dall'enter vengono inseriti nella posizione corrispondente all'ordine dei dati, non in coda al parent. Il meccanismo è: il nodo enter viene inserito prima del prossimo sibling che appartiene al gruppo update. Questo mantiene la coerenza tra l'ordine dei dati e l'ordine dei nodi nel DOM.

Il metodo `.order()` su `JoinedSel` ri-ordina tutti i nodi (enter e update) nel DOM secondo l'ordine della selezione (che riflette l'ordine dei dati). Utile quando i nodi update hanno cambiato posizione rispetto ai dati.

---

## Resilienza interna

### Tabella completa dei comportamenti

#### `Sel` e operazioni generiche

| Situazione | Comportamento |
|------------|---------------|
| `xml.get(xpath)` non trova nodi | `""` |
| `xml.set(xpath, value)` non trova nodi | No-op |
| `xml.sel(xpath)` non trova nodi | `Sel` vuoto |
| Operazione su `Sel` vuoto | No-op, restituisce lo stesso `Sel` vuoto |
| `.end()` su selezione radice | Restituisce sé stessa |
| `.data()` con lista vuota | Tutti i nodi in exit |
| `.data()` su `Sel` vuoto | Tutti i dati in enter |
| `.stream()` su `Sel` vuoto | `Stream.empty()` |
| `.list()` su `Sel` vuoto | Lista vuota |
| `.size()` su `Sel` vuoto | `0` |
| `.attr(name)` su primo nodo, attributo assente | `""` |
| `.text()` su primo nodo, senza text content | `""` |

#### `Node` vuoto (null object)

| Operazione | Comportamento |
|------------|---------------|
| `.attr(name)` | `""` |
| `.attr(name, value)` | No-op, restituisce lo stesso `Node` vuoto |
| `.text()` | `""` |
| `.text(value)` | No-op, restituisce lo stesso `Node` vuoto |
| `.name()` | `""` |
| `.children()` | `Sel` vuoto |
| `.parent()` | `Node` vuoto |
| `.sel(xpath)` | `Sel` vuoto |
| `.append(tag/xml)` | `Node` vuoto |
| `.prepend(tag/xml)` | `Node` vuoto |
| `.before(tag/xml)` | `Node` vuoto |
| `.after(tag/xml)` | `Node` vuoto |
| `.replace(xml)` | `Node` vuoto |
| `.remove()` | No-op |
| `.unwrap()` | `null` |

### Eccezioni ammesse

Le uniche eccezioni sono per errori di programmazione non recuperabili:

- **XPath malformato** — errore di sintassi nell'espressione → unchecked exception che wrappa `XPathExpressionException`.
- **File non trovato** — `XML.from()` con path inesistente → `UncheckedIOException`.
- **XML malformato** — `XML.parse()` con stringa non-XML → unchecked exception con messaggio descrittivo che wrappa `SAXException`.

---

## Thread safety

Nessun tipo della libreria (`XML`, `Sel`, `Node`, `BoundSel`, `JoinedSel`) è thread-safe. L'utente è responsabile della sincronizzazione se accede agli stessi oggetti da thread diversi.

Questa scelta è coerente con:
- Il `org.w3c.dom.Document` sottostante, che non è thread-safe.
- L'istanza `javax.xml.xpath.XPath`, che non è thread-safe.
- L'uso tipico: pipeline di trasformazione in un singolo thread.

---

## Dipendenze JVM

XML Artisan utilizza esclusivamente API standard della JVM:

| API | Uso |
|-----|-----|
| `javax.xml.parsers.DocumentBuilder` | Parsing XML |
| `org.w3c.dom.*` | Struttura dati DOM |
| `javax.xml.xpath.*` | Valutazione espressioni XPath |
| `javax.xml.transform.*` | Serializzazione (DOM → stringa/file) |

Nessuna dipendenza esterna. Compatibile con qualsiasi versione di Java che include queste API (Java 8+).
