package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.OrdenExistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link OrdenExistencia} en MySQL.
 * Administra el orden prioritario de los insumos por sucursal.
 */
public class OrdenExistenciaRepository {

    /**
     * Retorna todas las configuraciones de orden registradas.
     *
     * @return Lista de órdenes por sucursal y código.
     * @throws SQLException Si ocurre un error SQL.
     */
    public List<OrdenExistencia> findAll() throws SQLException {
        String sql = "SELECT * FROM orden_existencias ORDER BY sucursal_id ASC, orden ASC, codigo ASC";
        List<OrdenExistencia> items = new ArrayList<>();

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(map(rs));
            }
        }

        return items;
    }

    public OrdenExistencia findBySucursalIdAndCodigo(Long sucursalId, String codigo) throws SQLException {
        String sql = "SELECT * FROM orden_existencias WHERE sucursal_id = ? AND codigo = ? ORDER BY id ASC LIMIT 1";

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

    public int getSiguienteOrden(Long sucursalId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(orden), 0) + 1 FROM orden_existencias WHERE sucursal_id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, sucursalId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 1;
    }

    public OrdenExistencia saveOrUpdate(OrdenExistencia ordenExistencia) throws SQLException {
        OrdenExistencia existente = findBySucursalIdAndCodigo(
                ordenExistencia.getSucursalId(),
                ordenExistencia.getCodigo()
        );

        if (existente == null) {
            return insert(ordenExistencia);
        }

        existente.setOrden(ordenExistencia.getOrden());
        return update(existente);
    }

    public OrdenExistencia insert(OrdenExistencia ordenExistencia) throws SQLException {
        String sql = "INSERT INTO orden_existencias (sucursal_id, codigo, orden) VALUES (?, ?, ?)";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, ordenExistencia.getSucursalId());
            ps.setString(2, ordenExistencia.getCodigo());
            ps.setInt(3, ordenExistencia.getOrden() == null ? 0 : ordenExistencia.getOrden());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    ordenExistencia.setId(rs.getLong(1));
                }
            }
        }

        return ordenExistencia;
    }

    public OrdenExistencia update(OrdenExistencia ordenExistencia) throws SQLException {
        String sql = "UPDATE orden_existencias SET sucursal_id = ?, codigo = ?, orden = ? WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, ordenExistencia.getSucursalId());
            ps.setString(2, ordenExistencia.getCodigo());
            ps.setInt(3, ordenExistencia.getOrden() == null ? 0 : ordenExistencia.getOrden());
            ps.setLong(4, ordenExistencia.getId());

            ps.executeUpdate();
        }

        return ordenExistencia;
    }

    private OrdenExistencia map(ResultSet rs) throws SQLException {
        OrdenExistencia o = new OrdenExistencia();

        long id = rs.getLong("id");
        o.setId(rs.wasNull() ? null : id);

        long sucursalId = rs.getLong("sucursal_id");
        o.setSucursalId(rs.wasNull() ? null : sucursalId);

        o.setCodigo(rs.getString("codigo"));

        int orden = rs.getInt("orden");
        o.setOrden(rs.wasNull() ? null : orden);

        return o;
    }
}