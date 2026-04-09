# XML Artisan — Design Document

> Visione, principi e decisioni progettuali della libreria.

**Versione:** 0.4

---

## Indice

1. [Obiettivi e motivazioni](#obiettivi-e-motivazioni)
2. [Ispirazioni](#ispirazioni)
3. [Principi di design](#principi-di-design)
4. [Decisioni chiave e motivazioni](#decisioni-chiave-e-motivazioni)
5. [Confronto con l'esistente](#confronto-con-lesistente)

---

## Obiettivi e motivazioni

L'API standard Java per la manipolazione XML (`javax.xml.parsers`, `org.w3c.dom`, `javax.xml.xpath`) è potente ma estremamente verbosa. Operazioni comuni come selezionare nodi, modificare attributi o sincronizzare dati con una struttura XML richiedono decine di righe di codice boilerplate.

XML Artisan si propone di:

- Offrire un'API fluent e concisa per lettura, modifica e creazione di documenti XML.
- Introdurre il concetto di **selezioni** ispirate a D3.js, con supporto per data binding tramite il pattern **join** (enter/update/exit).
- Appoggiarsi interamente sulle API standard della JVM (DOM, XPath) senza dipendenze esterne.
- Distinguere chiaramente tra **manipolazione diretta** (immediata, mutabile) e **sincronizzazione dati** (lazy, dichiarativa).
- Non rompere mai il flusso del chaining: selezioni vuote, risultati assenti e operazioni su insiemi vuoti sono gestiti silenziosamente come no-op.

---

## Ispirazioni

### D3.js — Il sistema di selezioni

Il cuore di XML Artisan è ispirato al [sistema di selezioni di D3.js](https://d3js.org/d3-selection) e in particolare al [join pattern](https://d3js.org/d3-selection/joining). D3 ha dimostrato che il modello selezione + data join è estremamente potente per sincronizzare dati con una struttura a nodi. XML Artisan porta questo paradigma nel mondo Java/XML.

Le lezioni chiave prese da D3:

- **Il pattern join unificato** (`.join()`) come evoluzione del pattern esplicito enter/update/exit separati. D3 stesso si è evoluto verso `.join()` come API primaria perché più semplice e meno error-prone.
- **La merge di enter e update** come risultato del join, che permette di applicare operazioni comuni a entrambi i gruppi.
- **L'ordine di inserimento posizionale** nell'enter: i nuovi nodi vengono inseriti nella posizione corrispondente ai dati, non in coda al parent.
- **Parent multipli gestiti per gruppo**: se la selezione contiene nodi con parent diversi, il join opera per ciascun parent separatamente.

### jQuery — Il chaining fluent

L'API fluent con chaining e il metodo `.end()` per risalire alle selezioni precedenti sono ispirati a jQuery. Anche la filosofia di operare su insiemi di nodi con operazioni batch (`.attr()`, `.text()` applicati a tutta la selezione) viene da questo modello.

---

## Principi di design

### 1. Due modalità, un unico entry point

L'oggetto `XML` è il punto di partenza. La presenza o assenza di `.data()` determina se si opera in modalità diretta (immediata) o binding (lazy/dichiarativa). L'utente non deve scegliere una "modalità" prima di iniziare — il suo bisogno lo guida naturalmente.

### 2. Diretto = immediato

Le operazioni su selezioni senza data binding agiscono immediatamente sul DOM sottostante. `.attr("class", "active")` modifica il DOM nel momento in cui viene chiamato. Questo è il comportamento intuitivo per chi vuole fare manipolazioni puntuali.

### 3. Binding = lazy

Dopo `.data()`, si entra in un mondo dichiarativo. Le operazioni vengono accumulate e applicate solo con `.join()`. Questo è necessario perché il data join ha bisogno di conoscere il quadro completo (enter, update, exit) prima di agire sul DOM.

### 4. Tutto è una selezione

`Node` è una specializzazione di `Sel` (selezione di cardinalità 1). Questo significa che un singolo nodo supporta tutte le operazioni di selezione e la libreria lavora internamente sempre con selezioni. La conseguenza pratica è che si passa fluidamente tra operazioni su singoli nodi e operazioni batch.

### 5. Tipi diversi per semantiche diverse

`Sel`, `BoundSel<T>`, `JoinedSel<T>`, `JoinConfig<T>` sono tipi distinti. Il compilatore guida l'utente: non si può chiamare `.join()` senza prima aver chiamato `.data()`, non si può chiamare `.attrWith()` su un `Sel` che non proviene da un join. Gli errori si scoprono a compile-time, non a runtime.

### 6. XPath contestuale

Le sotto-selezioni (`.sel()` chiamato su un `Sel`) valutano l'espressione relativamente ai nodi della selezione padre. Le espressioni `//` vengono trattate automaticamente come `.//` nelle sotto-selezioni per evitare il comportamento contro-intuitivo di XPath standard.

### 7. Zero dipendenze esterne

Solo API standard della JVM (`javax.xml.parsers`, `org.w3c.dom`, `javax.xml.xpath`, `javax.xml.transform`). Nessuna dipendenza transitiva.

### 8. Mai rompere la catena

Nessuna operazione lancia eccezioni durante il chaining. Selezioni vuote, XPath senza risultati, operazioni su insiemi vuoti — tutto è gestito come no-op con valori sensibili (stringhe vuote, selezioni vuote, Node vuoti). Le uniche eccezioni sono per errori di programmazione non recuperabili, con tipi specifici: `ParseException` (XML malformato), `XPathException` (XPath malformato), `InvalidNameException` (nomi non validi) — tutte sottoclassi di `XmlArtisanException extends RuntimeException`.

### 9. Nomi corti e intuitivi

L'API privilegia nomi brevi e leggibili: `sel`, `attr`, `text`, `before`, `after`, `end`, `first`, `last`. Nessun overhead verboso tipo `setAttribute` o `getTextContent`.

---

## Decisioni chiave e motivazioni

### `Node extends Sel`

**Decisione:** `Node` è una sottoclasse di `Sel`, non un tipo separato.

**Motivazione:** Se "tutto è una selezione", un singolo nodo è semplicemente una selezione di cardinalità 1. Questo elimina la necessità di API duplicate e permette transizioni fluide: da un `Node` si può iniziare una nuova selezione con `.sel()`, da una selezione si può estrarre un `Node` con `.first()`. A livello implementativo la libreria lavora sempre con selezioni.

**Alternativa scartata:** Tipi completamente separati `Node` e `Sel` con metodi di conversione espliciti. Scartata perché aggiunge complessità senza benefici.

### `.children()` restituisce `Sel`, non `List<Node>`

**Decisione:** `node.children()` restituisce un `Sel` con i figli del nodo.

**Motivazione:** Coerente con il principio "tutto è una selezione". Permette chaining immediato: `node.children().attr("visible", "true")`. Se serve una lista, `.list()` è disponibile su qualsiasi `Sel` (come `.stream()`).

### Metodi `with*` per accesso ai dati post-join

**Decisione:** `JoinedSel<T>` aggiunge `.attrWith()`, `.textWith()`, `.eachWith()` con il suffisso `With` invece di sovraccaricare `.attr()` e `.text()`.

**Motivazione:** Elimina ambiguità del compilatore Java. Quando `T = String`, il compilatore potrebbe confondere `Function<String, String>` con `BiFunction<String, String, String>` negli overload di `.attr()`. Il suffisso `With` rende anche immediatamente visibile nel codice quando si sta accedendo al dato e quando no.

**Alternativa scartata:** Overload basati sul numero di parametri della lambda (opzione C). Funziona nella maggioranza dei casi ma fallisce con method reference ambigue. Troppo fragile.

### `.sel()` su `JoinedSel` restituisce `Sel` (perde il binding)

**Decisione:** Una sotto-selezione dopo un join perde il binding dati e restituisce un `Sel` normale.

**Motivazione:** Il dato è associato ai nodi del join, non ai loro discendenti. Non avrebbe senso propagare un dato di tipo `Person` ai figli `<address>` o `<email>` di un nodo `<person>`. Semanticamente `.sel()` su `JoinedSel` equivale a `.toSel().sel(...)`.

### `append`/`before`/`after` hanno semantica diversa su `Node` vs `Sel`

**Decisione:** Su `Node` restituiscono il **nuovo nodo** (per costruzione di strutture). Su `Sel` restituiscono la **selezione originale** (side-effect batch). `replace()` è l'eccezione su `Sel`: restituisce i nuovi nodi perché gli originali non esistono più.

**Motivazione:** Pattern d'uso diversi richiedono ritorni diversi. `Node` è usato per costruzione in profondità (`node.append("a").append("b").text("...")`). `Sel` è usato per modifiche batch dove il focus resta sulla selezione corrente. La coerenza è mantenuta dal fatto che `Node extends Sel` e il compilatore risolve al tipo più specifico.

### JoinConfig con builder e `.defaults()`

**Decisione:** La forma completa del join usa `JoinConfig<T>` con builder pattern, che supporta `.defaults("tag")` per attivare il comportamento della shorthand come base.

**Motivazione:** Risolve tre problemi. Primo, rende esplicita la differenza tra "non specificato" (non fare nulla) e `.defaults()` (comportamento standard: enter=append, update=identity, exit=remove). Secondo, permette di partire dai default e sovrascrivere solo ciò che serve. Terzo, passare `null` esplicito per un handler ha un significato diverso da non specificarlo: `null` = "non fare nulla per questo gruppo", non specificato senza `.defaults()` = "non fare nulla", non specificato con `.defaults()` = "usa il default".

**Alternativa scartata:** Tre lambda separate come parametri di `.join()`. Meno espressiva, non supporta il pattern defaults-con-override, non distingue tra null e non-specificato.

### Resilienza: Null Object Pattern ovunque

**Decisione:** `Node` vuoto, `Sel` vuoto, stringhe vuote — mai `null`, mai eccezioni durante il chaining.

**Motivazione:** La catena non si deve mai rompere. Un utente che scrive `xml.sel("//maybe-exists").first().children().attr("x", "y")` non deve preoccuparsi di NullPointerException a nessun livello. Ogni operazione su un oggetto vuoto è un no-op che restituisce un altro oggetto vuoto.

**Alternativa scartata:** `Optional<Node>` per `.first()` e `.last()`. Più idiomatico in Java moderno ma rompe il chaining fluent e obbliga l'utente a gestire il caso vuoto esplicitamente.

### XPath: rewrite automatico `//` → `.//` nelle sotto-selezioni

**Decisione:** Nelle sotto-selezioni, le espressioni che iniziano con `//` vengono automaticamente trattate come `.//`.

**Motivazione:** In XPath standard, `//a` cerca dalla radice del documento indipendentemente dal nodo contesto. Questo è contro-intuitivo quando l'utente scrive `xml.sel("//div").sel("//a")` aspettandosi di cercare gli `<a>` dentro i `<div>`. L'API Java XPath (`xpath.evaluate(expression, contextNode)`) supporta nativamente i nodi contesto, ma l'espressione deve essere relativa (`.//a`). Il rewrite automatico evita questa trappola.

### `text()` lavora sul testo diretto, non ricorsivo

**Decisione:** `text()` legge/scrive solo i nodi TEXT/CDATA figli diretti. `deepText()` fornisce la lettura ricorsiva (ex comportamento di `text()`).

**Motivazione:** L'XML di produzione contiene spesso mixed content: `<p>Hello <b>world</b> today</p>`. Il caso d'uso più comune è leggere/scrivere il testo diretto di un nodo, non quello ricorsivo. Con la vecchia semantica (`getTextContent()`), `text()` restituiva `"Hello world today"` ma `text("New")` distruggeva l'elemento `<b>`. La nuova semantica rende `text()` coerente tra lettura e scrittura: entrambe operano sul testo diretto, preservando gli elementi figli. `normalizeText()` unifica i nodi testo frammentati. `coalesceText()` è la variante distruttiva per quando serve appiattire tutto.

**Alternativa scartata:** Mantenere `text()` ricorsivo e aggiungere `directText()`. Scartata perché il caso d'uso diretto è più frequente e la semantica ricorsiva è sorprendente in scrittura (distrugge figli).

### `content(XML)` importa il root, `content(String)` i figli del wrapper

**Decisione:** `content(XML)` importa il root element del frammento come singolo figlio. `content(String)` avvolge la stringa in un wrapper sintetico `<_>...</_>`, parsa, e importa i figli del wrapper (supportando mixed content senza root singolo).

**Motivazione:** Le due varianti coprono casi d'uso diversi. `content(XML)` è per quando si ha già un `XML` parsato e si vuole inserire il suo root element. `content(String)` è per sostituire il contenuto con mixed content arbitrario (es. `<b>bold</b> text`) che non ha un singolo root. Il wrapper sintetico è trasparente all'utente.

### `cdata(String)` — solo in scrittura

**Decisione:** `cdata(String)` crea un CDATA_SECTION_NODE. Non esiste `cdata()` in lettura — `text()` già legge sia TEXT che CDATA.

**Motivazione:** La distinzione TEXT vs CDATA è rilevante solo in serializzazione (il contenuto è identico). Un metodo `cdata()` in lettura sarebbe ridondante con `text()`. L'utente sceglie CDATA in scrittura quando il contenuto contiene caratteri speciali XML (HTML, script, etc.) che vuole preservare senza entity encoding.

### Thread safety: nessuna garanzia

**Decisione:** Nessun tipo della libreria è thread-safe.

**Motivazione:** Il DOM API sottostante non è thread-safe. Aggiungere sincronizzazione introdurrebbe overhead significativo per un caso d'uso minoritario. L'uso tipico è pipeline di trasformazione in un singolo thread.

---

## Confronto con l'esistente

| Libreria | Stato | Approccio | Data binding | Selezioni D3 |
|----------|-------|-----------|--------------|---------------|
| DOM API (JVM) | Attiva | Verboso, basso livello | No | No |
| jOOX | Inattiva (~2 anni) | Fluent, stile jQuery | No | No |
| dom4j | Attiva | API propria, non fluent | No | No |
| **XML Artisan** | — | Fluent + selezioni D3 | **Sì** | **Sì** |

Il differenziatore principale rispetto a tutte le alternative è il **data binding con join pattern**. jOOX offre un'API fluent simile ma non supporta la sincronizzazione dati-nodi. dom4j ha la sua API di navigazione ma non è fluent. Nessuna libreria Java esistente porta il paradigma D3 nel mondo XML.

