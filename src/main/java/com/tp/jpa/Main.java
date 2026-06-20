package com.tp.jpa;

import com.tp.jpa.model.*;
import com.tp.jpa.model.enums.EstadoPedido;
import com.tp.jpa.model.enums.FormaPago;
import com.tp.jpa.model.enums.Rol;
import com.tp.jpa.repository.CategoriaRepository;
import com.tp.jpa.repository.PedidoRepository;
import com.tp.jpa.repository.ProductoRepository;
import com.tp.jpa.repository.UsuarioRepository;
import com.tp.jpa.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Clase principal: menú de consola del sistema Food Store. Orden de uso natural: Categorías ->
 * Productos -> Usuarios -> Pedidos.
 */
public class Main {

  private static final Scanner sc = new Scanner(System.in);

  private static final CategoriaRepository categoriaRepo = new CategoriaRepository();
  private static final ProductoRepository productoRepo = new ProductoRepository();
  private static final UsuarioRepository usuarioRepo = new UsuarioRepository();
  private static final PedidoRepository pedidoRepo = new PedidoRepository();

  public static void main(String[] args) {
    boolean salir = false;
    while (!salir) {
      System.out.println();
      System.out.println("===== FOOD STORE - MENÚ PRINCIPAL =====");
      System.out.println("1. Gestionar Categorías");
      System.out.println("2. Gestionar Productos");
      System.out.println("3. Gestionar Usuarios");
      System.out.println("4. Gestionar Pedidos");
      System.out.println("5. Reportes");
      System.out.println("0. Salir");
      System.out.print("Opción: ");
      String op = sc.nextLine().trim();
      switch (op) {
        case "1":
          menuCategorias();
          break;
        case "2":
          menuProductos();
          break;
        case "3":
          menuUsuarios();
          break;
        case "4":
          menuPedidos();
          break;
        case "5":
          menuReportes();
          break;
        case "0":
          salir = true;
          break;
        default:
          System.out.println("Opción inválida.");
      }
    }
    JPAUtil.close();
    System.out.println("Aplicación finalizada.");
  }

  // ── Submenús ─────────────────────────────────────────────────

  private static void menuCategorias() {
    boolean volver = false;
    while (!volver) {
      System.out.println();
      System.out.println("--- Categorías ---");
      System.out.println("1. Alta");
      System.out.println("2. Modificar");
      System.out.println("3. Baja");
      System.out.println("4. Listado");
      System.out.println("0. Volver");
      String op = leerLinea("Opción: ");
      switch (op) {
        case "1":
          {
            String nombre = "";
            while (nombre.isEmpty()) {
              nombre = leerLinea("Nombre (obligatorio): ");
              if (nombre.isEmpty()) System.out.println("El nombre no puede estar vacío.");
              else if (nombre.length() > 50) {
                System.out.println("El nombre no puede superar los 50 caracteres.");
                nombre = "";
              }
            }
            String descripcion = leerLinea("Descripción (opcional): ");
            if (excedeLongitud("descripción", descripcion, 255)) break;
            Categoria cat =
                Categoria.builder()
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .build();
            Categoria guardada = categoriaRepo.guardar(cat);
            System.out.println("Categoría creada con ID: " + guardada.getId());
            break;
          }
        case "2":
          {
            List<Categoria> activas = categoriaRepo.listarActivos();
            if (activas.isEmpty()) {
              System.out.println("No hay categorías activas.");
              break;
            }
            activas.forEach(
                c ->
                    System.out.printf(
                        "  [%d] %s — %s%n", c.getId(), c.getNombre(), c.getDescripcion()));
            long id = leerEntero("ID a modificar: ");
            Optional<Categoria> opt = categoriaRepo.buscarPorId(id);
            if (opt.isEmpty() || opt.get().isEliminado()) {
              System.out.println("Categoría no encontrada.");
              break;
            }
            Categoria cat = opt.get();
            System.out.printf("Nombre actual: %s%n", cat.getNombre());
            System.out.printf("Descripción actual: %s%n", cat.getDescripcion());
            String nuevoNombre = leerOpcional("Nuevo nombre", cat.getNombre());
            if (nuevoNombre.isEmpty()) {
              System.out.println("El nombre no puede estar vacío.");
              break;
            }
            if (excedeLongitud("nombre", nuevoNombre, 50)) break;
            String nuevaDesc =
                leerOpcional(
                    "Nueva descripción", cat.getDescripcion() == null ? "" : cat.getDescripcion());
            if (excedeLongitud("descripción", nuevaDesc, 255)) break;
            cat.setNombre(nuevoNombre);
            cat.setDescripcion(nuevaDesc.isEmpty() ? null : nuevaDesc);
            categoriaRepo.guardar(cat);
            System.out.println("Categoría actualizada.");
            break;
          }
        case "3":
          {
            long id = leerEntero("ID a dar de baja: ");
            Optional<Categoria> opt = categoriaRepo.buscarPorId(id);
            if (opt.isEmpty() || opt.get().isEliminado()) {
              System.out.println("Categoría no encontrada.");
              break;
            }
            String nombre = opt.get().getNombre();
            boolean ok = categoriaRepo.eliminarLogico(id);
            if (ok) System.out.println("Categoría '" + nombre + "' dada de baja.");
            else System.out.println("Error al dar de baja.");
            break;
          }
        case "4":
          {
            List<Categoria> activas = categoriaRepo.listarActivos();
            if (activas.isEmpty()) {
              System.out.println("No hay categorías activas.");
              break;
            }
            System.out.printf("%-5s %-20s %s%n", "ID", "Nombre", "Descripción");
            activas.forEach(
                c ->
                    System.out.printf(
                        "%-5d %-20s %s%n",
                        c.getId(),
                        c.getNombre(),
                        c.getDescripcion() == null ? "" : c.getDescripcion()));
            break;
          }
        case "0":
          volver = true;
          break;
        default:
          System.out.println("Opción inválida.");
      }
    }
  }

