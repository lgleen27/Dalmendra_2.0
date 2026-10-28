# Manual de Arquitectura y Documentación Técnica - Dalmendra 2.0

## 1. Visión General del Proyecto

**Dalmendra 2.0** es una aplicación de escritorio desarrollada con **Java 21** y **JavaFX**. Su objetivo primordial es servir como un **monitor de existencias en tiempo real multisucursal** para negocios gastronómicos / puntos de venta (diseñado para integrarse nativamente con el motor de base de datos de sistemas POS como Soft Restaurant).

### 1.1. El Problema que Resuelve
En un negocio gastronómico con múltiples sucursales físicas:
- Las existencias en el sistema de almacén central solo se descuentan de forma formal una vez cerrado el turno o cheque.
- Durante el servicio activo (en tiempo real), los comensales realizan pedidos que se registran en "cheques temporales / abiertos".
- Dalmendra extrae la existencia física del almacén y le **resta en vivo los insumos comprometidos en ventas abiertas no canceladas**:
  $$\text{Existencia Neta} = \text{Existencia en Almacén (idalmacen = 3)} - \sum (\text{Cantidad Vendida} \times \text{Receta Insumo})$$
- Consolida esta información en una base de datos local rápida (MySQL), clasifica los insumos por categorías mediante palabras clave, colorea advertencias de stock (mínimo y deseado con precisión decimal) y transmite los datos en formato JSON hacia un backend web (Laravel).
- Dispone de un **Tablero Panorámico Adaptativo** de alto impacto para pantallas de cocina/TV y una herramienta visual de **Auditoría de Entradas del Día** para conciliar compras y movimientos registrados en Soft Restaurant.

---

## 2. Tecnologías y Dependencias

- **Lenguaje:** Java 21 LTS (soporte para Records, Switch Expressions, HttpClient nativo).
- **Framework de Interfaz Gráfica:** JavaFX 21.0.2 (`javafx-controls`, `javafx-fxml`).
- **Base de Datos Local:** MySQL 8.x (mediante `mysql-connector-j` 8.2.0).
- **Base de Datos Externa (Sucursales):** Microsoft SQL Server 2012+ (mediante `mssql-jdbc` 13.4.0.jre11).
- **Serialización JSON:** FasterXML Jackson 2.17.0 (`jackson-databind`, `jackson-datatype-jsr310`).
- **Empaquetador y Build:** Apache Maven, Plugin Shade para Fat-JAR y JPackage + WiX Toolset v3.11 para instalador MSI de Windows.

---

## 3. Arquitectura del Software

El proyecto implementa una arquitectura por capas desacoplada:

```
src/main/java/com/frexal/dalmendra/app/
│
├── DalmendraApplication.java         # Entrada JavaFX, validación de conexión inicial
├── MainLauncher.java                 # Punto de entrada estático para evitar issues de JavaFX
├── module-info.java                  # Módulos Java 9+, permisos FXML y exportación de paquetes
│
├── config/                           # Configuración de conexiones y base de datos local
│   ├── DatabaseConfig.java           # Constantes de conexión local MySQL
│   ├── MySqlConnectionFactory.java   # Conexiones JDBC al servidor y a la BD
│   └── MySqlSchemaInitializer.java   # Inicialización y migración DDL automática
│
├── model/                            # Entidades POJO de dominio
│   ├── Sucursal.java
│   ├── Categoria.java
│   ├── Existencia.java
│   ├── ExistenciaStock.java
│   ├── OrdenExistencia.java
│   ├── Configuracion.java
│   ├── EntradasDelDia.java           # Métricas de conteo diario (compras y ajustes)
│   └── AuditoriaEntradaItem.java     # Renglón de detalle de movimientos de entrada
│
├── repository/                       # Capa de Persistencia (JDBC puro hacia MySQL)
│   ├── SucursalRepository.java
│   ├── CategoriaRepository.java
│   ├── ExistenciaRepository.java
│   ├── ExistenciaStockRepository.java
│   ├── OrdenExistenciaRepository.java
│   └── ConfiguracionRepository.java
│
├── service/                          # Lógica de Negocio y Comunicación
│   ├── AppState.java                 # Estado global reactivo en memoria
│   ├── SucursalService.java          # Gestión de sucursales activas
│   ├── CategoriaService.java         # Gestión de categorías
│   ├── OrdenExistenciaService.java   # Gestión de orden de productos
│   ├── ConfiguracionService.java     # Preferencias y tokens
│   ├── SqlServerSucursalClient.java  # Cliente JDBC para consultas remotas a SQL Server
│   ├── InventarioSyncService.java    # Orquestador del cálculo y sincronización de existencias
│   ├── SyncSchedulerService.java     # Tareas periódicas en segundo plano
│   ├── ReporteCategoriasDataService.java # Agrupación y semaforización de datos
│   ├── ReporteCategoriasJsonService.java # Generación y serialización de JSON
│   └── ReporteCategoriasApiClient.java   # Cliente HTTP REST hacia Laravel
│
├── dto/reporte/                      # Objetos de Transferencia de Datos para la API
│   ├── ReporteCategoriasJsonDto.java
│   ├── SucursalReporteJsonDto.java   # Incluye entradasComprasHoy, entradasMovimientosHoy, entradasTotalHoy
│   ├── CategoriaReporteJsonDto.java
│   └── ProductoReporteJsonDto.java
│
└── ui/                               # Capa de Presentación (Controladores FXML y Vistas)
    ├── main/
    │   ├── MainController.java       # Tablero principal y despachador de vistas
    │   ├── FilaListadoTriple.java    # Modelo auxiliar para vista de 3 columnas
    │   └── tablero/                  # Motor responsivo del tablero de categorías
    │       ├── TableroCategoriasView.java    # Componente visual dinámico con tablas y tarjetas
    │       └── TableroLayoutCalculator.java  # Cálculo binario de columnas, tipografía y altos
    ├── categorias/
    │   └── CategoriasController.java # Gestión de categorías y orden
    ├── existencias/
    │   └── ExistenciasController.java# Monitoreo, orden y stock mínimo/deseado
    ├── sucursal/
    │   └── SucursalesController.java # Configuración de servidores y credenciales SQL Server
    ├── config/
    │   └── ConfiguracionController.java # Configuración de tiempos, API REST y Kiosco
    └── entradas/
        └── AuditoriaEntradasController.java # Ventana de auditoría y conciliación de entradas
```

