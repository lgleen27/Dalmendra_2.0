# Dalmendra 2.0 - Sistema de Monitor de Inventarios y Existencias

**Dalmendra 2.0** es un sistema de escritorio en **Java 21** y **JavaFX** diseñado para el monitoreo, consolidación y visualización en tiempo real de existencias de insumos e inventario para múltiples sucursales gastronómicas (compatible con bases de datos POS de Microsoft SQL Server como Soft Restaurant).

---

## 🚀 Características Principales

- **Conexión Multi-Sucursal:** Monitoreo en vivo de inventarios conectándose a múltiples instancias remotas de SQL Server.
- **Cálculo de Existencias Reales:** Descuenta al vuelo las ventas registradas en cuentas abiertas / temporales no cerradas.
- **Categorización Inteligente:** Agrupación dinámica por prefijos / palabras clave de insumos.
- **Semáforos de Stock:** Indicadores visuales automáticos de stock mínimo (crítico) y stock deseado.
- **Tablero Visual Panorámico:** Diseñado a pantalla completa con distribución en columnas y colores de fondo por sucursal.
- **Integración con API Externa:** Generación de reportes en JSON y envío automático hacia plataformas web (ej. Laravel).

---

## 📚 Documentación Técnica Completa

Para una explicación exhaustiva de la arquitectura del software, diagrama entidad-relación, flujo de sincronización y catálogo de servicios, consulta el archivo:

👉 **[DOCUMENTACION.md](DOCUMENTACION.md)**

---

## 🛠️ Requisitos de Entorno

- **Java JDK:** Versión 21 o superior.
- **Gestor de Dependencias:** Apache Maven 3.8+.
- **Base de Datos Local:** MySQL 8.x en `localhost:3306` (usuario `root`, sin contraseña por defecto).
- **Acceso de Red:** Conectividad hacia los servidores SQL Server de las sucursales.

---

## 💻 Comandos de Ejecución y Compilación

### Ejecutar en modo desarrollo:
```bash
mvn clean javafx:run
```

### Compilar y generar Fat-JAR ejecutable:
```bash
mvn clean package
```
El ejecutable resultante estará en `target/dalmendra-app.jar`.