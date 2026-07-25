package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.ExistenciaStock;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExistenciaStockRepository {

    public ExistenciaStock findBySucursalIdAndCodigo(Long sucursalId, String codigo) throws SQLException {
        String sql = "SELECT * FROM existencias_stock WHERE sucursal_id = ? AND codigo = ?";

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

    public List<ExistenciaStock> findBySucursalId(Long sucursalId) throws SQLException {
        String sql = "SELECT * FROM existencias_stock WHERE sucursal_id = ?";
        List<ExistenciaStock> items = new ArrayList<>();

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

    public ExistenciaStock save(ExistenciaStock item) throws SQLException {
        String sql =
                "INSERT INTO existencias_stock " +
                "(sucursal_id, codigo, stock_minimo, stock_deseado, fecha_actualizacion) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "stock_minimo = VALUES(stock_minimo), " +
                "stock_deseado = VALUES(stock_deseado), " +
                "fecha_actualizacion = VALUES(fecha_actualizacion)";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setLong(1, item.getSucursalId());
            ps.setString(2, item.getCodigo());

            if (item.getStockMinimo() != null) {
                ps.setInt(3, item.getStockMinimo());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            if (item.getStockDeseado() != null) {
                ps.setInt(4, item.getStockDeseado());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            if (item.getFechaActualizacion() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(item.getFechaActualizacion()));
            } else {
                ps.setTimestamp(5, null);
            }

            ps.executeUpdate();
        }

        return findBySucursalIdAndCodigo(item.getSucursalId(), item.getCodigo());
    }

    private ExistenciaStock map(ResultSet rs) throws SQLException {
        ExistenciaStock e = new ExistenciaStock();

        long id = rs.getLong("id");
        e.setId(rs.wasNull() ? null : id);

        long sucursalId = rs.getLong("sucursal_id");
        e.setSucursalId(rs.wasNull() ? null : sucursalId);

        e.setCodigo(rs.getString("codigo"));

        int stockMinimo = rs.getInt("stock_minimo");
        e.setStockMinimo(rs.wasNull() ? null : stockMinimo);

        int stockDeseado = rs.getInt("stock_deseado");
        e.setStockDeseado(rs.wasNull() ? null : stockDeseado);

        Timestamp ts = rs.getTimestamp("fecha_actualizacion");
        e.setFechaActualizacion(ts != null ? ts.toLocalDateTime() : null);

        return e;
    }
}