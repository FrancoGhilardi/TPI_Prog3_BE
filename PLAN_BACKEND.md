y an# Plan de Desarrollo — Parte 2: Backend JPA + Hibernate + H2 (Food Store)

Guía de implementación en **fases atómicas** partiendo de la **plantilla provista** (`plantilla-foodstore-jpa`). Las casillas `[ ]` son tareas atómicas; los bloques **DoD** son el criterio de cierre, mapeados a las Historias de Usuario (HU) y a la rúbrica (135 pts).

> Stack: **Java 21 · JPA / Hibernate 6 · H2 en archivo (`./data/jpa_db`) · Lombok · Gradle**. Ejecutar con `./gradlew run`.

---

## ⚠️ Advertencias críticas: la plantilla NO modela las relaciones como el PDF

Esto es lo más importante de todo el backend. La plantilla tomó una decisión de modelado **válida pero distinta** a la que describe el PDF, y por eso **los snippets JPQL del PDF no compilan tal cual**. La plantilla usa `@OneToMany` **unidireccional con `@JoinColumn` desde el lado padre**, en lugar de `@ManyToOne` desde el hijo.

| Relación | Lo que dice el PDF | Lo que tiene la plantilla (real) |
|---|---|---|
| Categoría–Producto | `Producto` tiene `@ManyToOne categoria` | **`Producto` NO tiene `categoria`.** `Categoria` tiene `@OneToMany @JoinColumn("categoria_id") Set<Producto> productos` |
| Usuario–Pedido | (Usuario→Pedido unidireccional) | `Usuario` tiene `@OneToMany @JoinColumn("usuario_id") Set<Pedido> pedidos`. **`Pedido` NO tiene `usuario`.** |
| Pedido–DetallePedido | bidireccional con `mappedBy="pedido"` y `DetallePedido.pedido` | **unidireccional.** `Pedido` tiene `@OneToMany @JoinColumn("pedido_id") Set<DetallePedido> detalles`. **`DetallePedido` NO tiene `pedido`.** Es `Set`, no `List`. |

**Consecuencias directas (lo que tenés que adaptar):**

1. **El enum se llama `EstadoPedido`, no `Estado`.** Importá `com.tp.jpa.model.enums.EstadoPedido`. La firma real es `buscarPorEstado(EstadoPedido estadoPedido)`.
2. **`buscarPorCategoria`**: el JPQL del PDF `WHERE p.categoria.id = :catId` **no compila** (Producto no navega a categoría). Usá un JOIN a través del padre:
   ```java
   // Productos activos de una categoría: navega desde Categoria.productos
   String jpql = "SELECT p FROM Categoria c JOIN c.productos p " +
                 "WHERE c.id = :catId AND p.eliminado = false";
   ```
3. **`buscarPorUsuario`**: idem, `WHERE p.usuario.id = :uid` no compila. Usá:
   ```java
   String jpql = "SELECT p FROM Usuario u JOIN u.pedidos p " +
                 "WHERE u.id = :uid AND p.eliminado = false";
   ```
4. **`buscarPorEstado`**: este sí funciona como el PDF, porque `estado` está en `Pedido`:
   ```java
   String jpql = "SELECT p FROM Pedido p WHERE p.estado = :estado AND p.eliminado = false";
   ```
5. **Alta de pedido**: como `Pedido` no tiene campo `usuario`, la FK `usuario_id` solo se setea agregando el pedido a la colección del **Usuario gestionado** (`usuario.getPedidos().add(pedido)`). Ver Fase 6.A en detalle.
6. **Campo de contraseña**: en `Usuario` el atributo Java es `contraseña` (con ñ) → getters `getContraseña()/setContraseña()`. Columna `contrasena`.
7. **`stock` es `Integer` y `disponible` es `Boolean`** (tipos envoltorio), no primitivos.

> No conviene "arreglar" el modelo para que calce con el PDF: el README de la plantilla pide **no modificar** las entidades. Adaptá las consultas (JOIN a través del padre) y dejá el modelo como está. Si igualmente preferís el modelo del PDF, documentá el cambio — pero la ruta de menor riesgo es respetar la plantilla.

### ✅ Decisión adoptada (para compilar sin tocar el modelo y respetar la consigna)

- **No se modifica ninguna entidad ni enum.** Se respeta la plantilla tal cual (cumple la consigna y evita romper el esquema H2 ya generado).
- Las 3 consultas que el PDF navegaba desde el hijo se resuelven con **JOIN a través del lado padre** (`buscarPorCategoria`, `buscarPorUsuario`) o directo sobre `Pedido` (`buscarPorEstado`). Todas compilan, son JPQL tipado, con parámetro nombrado y filtran `eliminado = false` — exactamente lo que pide la rúbrica (HU-02/03/04/11).
- En el alta de pedido, la asociación al usuario se hace por la **colección del `Usuario` gestionado** (única vía para setear la FK con este modelo). Detalle en Fase 6.A.

