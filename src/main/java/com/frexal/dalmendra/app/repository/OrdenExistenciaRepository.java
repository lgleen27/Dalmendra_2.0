/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.OrdenExistencia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdenExistenciaRepository {

    public List<OrdenExistencia> findAll() throws SQLException {
        String sql = "SELECT * FROM orden_existencias ORDER BY orden DESC";
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
        String sql = "SELECT * FROM orden_existencias WHERE sucursal_id = ? AND codigo = ?";

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

    public void saveOrUpdate(OrdenExistencia ordenExistencia) throws SQLException {
        String sql = 
            "INSERT INTO orden_existencias (sucursal_id, codigo, orden)"
            + "VALUES (?, ?, ?)"
            + "ON DUPLICATE KEY UPDATE orden = VALUES(orden)"
        ;

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, ordenExistencia.getSucursalId());
            ps.setString(2, ordenExistencia.getCodigo());
            ps.setInt(3, ordenExistencia.getOrden());
            ps.executeUpdate();
        }
    }

    private OrdenExistencia map(ResultSet rs) throws SQLException {
        OrdenExistencia o = new OrdenExistencia();
        o.setId(rs.getLong("id"));
        o.setSucursalId(rs.getLong("sucursal_id"));
        o.setCodigo(rs.getString("codigo"));
        o.setOrden(rs.getInt("orden"));
        return o;
    }
}