package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.Sucursal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SucursalRepository {

    public List<Sucursal> findAll() throws SQLException {
        String sql = "SELECT * FROM sucursales ORDER BY orden ASC, nombre_sucursal ASC";
        List<Sucursal> items = new ArrayList<>();

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(map(rs));
            }
        }

        return items;
    }

    public Sucursal save(Sucursal sucursal) throws SQLException {
        if (sucursal.getId() == null) {
            return insert(sucursal);
        }
        return update(sucursal);
    }

    public Sucursal insert(Sucursal sucursal) throws SQLException {
        String sql =
                "INSERT INTO sucursales " +
                "(nombre_sucursal, data_source, catalog, user_id, password, orden, fecha_hora_actualizacion, color, activa) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sucursal.getNombreSucursal());
            ps.setString(2, sucursal.getDataSource());
            ps.setString(3, sucursal.getCatalog());
            ps.setString(4, sucursal.getUserId());
            ps.setString(5, sucursal.getPassword());

            if (sucursal.getOrden() != null) {
                ps.setInt(6, sucursal.getOrden());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            if (sucursal.getFechaHoraActualizacion() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(sucursal.getFechaHoraActualizacion()));
            } else {
                ps.setTimestamp(7, null);
            }

            ps.setString(8, sucursal.getColor());

            if (sucursal.getActiva() != null) {
                ps.setBoolean(9, sucursal.getActiva());
            } else {
                ps.setNull(9, java.sql.Types.BOOLEAN);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    sucursal.setId(rs.getLong(1));
                }
            }
        }

        return sucursal;
    }

    public Sucursal update(Sucursal sucursal) throws SQLException {
        String sql =
                "UPDATE sucursales SET " +
                "nombre_sucursal = ?, " +
                "data_source = ?, " +
                "catalog = ?, " +
                "user_id = ?, " +
                "password = ?, " +
                "orden = ?, " +
                "fecha_hora_actualizacion = ?, " +
                "color = ?, " +
                "activa = ? " +
                "WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, sucursal.getNombreSucursal());
            ps.setString(2, sucursal.getDataSource());
            ps.setString(3, sucursal.getCatalog());
            ps.setString(4, sucursal.getUserId());
            ps.setString(5, sucursal.getPassword());

            if (sucursal.getOrden() != null) {
                ps.setInt(6, sucursal.getOrden());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            if (sucursal.getFechaHoraActualizacion() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(sucursal.getFechaHoraActualizacion()));
            } else {
                ps.setTimestamp(7, null);
            }

            ps.setString(8, sucursal.getColor());

            if (sucursal.getActiva() != null) {
                ps.setBoolean(9, sucursal.getActiva());
            } else {
                ps.setNull(9, java.sql.Types.BOOLEAN);
            }

            ps.setLong(10, sucursal.getId());

            ps.executeUpdate();
        }

        return sucursal;
    }

    public Sucursal updateSinPassword(Sucursal sucursal) throws SQLException {
        String sql =
                "UPDATE sucursales SET " +
                "nombre_sucursal = ?, " +
                "data_source = ?, " +
                "catalog = ?, " +
                "user_id = ?, " +
                "orden = ?, " +
                "fecha_hora_actualizacion = ?, " +
                "color = ?, " +
                "activa = ? " +
                "WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, sucursal.getNombreSucursal());
            ps.setString(2, sucursal.getDataSource());
            ps.setString(3, sucursal.getCatalog());
            ps.setString(4, sucursal.getUserId());

            if (sucursal.getOrden() != null) {
                ps.setInt(5, sucursal.getOrden());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }

            if (sucursal.getFechaHoraActualizacion() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(sucursal.getFechaHoraActualizacion()));
            } else {
                ps.setTimestamp(6, null);
            }

            ps.setString(7, sucursal.getColor());

            if (sucursal.getActiva() != null) {
                ps.setBoolean(8, sucursal.getActiva());
            } else {
                ps.setNull(8, java.sql.Types.BOOLEAN);
            }

            ps.setLong(9, sucursal.getId());

            ps.executeUpdate();
        }

        return sucursal;
    }

    public void deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM sucursales WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void updateFechaActualizacion(Long id, Timestamp fecha) throws SQLException {
        String sql = "UPDATE sucursales SET fecha_hora_actualizacion = ? WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setTimestamp(1, fecha);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void updateOrden(Long id, Integer orden) throws SQLException {
        String sql = "UPDATE sucursales SET orden = ? WHERE id = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (orden != null) {
                ps.setInt(1, orden);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private Sucursal map(ResultSet rs) throws SQLException {
        Sucursal s = new Sucursal();

        long id = rs.getLong("id");
        s.setId(rs.wasNull() ? null : id);

        s.setNombreSucursal(rs.getString("nombre_sucursal"));
        s.setDataSource(rs.getString("data_source"));
        s.setCatalog(rs.getString("catalog"));
        s.setUserId(rs.getString("user_id"));
        s.setPassword(rs.getString("password"));

        int orden = rs.getInt("orden");
        s.setOrden(rs.wasNull() ? null : orden);

        Timestamp ts = rs.getTimestamp("fecha_hora_actualizacion");
        s.setFechaHoraActualizacion(ts != null ? ts.toLocalDateTime() : null);

        s.setColor(rs.getString("color"));

        boolean activa = rs.getBoolean("activa");
        s.setActiva(rs.wasNull() ? null : activa);

        return s;
    }
}