---

## 4. Diagrama Entidad-Relación (MySQL Local)

```mermaid
erDiagram
    SUCURSALES ||--o{ EXISTENCIAS : "posee"
    CATEGORIAS ||--o{ EXISTENCIAS : "clasifica (opcional)"
    SUCURSALES ||--o{ EXISTENCIAS_STOCK : "configura umbrales"
    SUCURSALES ||--o{ ORDEN_EXISTENCIAS : "define orden"

    SUCURSALES {
        bigint id PK
        varchar nombre_sucursal
        varchar data_source
        varchar catalog
        varchar user_id
        varchar password
        int orden
        datetime fecha_hora_actualizacion
        varchar color
        tinyint activa
    }

    CATEGORIAS {
        bigint id PK
        varchar descripcion
        varchar palabra_clave
        int orden
        tinyint estado
        int stock_minimo
        int stock_deseado
    }

    EXISTENCIAS {
        bigint id PK
        bigint sucursal_id FK
        varchar codigo
        varchar descripcion
        bigint categoria_id FK
        decimal existencia
        int orden
        datetime fecha_actualizacion
    }

    EXISTENCIAS_STOCK {
        bigint id PK
        bigint sucursal_id FK
        varchar codigo
        int stock_minimo
        int stock_deseado
        datetime fecha_actualizacion
    }

    ORDEN_EXISTENCIAS {
        bigint id PK
        bigint sucursal_id FK
        varchar codigo
        int orden
    }

    CONFIGURACION {
        bigint id PK
        varchar descripcion UK
        varchar valor
    }

    SYNC_LOG {
        bigint id PK
        bigint sucursal_id
        datetime fecha
        varchar tipo
        text mensaje
    }
```

---

## 5. Explicación Detallada de Procesos Clave

### 5.1. Proceso de Sincronización Remota (`InventarioSyncService`)

1. **Lectura de sucursales activas:** Itera sobre las sucursales con `activa = 1` y contraseña válida.
2. **Conexión a SQL Server:** A través de `SqlServerSucursalClient`, conecta a cada sucursal física.
3. **Consulta de Insumos:**
   ```sql
   SELECT i.idinsumo AS CODIGO, i.descripcion AS DESCRIPCION, a.existencia AS EXISTENCIA
   FROM insumos AS i
   INNER JOIN acumuladoinsumos AS a ON i.idinsumo = a.idinsumo
   WHERE a.idalmacen = 3
   ```
4. **Consulta de Ventas en Vivo:**
   ```sql
   SELECT c.idinsumo AS Insumo,
          SUM(t.cantidad * c.cantidad) AS Existencias,
          (SELECT i.descripcion FROM insumos AS i WHERE c.idinsumo = i.idinsumo) AS Descripcion
   FROM tempcheqdet AS t
   INNER JOIN costos AS c ON t.idproducto = c.idproducto
   INNER JOIN tempcheques AS f ON t.foliodet = f.folio
   WHERE f.cancelado = 0
   GROUP BY c.idinsumo
   ```