### 🔗 Alineación con el Frontend (Parte 1)

Aunque ambas partes se entregan por separado, los **contratos de datos deben ser idénticos** para que la futura API sea un reemplazo trivial. Mantené exactamente estos valores (el frontend usa los mismos):

| Concepto | Valores canónicos | Dónde |
|---|---|---|
| Estado del pedido | `PENDIENTE`, `CONFIRMADO`, `TERMINADO`, `CANCELADO` | `EstadoPedido` (no agregar/renombrar valores) |
| Forma de pago | `TARJETA`, `TRANSFERENCIA`, `EFECTIVO` | `FormaPago` |
| Rol | `ADMIN`, `USUARIO` | `Rol` |

> El `pedidos.json` provisto al frontend trae estados fuera de este enum (`ENTREGADO`, `EN_PREPARACION`); el plan del frontend los **normaliza** a estos 4. No cambies el enum del backend para "calzar" con ese JSON: el canónico es el del backend. Como el modelo es inverso (sin `categoria`/`usuario` en el hijo), una eventual API expondría **DTOs** con los objetos ya anidados — que es justo la forma del JSON que ya consume el frontend (`usuarioDto`, `categoria` anidada). Es decir: respetando estos enums, ambas partes quedan compatibles **sin** modificar el modelo.

### Sobre la conexión Backend ↔ Frontend

El PDF **no pide** exponer una API REST ni usar Spring Boot en esta entrega. La Parte 2 es una **app de consola** con JPA. La integración por API es explícitamente una *iteración futura* (F1, F2, F7). Por lo tanto, en esta entrega **los dos repositorios se desarrollan y corren por separado**; no hay punto de conexión a implementar. El día que se construya la API (fuera de alcance), recién ahí se le agregaría al backend una capa web y el frontend cambiaría sus URLs de `fetch`.

---

## Estado de la plantilla (qué viene hecho)

| Componente | Estado |
|---|---|
| `JPAUtil` | ✅ Completo (no tocar) |
| `BaseRepository` (`guardar`, `buscarPorId`, `listarActivos`, `eliminarLogico`) | ✅ Completo (no tocar) |
| `CategoriaRepository` | ✅ Completo (hereda CRUD) |
| Modelo + enums (`EstadoPedido`, `FormaPago`, `Rol`) | ✅ Completo (no tocar) |
| `persistence.xml`, `build.gradle` | ✅ Completo |
| `Main` | ✅ Solo esqueleto: dispatcher del menú principal + 5 métodos con `TODO` |
| `ProductoRepository.buscarPorCategoria` | ⛔ TODO (lanza `UnsupportedOperationException`) |
| `UsuarioRepository.buscarPorMail` | ⛔ TODO |
| `PedidoRepository.buscarPorUsuario` / `buscarPorEstado` | ⛔ TODO |

---

## Fase 0 — Preparación y verificación del entorno

- [ ] Importar `plantilla-foodstore-jpa` como proyecto Gradle (IntelliJ/VS Code) con **JDK 21**.
- [ ] Ejecutar `./gradlew build` (o `run`) para confirmar que la plantilla compila y arranca el menú principal antes de tocar nada.
- [ ] Verificar que al arrancar se crea `./data/jpa_db.mv.db` (H2 en archivo).
- [ ] Leer las 5 entidades y los 3 enums; confirmar de primera mano las divergencias de la sección anterior.
- [ ] Confirmar que `BaseRepository` ya da: `guardar` (persist si id null / merge si tiene id), `buscarPorId` (Optional), `listarActivos` (JPQL `eliminado=false`), `eliminarLogico`.

**DoD:** La plantilla compila y ejecuta el menú principal; entendés el modelo real y dónde están los `TODO`.

---

## Fase 1 — Repositorios con consultas JPQL  → *HU-02, HU-03, HU-04 (12+8+10 = 30 pts)*

Implementar los 4 métodos `TODO`. Cada método: **abre su propio `EntityManager`, cierra en `finally`, usa parámetro nombrado y lleva comentario explicando la consulta**.

