package com.tp.jpa.repository;

import com.tp.jpa.model.Categoria;
import com.tp.jpa.model.Producto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;

/**
 * Repositorio de Producto. Además del CRUD heredado implementa la consulta de productos activos por
 * categoría.
 */
public class ProductoRepository extends BaseRepository<Producto> {

  public ProductoRepository() {
    super(Producto.class);
  }

  /**
   * Persiste un producto nuevo asociándolo a la categoría gestionada. Usa cascade ALL de
   * Categoria.productos para setear la FK categoria_id.
   */
  public Producto guardarEnCategoria(Producto producto, Long categoriaId) {
    EntityManager em = emf.createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Categoria cat = em.find(Categoria.class, categoriaId);
      if (cat == null || cat.isEliminado()) {
        throw new IllegalArgumentException("Categoría no encontrada: " + categoriaId);
      }
      cat.addProducto(producto);
      tx.commit();
      return producto;
    } catch (RuntimeException e) {
      if (tx.isActive()) tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * Retorna los productos activos de una categoría. Navega desde Categoria.productos porque
   * Producto no tiene campo categoria (relación unidireccional, dueño: Categoria).
   */
  public List<Producto> buscarPorCategoria(Long categoriaId) {
    EntityManager em = emf.createEntityManager();
    try {
      String jpql =
          "SELECT p FROM Categoria c JOIN c.productos p "
              + "WHERE c.id = :catId AND p.eliminado = false";
      return em.createQuery(jpql, Producto.class)
          .setParameter("catId", categoriaId)
          .getResultList();
    } finally {
      em.close();
    }
  }
}