5. **Cálculo de Existencia:** Resta las existencias vendidas de la existencia inicial. Si un insumo se vendió sin existencia previa, el valor resultará negativo.
6. **Persistencia Local:** Se eliminan los registros antiguos de esa sucursal en `existencias` y se insertan los nuevos con la fecha/hora actual y el orden predeterminado en `orden_existencias`.

### 5.2. Categorización Dinámica y Limpieza Visual

- **Asociación Dinámica:** Dalmendra no depende de claves foráneas fijas para categorizar los productos. En `ReporteCategoriasDataService` y en el tablero responsivo, evalúa:
  ```java
  descripcionNormalizada.startsWith(palabraClaveNormalizada)
  ```
  Si la palabra clave es `"PAN"`, insumos como `"PAN BLANCO"`, `"PAN HAMBURGUESA"` o `"PAN BRIOCHE"` entran automáticamente a dicha categoría.
- **Limpieza de Descripción:** En la pantalla del tablero, se retira la palabra clave al inicio para que el texto sea más limpio y legible (ej. `"BLANCO"`, `"HAMBURGUESA"`).

### 5.3. Semáforo de Control de Stock con Precisión Decimal

Se compara la existencia actual con los umbrales configurados en `existencias_stock` utilizando comparación en decimales (`BigDecimal.doubleValue()`):
| Condición | Estado | Color Visual en Tablero |
|---|---|---|
| `existencia <= stock_minimo` | `MINIMO_CRITICO` | Rojo pastel (`#ffb3b3`) |
| `existencia <= stock_deseado` | `BAJO_DESEADO` | Amarillo pastel (`#fff3a3`) |
| `existencia > stock_deseado` | `NORMAL` | Fondo blanco |
| Sin umbrales configurados | `SIN_CONFIGURAR` | Fondo blanco |

### 5.4. Motor de Tablero Panorámico Responsivo (`TableroCategoriasView`)

Para optimizar la visualización en pantallas de televisión en cocinas:
1. **Cálculo de Espacio (`TableroLayoutCalculator`):** Algoritmo de búsqueda binaria que analiza el ancho y alto disponible en píxeles. Ajusta dinámicamente entre 1 y 6 columnas y fuentes entre 14px y 40px, garantizando legibilidad en nombres y celdas numéricas fijas.
2. **Diseño de Tablas:** Cada columna se estructura como una tabla blanca con marco exterior (`#7d8f94`), títulos de categoría en franja oscura azulada (`#30505b`) con letra blanca, y renglones individuales con líneas divisorias.
3. **Alerta de Capacidad:** Si los artículos superan la capacidad física de la pantalla, se despliega una barra flotante roja inferior indicando: `"¡Ojo! Faltan N artículos. Ajuste el tamaño o aumente pantallas"`.

### 5.5. Auditoría de Entradas del Día (Kardex Soft Restaurant)

Dalmendra monitorea las entradas de inventario del día directamente desde la tabla `movsinv` (Kardex de Soft Restaurant):
- **Criterio de Entrada:** `movto = 0` y fecha del día (`CAST(fecha AS DATE) = CAST(GETDATE() AS DATE)`).
- **Desglose de Origen:**
  - **Compras:** `idcompra > 0` (Entradas por órdenes de compra).
  - **Movimientos:** `idcompra = 0 OR idcompra IS NULL` (Ajustes y recepciones manuales).
- **Filtro de Producción:** Para el conteo principal se enfoca en `idalmacen = 3` (Producción/Cocina).
- **Feature Flag en Código:**
  En `MainController.java`, la constante `MODULO_ENTRADAS_HABILITADO` controla:
  - `false`: Apaga las consultas a SQL Server, oculta el recuadro verde y omite los campos del JSON (mediante `JsonInclude.Include.NON_NULL`).
  - `true`: Activa la consulta periódica, muestra el recuadro verde inferior derecho y transmite `entradasComprasHoy`, `entradasMovimientosHoy` y `entradasTotalHoy` a la API web.
- **Ventana de Auditoría:** Menú *Reportes > Auditoría de Entradas...* permite inspeccionar cada renglón recibido hoy, mostrando hora, insumo, cantidad, folio y resaltando en amarillo los artículos que hayan ingresado a otros almacenes (`idalmacen <> 3`).

### 5.6. Modos de Visualización Kiosco y División de Categorías

- **Modo Kiosco (`ModoKiosco`):** Al activarse en la configuración (`valor = '1'`), la ventana principal entra en modo de pantalla completa real (`stage.setFullScreen(true)`), ocultando la barra de título de Windows y la barra de tareas para exhibición en TVs.
- **División de Categorías (`DividirCategoriasTablero`):**
  - `'0'`: Mantiene cada categoría como un bloque indivisible en su columna.
  - `'1'`: Permite que categorías extensas continúen en la siguiente columna con el encabezado `"CATEGORÍA (cont.)"`.

