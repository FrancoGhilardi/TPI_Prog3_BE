# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the console app (enables stdin for interactive menus)
./gradlew run          # Linux/Mac
gradlew.bat run        # Windows

# Build only
./gradlew build

# Build and run the JAR directly
./gradlew jar
java -jar build/libs/foodstore-jpa-1.0.0.jar
```

The H2 database is created automatically at `./data/jpa_db.mv.db` on first run. Data order matters for seeding: Categorías → Productos → Usuarios → Pedidos.

## Architecture

Console-only Java 21 app. No REST, no Spring Boot. Single entry point: `Main.java`.

```
com.tp.jpa/
├── model/          ← JPA entities — DO NOT MODIFY
├── util/JPAUtil    ← singleton EntityManagerFactory — DO NOT MODIFY
├── repository/
│   ├── BaseRepository<T>   ← generic CRUD — DO NOT MODIFY
│   ├── CategoriaRepository ← complete (inherits CRUD)
│   ├── ProductoRepository  ← TODO: buscarPorCategoria()
│   ├── UsuarioRepository   ← TODO: buscarPorMail()
│   └── PedidoRepository    ← TODO: buscarPorUsuario(), buscarPorEstado()
└── Main.java       ← TODO: all five submenus
```

**BaseRepository** provides: `guardar(T)`, `buscarPorId(Long)`, `listarActivos()`, `eliminarLogico(Long)`. Each method opens its own `EntityManager` and closes it in `finally`. Write operations use `begin/commit/rollback`.

## Critical: Unidirectional @OneToMany Model

The template uses `@JoinColumn` on the **parent** side everywhere. Children have **no back-reference**:

| Parent | Collection | Child | Missing field |
|--------|-----------|-------|---------------|
| `Categoria` | `Set<Producto> productos` | `Producto` | no `categoria` field |
| `Usuario` | `Set<Pedido> pedidos` | `Pedido` | no `usuario` field |
| `Pedido` | `Set<DetallePedido> detalles` | `DetallePedido` | no `pedido` field |

**Consequence for JPQL** — navigate from the parent:
```java
// buscarPorCategoria — Producto has no categoria field
"SELECT p FROM Categoria c JOIN c.productos p WHERE c.id = :catId AND p.eliminado = false"

// buscarPorUsuario — Pedido has no usuario field
"SELECT p FROM Usuario u JOIN u.pedidos p WHERE u.id = :uid AND p.eliminado = false"

// buscarPorEstado — estado is ON Pedido, works directly
"SELECT p FROM Pedido p WHERE p.estado = :estado AND p.eliminado = false"
```

**Consequence for alta de Pedido** — the FK `usuario_id` is only set via the **managed** `Usuario`'s collection:
```java
EntityManager em = emf.createEntityManager();
em.getTransaction().begin();
Usuario usuario = em.find(Usuario.class, idUsuario);   // managed
Pedido pedido = Pedido.builder().formaPago(fp).build();
// em.find each producto, call pedido.addDetallePedido(), decrement stock
usuario.getPedidos().add(pedido);  // cascade ALL sets usuario_id + persists pedido+detalles
em.getTransaction().commit();
```
Never mix entities from different `EntityManager` instances. Collect `(idProducto, cantidad)` pairs in memory before opening the transaction, then use `em.find()` inside.

**Consequence for Producto → Categoria FK** — assign by adding to the managed Categoria:
```java
categoria.getProductos().add(producto); // or categoria.addProducto(producto)
```

## Notable Gotchas

- **`contraseña` (with ñ)**: Java field name uses ñ → `getContraseña()` / `setContraseña()`. DB column is `contrasena`.
- **`stock` is `Integer`, `disponible` is `Boolean`** (boxed, not primitives) — null-safe access needed.
- **Enum is `EstadoPedido`** (not `Estado`). Values: `PENDIENTE`, `CONFIRMADO`, `TERMINADO`, `CANCELADO`.
- **`FormaPago`**: `TARJETA`, `TRANSFERENCIA`, `EFECTIVO`.
- **`Rol`**: `ADMIN`, `USUARIO`.
- Do **not** rename or add enum values — the frontend (Part 1) expects these exact strings.
- `Pedido.addDetallePedido(int cantidad, Producto p)` accumulates `total` as it adds detalles. Call `calcularTotal()` only as a defensive recalculation.
- `guardar()` uses `persist` when `id == null`, `merge` when id is set. After `persist`, read the ID from the **returned** entity (same reference for persist, new managed instance for merge).