  private static void menuProductos() {
    boolean volver = false;
    while (!volver) {
      System.out.println();
      System.out.println("--- Productos ---");
      System.out.println("1. Alta");
      System.out.println("2. Modificar");
      System.out.println("3. Baja");
      System.out.println("4. Listado");
      System.out.println("0. Volver");
      String op = leerLinea("Opción: ");
      switch (op) {
        case "1":
          {
            List<Categoria> cats = categoriaRepo.listarActivos();
            if (cats.isEmpty()) {
              System.out.println("No hay categorías activas. Cree una primero.");
              break;
            }
            cats.forEach(c -> System.out.printf("  [%d] %s%n", c.getId(), c.getNombre()));
            long catId = leerEntero("ID de categoría: ");

            String nombre = "";
            while (nombre.isEmpty()) {
              nombre = leerLinea("Nombre (obligatorio): ");
              if (nombre.isEmpty()) System.out.println("El nombre no puede estar vacío.");
              else if (nombre.length() > 50) {
                System.out.println("El nombre no puede superar los 50 caracteres.");
                nombre = "";
              }
            }
            String descripcion = leerLinea("Descripción (opcional): ");
            if (excedeLongitud("descripción", descripcion, 255)) break;

            double precio = 0;
            while (precio <= 0) {
              precio = leerDouble("Precio (> 0): ");
              if (precio <= 0) System.out.println("El precio debe ser mayor que 0.");
            }

            int stock = -1;
            while (stock < 0) {
              stock = leerEntero("Stock (>= 0): ");
              if (stock < 0) System.out.println("El stock no puede ser negativo.");
            }

            String imagen = leerLinea("Imagen URL (opcional): ");
            boolean disponible = confirmar("¿Disponible?");

            Producto prod =
                Producto.builder()
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .precio(precio)
                    .stock(stock)
                    .imagen(imagen.isEmpty() ? null : imagen)
                    .disponible(disponible)
                    .build();
            try {
              Producto guardado = productoRepo.guardarEnCategoria(prod, catId);
              System.out.println(
                  "Producto creado con ID: " + guardado.getId() + ", Categoría ID: " + catId);
            } catch (RuntimeException e) {
              System.out.println("Error: " + e.getMessage());
            }
            break;
          }
        case "2":
          {
            List<Producto> activos = productoRepo.listarActivos();
            if (activos.isEmpty()) {
              System.out.println("No hay productos activos.");
              break;
            }
            activos.forEach(
                p ->
                    System.out.printf(
                        "  [%d] %s — $%.2f stock:%d%n",
                        p.getId(), p.getNombre(), p.getPrecio(), p.getStock()));
            long id = leerEntero("ID a modificar: ");
            Optional<Producto> opt = productoRepo.buscarPorId(id);
            if (opt.isEmpty() || opt.get().isEliminado()) {
              System.out.println("Producto no encontrado.");
              break;
            }
            Producto prod = opt.get();

            System.out.printf(
                "Nombre: %s | Desc: %s | Precio: %.2f | Stock: %d | Disponible: %s%n",
                prod.getNombre(),
                prod.getDescripcion() == null ? "(vacío)" : prod.getDescripcion(),
                prod.getPrecio(),
                prod.getStock(),
                Boolean.TRUE.equals(prod.getDisponible()) ? "S" : "N");

            String nuevoNombre = leerOpcional("Nombre", prod.getNombre());
            if (nuevoNombre.isEmpty()) {
              System.out.println("El nombre no puede estar vacío.");
              break;
            }
            if (excedeLongitud("nombre", nuevoNombre, 50)) break;
            prod.setNombre(nuevoNombre);

            String descActual = prod.getDescripcion() == null ? "" : prod.getDescripcion();
            String nuevaDescProd = leerOpcional("Descripción", descActual);
            if (excedeLongitud("descripción", nuevaDescProd, 255)) break;
            prod.setDescripcion(nuevaDescProd.isEmpty() ? null : nuevaDescProd);

            String precioStr = leerLinea("Precio [" + prod.getPrecio() + "] (Enter=conservar): ");
            if (!precioStr.isEmpty()) {
              try {
                double np = Double.parseDouble(precioStr);
                if (np <= 0) {
                  System.out.println("Precio debe ser > 0.");
                  break;
                }
                prod.setPrecio(np);
              } catch (NumberFormatException e) {
                System.out.println("Precio inválido.");
                break;
              }
            }

            String stockStr = leerLinea("Stock [" + prod.getStock() + "] (Enter=conservar): ");
            if (!stockStr.isEmpty()) {
              try {
                int ns = Integer.parseInt(stockStr);
                if (ns < 0) {
                  System.out.println("Stock debe ser >= 0.");
                  break;
                }
                prod.setStock(ns);
              } catch (NumberFormatException e) {
                System.out.println("Stock inválido.");
                break;
              }
            }

            String imagenActual = prod.getImagen() == null ? "" : prod.getImagen();
            prod.setImagen(leerOpcional("Imagen", imagenActual));

            String dispStr =
                leerLinea(
                    "Disponible ["
                        + (Boolean.TRUE.equals(prod.getDisponible()) ? "S" : "N")
                        + "] (Enter=conservar): ");
            if (!dispStr.isEmpty()) prod.setDisponible(dispStr.equalsIgnoreCase("S"));

            productoRepo.guardar(prod);
            System.out.println("Producto actualizado.");
            break;
          }
        case "3":
          {
            long id = leerEntero("ID a dar de baja: ");
            Optional<Producto> opt = productoRepo.buscarPorId(id);
            if (opt.isEmpty() || opt.get().isEliminado()) {
              System.out.println("Producto no encontrado.");
              break;
            }
            String nombre = opt.get().getNombre();
            if (productoRepo.eliminarLogico(id))
              System.out.println("Producto '" + nombre + "' dado de baja.");
            else System.out.println("Error al dar de baja.");
            break;
          }
        case "4":
          {
            // Mapa productoId→nombreCategoria navegando desde el lado propietario
            // (Categoria.productos)
            List<Categoria> cats = categoriaRepo.listarActivos();
            Map<Long, String> catPorProd = new HashMap<>();
            for (Categoria c : cats) {
              productoRepo
                  .buscarPorCategoria(c.getId())
                  .forEach(p -> catPorProd.put(p.getId(), c.getNombre()));
            }
            List<Producto> activos = productoRepo.listarActivos();
            if (activos.isEmpty()) {
              System.out.println("No hay productos activos.");
              break;
            }
            System.out.printf(
                "%-5s %-20s %-10s %-7s %-12s %-15s%n",
                "ID", "Nombre", "Precio", "Stock", "Disponible", "Categoría");
            activos.forEach(
                p ->
                    System.out.printf(
                        "%-5d %-20s $%-9.2f %-7d %-12s %-15s%n",
                        p.getId(),
                        p.getNombre(),
                        p.getPrecio(),
                        p.getStock(),
                        Boolean.TRUE.equals(p.getDisponible()) ? "Sí" : "No",
                        catPorProd.getOrDefault(p.getId(), "—")));
            break;
          }
        case "0":
          volver = true;
          break;
        default:
          System.out.println("Opción inválida.");
      }
    }
  }