### 5.7. Sincronización Automática y Pausas de Interfaz

- **Programación:** `SyncSchedulerService` utiliza un `ScheduledExecutorService` con intervalo configurable en minutos.
- **Protección de Edición:** Cuando el usuario abre una ventana modal (Sucursales, Categorías, Configuración, Existencias o Auditoría de Entradas), el método `configurarPausaSincronizacionEnVentana(Stage stage)` activa la bandera `appState.setBanActualizacion(false)`. Esto evita que las existencias se sincronicen en segundo plano mientras el usuario realiza modificaciones manuales. Al cerrarse la ventana, se reactiva la bandera.

### 5.8. Transmisión HTTP a Backend Externo (Laravel)

Tras cada sincronización correcta:
1. `ReporteCategoriasDataService` crea el DTO completo con las existencias y métricas de entradas (si están activas).
2. `ReporteCategoriasJsonService` guarda el archivo en `%APPDATA%\Dalmendra\reportes\reporte_categorias.json`.
3. `ReporteCategoriasApiClient` lee las credenciales `ApiUrlReporteCategorias` y `ApiTokenReporteCategorias` de la tabla `configuracion` y realiza una petición `POST` con `Bearer Token`.

---

## 6. Puesta en Marcha y Configuración

### 6.1. Requisitos Previos
1. **Java JDK 21** o superior (el JDK incluido en NetBeans se encuentra en `C:\Program Files\Apache NetBeans\jdk`).
2. **Apache Maven 3.8+**.
3. **Servidor MySQL local** corriendo en el puerto 3306 (por ejemplo XAMPP, WampServer o servicio nativo MySQL) con usuario `root` y contraseña vacía (por defecto en `DatabaseConfig.java`).

### 6.2. Ejecución en Desarrollo
Para ejecutar la aplicación localmente:
```bash
mvn clean javafx:run
```
*(O desde NetBeans con el botón "Run Project" / F6).*

### 6.3. Generación del Paquete Shaded (Fat-JAR)
```bash
mvn clean package
```
*(O en NetBeans mediante "Clean and Build" / Shift+F11).*
El archivo JAR ejecutable con todas las dependencias empaquetadas se genera en:
`target/dalmendra-app.jar`.

---

## 7. Glosario de Parámetros de Configuración (`configuracion`)

- **`IdDbSelect`:** ID de la sucursal seleccionada por defecto.
- **`TimeSyncSucursal`:** Intervalo en minutos para la sincronización periódica de inventarios.
- **`TimeChangeSucursal`:** Tiempo en segundos de rotación automática entre sucursales (para visualización estilo Kiosco).
- **`FirstReport`:** Reporte inicial al abrir la aplicación (`PorCategorias`, `ListadoConCodigo`, `ListadoSinCodigo`).
- **`ApiUrlReporteCategorias`:** URL del endpoint REST donde se reporta el JSON de existencias.
- **`ApiTokenReporteCategorias`:** Token Bearer de autenticación para la API REST.
- **`ModoKiosco`:** Activa la pantalla completa real para TVs (`1` = activo, `0` = inactivo).
- **`DividirCategoriasTablero`:** Permite dividir categorías largas entre columnas (`1` = permitido, `0` = no dividir).

---

## 8. Empaquetado del Instalador Oficial de Windows (.msi)

Para compilar el instalador `.msi` autocontenido (con Java embebido y accesos directos):

1. **Requisito:** Instalar **WiX Toolset v3.11** en Windows (`winget install WiX.Toolset`).
2. **Preparar archivos:** Copiar `target\*.jar` y la carpeta `assets\` dentro de `jpackage-input\`.
3. **Ejecutar `jpackage`:**
   ```cmd
   "C:\Program Files\Apache NetBeans\jdk\bin\jpackage.exe" ^
     --type msi ^
     --name "Dalmendra" ^
     --app-version "2.0.1" ^
     --description "Sistema de Gestión y Operaciones" ^
     --vendor "Frexal Soluciones Digitales" ^
     --input jpackage-input ^
     --main-jar dalmendra-app.jar ^
     --main-class com.frexal.dalmendra.app.MainLauncher ^
     --icon jpackage-input\assets\logo.ico ^
     --java-options "-Djava.security.properties=$APPDIR\assets\seguridad_legacy.properties" ^
     --runtime-image "C:\Program Files\Apache NetBeans\jdk" ^
     --dest dist ^
     --win-menu ^
     --win-shortcut ^
     --win-dir-chooser
   ```
4. El instalador generado estará disponible en `dist/Dalmendra-2.0.1.msi`.
