# XML Artisan — Strategia di Testing

> Approccio, struttura e copertura dei test.

---

## Indice

1. [Approccio generale](#approccio-generale)
2. [Fasi di implementazione](#fasi-di-implementazione)
3. [Struttura dei test](#struttura-dei-test)
4. [Suites di test](#suites-di-test)
5. [Convenzioni](#convenzioni)
6. [Documenti XML di test](#documenti-xml-di-test)

---

## Approccio generale

### Test-first sull'API pubblica

L'approccio di testing è:

1. **Implementare prima tutte le interfacce e i tipi** necessari a soddisfare il compilatore (stub vuoti).
2. **Scrivere tutti i test** concentrandosi esclusivamente sull'API pubblica della libreria.
3. **Implementare** fino a far passare i test.

### Focus sui test funzionali

I test lavorano con l'API esterna come farebbe un utente della libreria. Non testiamo le classi interne direttamente — se un comportamento interno è critico, deve essere osservabile attraverso l'API pubblica.

**Eccezione:** Test unit interni sono ammessi per logica particolarmente complessa e isolata che sarebbe difficile verificare indirettamente (es. l'algoritmo di matching per key function, il rewrite XPath `//` → `.//`).

### Ogni test è autocontenuto

Ogni test crea il proprio documento XML (da stringa o vuoto) e verifica il risultato. Nessuna dipendenza tra test, nessuno stato condiviso.

---

## Fasi di implementazione

### Fase 1: Tipi e compilazione (v1.0)

Creare le interfacce/classi pubbliche con metodi stub:

```
net.fndanko.xml.artisan.XML
net.fndanko.xml.artisan.Sel
net.fndanko.xml.artisan.Node
net.fndanko.xml.artisan.OutputOptions
```

Obiettivo: il progetto compila, i test si scrivono, i test falliscono (red).

### Fase 2: Test per il core (v1.0)

Scrivere tutti i test per le funzionalità core: entry point, selezioni dirette, navigazione, inserimento strutturale, serializzazione, resilienza.

### Fase 3: Implementazione core (v1.0)

Implementare fino a far passare tutti i test della fase 2.

### Fase 4: Tipi data binding (v1.1)

Aggiungere i tipi stub:

```
net.fndanko.xml.artisan.BoundSel
net.fndanko.xml.artisan.JoinedSel
net.fndanko.xml.artisan.JoinConfig
```

### Fase 5: Test data binding (v1.1)

Scrivere tutti i test per il join pattern.

### Fase 6: Implementazione data binding (v1.1)

Implementare fino a far passare tutti i test della fase 5.

---

## Struttura dei test

I test sono organizzati per area funzionale, specchiando le sezioni dell'API:

```
src/test/java/net/fndanko/xml/artisan/
 ├── XMLEntryPointTest.java           // factory methods, get/set, namespace
 ├── SelReadTest.java                 // .attr(), .text(), .size(), .empty()
 ├── SelModifyTest.java               // .attr(name,value), .text(value), .remove()
 ├── SelTransformTest.java            // .attr(name,fn), .text(fn)
 ├── SelNavigationTest.java           // .sel(), .end(), sotto-selezioni, XPath contestuale
 ├── SelStructuralTest.java           // .append(), .prepend(), .before(), .after(), .replace()
 ├── SelIterationTest.java            // .stream(), .list(), .first(), .last(), Iterable, .order()
 ├── NodeNavigationTest.java          // .children(), .parent(), .name()
 ├── NodeStructuralTest.java          // append/prepend/before/after/replace su Node (ritorno diverso)
 ├── NodeAsSelTest.java               // Node usato come Sel: .sel(), data binding da Node
 ├── SerializationTest.java           // .toString(), .toFragment(), .writeTo(), OutputOptions
 ├── XPathTest.java                   // XPath contestuale, rewrite //, namespace
 ├── ResilienceTest.java              // Sel vuoto, Node vuoto, chaining sicuro, no-op
 ├── TextNodeTest.java               // normalizeText, text (nuova semantica), deepText, coalesceText
 ├── DataBindingTest.java             // .data(), matching posizionale e per chiave
 ├── JoinShorthandTest.java           // .join("tag"), default enter/update/exit
 ├── JoinConfigTest.java              // JoinConfig builder, .defaults(), null esplicito, override
 ├── JoinedSelTest.java               // .attrWith(), .textWith(), .eachWith(), .toSel(), .order()
 └── JoinAdvancedTest.java            // parent multipli, ordine inserimento, edge case
```

---

## Suites di test

### 1. `XMLEntryPointTest` — Entry point e operazioni base

```
Caricare XML da stringa con XML.parse()
Caricare XML da file con XML.from()
Creare documento vuoto con XML.create()
Leggere attributo con .get()
Leggere testo con .get()
Scrivere attributo con .set()
Scrivere testo con .set()
.get() su XPath senza risultati → stringa vuota
.set() su XPath senza risultati → no-op
Registrare namespace e usarlo in query
Registrare namespace multipli
```

### 2. `SelReadTest` — Lettura da selezioni

```
.attr(name) restituisce valore del primo nodo
.attr(name) su attributo inesistente → stringa vuota
.attr(name) su selezione vuota → stringa vuota
.text() restituisce testo diretto del primo nodo (non ricorsivo)
.text() su nodo senza testo diretto → stringa vuota
.text() su selezione vuota → stringa vuota
.size() restituisce numero corretto di nodi
.size() su selezione vuota → 0
.empty() su selezione con nodi → false
.empty() su selezione vuota → true
```

### 3. `SelModifyTest` — Modifica con valori fissi

```
.attr(name, value) imposta su tutti i nodi
.attr(name, value) su selezione vuota → no-op, chaining continua
.text(value) imposta testo diretto su tutti i nodi, preserva elementi figli
.text(value) su selezione vuota → no-op, chaining continua
.remove() rimuove i nodi dal DOM
.remove() su selezione vuota → no-op
.remove() restituisce la selezione padre
Chaining multiplo: .attr().text().attr() funziona
```

### 4. `SelTransformTest` — Modifica con funzioni

```
.attr(name, fn) trasforma il valore di ogni nodo
.attr(name, fn) con attributo inesistente → fn riceve stringa vuota
.text(fn) trasforma il testo diretto di ogni nodo, preserva elementi figli
.text(fn) con testo vuoto → fn riceve stringa vuota
Funzioni di trasformazione eseguite su ogni nodo (non solo il primo)
```

### 5. `SelNavigationTest` — Selezioni e navigazione

```
.sel() crea sotto-selezione contestuale
.sel() su selezione vuota → Sel vuoto
.sel() annidato: sel().sel().sel()
.end() ritorna alla selezione padre
.end() su selezione radice → ritorna sé stessa
.end() dopo sotto-selezione multipla: end().end()
Sotto-selezione cerca solo nei nodi della selezione padre (non globale)
Chaining completo: sel().attr().end().attr()
```

### 6. `SelStructuralTest` — Inserimento strutturale su Sel

```
.append(tag) aggiunge figlio a ogni nodo della selezione
.append(tag) restituisce la selezione originale (non i nuovi nodi)
.append(XML) aggiunge frammento a ogni nodo
.prepend(tag) aggiunge figlio in testa a ogni nodo
.prepend(XML) aggiunge frammento in testa
.before(tag) inserisce sibling prima di ogni nodo
.before(tag) restituisce la selezione originale
.before(XML) inserisce frammento come sibling prima
.after(tag) inserisce sibling dopo ogni nodo
.after(XML) inserisce frammento come sibling dopo
.replace(XML) sostituisce ogni nodo
.replace(XML) restituisce Sel con i nuovi nodi (non gli originali)
Tutti i metodi su selezione vuota → no-op, chaining continua
```

### 7. `SelIterationTest` — Iterazione e conversione

```
For-each su Sel itera tutti i nodi
For-each su Sel vuota → nessuna iterazione
.stream() restituisce stream con tutti i nodi
.stream() su Sel vuota → stream vuoto
.list() restituisce lista con tutti i nodi
.list() su Sel vuota → lista vuota
.first() restituisce il primo nodo
.first() su Sel vuota → Node vuoto
.last() restituisce l'ultimo nodo
.last() su Sel vuota → Node vuoto
.order() riordina i nodi nel DOM secondo l'ordine della selezione
```

### 8. `NodeNavigationTest` — Navigazione DOM

```
.children() restituisce Sel con i figli
.children() su nodo foglia → Sel vuoto
.children() chainable: node.children().attr(...)
.children().list() converte a lista
.parent() restituisce il nodo padre
.parent() su nodo radice → Node vuoto
.name() restituisce il nome del tag
.name() su Node vuoto → stringa vuota
```

### 9. `NodeStructuralTest` — Inserimento su Node

```
.append(tag) su Node restituisce il nuovo nodo (non il nodo originale)
.append(tag) permette costruzione profonda: node.append("a").append("b")
.append(XML) restituisce il nodo radice del frammento inserito
.prepend(tag) restituisce il nuovo nodo
.insert(tag, before) inserisce nella posizione corretta
.before(tag) restituisce il nuovo sibling
.before(XML) restituisce il nuovo nodo
.after(tag) restituisce il nuovo sibling
.after(XML) restituisce il nuovo nodo
.replace(XML) restituisce il nuovo nodo sostituto
.replace(XML) il nodo originale non è più nel DOM
```

### 10. `NodeAsSelTest` — Node usato come Sel

```
Node ha tutti i metodi di Sel
node.sel(xpath) crea sotto-selezione dal nodo
node.attr(name, value) funziona come Sel di cardinalità 1
node.data(list) entra in modalità binding
Node ottenuto da iterazione è utilizzabile come Sel
```

### 11. `SerializationTest` — Output

```
.toString() produce XML con dichiarazione
.toFragment() produce XML senza dichiarazione
.writeTo(path) scrive su file
.writeTo(path, options) con indentazione
.writeTo(path, options) con encoding specifico
.writeTo(path, options) con dichiarazione omessa
Documento modificato si serializza con le modifiche
```

### 12. `XPathTest` — XPath contestuale

```
XPath assoluto da xml.sel() cerca dalla radice
XPath relativo in sotto-selezione cerca dal contesto
"//" in sotto-selezione trattato come ".//" (relativo)
".//" esplicito funziona in sotto-selezione
XPath con predicati funziona
XPath per attributi (@attr) funziona
XPath con namespace funziona
XPath malformato → eccezione unchecked
```

### 13. `ResilienceTest` — Resilienza e null object

```
Selezione vuota: tutte le operazioni sono no-op
Selezione vuota: chaining lungo non lancia eccezioni
Node vuoto da .first() su Sel vuota: tutte le operazioni sono no-op
Node vuoto: .attr(name) → ""
Node vuoto: .text() → ""
Node vuoto: .name() → ""
Node vuoto: .children() → Sel vuoto
Node vuoto: .parent() → Node vuoto
Node vuoto: .sel(xpath) → Sel vuoto
Node vuoto: .append(tag) → Node vuoto
Node vuoto: .prepend(tag) → Node vuoto
Node vuoto: .before(tag) → Node vuoto
Node vuoto: .after(tag) → Node vuoto
Node vuoto: .replace(xml) → Node vuoto
Node vuoto: .remove() → no-op
Node vuoto: .unwrap() → null
Chaining misto con selezioni e nodi vuoti: xml.sel("//nothing").first().children().sel("//x").attr("a","b") → no-op senza eccezioni
.end() su selezione radice → sé stessa (no eccezione)
Sel vuoto: .deepText() → ""
Sel vuoto: .normalizeText() → no-op
Sel vuoto: .coalesceText() → no-op
Node vuoto: .deepText() → ""
Node vuoto: .normalizeText() → no-op
Node vuoto: .coalesceText() → no-op
Sel vuoto: .cdata() → no-op
Node vuoto: .cdata() → no-op
Sel vuoto: .content(XML) → no-op
Sel vuoto: .content(String) → no-op
Node vuoto: .content(XML) → no-op
Node vuoto: .content(String) → no-op
.data() con lista vuota → tutti nodi in exit
.data() su Sel vuota → tutti dati in enter
```

### 14. `TextNodeTest` — Text node handling

```
normalizeText: nodi testo adiacenti unificati in uno
normalizeText: testo attorno ad elementi unificato come primo figlio
normalizeText: mix TEXT + CDATA produce CDATA
normalizeText: solo CDATA resta CDATA
normalizeText: nessun nodo testo → no-op
normalizeText: elemento vuoto → no-op
normalizeText: preserva elementi figli
normalizeText: preserva attributi
normalizeText: applicato a tutti i nodi della selezione
normalizeText: chainable (ritorna self)
text() lettura: testo diretto semplice
text() lettura: mixed content → solo testo diretto
text() lettura: nessun testo diretto → stringa vuota
text() lettura: non modifica il DOM
text() lettura: nodi testo frammentati concatenati
text() lettura: selezione vuota → stringa vuota
text() scrittura: sostituisce testo diretto, preserva figli
text() scrittura: stringa vuota rimuove testo diretto
text() scrittura: aggiunge testo su nodo senza testo
text() scrittura: applicato a tutti i nodi
text() scrittura: chainable
text() trasformazione: applicata a ogni nodo
text() trasformazione: preserva figli su mixed content
text() trasformazione: testo vuoto → fn riceve stringa vuota
deepText: elemento semplice → testo completo
deepText: mixed content → tutto il testo discendente
deepText: nidificato → concatena tutto
deepText: selezione vuota → stringa vuota
deepText: nessun side-effect
coalesceText: mixed content → testo singolo, figli rimossi
coalesceText: elementi nidificati → appiattisce tutto
coalesceText: nessun mixed content → invariato
coalesceText: preserva attributi
coalesceText: applicato a tutti i nodi
coalesceText: chainable
coalesceText poi text → legge stringa singola
XML.normalizeText: ricorsivo su tutti i livelli
XML.normalizeText: preserva struttura
XML.normalizeText: chainable (ritorna XML)
cdata: valore semplice crea CDATA_SECTION_NODE
cdata: preserva elementi figli
cdata: rimpiazza TEXT_NODE esistente con CDATA
cdata: rimpiazza CDATA esistente con nuovo CDATA
cdata: applicato a tutti i nodi
cdata: chainable
cdata: selezione vuota → no-op
cdata: text() legge contenuto CDATA
cdata: caratteri speciali preservati
content(XML): rimpiazza tutti i figli
content(XML): preserva attributi del nodo
content(XML): preserva posizione del nodo
content(XML): importa root element del frammento
content(XML): applicato a tutti i nodi
content(XML): chainable
content(XML): selezione vuota → no-op
content(String): parsa e rimpiazza
content(String): mixed content supportato
content(String): singolo elemento
content(String): stringa vuota rimuove tutti i figli
```

### 15. `DataBindingTest` — Data binding base

```
.data() con matching posizionale: corrispondenza corretta
.data() con più dati che nodi: eccesso in enter
.data() con più nodi che dati: eccesso in exit
.data() con stessa cardinalità: tutti in update
.data() con key function: matching per chiave
.data() con key function e ordine diverso: matching corretto
.data() con chiavi duplicate nei nodi: duplicati in exit
.data() con chiavi duplicate nei dati: duplicati in enter
```

### 16. `JoinShorthandTest` — Join forma abbreviata

```
.join("tag") crea nodi mancanti con il tag specificato
.join("tag") non modifica nodi update (identity)
.join("tag") rimuove nodi exit
.join("tag") restituisce merged enter + update
.join("tag") su Sel vuota con dati: tutti creati
.join("tag") con lista vuota: tutti rimossi
Operazioni post-join con .attrWith() funzionano
```

### 17. `JoinConfigTest` — JoinConfig builder

```
.defaults("tag") attiva comportamento shorthand
.defaults("tag") con .update() override: solo update cambiato
.defaults("tag") con .exit(null): exit disattivato
.defaults("tag") con .enter(handler) override: solo enter cambiato
Senza .defaults(): enter non specificato → ignora
Senza .defaults(): exit non specificato → ignora
.enter(null) esplicito → ignora anche con .defaults()
.update(null) esplicito → identity
.exit(null) esplicito → ignora
Handler personalizzati enter/update/exit eseguiti correttamente
```

### 18. `JoinedSelTest` — Operazioni post-join

```
.attrWith() accede al dato associato
.attrWith() riceve il valore corrente dell'attributo come primo parametro
.textWith() imposta testo dal dato
.eachWith() esegue operazione con node e dato
.toSel() restituisce Sel senza binding
.sel() su JoinedSel restituisce Sel (perde binding)
.attr() (ereditato da Sel) funziona senza dato
.order() riordina nodi secondo ordine dati
Chaining: .attrWith().textWith().attr().toSel().sel()
```

### 19. `JoinAdvancedTest` — Scenari avanzati

```
Join con parent multipli: enter eseguito per ciascun parent
Ordine inserimento: nodi enter nella posizione corretta (non in coda)
Ordine inserimento con key function: posizione rispettata
.order() dopo join con dati riordinati: DOM riflette nuovo ordine
Join su selezione con un solo nodo
Join ripetuto sullo stesso documento
Join con tipi dato diversi (String, POJO, record)
Join completo: enter + update + exit tutti presenti
```

---

## Convenzioni

### Naming dei test

Pattern: `<cosa>_<condizione>_<risultatoAtteso>`

```java
@Test void attr_onEmptySel_returnsEmptyString() { ... }
@Test void join_withMoreDataThanNodes_createsEnterNodes() { ... }
@Test void end_onRootSelection_returnsSelf() { ... }
```

### Struttura di ogni test

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

### Asserzioni

- Verificare lo stato dell'XML risultante, non gli internals.
- Usare `xml.get()` per verificare valori puntuali.
- Usare `xml.sel().size()` per verificare cardinalità.
- Usare `xml.toString()` o `xml.toFragment()` per verificare la struttura complessiva solo quando necessario.

---

## Documenti XML di test

I test usano XML inline (stringhe) quando si tratta di xml molto semplici e autocontenuti, o quando si sta testando la lettura file da stringa:

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

Per file più complessi si posizionano nella cartella `src/test/resources` e si leggono da lì.

Per i test di `XML.from()` (lettura da file), creare file temporanei con `@TempDir` di JUnit.

Per i test di `XML.writeTo()`, scrivere in `@TempDir` e rileggere per verifica.