  private static void menuUsuarios() {
    boolean volver = false;
    while (!volver) {
      System.out.println();
      System.out.println("--- Usuarios ---");
      System.out.println("1. Alta");
      System.out.println("2. Modificar");
      System.out.println("3. Baja");
      System.out.println("4. Listado");
      System.out.println("5. Buscar por mail");
      System.out.println("0. Volver");
      String op = leerLinea("Opción: ");
      switch (op) {
        case "1":
          {
            String nombre = "";
            while (nombre.isEmpty()) {
              nombre = leerLinea("Nombre (obligatorio): ");
              if (nombre.isEmpty()) System.out.println("El nombre no puede estar vacío.");
              else if (nombre.length() > 50) {
                System.out.println("El nombre no puede superar los 50 caracteres.");
                nombre = "";
              }
            }
            String apellido = "";
            while (apellido.isEmpty()) {
              apellido = leerLinea("Apellido (obligatorio): ");
              if (apellido.isEmpty()) System.out.println("El apellido no puede estar vacío.");
              else if (apellido.length() > 100) {
                System.out.println("El apellido no puede superar los 100 caracteres.");
                apellido = "";
              }
            }
            String mail = "";
            while (mail.isEmpty()) {
              mail = leerLinea("Mail (obligatorio): ");
              if (mail.isEmpty()) {
                System.out.println("El mail no puede estar vacío.");
                continue;
              }
              if (!esMailValido(mail)) {
                System.out.println("Formato de mail inválido (ej: usuario@dominio.com).");
                mail = "";
                continue;
              }
              if (mail.length() > 100) {
                System.out.println("El mail no puede superar los 100 caracteres.");
                mail = "";
                continue;
              }
              if (usuarioRepo.buscarPorMail(mail).isPresent()) {
                System.out.println("El mail ya está en uso por otro usuario.");
                mail = "";
              }
            }
            String celular = leerLinea("Celular (opcional): ");
            if (excedeLongitud("celular", celular, 100)) break;
            String contrasena = "";
            while (contrasena.isEmpty()) {
              contrasena = leerLinea("Contraseña (obligatorio): ");
              if (contrasena.isEmpty()) System.out.println("La contraseña no puede estar vacía.");
              else if (contrasena.length() > 100) {
                System.out.println("La contraseña no puede superar los 100 caracteres.");
                contrasena = "";
              }
            }
            Rol rol = leerEnum("Rol:", Rol.class);
            Usuario guardado =
                usuarioRepo.guardar(
                    Usuario.builder()
                        .nombre(nombre)
                        .apellido(apellido)
                        .mail(mail)
                        .celular(celular.isEmpty() ? null : celular)
                        .contraseña(contrasena)
                        .rol(rol)
                        .build());
            System.out.println("Usuario creado con ID: " + guardado.getId());
            break;
          }
        case "2":
          {
            List<Usuario> activos = usuarioRepo.listarActivos();
            if (activos.isEmpty()) {
              System.out.println("No hay usuarios activos.");
              break;
            }
            activos.forEach(
                u ->
                    System.out.printf(
                        "  [%d] %s %s — %s [%s]%n",
                        u.getId(), u.getNombre(), u.getApellido(), u.getMail(), u.getRol()));
            long id = leerEntero("ID a modificar: ");
            Optional<Usuario> opt = usuarioRepo.buscarPorId(id);
            if (opt.isEmpty() || opt.get().isEliminado()) {
              System.out.println("Usuario no encontrado.");
              break;
            }
            Usuario usuario = opt.get();

            System.out.printf(
                "Nombre: %s | Apellido: %s | Mail: %s | Celular: %s | Rol: %s%n",
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getMail(),
                usuario.getCelular() == null ? "(vacío)" : usuario.getCelular(),
                usuario.getRol());

            String nuevoNombreU = leerOpcional("Nombre", usuario.getNombre());
            if (excedeLongitud("nombre", nuevoNombreU, 50)) break;
            usuario.setNombre(nuevoNombreU);

            String nuevoApellido = leerOpcional("Apellido", usuario.getApellido());
            if (excedeLongitud("apellido", nuevoApellido, 100)) break;
            usuario.setApellido(nuevoApellido);

            String nuevoMail = leerOpcional("Mail", usuario.getMail());
            if (!esMailValido(nuevoMail)) {
              System.out.println("Formato de mail inválido (ej: usuario@dominio.com).");
              break;
            }
            if (excedeLongitud("mail", nuevoMail, 100)) break;
            Optional<Usuario> conflicto = usuarioRepo.buscarPorMail(nuevoMail);
            if (conflicto.isPresent() && !conflicto.get().getId().equals(usuario.getId())) {
              System.out.println("El mail ya está en uso por otro usuario.");
              break;
            }
            usuario.setMail(nuevoMail);

            String celularActual = usuario.getCelular() == null ? "" : usuario.getCelular();
            String nuevoCelular = leerOpcional("Celular", celularActual);
            if (excedeLongitud("celular", nuevoCelular, 100)) break;
            usuario.setCelular(nuevoCelular.isEmpty() ? null : nuevoCelular);

            String nuevaContrasena = leerLinea("Contraseña [****] (Enter=conservar): ");
            if (!nuevaContrasena.isEmpty()) {
              if (excedeLongitud("contraseña", nuevaContrasena, 100)) break;
              usuario.setContraseña(nuevaContrasena);
            }

            if (confirmar("¿Cambiar rol? Actual: " + usuario.getRol())) {
              usuario.setRol(leerEnum("Nuevo rol:", Rol.class));
            }

            usuarioRepo.guardar(usuario);
            System.out.println("Usuario actualizado.");
            break;
          }
        case "3":
          {
            long id = leerEntero("ID a dar de baja: ");
            Optional<Usuario> opt = usuarioRepo.buscarPorId(id);
            if (opt.isEmpty() || opt.get().isEliminado()) {
              System.out.println("Usuario no encontrado.");
              break;
            }
            Usuario u = opt.get();
            if (usuarioRepo.eliminarLogico(id)) {
              System.out.println(
                  "Usuario '" + u.getNombre() + " " + u.getApellido() + "' dado de baja.");
            } else {
              System.out.println("Error al dar de baja.");
            }
            break;
          }
        case "4":
          {
            List<Usuario> activos = usuarioRepo.listarActivos();
            if (activos.isEmpty()) {
              System.out.println("No hay usuarios activos.");
              break;
            }
            System.out.printf(
                "%-5s %-15s %-15s %-30s %-10s%n", "ID", "Nombre", "Apellido", "Mail", "Rol");
            activos.forEach(
                u ->
                    System.out.printf(
                        "%-5d %-15s %-15s %-30s %-10s%n",
                        u.getId(), u.getNombre(), u.getApellido(), u.getMail(), u.getRol()));
            break;
          }
        case "5":
          {
            String mail = leerLinea("Mail a buscar: ");
            Optional<Usuario> opt = usuarioRepo.buscarPorMail(mail);
            if (opt.isEmpty()) {
              System.out.println("No se encontró usuario con ese mail.");
              break;
            }
            Usuario u = opt.get();
            System.out.printf(
                "ID: %d | Nombre: %s %s | Mail: %s | Celular: %s | Rol: %s%n",
                u.getId(),
                u.getNombre(),
                u.getApellido(),
                u.getMail(),
                u.getCelular() == null ? "(vacío)" : u.getCelular(),
                u.getRol());
            break;
          }
        case "0":
          volver = true;
          break;
        default:
          System.out.println("Opción inválida.");
      }
    }
  }

