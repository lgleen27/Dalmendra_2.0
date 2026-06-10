/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.Existencia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExistenciaRepository {

    public List<Existencia> findBySucursalId(Long sucursalId) throws SQLException {
        String sql = "SELECT * FROM existencias WHERE sucursal_id = ? ORDER BY orden ASC";
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
            "INSERT INTO existencias"
            + "(sucursal_id, codigo, descripcion, existencia, orden, fecha_actualizacion)"
            + "VALUES (?, ?, ?, ?, ?, ?)"
        ;

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, existencia.getSucursalId());
            ps.setString(2, existencia.getCodigo());
            ps.setString(3, existencia.getDescripcion());
            ps.setBigDecimal(4, existencia.getExistencia());
            ps.setInt(5, existencia.getOrden() == null ? 0 : existencia.getOrden());
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
            "UPDATE existencias"
            + "SET descripcion = ?, existencia = ?, orden = ?, fecha_actualizacion = ?"
            + "WHERE id = ?"
        ;

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, existencia.getDescripcion());
            ps.setBigDecimal(2, existencia.getExistencia());
            ps.setInt(3, existencia.getOrden() == null ? 0 : existencia.getOrden());
            if (existencia.getFechaActualizacion() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(existencia.getFechaActualizacion()));
            } else {
                ps.setTimestamp(4, null);
            }
            ps.setLong(5, existencia.getId());
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
        e.setId(rs.getLong("id"));
        e.setSucursalId(rs.getLong("sucursal_id"));
        e.setCodigo(rs.getString("codigo"));
        e.setDescripcion(rs.getString("descripcion"));
        e.setExistencia(rs.getBigDecimal("existencia"));
        e.setOrden(rs.getInt("orden"));
        Timestamp ts = rs.getTimestamp("fecha_actualizacion");
        e.setFechaActualizacion(ts != null ? ts.toLocalDateTime() : null);
        return e;
    }
}