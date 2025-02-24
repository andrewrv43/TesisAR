# Cálculo de Velocidad y Detección de Excesos (Android)

Proyecto de Android Studio desarrollado en **Kotlin**, compatible con dispositivos con **Android 7.0 (Nougat)** o superior.  
El objetivo principal de esta aplicación es **calcular la velocidad de movimiento**, detectar **excesos de velocidad** y asociar la posición geográfica.

---

## Tabla de Contenidos
1. [Descripción](#descripción)
2. [Características](#características)
3. [Requisitos del Sistema](#requisitos-del-sistema)
4. [Configuración e Instalación](#configuración-e-instalación)
5. [Uso](#uso)
6. [Contribución](#contribución)

---

## Descripción
Esta aplicación realiza la medición de la **velocidad en tiempo real** a partir de datos de localización GPS.  
Utiliza una **SRTree** (Spatial R-Tree) para ubicar al usuario en la vía correspondiente y así poder identificar la calle o carretera en la que se encuentra.  
- Cuando la velocidad supera el límite establecido, se **registra el evento** (exceso de velocidad).  
- Permite mostrar el **historial de excesos de velocidad**, así como la calle o zona específica donde ocurrieron.

---

## Características
- **Cálculo de velocidad** en tiempo real usando proveedores de localización (GPS).  
- **Detección de exceso de velocidad** y registro automático del evento.  
- Utilización de **SRTree** para determinar la calle o área correspondiente según la ubicación.  
- **Compatibilidad** con Android 7.0 Nougat (API 24) o superior.  
- **Interfaz amigable** e intuitiva para visualizar la velocidad y alertas de exceso.  

---

## Requisitos del Sistema
- **Android Studio** (versión recomendada: Electric Eel o superior).  
- **Gradle** (versión que provee Android Studio por defecto, generalmente la última estable).  
- **Kotlin** (compatibilidad completa con Android Studio por defecto).  
- **Dispositivo o emulador con Android 7.0 (Nougat) o superior**.  
- Conexión a Internet o **GPS habilitado** (para obtener datos de ubicación).  

---

## Configuración e Instalación

1. **Clona este repositorio** en tu equipo local:
   ```bash
   git clone https://github.com/andrewrv43/TesisAR.git
   ```
2. Abre el proyecto en Android Studio:
   - Selecciona *File > Open...* y navega hasta la carpeta donde clonaste el repositorio.
3. Sincroniza el proyecto con Gradle:
   - Android Studio solicitará sincronizar el proyecto. Acepta para descargar las dependencias necesarias.
4. Configura las credenciales de mapas / servicios de localización (opcional):
   - De ser necesario, agrega tus claves de API (por ejemplo, Google Maps) en el archivo adecuado si vas a usar tu API de mapa.
5. Ejecuta la app:
   - Conecta un dispositivo físico o inicia un emulador con Android 7.0 o superior.
   - Haz clic en *Run* o presiona **Shift + F10**.

---

## Uso

1. **Permisos de ubicación**  
   - Asegúrate de conceder los permisos de ubicación en tu dispositivo. La aplicación solicitará permisos cuando inicie.  
2. **Medición de Velocidad**  
   - Al iniciar la aplicación, comenzará a obtener la ubicación y calcular la velocidad automáticamente.  
3. **Visualización del Historial**  
   - Puedes acceder a la sección de *Perfil* para ver los registros de excesos de velocidad contribuidos por el usuario.

---

## Contribución
¡Las contribuciones son bienvenidas!  
Para contribuir:
1. Haz un **fork** del repositorio.  
2. Crea una nueva rama para tu funcionalidad o corrección (`git checkout -b mi-rama`).  
3. Realiza tus cambios y haz commits descriptivos (`git commit -m "Agrego nueva función de notificaciones"`).  
4. Envía tus cambios a tu repositorio fork (`git push origin mi-rama`).  
5. Crea un **Pull Request** en este repositorio explicando detalladamente tus cambios.  

---

**¡Gracias por usar la aplicación y contribuir a su desarrollo!** 
