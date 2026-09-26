package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.Categoria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Categoria} en MySQL.
 * Proporciona métodos para consultar categorías activas, guardar cambios y actualizar su orden.
 */
public class CategoriaRepository {

    /**
     * Retorna todas las categorías registradas ordenadas ascendentemente.
     *
     * @return Lista de categorías.
     * @throws SQLException Si ocurre un error SQL.
     */
    public List<Categoria> findAll() throws SQLException {
        String sql = "SELECT * FROM categorias ORDER BY orden ASC";
        List<Categoria> items = new ArrayList<>();

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(map(rs));
            }
        }
        return items;
    }

    public List<Categoria> findActivas() throws SQLException {
        String sql = "SELECT * FROM categorias WHERE estado = 1 ORDER BY orden ASC, descripcion ASC";
        List<Categoria> items = new ArrayList<>();

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(map(rs));
            }
        }

        return items;
    }

    public Categoria save(Categoria categoria) throws SQLException {
        if (categoria.getId() == null) {
            return insert(categoria);
        }
        return update(categoria);
    }

    public Categoria insert(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO categorias (descripcion, palabra_clave, orden, estado) VALUES (?, ?, ?, ?)";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, categoria.getDescripcion());
            ps.setString(2, categoria.getPalabraClave());
            ps.setInt(3, categoria.getOrden() == null ? 0 : categoria.getOrden());
            ps.setBoolean(4, categoria.getEstado() != null && categoria.getEstado());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    categoria.setId(rs.getLong(1));
                }
            }
        }

        return categoria;
    }

    public Categoria update(Categoria categoria) throws SQLException {
        String sql = "UPDATE categorias SET descripcion = ?, palabra_clave = ?, orden = ?, estado = ? WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, categoria.getDescripcion());
            ps.setString(2, categoria.getPalabraClave());
            ps.setInt(3, categoria.getOrden() == null ? 0 : categoria.getOrden());
            ps.setBoolean(4, categoria.getEstado() != null && categoria.getEstado());
            ps.setLong(5, categoria.getId());

            ps.executeUpdate();
        }

        return categoria;
    }

    public void deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM categorias WHERE id = ?";
        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void updateOrden(Long id, Integer orden) throws SQLException {
        String sql = "UPDATE categorias SET orden = ? WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, orden);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private Categoria map(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getLong("id"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setPalabraClave(rs.getString("palabra_clave"));
        c.setOrden(rs.getInt("orden"));
        c.setEstado(rs.getBoolean("estado"));
        return c;
    }
}