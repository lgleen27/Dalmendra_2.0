# 📦 Guía de Generación del Instalador de Windows (.msi) - Dalmendra 2.0

Esta guía describe el procedimiento exacto paso a paso para generar el **instalador oficial de Windows (`Dalmendra-2.0.1.msi`)**.

---

## 🛠️ Requisitos Previos

Para que `jpackage` pueda compilar un instalador `.msi` en Windows, se requieren dos cosas:

1. **JDK de NetBeans (Ya instalado):**
   * Ruta: `C:\Program Files\Apache NetBeans\jdk\bin\jpackage.exe`

2. **WiX Toolset v3.11 (Obligatorio para archivos .msi):**
   * Es la herramienta oficial que utiliza Windows y Java para empaquetar archivos `.msi`.
   * **¿Cómo instalarlo?**
     * **Opción A (Rápida por consola):** Abre una terminal (CMD o PowerShell) y escribe:
       ```cmd
       winget install WiX.Toolset
       ```
     * **Opción B (Descarga manual):** Descarga e instala el archivo `wix311.exe` desde su [GitHub Oficial](https://github.com/wixtoolset/wix3/releases/tag/wix3112rtm).

---

## 🚀 Proceso Paso a Paso para Generar el Instalador

### PASO 1: Compilar el proyecto en NetBeans
1. Abre **Apache NetBeans**.
2. En la lista de proyectos a la izquierda, da **clic derecho** sobre `Dalmendra_2.0`.
3. Selecciona **Clean and Build** (o presiona `Shift + F11`).
4. Espera a que la ventana de salida indique **`BUILD SUCCESS`**.

> **¿Qué hace este paso?** Genera en la carpeta `target\` el archivo `dalmendra-app.jar` que contiene el código actualizado y todas sus dependencias.

---

### PASO 2: Preparar la carpeta intermedia (`jpackage-input`)
1. Ve a la carpeta raíz de tu proyecto (`C:\Users\Fredy\Downloads\Dalmendra_2.0`).
2. Si no existe, crea una carpeta llamada **`jpackage-input`**.
3. Entra a la carpeta **`target\`** y copia los archivos `.jar`:
   * `dalmendra-app.jar`
   * `original-dalmendra-app.jar`
   Pégalos dentro de `jpackage-input\`.
4. Copia la carpeta **`assets\`** de la raíz del proyecto y pégala dentro de `jpackage-input\`.

**Estructura esperada:**
```text
Dalmendra_2.0/
└── jpackage-input/
    ├── dalmendra-app.jar
    ├── original-dalmendra-app.jar
    └── assets/
        ├── logo.ico
        └── seguridad_legacy.properties
```

---

### PASO 3: Generar el instalador `.msi` con `jpackage`
1. Abre el **CMD** (Símbolo del sistema) o **PowerShell** en Windows.
2. Navega a la raíz del proyecto:
   ```cmd
   cd C:\Users\Fredy\Downloads\Dalmendra_2.0
   ```
3. Ejecuta el comando para construir el paquete instalador:

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

---

### 🧠 Parámetros especiales del instalador incluidos en este comando:
* **`--type msi`**: Genera el archivo instalable estándar de Windows.
* **`--win-shortcut`**: Le dice al instalador que cree un acceso directo en el **Escritorio** de Windows.
* **`--win-menu`**: Le dice al instalador que agregue Dalmendra al **Menú Inicio**.
* **`--win-dir-chooser`**: Permite al usuario elegir en qué carpeta instalar el programa (por defecto en `Archivos de programa / Program Files`).
* **`--runtime-image "C:\Program Files\Apache NetBeans\jdk"`**: Empaqueta el motor de Java directamente desde NetBeans para que el instalador funcione en cualquier máquina sin requerir Java previo y respetando las configuraciones de seguridad legacy de SQL Server.

---

### PASO 4: Tu Instalador Listo
Al finalizar el comando, dentro de la carpeta:
📁 `dist\`

Encontrarás tu instalador:
👉 **`Dalmendra-2.0.1.msi`**

Este es el archivo instalador final que puedes llevar a la sucursal, ejecutar con doble clic y darle *Siguiente, Siguiente, Instalar*.
