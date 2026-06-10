/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.Sucursal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SucursalRepository {

    public List<Sucursal> findAll() throws SQLException {
        String sql = "SELECT * FROM sucursales ORDER BY orden ASC";
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
            "INSERT INTO sucursales"
            + "(nombre_sucursal, data_source, catalog, user_id, password, orden, fecha_hora_actualizacion, color, activa)"
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        ;

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sucursal.getNombreSucursal());
            ps.setString(2, sucursal.getDataSource());
            ps.setString(3, sucursal.getCatalog());
            ps.setString(4, sucursal.getUserId());
            ps.setString(5, sucursal.getPassword());
            ps.setInt(6, sucursal.getOrden() == null ? 0 : sucursal.getOrden());
            if (sucursal.getFechaHoraActualizacion() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(sucursal.getFechaHoraActualizacion()));
            } else {
                ps.setTimestamp(7, null);
            }
            ps.setString(8, sucursal.getColor());
            ps.setBoolean(9, sucursal.getActiva() == null || sucursal.getActiva());

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
            "UPDATE sucursales"
            + "SET nombre_sucursal = ?, data_source = ?, catalog = ?, user_id = ?, password = ?,"
                 + "orden = ?, fecha_hora_actualizacion = ?, color = ?, activa = ?"
            + "WHERE id = ?"
        ;

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, sucursal.getNombreSucursal());
            ps.setString(2, sucursal.getDataSource());
            ps.setString(3, sucursal.getCatalog());
            ps.setString(4, sucursal.getUserId());
            ps.setString(5, sucursal.getPassword());
            ps.setInt(6, sucursal.getOrden() == null ? 0 : sucursal.getOrden());
            if (sucursal.getFechaHoraActualizacion() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(sucursal.getFechaHoraActualizacion()));
            } else {
                ps.setTimestamp(7, null);
            }
            ps.setString(8, sucursal.getColor());
            ps.setBoolean(9, sucursal.getActiva() == null || sucursal.getActiva());
            ps.setLong(10, sucursal.getId());

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

    private Sucursal map(ResultSet rs) throws SQLException {
        Sucursal s = new Sucursal();
        s.setId(rs.getLong("id"));
        s.setNombreSucursal(rs.getString("nombre_sucursal"));
        s.setDataSource(rs.getString("data_source"));
        s.setCatalog(rs.getString("catalog"));
        s.setUserId(rs.getString("user_id"));
        s.setPassword(rs.getString("password"));
        s.setOrden(rs.getInt("orden"));
        Timestamp ts = rs.getTimestamp("fecha_hora_actualizacion");
        s.setFechaHoraActualizacion(ts != null ? ts.toLocalDateTime() : null);
        s.setColor(rs.getString("color"));
        s.setActiva(rs.getBoolean("activa"));
        return s;
    }
}