### 1.A — `ProductoRepository.buscarPorCategoria(Long categoriaId)`  → *HU-02 / HU-11*
- [ ] Reemplazar el `throw` por el JPQL con JOIN a `Categoria.productos` (ver Advertencia #2).
- [ ] Retornar `List<Producto>` con `TypedQuery`, sin casteos manuales.
- [ ] `try/finally` cerrando el `EntityManager`. Comentario sobre la consulta.

### 1.B — `UsuarioRepository.buscarPorMail(String mail)`  → *HU-03*
- [ ] JPQL: `SELECT u FROM Usuario u WHERE u.mail = :mail AND u.eliminado = false`.
- [ ] `TypedQuery<Usuario>` + `getResultList()`; retornar `Optional.of(res.get(0))` si hay, `Optional.empty()` si no.
- [ ] `try/finally`. Comentario.

### 1.C — `PedidoRepository.buscarPorUsuario(Long idUsuario)`  → *HU-04 / HU-19*
- [ ] JPQL con JOIN a `Usuario.pedidos` (ver Advertencia #3). Filtra `eliminado = false`.
- [ ] `List<Pedido>`, parámetro `:uid`, `try/finally`, comentario.

### 1.D — `PedidoRepository.buscarPorEstado(EstadoPedido estado)`  → *HU-04 / HU-20*
- [ ] JPQL directo sobre `Pedido` (ver Advertencia #4). Filtra `eliminado = false`.
- [ ] `List<Pedido>`, parámetro `:estado`, `try/finally`, comentario.

**DoD:** Un test manual rápido (o un `main` temporal) confirma que cada método devuelve resultados coherentes sin lanzar `UnsupportedOperationException` ni errores de JPQL.

---

## Fase 2 — Helpers de consola en `Main`

Antes de los submenús, crear utilidades privadas reutilizables (evitan repetir parsing y son clave para "campo vacío conserva valor").

- [ ] `leerLinea(String prompt)`: imprime prompt y devuelve `sc.nextLine().trim()`.
- [ ] `leerEntero(...)` y `leerDouble(...)`: parsean con manejo de error (reintento o retorno nulo controlado).
- [ ] `leerOpcional(String actual)`: si el input está vacío devuelve `actual` (conservar valor previo en modificaciones).
- [ ] `leerEnum` genérico para elegir `FormaPago` / `EstadoPedido` / `Rol` mostrando opciones numeradas.
- [ ] Helper para confirmar (S/N).

**DoD:** Helpers compilados y probados con un par de llamadas; el patrón "Enter conserva valor" funciona.

---

## Fase 3 — Submenú Categorías  → *HU-05/06/07 (10 pts)*

`menuCategorias()` con loop y opciones 1-Alta, 2-Modificar, 3-Baja, 4-Listado, 0-Volver.
- [ ] **Alta:** pedir nombre (obligatorio, no vacío) y descripción (opcional). `Categoria.builder()...`, `categoriaRepo.guardar(cat)`, **mostrar el ID** leído de la entidad retornada.
- [ ] **Modificar:** `listarActivos()`, pedir ID, validar que exista activa; mostrar valores actuales; nuevos valores con `leerOpcional`; `guardar()`.
- [ ] **Baja lógica:** pedir ID, `eliminarLogico(id)`; si `false` → error; si `true` → confirmar con el nombre.
- [ ] **Listado:** `listarActivos()` → ID, nombre, descripción.

**DoD:** ABM de categorías completo; las bajas no aparecen en el listado; campo vacío conserva valor.

---

## Fase 4 — Submenú Productos  → *HU-08/09/10 (12 pts)*

`menuProductos()`.
- [ ] **Alta:** `categoriaRepo.listarActivos()`; si no hay ninguna → informar y cancelar. Seleccionar categoría por ID. Pedir nombre (obligatorio), descripción, precio (`> 0`), stock (`>= 0`), imagen (opcional), disponible (S/N, default true). Validar precio/stock → si inválidos, no persistir.
  - [ ] **Importante (modelo invertido):** la FK `categoria_id` la posee `Categoria.productos`. Para asociar el producto, agregalo a la colección de la categoría gestionada y guardá por esa vía, **o** persistí el producto y luego agregalo a `categoria.getProductos()` dentro de una transacción para que se setee la FK. Documentá la estrategia elegida.
  - [ ] Mostrar ID generado y categoría asignada.
- [ ] **Modificar:** `listarActivos()`, pedir ID; mostrar valores; nuevos valores con `leerOpcional`; validar `precio > 0` y `stock >= 0` solo si se ingresan; `guardar()`.
- [ ] **Baja lógica:** `eliminarLogico(id)` + confirmación con nombre.
- [ ] **Listado:** ID, nombre, precio, stock, disponibilidad y **nombre de su categoría**.

**DoD:** ABM de productos con selección de categoría y validaciones; el listado muestra la categoría correctamente.

---

## Fase 5 — Submenú Usuarios  → *HU-12/13/14/15 (10 + 5 pts)*

`menuUsuarios()`.
- [ ] **Alta:** pedir nombre, apellido, mail, celular (opcional), contraseña, rol (`ADMIN`/`USUARIO`). Validar mail único con `usuarioRepo.buscarPorMail(mail)` → si existe activo, no persistir. `guardar()`, mostrar ID. (Recordá `setContraseña`, con ñ.)
- [ ] **Modificar:** `listarActivos()`, pedir ID; valores con `leerOpcional`; si cambia el mail, validar unicidad contra **otro** usuario (mismo mail con ID distinto = error); `guardar()`.
- [ ] **Baja lógica:** `eliminarLogico(id)` + confirmación con nombre y apellido. (Sus pedidos permanecen.)
- [ ] **Listado:** ID, nombre completo, mail, rol.
- [ ] **Buscar por mail:** `buscarPorMail(mail)`; si Optional presente → todos los datos **sin contraseña**; si vacío → informar.

**DoD:** ABM de usuarios con mail único en alta y modificación; búsqueda por mail oculta la contraseña.

---

## Fase 6 — Submenú Pedidos  → *HU-16/17/18/19/20 (20 + 8 pts) — la fase más compleja*

`menuPedidos()` con opciones 1-Alta, 2-Cambiar estado, 3-Baja, 4-Listado, 5-Por usuario, 6-Por estado, 0-Volver.

### 6.A — Alta de pedido (transacción atómica única)  → *HU-16 (20 pts)*

**Fase de recolección (en memoria, sin persistir):**
- [ ] Listar usuarios activos; seleccionar uno por ID. Si no hay → cancelar.
- [ ] Seleccionar `FormaPago` (TARJETA/TRANSFERENCIA/EFECTIVO).
- [ ] Ciclo de productos: mostrar catálogo activo (ID, nombre, precio, stock); pedir ID; validar que exista, esté activo y `disponible = true`; pedir cantidad (`> 0`); validar `stock` suficiente. Guardar **solo `(idProducto, cantidad)`** en una lista temporal. Preguntar si agrega otro.
- [ ] Si la lista temporal queda vacía → informar "el pedido debe tener al menos un detalle" y cancelar.

**Fase transaccional (un único `EntityManager`, una sola transacción):**
- [ ] Abrir `em`, `tx.begin()`.
- [ ] `Usuario usuario = em.find(Usuario.class, idUsuario)` (queda **gestionado**).
- [ ] Crear `Pedido` (fecha `LocalDate.now()`, estado `PENDIENTE`, `formaPago`).
- [ ] Por cada `(idProducto, cantidad)`: `Producto p = em.find(Producto.class, idProducto)` (gestionado); `pedido.addDetallePedido(cantidad, p)` (el método ya calcula subtotal = precio*cantidad y acumula el total); `p.setStock(p.getStock() - cantidad)` (al estar gestionado, se sincroniza solo en commit).
- [ ] **Asociar al usuario (clave por el modelo invertido):** `usuario.getPedidos().add(pedido)` (o `usuario.addPedido(pedido)`). Como `Usuario.pedidos` es `cascade = ALL` con `@JoinColumn("usuario_id")`, al hacer commit se persiste el pedido, sus detalles (cascade en `Pedido.detalles`) y se setean ambas FKs (`usuario_id`, `pedido_id`).
- [ ] (Opcional defensivo) `pedido.calcularTotal()` antes del commit para asegurar el total.
- [ ] `tx.commit()`. Ante **cualquier** excepción → `tx.rollback()` (nada se modifica: ni stock, ni pedido).
- [ ] `em.close()` en `finally`.
- [ ] Al éxito, mostrar: ID generado, fecha, usuario, forma de pago, listado de productos con cantidades y subtotales, y total.

> Nota: NO mezcles entidades de distintos `EntityManager`. Por eso la lista temporal guarda IDs+cantidad, y los `Producto`/`Usuario` se recuperan con `em.find()` dentro de la misma transacción.

### 6.B — Cambiar estado  → *HU-17*
- [ ] Pedir ID; si no existe o está dado de baja → error. Mostrar estado actual. Elegir nuevo `EstadoPedido`. Setear y `pedidoRepo.guardar()`. Confirmar ID + nuevo estado.

### 6.C — Baja lógica  → *HU-18*
- [ ] `eliminarLogico(id)`; si `true` confirmar mostrando ID y total. **El stock NO se restaura**; los `DetallePedido` permanecen.

### 6.D — Listados  → *HU-19/20*
- [ ] **Listado general:** `listarActivos()` → ID, fecha, estado, forma de pago, nombre de usuario, total.
- [ ] **Por usuario:** seleccionar usuario activo → `buscarPorUsuario(id)` → ID, fecha, estado, total; si vacío, informar.
- [ ] **Por estado:** seleccionar `EstadoPedido` → `buscarPorEstado(estado)` → ID, fecha, nombre usuario, total; si vacío, informar.

**DoD:** Se puede crear un pedido con ≥2 productos viendo subtotales/total, el stock baja, y un fallo a mitad de camino hace rollback completo. Cambio de estado, baja y los 3 listados funcionan.

---

## Fase 7 — Submenú Reportes  → *HU-11, HU-21 + parte de HU-19/20 (5 + 10 pts)*

`menuReportes()`.
- [ ] **1. Productos por categoría:** listar categorías activas → `productoRepo.buscarPorCategoria(id)` → ID, nombre, precio, stock; si vacío, informar explícitamente.  *(HU-11, 5 pts)*
- [ ] **2. Pedidos por usuario:** `buscarPorUsuario(id)` → ID, fecha, estado, forma de pago, total; si vacío, informar.
- [ ] **3. Pedidos por estado:** `buscarPorEstado(estado)` → ID, fecha, nombre usuario, total; si vacío, informar.
- [ ] **4. Total facturado:** `buscarPorEstado(EstadoPedido.TERMINADO)`, sumar `total` (tratar `null` como 0, ej. `mapToDouble(p -> p.getTotal()==null?0:p.getTotal()).sum()`), formatear con `String.format(Locale.US, "$%.2f", total)`. Si no hay terminados → `$0.00`.

**DoD:** Los 4 reportes devuelven datos correctos; el total facturado sale formateado a 2 decimales sin error de representación de `Double`.

---

## Fase 8 — Reglas técnicas, README y verificación final

- [ ] Confirmar que **toda escritura** (`guardar`, `eliminarLogico`, alta de pedido) maneja `begin/commit/rollback` y cierra `EntityManager` en `finally`.
- [ ] Confirmar que **todas las bajas son lógicas** (`eliminado = true`) y que los registros dados de baja no aparecen en listados activos.
- [ ] Confirmar que cada método con JPQL personalizado tiene su comentario.
- [ ] Verificar que la opción `0` del menú principal llama a `JPAUtil.close()`.
- [ ] Cargar **datos de prueba coherentes** desde el menú (mínimo de la consigna: 2 categorías, 5 productos, 2 usuarios, 2 pedidos) y dejar la BD `./data/jpa_db.mv.db` poblada o documentar el orden de carga.
- [ ] Completar el `README.md` del backend: descripción, `./gradlew run`, orden de carga (Categorías → Productos → Usuarios → Pedidos), y **nota sobre el modelo de relaciones unidireccional** (por qué los JPQL usan JOIN a través del padre).
- [ ] Verificación contra la rúbrica (135 pts) — ver tabla.
- [ ] **Compilar y ejecutar sin errores** desde línea de comandos (cada error de compilación que impida ejecutar descuenta 10 pts).

**DoD:** El proyecto compila y corre con `./gradlew run`, el menú es navegable, las reglas técnicas se cumplen y la BD persiste entre ejecuciones.

---

## Checklist de mapeo a la rúbrica (135 pts)

| Fase | HU | Ítem rúbrica | Pts |
|---|---|---|---|
| 0 | HU-01 | `BaseRepository<T>` (ya provisto — verificar) | 18 |
| 1.A | HU-02 | Categoria/ProductoRepo + `buscarPorCategoria` | 12 |
| 1.B | HU-03 | `UsuarioRepository` + `buscarPorMail` | 8 |
| 1.C/1.D | HU-04 | `PedidoRepository` (usuario/estado) | 10 |
| 3 | HU-05/06/07 | ABM Categorías | 10 |
| 4 | HU-08/09/10 | ABM Productos | 12 |
| 7 (op.1) | HU-11 | JPQL Productos/Categoría | 5 |
| 5 | HU-12/13/14 | ABM Usuarios | 10 |
| 5 | HU-15 | Búsqueda por mail | 5 |
| 6.A | HU-16 | Alta de pedido (transacción atómica) | 20 |
| 6.B/6.C | HU-17/18 | Estado y baja de pedido | 8 |
| 6.D / 7 | HU-19/20/21 | Reportes de pedidos | 10 |
| 8 | — | Integración general (compila, menú navegable) | 7 |