  private static void menuPedidos() {
    boolean volver = false;
    while (!volver) {
      System.out.println();
      System.out.println("--- Pedidos ---");
      System.out.println("1. Alta");
      System.out.println("2. Cambiar estado");
      System.out.println("3. Baja");
      System.out.println("4. Listado general");
      System.out.println("5. Por usuario");
      System.out.println("6. Por estado");
      System.out.println("0. Volver");
      String op = leerLinea("Opción: ");
      switch (op) {
        case "1":
          {
            // ── FASE DE RECOLECCIÓN (en memoria, sin persistir) ──────────
            List<Usuario> usuarios = usuarioRepo.listarActivos();
            if (usuarios.isEmpty()) {
              System.out.println("No hay usuarios activos.");
              break;
            }
            usuarios.forEach(
                u ->
                    System.out.printf("  [%d] %s %s%n", u.getId(), u.getNombre(), u.getApellido()));
            long idUsuario = leerEntero("ID de usuario: ");
            Optional<Usuario> optU = usuarioRepo.buscarPorId(idUsuario);
            if (optU.isEmpty() || optU.get().isEliminado()) {
              System.out.println("Usuario no encontrado.");
              break;
            }

            FormaPago formaPago = leerEnum("Forma de pago:", FormaPago.class);

            List<Producto> catalogo = productoRepo.listarActivos();
            if (catalogo.isEmpty()) {
              System.out.println("No hay productos disponibles.");
              break;
            }

            // Temporal: sólo IDs y cantidades, entidades se re-cargan en la transacción
            record ItemPedido(Long idProducto, int cantidad) {}
            List<ItemPedido> items = new ArrayList<>();

            boolean agregando = true;
            while (agregando) {
              System.out.printf(
                  "%-5s %-20s %-10s %-7s %-10s%n", "ID", "Nombre", "Precio", "Stock", "Disp.");
              catalogo.forEach(
                  p ->
                      System.out.printf(
                          "%-5d %-20s $%-9.2f %-7d %-10s%n",
                          p.getId(),
                          p.getNombre(),
                          p.getPrecio(),
                          p.getStock(),
                          Boolean.TRUE.equals(p.getDisponible()) ? "Sí" : "No"));
              long idProd = leerEntero("ID de producto: ");
              Optional<Producto> optP = productoRepo.buscarPorId(idProd);
              if (optP.isEmpty() || optP.get().isEliminado()) {
                System.out.println("Producto no encontrado.");
              } else if (!Boolean.TRUE.equals(optP.get().getDisponible())) {
                System.out.println("Producto no disponible.");
              } else {
                Producto p = optP.get();
                int cantidad = 0;
                while (cantidad <= 0) {
                  cantidad = leerEntero("Cantidad (stock disponible: " + p.getStock() + "): ");
                  if (cantidad <= 0) {
                    System.out.println("La cantidad debe ser mayor que 0.");
                  } else if (cantidad > p.getStock()) {
                    System.out.printf(
                        "Stock insuficiente. Disponible: %d. Ingresá una cantidad válida.%n",
                        p.getStock());
                    cantidad = 0;
                  }
                }
                items.add(new ItemPedido(idProd, cantidad));
                System.out.println("Producto agregado.");
              }
              agregando = confirmar("¿Agregar otro producto?");
            }

            if (items.isEmpty()) {
              System.out.println("El pedido debe tener al menos un detalle.");
              break;
            }

            // ── FASE TRANSACCIONAL (único EntityManager, única transacción) ──
            EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
              tx.begin();
              Usuario usuario = em.find(Usuario.class, idUsuario);
              Pedido pedido = Pedido.builder().formaPago(formaPago).build();
              for (ItemPedido item : items) {
                Producto p = em.find(Producto.class, item.idProducto());
                pedido.addDetallePedido(item.cantidad(), p);
                p.setStock(p.getStock() - item.cantidad());
                if (p.getStock() == 0) {
                  p.setDisponible(false);
                }
              }
              pedido.calcularTotal();
              usuario.addPedido(pedido); // CASCADE ALL setea usuario_id y persiste pedido+detalles
              tx.commit();
              System.out.printf(
                  "Pedido creado ID: %d | Fecha: %s | %s %s | Pago: %s%n",
                  pedido.getId(),
                  pedido.getFecha(),
                  usuario.getNombre(),
                  usuario.getApellido(),
                  formaPago);
              System.out.printf("%-20s %-8s %-10s%n", "Producto", "Cant.", "Subtotal");
              pedido
                  .getDetalles()
                  .forEach(
                      d ->
                          System.out.printf(
                              "%-20s %-8d $%.2f%n",
                              d.getProducto().getNombre(), d.getCantidad(), d.getSubtotal()));
              System.out.printf("Total: $%.2f%n", pedido.getTotal());
            } catch (RuntimeException e) {
              if (tx.isActive()) tx.rollback();
              System.out.println("Error al crear pedido (rollback): " + e.getMessage());
            } finally {
              em.close();
            }
            break;
          }
        case "2":
          {
            long id = leerEntero("ID de pedido: ");
            Optional<Pedido> opt = pedidoRepo.buscarPorId(id);
            if (opt.isEmpty() || opt.get().isEliminado()) {
              System.out.println("Pedido no encontrado.");
              break;
            }
            Pedido pedido = opt.get();
            System.out.println("Estado actual: " + pedido.getEstado());
            pedido.setEstado(leerEnum("Nuevo estado:", EstadoPedido.class));
            pedidoRepo.guardar(pedido);
            System.out.println("Pedido #" + id + " → " + pedido.getEstado());
            break;
          }
        case "3":
          {
            long id = leerEntero("ID de pedido a dar de baja: ");
            Optional<Pedido> opt = pedidoRepo.buscarPorId(id);
            if (opt.isEmpty() || opt.get().isEliminado()) {
              System.out.println("Pedido no encontrado.");
              break;
            }
            double total = opt.get().getTotal() == null ? 0 : opt.get().getTotal();
            if (pedidoRepo.eliminarLogico(id)) {
              System.out.printf("Pedido #%d dado de baja. Total: $%.2f%n", id, total);
            } else {
              System.out.println("Error al dar de baja.");
            }
            break;
          }
        case "4":
          {
            // Mapa pedidoId→nombreUsuario navegando desde Usuario.pedidos
            Map<Long, String> usuarioPorPedido = new HashMap<>();
            usuarioRepo
                .listarActivos()
                .forEach(
                    u ->
                        pedidoRepo
                            .buscarPorUsuario(u.getId())
                            .forEach(
                                p ->
                                    usuarioPorPedido.put(
                                        p.getId(), u.getNombre() + " " + u.getApellido())));
            List<Pedido> activos = pedidoRepo.listarActivos();
            if (activos.isEmpty()) {
              System.out.println("No hay pedidos activos.");
              break;
            }
            System.out.printf(
                "%-5s %-12s %-12s %-15s %-25s %-10s%n",
                "ID", "Fecha", "Estado", "Forma Pago", "Usuario", "Total");
            activos.forEach(
                p ->
                    System.out.printf(
                        "%-5d %-12s %-12s %-15s %-25s $%.2f%n",
                        p.getId(),
                        p.getFecha(),
                        p.getEstado(),
                        p.getFormaPago(),
                        usuarioPorPedido.getOrDefault(p.getId(), "—"),
                        p.getTotal() == null ? 0 : p.getTotal()));
            break;
          }
        case "5":
          {
            List<Usuario> activos = usuarioRepo.listarActivos();
            if (activos.isEmpty()) {
              System.out.println("No hay usuarios activos.");
              break;
            }
            activos.forEach(
                u ->
                    System.out.printf("  [%d] %s %s%n", u.getId(), u.getNombre(), u.getApellido()));
            long id = leerEntero("ID de usuario: ");
            Optional<Usuario> optU = usuarioRepo.buscarPorId(id);
            if (optU.isEmpty() || optU.get().isEliminado()) {
              System.out.println("Usuario no encontrado.");
              break;
            }
            List<Pedido> pedidos = pedidoRepo.buscarPorUsuario(id);
            if (pedidos.isEmpty()) {
              System.out.println("El usuario no tiene pedidos activos.");
              break;
            }
            System.out.printf(
                "%-5s %-12s %-12s %-15s %-10s%n", "ID", "Fecha", "Estado", "Forma Pago", "Total");
            pedidos.forEach(
                p ->
                    System.out.printf(
                        "%-5d %-12s %-12s %-15s $%.2f%n",
                        p.getId(),
                        p.getFecha(),
                        p.getEstado(),
                        p.getFormaPago(),
                        p.getTotal() == null ? 0 : p.getTotal()));
            break;
          }
        case "6":
          {
            EstadoPedido estado = leerEnum("Estado a buscar:", EstadoPedido.class);
            // Mapa pedidoId→nombreUsuario navegando desde Usuario.pedidos
            Map<Long, String> usuarioPorPedido = new HashMap<>();
            usuarioRepo
                .listarActivos()
                .forEach(
                    u ->
                        pedidoRepo
                            .buscarPorUsuario(u.getId())
                            .forEach(
                                p ->
                                    usuarioPorPedido.put(
                                        p.getId(), u.getNombre() + " " + u.getApellido())));
            List<Pedido> pedidos = pedidoRepo.buscarPorEstado(estado);
            if (pedidos.isEmpty()) {
              System.out.println("No hay pedidos con estado " + estado + ".");
              break;
            }
            System.out.printf("%-5s %-12s %-25s %-10s%n", "ID", "Fecha", "Usuario", "Total");
            pedidos.forEach(
                p ->
                    System.out.printf(
                        "%-5d %-12s %-25s $%.2f%n",
                        p.getId(),
                        p.getFecha(),
                        usuarioPorPedido.getOrDefault(p.getId(), "—"),
                        p.getTotal() == null ? 0 : p.getTotal()));
            break;
          }
        case "0":
          volver = true;
          break;
        default:
          System.out.println("Opción inválida.");
      }
    }
  }

  private static void menuReportes() {
    boolean volver = false;
    while (!volver) {
      System.out.println();
      System.out.println("--- Reportes ---");
      System.out.println("1. Productos por categoría");
      System.out.println("2. Pedidos por usuario");
      System.out.println("3. Pedidos por estado");
      System.out.println("4. Total facturado");
      System.out.println("0. Volver");
      String op = leerLinea("Opción: ");
      switch (op) {
        case "1":
          {
            List<Categoria> cats = categoriaRepo.listarActivos();
            if (cats.isEmpty()) {
              System.out.println("No hay categorías activas.");
              break;
            }
            cats.forEach(c -> System.out.printf("  [%d] %s%n", c.getId(), c.getNombre()));
            long id = leerEntero("ID de categoría: ");
            Optional<Categoria> optC = categoriaRepo.buscarPorId(id);
            if (optC.isEmpty() || optC.get().isEliminado()) {
              System.out.println("Categoría no encontrada.");
              break;
            }
            List<Producto> prods = productoRepo.buscarPorCategoria(id);
            if (prods.isEmpty()) {
              System.out.println("No hay productos en esa categoría.");
              break;
            }
            System.out.printf("%-5s %-20s %-10s %-7s%n", "ID", "Nombre", "Precio", "Stock");
            prods.forEach(
                p ->
                    System.out.printf(
                        "%-5d %-20s $%-9.2f %-7d%n",
                        p.getId(), p.getNombre(), p.getPrecio(), p.getStock()));
            break;
          }
        case "2":
          {
            List<Usuario> usuarios = usuarioRepo.listarActivos();
            if (usuarios.isEmpty()) {
              System.out.println("No hay usuarios activos.");
              break;
            }
            usuarios.forEach(
                u ->
                    System.out.printf("  [%d] %s %s%n", u.getId(), u.getNombre(), u.getApellido()));
            long id = leerEntero("ID de usuario: ");
            Optional<Usuario> optU = usuarioRepo.buscarPorId(id);
            if (optU.isEmpty() || optU.get().isEliminado()) {
              System.out.println("Usuario no encontrado.");
              break;
            }
            List<Pedido> pedidos = pedidoRepo.buscarPorUsuario(id);
            if (pedidos.isEmpty()) {
              System.out.println("El usuario no tiene pedidos activos.");
              break;
            }
            System.out.printf(
                "%-5s %-12s %-12s %-15s %-10s%n", "ID", "Fecha", "Estado", "Forma Pago", "Total");
            pedidos.forEach(
                p ->
                    System.out.printf(
                        "%-5d %-12s %-12s %-15s $%.2f%n",
                        p.getId(),
                        p.getFecha(),
                        p.getEstado(),
                        p.getFormaPago(),
                        p.getTotal() == null ? 0 : p.getTotal()));
            break;
          }
        case "3":
          {
            EstadoPedido estado = leerEnum("Estado a buscar:", EstadoPedido.class);
            Map<Long, String> usuarioPorPedido = new HashMap<>();
            usuarioRepo
                .listarActivos()
                .forEach(
                    u ->
                        pedidoRepo
                            .buscarPorUsuario(u.getId())
                            .forEach(
                                p ->
                                    usuarioPorPedido.put(
                                        p.getId(), u.getNombre() + " " + u.getApellido())));
            List<Pedido> pedidos = pedidoRepo.buscarPorEstado(estado);
            if (pedidos.isEmpty()) {
              System.out.println("No hay pedidos con estado " + estado + ".");
              break;
            }
            System.out.printf("%-5s %-12s %-25s %-10s%n", "ID", "Fecha", "Usuario", "Total");
            pedidos.forEach(
                p ->
                    System.out.printf(
                        "%-5d %-12s %-25s $%.2f%n",
                        p.getId(),
                        p.getFecha(),
                        usuarioPorPedido.getOrDefault(p.getId(), "—"),
                        p.getTotal() == null ? 0 : p.getTotal()));
            break;
          }
        case "4":
          {
            List<Pedido> terminados = pedidoRepo.buscarPorEstado(EstadoPedido.TERMINADO);
            double total =
                terminados.stream().mapToDouble(p -> p.getTotal() == null ? 0 : p.getTotal()).sum();
            System.out.println("Total facturado: " + String.format(Locale.US, "$%.2f", total));
            break;
          }
        case "0":
          volver = true;
          break;
        default:
          System.out.println("Opción inválida.");
      }
    }
  }

  // ── Helpers de consola ────────────────────────────────────────

  /** Lee una línea y la devuelve sin espacios. */
  private static String leerLinea(String prompt) {
    System.out.print(prompt);
    return sc.nextLine().trim();
  }

  /**
   * Si el usuario ingresa algo no vacío lo devuelve; si Enter en blanco devuelve el valor actual
   * (patrón "campo vacío = conservar").
   */
  private static String leerOpcional(String prompt, String actual) {
    System.out.print(prompt + " [" + actual + "]: ");
    String input = sc.nextLine().trim();
    return input.isEmpty() ? actual : input;
  }

  /** Pide un entero hasta que sea válido. */
  private static int leerEntero(String prompt) {
    while (true) {
      String input = leerLinea(prompt);
      try {
        return Integer.parseInt(input);
      } catch (NumberFormatException e) {
        System.out.println("Ingresá un número entero válido.");
      }
    }
  }

  /** Pide un Double hasta que sea válido. */
  private static double leerDouble(String prompt) {
    while (true) {
      String input = leerLinea(prompt);
      try {
        return Double.parseDouble(input);
      } catch (NumberFormatException e) {
        System.out.println("Ingresá un número decimal válido (usá punto).");
      }
    }
  }

  /** Muestra las constantes del enum numeradas y pide selección. Reintenta hasta que sea válida. */
  private static <E extends Enum<E>> E leerEnum(String prompt, Class<E> enumClass) {
    E[] valores = enumClass.getEnumConstants();
    while (true) {
      System.out.println(prompt);
      for (int i = 0; i < valores.length; i++) {
        System.out.println("  " + (i + 1) + ". " + valores[i].name());
      }
      System.out.print("Opción: ");
      String input = sc.nextLine().trim();
      try {
        int idx = Integer.parseInt(input) - 1;
        if (idx >= 0 && idx < valores.length) {
          return valores[idx];
        }
      } catch (NumberFormatException ignored) {
      }
      System.out.println("Opción inválida.");
    }
  }

  private static boolean esMailValido(String mail) {
    return mail.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$");
  }

  private static boolean excedeLongitud(String campo, String valor, int max) {
    if (valor.length() > max) {
      System.out.printf("El %s no puede superar los %d caracteres.%n", campo, max);
      return true;
    }
    return false;
  }

  /** Pide confirmación S/N. Devuelve true si el usuario escribe S o s. */
  private static boolean confirmar(String prompt) {
    String resp = leerLinea(prompt + " (S/N): ");
    return resp.equalsIgnoreCase("S");
  }
}
