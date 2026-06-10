package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Sucursal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlServerSucursalClient {

    public boolean validarConexion(Sucursal sucursal) {
        String connectionString = buildConnectionString(sucursal);

        try (Connection cn = DriverManager.getConnection(connectionString)) {
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    public List<InventarioRemotoRow> consultarInventario(Sucursal sucursal) throws SQLException {
        String sql =
                "SELECT i.idinsumo AS CODIGO, i.descripcion AS DESCRIPCION, a.existencia AS EXISTENCIA "
                + "FROM insumos AS i "
                + "INNER JOIN acumuladoinsumos AS a ON i.idinsumo = a.idinsumo "
                + "WHERE a.idalmacen = 3";

        List<InventarioRemotoRow> resultado = new ArrayList<InventarioRemotoRow>();

        try (Connection cn = DriverManager.getConnection(buildConnectionString(sucursal));
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultado.add(new InventarioRemotoRow(
                        rs.getString("CODIGO"),
                        rs.getString("DESCRIPCION"),
                        rs.getBigDecimal("EXISTENCIA")
                ));
            }
        }

        return resultado;
    }

    public List<VentaRemotaRow> consultarVentas(Sucursal sucursal) throws SQLException {
        String sql =
                "SELECT c.idinsumo AS Insumo, "
                + "SUM(t.cantidad * c.cantidad) AS Existencias, "
                + "(SELECT i.descripcion FROM insumos AS i WHERE c.idinsumo = i.idinsumo) AS Descripcion "
                + "FROM tempcheqdet AS t "
                + "INNER JOIN costos AS c ON t.idproducto = c.idproducto "
                + "INNER JOIN tempcheques AS f ON t.foliodet = f.folio "
                + "WHERE f.cancelado = 0 "
                + "GROUP BY c.idinsumo";

        List<VentaRemotaRow> resultado = new ArrayList<VentaRemotaRow>();

        try (Connection cn = DriverManager.getConnection(buildConnectionString(sucursal));
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultado.add(new VentaRemotaRow(
                        rs.getString("Insumo"),
                        rs.getString("Descripcion"),
                        rs.getBigDecimal("Existencias")
                ));
            }
        }

        return resultado;
    }

    private String buildConnectionString(Sucursal sucursal) {
        return "jdbc:sqlserver://" + sucursal.getDataSource()
                + ";databaseName=" + sucursal.getCatalog()
                + ";user=" + sucursal.getUserId()
                + ";password=" + sucursal.getPassword()
                + ";encrypt=false;trustServerCertificate=true";
    }

    public static class InventarioRemotoRow {
        private final String codigo;
        private final String descripcion;
        private final BigDecimal existencia;

        public InventarioRemotoRow(String codigo, String descripcion, BigDecimal existencia) {
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.existencia = existencia;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public BigDecimal getExistencia() {
            return existencia;
        }
    }

    public static class VentaRemotaRow {
        private final String codigo;
        private final String descripcion;
        private final BigDecimal existenciaVendida;

        public VentaRemotaRow(String codigo, String descripcion, BigDecimal existenciaVendida) {
            this.codigo = codigo;
            this.descripcion = descripcion;
            this.existenciaVendida = existenciaVendida;
        }

        public String getCodigo() {
            return codigo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public BigDecimal getExistenciaVendida() {
            return existenciaVendida;
        }
    }
}