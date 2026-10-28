# Dalmendra 2.0 - Sistema de Monitor de Inventarios y Existencias

**Dalmendra 2.0** es un sistema de escritorio en **Java 21** y **JavaFX** diseñado para el monitoreo, consolidación y visualización en tiempo real de existencias de insumos e inventario para múltiples sucursales gastronómicas (compatible nativamente con bases de datos POS de Microsoft SQL Server como Soft Restaurant).

---

## 🚀 Características Principales

- **Conexión Multi-Sucursal:** Monitoreo en vivo de inventarios conectándose a múltiples instancias remotas de SQL Server.
- **Cálculo de Existencias Reales:** Descuenta al vuelo las ventas registradas en cuentas abiertas / temporales no cerradas de Soft Restaurant.
- **Tablero Panorámico Adaptativo:** Motor inteligente que calcula dinámicamente columnas (1 a 6) y tamaño tipográfico óptimo para pantallas de cocina y TV.
- **Semáforos de Stock Decimal:** Indicadores visuales exactos en decimales para stock mínimo (crítico) y stock deseado.
- **Auditoría de Entradas del Día:** Herramienta visual interactiva para consultar cada artículo recibido hoy en el Kardex de Soft Restaurant (`movsinv`), desglosando Compras vs Ajustes y Almacén de Producción (3) vs Otros Almacenes.
- **Modo Kiosco y Continuidad:** Visualización en pantalla completa real sin barras de Windows y opción para dividir o mantener compactas las categorías extensas.
- **Integración con API Externa:** Generación de reportes en JSON y envío automático hacia plataformas web (ej. Laravel).

---

## 📚 Documentación Técnica Completa

Para una explicación exhaustiva de la arquitectura del software, diagrama entidad-relación, flujo de sincronización y catálogo de servicios, consulta el archivo:

👉 **[DOCUMENTACION.md](DOCUMENTACION.md)**

---

## 🛠️ Requisitos de Entorno

- **Java JDK:** Versión 21 o superior (incluido en NetBeans en `C:\Program Files\Apache NetBeans\jdk`).
- **Gestor de Dependencias:** Apache Maven 3.8+.
- **Base de Datos Local:** MySQL 8.x en `localhost:3306` (usuario `root`, sin contraseña por defecto).
- **Acceso de Red:** Conectividad hacia los servidores SQL Server de las sucursales.
- **Para Instalador (.msi):** WiX Toolset v3.11 (`winget install WiX.Toolset`).

---

## 💻 Comandos de Ejecución y Compilación

### Ejecutar en modo desarrollo:
```bash
mvn clean javafx:run
```
*(O ejecutar directamente desde NetBeans con F6).*

### Compilar y generar Fat-JAR:
```bash
mvn clean package
```
*(O en NetBeans mediante "Clean and Build" / Shift+F11).*
El archivo ejecutable estará en `target/dalmendra-app.jar`.

### Generar Instalador de Windows (.msi):
Consulta la guía completa en **[INSTRUCCIONES_EMPAQUETADO.md](INSTRUCCIONES_EMPAQUETADO.md)**.
