package com.tp.jpa.repository;

import com.tp.jpa.model.Usuario;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Usuario. Además del CRUD heredado implementa la búsqueda de un usuario activo por
 * su mail.
 */
public class UsuarioRepository extends BaseRepository<Usuario> {

  public UsuarioRepository() {
    super(Usuario.class);
  }

  /** Retorna el usuario activo con el mail indicado, o empty si no existe. */
  public Optional<Usuario> buscarPorMail(String mail) {
    EntityManager em = emf.createEntityManager();
    try {
      String jpql = "SELECT u FROM Usuario u WHERE u.mail = :mail AND u.eliminado = false";
      List<Usuario> resultado =
          em.createQuery(jpql, Usuario.class).setParameter("mail", mail).getResultList();
      return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    } finally {
      em.close();
    }
  }
}
