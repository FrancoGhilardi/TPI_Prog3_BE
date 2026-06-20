package com.tp.jpa.repository;

import com.tp.jpa.model.Pedido;
import com.tp.jpa.model.enums.EstadoPedido;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Repositorio de Pedido. Además del CRUD heredado implementa consultas por usuario y por estado.
 */
public class PedidoRepository extends BaseRepository<Pedido> {

  public PedidoRepository() {
    super(Pedido.class);
  }

  /**
   * Retorna los pedidos activos del usuario indicado. Navega desde Usuario.pedidos porque Pedido no
   * tiene campo usuario (relación @OneToMany unidireccional con @JoinColumn en el padre).
   */
  public List<Pedido> buscarPorUsuario(Long idUsuario) {
    EntityManager em = emf.createEntityManager();
    try {
      String jpql =
          "SELECT p FROM Usuario u JOIN u.pedidos p " + "WHERE u.id = :uid AND p.eliminado = false";
      return em.createQuery(jpql, Pedido.class).setParameter("uid", idUsuario).getResultList();
    } finally {
      em.close();
    }
  }

  /**
   * Retorna los pedidos activos que coinciden con el estado indicado. Pedido tiene campo estado
   * directamente, no requiere JOIN.
   */
  public List<Pedido> buscarPorEstado(EstadoPedido estadoPedido) {
    EntityManager em = emf.createEntityManager();
    try {
      String jpql = "SELECT p FROM Pedido p WHERE p.estado = :estado AND p.eliminado = false";
      return em.createQuery(jpql, Pedido.class)
          .setParameter("estado", estadoPedido)
          .getResultList();
    } finally {
      em.close();
    }
  }
}
