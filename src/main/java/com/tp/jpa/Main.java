package com.tp.jpa;

import com.tp.jpa.model.enums.EstadoPedido;
import com.tp.jpa.model.*;
import com.tp.jpa.model.enums.FormaPago;
import com.tp.jpa.model.enums.Rol;
import com.tp.jpa.repository.CategoriaRepository;
import com.tp.jpa.repository.PedidoRepository;
import com.tp.jpa.repository.ProductoRepository;
import com.tp.jpa.repository.UsuarioRepository;
import com.tp.jpa.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

/**
 * Clase principal: menú de consola del sistema Food Store.
 * Orden de uso natural: Categorías -> Productos -> Usuarios -> Pedidos.
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
                case "1": menuCategorias(); break;
                case "2": menuProductos(); break;
                case "3": menuUsuarios(); break;
                case "4": menuPedidos(); break;
                case "5": menuReportes(); break;
                case "0": salir = true; break;
                default: System.out.println("Opción inválida.");
            }
        }
        JPAUtil.close();
        System.out.println("Aplicación finalizada.");
    }

    // ── Submenús ─────────────────────────────────────────────────

    private static void menuCategorias() {
        // TODO: Implementar submenú de Categorías.
        // Opciones: 1-Alta  2-Modificar  3-Baja lógica  4-Listado  0-Volver
        System.out.println("[Categorías] → TODO: implementar");
    }

    private static void menuProductos() {
        // TODO: Implementar submenú de Productos.
        // Opciones: 1-Alta  2-Modificar  3-Baja lógica  4-Listado  0-Volver
        System.out.println("[Productos] → TODO: implementar");
    }

    private static void menuUsuarios() {
        // TODO: Implementar submenú de Usuarios.
        // Opciones: 1-Alta  2-Modificar  3-Baja lógica  4-Listado  5-Buscar por mail  0-Volver
        System.out.println("[Usuarios] → TODO: implementar");
    }

    private static void menuPedidos() {
        // TODO: Implementar submenú de Pedidos.
        // Opciones: 1-Alta  2-Cambiar estado  3-Baja lógica  4-Listado
        //           5-Por usuario  6-Por estado  0-Volver
        System.out.println("[Pedidos] → TODO: implementar");
    }

    private static void menuReportes() {
        // TODO: Implementar submenú de Reportes.
        // Opciones: 1-Productos por categoría  2-Pedidos por usuario
        //           3-Pedidos por estado  4-Total facturado  0-Volver
        System.out.println("[Reportes] → TODO: implementar");
    }

    // ── Helpers de consola ────────────────────────────────────────

    /** Lee una línea y la devuelve sin espacios. */
    private static String leerLinea(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    /**
     * Si el usuario ingresa algo no vacío lo devuelve; si Enter en blanco
     * devuelve el valor actual (patrón "campo vacío = conservar").
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

    /**
     * Muestra las constantes del enum numeradas y pide selección.
     * Reintenta hasta que sea válida.
     */
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
            } catch (NumberFormatException ignored) {}
            System.out.println("Opción inválida.");
        }
    }

    /** Pide confirmación S/N. Devuelve true si el usuario escribe S o s. */
    private static boolean confirmar(String prompt) {
        String resp = leerLinea(prompt + " (S/N): ");
        return resp.equalsIgnoreCase("S");
    }

}
