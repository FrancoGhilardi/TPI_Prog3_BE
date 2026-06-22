# Food Store — Backend JPA / Consola (TPI Prog 3 — Parte 2)

Sistema de gestión de pedidos de comida con menú de consola, persistencia JPA/Hibernate
y base de datos H2 en archivo. Permite gestionar categorías, productos, usuarios y pedidos
(con sus líneas de detalle), con soft delete, consultas JPQL personalizadas y reportes.

---

## Tecnologías

- Java 21
- JPA / Hibernate 6
- H2 (base de datos en archivo — `./data/jpa_db`)
- Lombok
- Gradle 8

---

## Cómo ejecutar

```bash
# Ejecutar la app (consola interactiva — requerida para los menús)
./gradlew run

# Compilar el JAR
./gradlew build

# Ejecutar el JAR generado
java -jar build/libs/foodstore-backend-1.0.0.jar
```

En Windows usar `gradlew.bat` en lugar de `./gradlew`.

La base de datos H2 se crea automáticamente en `./data/jpa_db.mv.db` al primer arranque.
Hibernate gestiona el esquema con `hbm2ddl.auto = update`.

---

## Estructura del proyecto

```
src/main/java/com/tp/jpa/
│
├── model/                        # Entidades JPA
│   ├── Base.java                 # Clase abstracta base (id, eliminado, createdAt)
│   ├── Calculable.java           # Interfaz con calcularTotal()
│   ├── Categoria.java
│   ├── Producto.java
│   ├── Usuario.java
│   ├── Pedido.java               # implements Calculable
│   ├── DetallePedido.java
│   └── enums/
│       ├── Rol.java              # ADMIN, USUARIO
│       ├── EstadoPedido.java     # PENDIENTE, CONFIRMADO, TERMINADO, CANCELADO
│       └── FormaPago.java        # TARJETA, TRANSFERENCIA, EFECTIVO
│
├── util/
│   └── JPAUtil.java              # Factory singleton de EntityManagerFactory
│
├── repository/
│   ├── BaseRepository.java       # CRUD genérico (guardar, buscarPorId, listarActivos, eliminarLogico)
│   ├── CategoriaRepository.java  # Hereda todo el CRUD de Base
│   ├── ProductoRepository.java   # + buscarPorCategoria(), guardarEnCategoria()
│   ├── UsuarioRepository.java    # + buscarPorMail()
│   └── PedidoRepository.java     # + buscarPorUsuario(), buscarPorEstado()
│
└── Main.java                     # Menú de consola
```

La configuración de la base de datos está en `src/main/resources/META-INF/persistence.xml`
(unidad de persistencia `foodstorePU`).

---

## Funcionalidades del menú

| Opción | Descripción |
|---|---|
| 1. Gestionar Categorías | Alta, modificar, baja lógica, listado |
| 2. Gestionar Productos | Alta (con selección de categoría), modificar, baja lógica, listado |
| 3. Gestionar Usuarios | Alta (mail único), modificar, baja lógica, listado, buscar por mail |
| 4. Gestionar Pedidos | Alta (transacción atómica con reducción de stock), cambiar estado, baja lógica, listados |
| 5. Reportes | Productos por categoría, pedidos por usuario, pedidos por estado, total facturado |
| 0. Salir | Cierra el EntityManagerFactory y termina la aplicación |

---

## Notas técnicas

- **Bajas lógicas:** todas las eliminaciones marcan `eliminado = true`; el registro permanece
  en la BD y no aparece en los listados de activos.
- **Alta de pedido:** se ejecuta en una única transacción atómica. Recupera los productos con
  `em.find()` dentro del mismo EntityManager, calcula subtotales y total, reduce el stock y persiste
  el pedido (cascade a los detalles). Ante cualquier error hace rollback completo.
- **Consultas JPQL:** las queries personalizadas filtran por `eliminado = false` y usan parámetros
  nombrados. Cada una incluye comentario explicativo en el repositorio.
- **Total facturado:** suma los pedidos en estado `TERMINADO` y formatea con
  `String.format(Locale.US, "$%.2f", total)`.

---

## Orden de carga de datos

No hay carga inicial automática. Crear los datos desde el menú en este orden:

1. Categorías
2. Productos (requieren una categoría existente)
3. Usuarios
4. Pedidos (requieren un usuario y productos existentes)

---

## Entrega

- **Video demostrativo:** [link aquí]
