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
                "(sucursal_id, codigo, descripcion, existencia, orden, fecha_actualizacion) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, existencia.getSucursalId());
            ps.setString(2, existencia.getCodigo());
            ps.setString(3, existencia.getDescripcion());
            ps.setBigDecimal(4, existencia.getExistencia());

            if (existencia.getOrden() != null) {
                ps.setInt(5, existencia.getOrden());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            if (existencia.getFechaActualizacion() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(existencia.getFechaActualizacion()));
            } else {
                ps.setTimestamp(6, null);
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
                "codigo = ?, " +
                "descripcion = ?, " +
                "existencia = ?, " +
                "orden = ?, " +
                "fecha_actualizacion = ? " +
                "WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, existencia.getSucursalId());
            ps.setString(2, existencia.getCodigo());
            ps.setString(3, existencia.getDescripcion());
            ps.setBigDecimal(4, existencia.getExistencia());

            if (existencia.getOrden() != null) {
                ps.setInt(5, existencia.getOrden());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            if (existencia.getFechaActualizacion() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(existencia.getFechaActualizacion()));
            } else {
                ps.setTimestamp(6, null);
            }

            ps.setLong(7, existencia.getId());
            ps.executeUpdate();
        }

        return existencia;
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