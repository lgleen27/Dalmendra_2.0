/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frexal.dalmendra.app.repository;

import com.frexal.dalmendra.app.config.MySqlConnectionFactory;
import com.frexal.dalmendra.app.model.Configuracion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfiguracionRepository {

    public List<Configuracion> findAll() throws SQLException {
        String sql = "SELECT * FROM configuracion ORDER BY id ASC";
        List<Configuracion> items = new ArrayList<>();

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(map(rs));
            }
        }
        return items;
    }

    public Optional<Configuracion> findByDescripcion(String descripcion) throws SQLException {
        String sql = "SELECT * FROM configuracion WHERE descripcion = ?";

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, descripcion);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void upsert(String descripcion, String valor) throws SQLException {
        String sql = 
            "INSERT INTO configuracion (descripcion, valor)"
            + "VALUES (?, ?)"
            + "ON DUPLICATE KEY UPDATE valor = VALUES(valor)"
        ;

        try (Connection cn = MySqlConnectionFactory.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, descripcion);
            ps.setString(2, valor);
            ps.executeUpdate();
        }
    }

    private Configuracion map(ResultSet rs) throws SQLException {
        Configuracion c = new Configuracion();
        c.setId(rs.getLong("id"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setValor(rs.getString("valor"));
        return c;
    }
}