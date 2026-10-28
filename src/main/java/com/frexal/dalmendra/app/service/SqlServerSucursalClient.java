package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.AuditoriaEntradaItem;
import com.frexal.dalmendra.app.model.Sucursal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente JDBC especializado en la comunicación con instancias remotas de Microsoft SQL Server
 * correspondientes a cada sucursal (típicamente bases de datos de punto de venta como Soft Restaurant).
 *
 * Responsabilidades:
 * - Construir URLs de conexión seguras tolerando instancias con nombre y puertos no estándar.
 * - Probar la conectividad de red con timeouts estrictos para no congelar la app.
 * - Consultar existencias físicas en el almacén de insumos (idalmacen = 3).
 * - Consultar ventas en cuentas abiertas/temporales no canceladas para descontar insumos en tiempo real.
 */
public class SqlServerSucursalClient {

    /**
     * Valida si es posible establecer conexión con el servidor SQL Server de la sucursal.
     *
     * @param sucursal Datos de conexión de la sucursal.
     * @return {@code true} si la conexión fue exitosa.
     * @throws RuntimeException Si ocurre un error de comunicación o autenticación.
     */
    public boolean validarConexion(Sucursal sucursal) {
        try (Connection cn = openConnection(sucursal)) {
            return true;
        } catch (Exception ex) {
            throw new RuntimeException("No fue posible conectar con la sucursal " + sucursal.getNombreSucursal() + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Valida la conexión lanzando una {@link SQLException} nativa sin encapsular.
     *
     * @param sucursal Datos de conexión.
     * @throws SQLException Si falla la conexión.
     */
    public void validarConexionOrThrow(Sucursal sucursal) throws SQLException {
        try (Connection cn = openConnection(sucursal)) {
            // conexión correcta
        }
    }

    public List<InventarioRemotoRow> consultarInventario(Sucursal sucursal) throws SQLException {
        String sql =
                "SELECT i.idinsumo AS CODIGO, i.descripcion AS DESCRIPCION, a.existencia AS EXISTENCIA " +
                "FROM insumos AS i " +
                "INNER JOIN acumuladoinsumos AS a ON i.idinsumo = a.idinsumo " +
                "WHERE a.idalmacen = 3";

        List<InventarioRemotoRow> resultado = new ArrayList<>();

        try (Connection cn = openConnection(sucursal);
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
                "SELECT c.idinsumo AS Insumo, " +
                "SUM(t.cantidad * c.cantidad) AS Existencias, " +
                "(SELECT i.descripcion FROM insumos AS i WHERE c.idinsumo = i.idinsumo) AS Descripcion " +
                "FROM tempcheqdet AS t " +
                "INNER JOIN costos AS c ON t.idproducto = c.idproducto " +
                "INNER JOIN tempcheques AS f ON t.foliodet = f.folio " +
                "WHERE f.cancelado = 0 " +
                "GROUP BY c.idinsumo";

        List<VentaRemotaRow> resultado = new ArrayList<>();

        try (Connection cn = openConnection(sucursal);
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

    public com.frexal.dalmendra.app.model.EntradasDelDia consultarEntradasHoy(Sucursal sucursal) {
        return consultarEntradasHoy(sucursal, null);
    }

    public com.frexal.dalmendra.app.model.EntradasDelDia consultarEntradasHoy(Sucursal sucursal, String prefijoOmitir) {
        int compras = 0;
        int movimientos = 0;

        boolean filtrarPrefijo = prefijoOmitir != null && !prefijoOmitir.trim().isEmpty();
        String filtroLike = filtrarPrefijo ? prefijoOmitir.trim() + "%" : "";

        String sqlCompras = 
            "SELECT COALESCE(SUM(m.cantidad), 0) AS total " +
            "FROM movsinv m " +
            (filtrarPrefijo ? "LEFT JOIN insumos i ON m.idinsumo = i.idinsumo " : "") +
            "WHERE m.movto = 0 " +
            "  AND m.idcompra > 0 " +
            "  AND m.idalmacen = 3 " +
            (filtrarPrefijo ? "  AND (i.descripcion IS NULL OR (NOT (i.descripcion LIKE ? OR m.idinsumo LIKE ?))) " : "") +
            "  AND CAST(m.fecha AS DATE) = CAST(GETDATE() AS DATE)";

        String sqlMovimientos = 
            "SELECT COALESCE(SUM(m.cantidad), 0) AS total " +
            "FROM movsinv m " +
            (filtrarPrefijo ? "LEFT JOIN insumos i ON m.idinsumo = i.idinsumo " : "") +
            "WHERE (m.idcompra IS NULL OR m.idcompra = 0) " +
            "  AND m.idalmacen = 3 " +
            "  AND (m.idconcepto IS NULL OR m.idconcepto <> 'SPV') " +
            (filtrarPrefijo ? "  AND (i.descripcion IS NULL OR (NOT (i.descripcion LIKE ? OR m.idinsumo LIKE ?))) " : "") +
            "  AND CAST(m.fecha AS DATE) = CAST(GETDATE() AS DATE)";

        try (Connection cn = openConnection(sucursal)) {
            // 1. Consultar entradas provenientes de compras del día
            try (PreparedStatement ps = cn.prepareStatement(sqlCompras)) {
                if (filtrarPrefijo) {
                    ps.setString(1, filtroLike);
                    ps.setString(2, filtroLike);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        compras = rs.getInt("total");
                    }
                }
            } catch (SQLException ex) {
                System.out.println("[ENTRADAS] Error en compras (" + sucursal.getNombreSucursal() + "): " + ex.getMessage());
            }

            // 2. Consultar entradas directas/manuales de inventario del día
            try (PreparedStatement ps = cn.prepareStatement(sqlMovimientos)) {
                if (filtrarPrefijo) {
                    ps.setString(1, filtroLike);
                    ps.setString(2, filtroLike);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        movimientos = rs.getInt("total");
                    }
                }
            } catch (SQLException ex) {
                System.out.println("[ENTRADAS] Error en movimientos (" + sucursal.getNombreSucursal() + "): " + ex.getMessage());
            }
        } catch (SQLException ex) {
            System.out.println("[ENTRADAS] Error al conectar para entradas (" + sucursal.getNombreSucursal() + "): " + ex.getMessage());
        }

        System.out.println("[ENTRADAS] Total calculado hoy para " + sucursal.getNombreSucursal() 
                + " (Almacén 3) -> Compras: " + compras + ", Movimientos: " + movimientos + ", Total: " + (compras + movimientos));

        return new com.frexal.dalmendra.app.model.EntradasDelDia(compras, movimientos);
    }

    /**
     * Consulta el detalle individual de cada artículo ingresado hoy en movsinv
     * para todas las entradas y movimientos (excluyendo SPV), abarcando todos los almacenes para auditoría.
     */
    public List<AuditoriaEntradaItem> consultarDetalleEntradasHoy(Sucursal sucursal) {
        return consultarDetalleEntradasHoy(sucursal, null);
    }

    public List<AuditoriaEntradaItem> consultarDetalleEntradasHoy(Sucursal sucursal, String prefijoOmitir) {
        List<AuditoriaEntradaItem> resultado = new ArrayList<>();

        boolean filtrarPrefijo = prefijoOmitir != null && !prefijoOmitir.trim().isEmpty();
        String filtroLike = filtrarPrefijo ? prefijoOmitir.trim() + "%" : "";

        String sql =
                "SELECT m.fecha, " +
                "       m.idinsumo AS codigo, " +
                "       COALESCE(i.descripcion, '') AS descripcion, " +
                "       m.cantidad, " +
                "       m.idalmacen, " +
                "       m.idcompra, " +
                "       m.movto, " +
                "       m.traspaso, " +
                "       m.idconcepto " +
                "FROM movsinv m " +
                "LEFT JOIN insumos i ON m.idinsumo = i.idinsumo " +
                "WHERE (m.idconcepto IS NULL OR m.idconcepto <> 'SPV') " +
                (filtrarPrefijo ? "  AND (i.descripcion IS NULL OR (NOT (i.descripcion LIKE ? OR m.idinsumo LIKE ?))) " : "") +
                "  AND CAST(m.fecha AS DATE) = CAST(GETDATE() AS DATE) " +
                "ORDER BY m.fecha DESC";

        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm:ss");

        try (Connection cn = openConnection(sucursal);
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (filtrarPrefijo) {
                ps.setString(1, filtroLike);
                ps.setString(2, filtroLike);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha");
                    String hora = ts != null ? sdfHora.format(ts) : "--:--:--";
                    String codigo = rs.getString("codigo");
                    String descripcion = rs.getString("descripcion");
                    BigDecimal cantidad = rs.getBigDecimal("cantidad");
                    int idAlmacen = rs.getInt("idalmacen");
                    long idCompra = rs.getLong("idcompra");
                    long traspaso = rs.getLong("traspaso");

                    boolean esProduccion = (idAlmacen == 3);
                    String nombreAlmacen = esProduccion ? "Almacén 3 (Producción)" : "Almacén " + idAlmacen + " (Otro)";

                    String origen;
                    if (idCompra > 0) {
                        origen = "Compra #" + idCompra;
                    } else if (traspaso > 0) {
                        origen = "Traspaso #" + traspaso;
                    } else {
                        if (cantidad != null && cantidad.compareTo(BigDecimal.ZERO) < 0) {
                            origen = "Ajuste / Merma (-)";
                        } else {
                            origen = "Ajuste / Alta Manual (+)";
                        }
                    }

                    resultado.add(new AuditoriaEntradaItem(
                            hora,
                            codigo,
                            descripcion,
                            cantidad != null ? cantidad : BigDecimal.ZERO,
                            idAlmacen,
                            nombreAlmacen,
                            esProduccion,
                            origen,
                            idCompra
                    ));
                }
            }
        } catch (SQLException ex) {
            System.out.println("[AUDITORIA] Error al consultar detalle de entradas (" + sucursal.getNombreSucursal() + "): " + ex.getMessage());
        }

        return resultado;
    }

    private Connection openConnection(Sucursal sucursal) throws SQLException {
        validarSucursal(sucursal);

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException(
                    "No se encontró el driver JDBC de SQL Server. Agrega la dependencia mssql-jdbc en Maven.",
                    ex
            );
        }

        return DriverManager.getConnection(buildConnectionString(sucursal));
    }

    private String buildConnectionString(Sucursal sucursal) {
        SqlServerEndpoint endpoint = parseDataSource(sucursal.getDataSource());

        StringBuilder sb = new StringBuilder("jdbc:sqlserver://");
        sb.append(endpoint.host);

        if (endpoint.instanceName != null && !endpoint.instanceName.isBlank()) {
            sb.append("\\").append(endpoint.instanceName);
        }

        if (endpoint.port != null) {
            sb.append(":").append(endpoint.port);
        }

        sb.append(";databaseName=").append(escapeProperty(sucursal.getCatalog()));
        sb.append(";user=").append(escapeProperty(sucursal.getUserId()));
        sb.append(";password=").append(escapeProperty(sucursal.getPassword()));
        sb.append(";encrypt=false");
        sb.append(";trustServerCertificate=true");
        sb.append(";loginTimeout=5");
        sb.append(";socketTimeout=10000");
        sb.append(";applicationName=Dalmendra");

        return sb.toString();
    }

    private void validarSucursal(Sucursal sucursal) {
        if (sucursal == null) {
            throw new IllegalArgumentException("La sucursal es obligatoria.");
        }
        if (isBlank(sucursal.getDataSource())) {
            throw new IllegalArgumentException("Debes capturar el servidor SQL Server.");
        }
        if (isBlank(sucursal.getCatalog())) {
            throw new IllegalArgumentException("Debes capturar la base de datos.");
        }
        if (isBlank(sucursal.getUserId())) {
            throw new IllegalArgumentException("Debes capturar el usuario.");
        }
        if (isBlank(sucursal.getPassword())) {
            throw new IllegalArgumentException("Debes capturar la contraseña.");
        }
    }

    private SqlServerEndpoint parseDataSource(String dataSource) {
        String value = dataSource.trim();

        String host = value;
        String instanceName = null;
        Integer port = null;

        if (value.contains("\\")) {
            String[] parts = value.split("\\\\", 2);
            host = parts[0].trim();

            String remainder = parts.length > 1 ? parts[1].trim() : "";

            if (remainder.contains(",")) {
                String[] instancePort = remainder.split(",", 2);
                instanceName = emptyToNull(instancePort[0]);
                port = parsePort(instancePort[1]);
            } else if (remainder.contains(":")) {
                String[] instancePort = remainder.split(":", 2);
                instanceName = emptyToNull(instancePort[0]);
                port = parsePort(instancePort[1]);
            } else {
                instanceName = emptyToNull(remainder);
            }
        } else if (value.contains(",")) {
            String[] parts = value.split(",", 2);
            host = parts[0].trim();
            port = parsePort(parts[1]);
        } else if (value.contains(":")) {
            String[] parts = value.split(":", 2);
            host = parts[0].trim();
            port = parsePort(parts[1]);
        }

        if (isBlank(host)) {
            throw new IllegalArgumentException("El servidor SQL Server es inválido.");
        }

        return new SqlServerEndpoint(host, instanceName, port);
    }

    private Integer parsePort(String value) {
        String portText = value == null ? "" : value.trim();

        if (portText.isEmpty()) {
            return null;
        }

        try {
            int port = Integer.parseInt(portText);

            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("El puerto SQL Server está fuera de rango: " + portText);
            }

            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El puerto SQL Server es inválido: " + portText);
        }
    }

    private String escapeProperty(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();

        if (trimmed.contains(";") || trimmed.contains("=") || trimmed.contains("[" ) || trimmed.contains("]") || trimmed.contains("{") || trimmed.contains("}")) {
            return "{" + trimmed.replace("}", "}}") + "}";
        }

        return trimmed;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class SqlServerEndpoint {
        private final String host;
        private final String instanceName;
        private final Integer port;

        private SqlServerEndpoint(String host, String instanceName, Integer port) {
            this.host = host;
            this.instanceName = instanceName;
            this.port = port;
        }
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