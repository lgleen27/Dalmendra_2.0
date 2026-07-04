package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.Existencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ExistenciaRepository {

    public List<Existencia> findBySucursalId(Long sucursalId) throws SQLException {
        String sql = "SELECT * FROM existencias WHERE sucursal_id = ? ORDER BY orden ASC, codigo ASC";
        List<Existencia> items = new ArrayList<>();

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, sucursalId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }

        return items;
    }
    
    public List<Existencia> findBySucursalIdAndPalabraClave(Long sucursalId, String palabraClave) throws SQLException {
        String sql =
                "SELECT * FROM existencias " +
                "WHERE sucursal_id = ? " +
                "AND UPPER(descripcion) LIKE UPPER(?) " +
                "ORDER BY orden ASC, codigo ASC";

        List<Existencia> items = new ArrayList<>();

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, sucursalId);
            ps.setString(2, palabraClave + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }

        return items;
    }
    
    public List<Existencia> findBySucursalIdOrderByOrden(Long sucursalId) throws SQLException {
        String sql =
                "SELECT * FROM existencias " +
                "WHERE sucursal_id = ? " +
                "ORDER BY orden ASC, codigo ASC";

        List<Existencia> items = new ArrayList<>();

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, sucursalId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }

        return items;
    }

    public Existencia findBySucursalIdAndCodigo(Long sucursalId, String codigo) throws SQLException {
        String sql = "SELECT * FROM existencias WHERE sucursal_id = ? AND codigo = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, sucursalId);
            ps.setString(2, codigo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }

        return null;
    }

    public Existencia save(Existencia existencia) throws SQLException {
        if (existencia.getId() == null) {
            return insert(existencia);
        }
        return update(existencia);
    }

    public Existencia insert(Existencia existencia) throws SQLException {
        String sql =
                "INSERT INTO existencias " +
                "(sucursal_id, categoria_id, codigo, descripcion, existencia, orden, fecha_actualizacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, existencia.getSucursalId());

            if (existencia.getCategoriaId() != null) {
                ps.setLong(2, existencia.getCategoriaId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }

            ps.setString(3, existencia.getCodigo());
            ps.setString(4, existencia.getDescripcion());
            ps.setBigDecimal(5, existencia.getExistencia());

            if (existencia.getOrden() != null) {
                ps.setInt(6, existencia.getOrden());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            if (existencia.getFechaActualizacion() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(existencia.getFechaActualizacion()));
            } else {
                ps.setTimestamp(7, null);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    existencia.setId(rs.getLong(1));
                }
            }
        }

        return existencia;
    }

    public Existencia update(Existencia existencia) throws SQLException {
        String sql =
                "UPDATE existencias SET " +
                "sucursal_id = ?, " +
                "categoria_id = ?, " +
                "codigo = ?, " +
                "descripcion = ?, " +
                "existencia = ?, " +
                "orden = ?, " +
                "fecha_actualizacion = ? " +
                "WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, existencia.getSucursalId());

            if (existencia.getCategoriaId() != null) {
                ps.setLong(2, existencia.getCategoriaId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }

            ps.setString(3, existencia.getCodigo());
            ps.setString(4, existencia.getDescripcion());
            ps.setBigDecimal(5, existencia.getExistencia());

            if (existencia.getOrden() != null) {
                ps.setInt(6, existencia.getOrden());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            if (existencia.getFechaActualizacion() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(existencia.getFechaActualizacion()));
            } else {
                ps.setTimestamp(7, null);
            }

            ps.setLong(8, existencia.getId());
            ps.executeUpdate();
        }

        return existencia;
    }

    public void updateOrden(Long id, Integer orden) throws SQLException {
        String sql = "UPDATE existencias SET orden = ? WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, orden);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void deleteBySucursalId(Long sucursalId) throws SQLException {
        String sql = "DELETE FROM existencias WHERE sucursal_id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, sucursalId);
            ps.executeUpdate();
        }
    }

    private Existencia map(ResultSet rs) throws SQLException {
        Existencia e = new Existencia();

        long id = rs.getLong("id");
        e.setId(rs.wasNull() ? null : id);

        long sucursalId = rs.getLong("sucursal_id");
        e.setSucursalId(rs.wasNull() ? null : sucursalId);

        long categoriaId = rs.getLong("categoria_id");
        e.setCategoriaId(rs.wasNull() ? null : categoriaId);

        e.setCodigo(rs.getString("codigo"));
        e.setDescripcion(rs.getString("descripcion"));
        e.setExistencia(rs.getBigDecimal("existencia"));

        int orden = rs.getInt("orden");
        e.setOrden(rs.wasNull() ? null : orden);

        Timestamp ts = rs.getTimestamp("fecha_actualizacion");
        e.setFechaActualizacion(ts != null ? ts.toLocalDateTime() : null);

        return e;
    }
}