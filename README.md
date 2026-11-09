# Liquid Player

Reproductor de música offline, diseño liquid glass. Android 8.0+ (minSdk 26), optimizado para Galaxy S25 Ultra (Android 15).

## Cómo generar el APK

### Opción A – Desde el móvil (sin PC), con GitHub
1. Crea un repositorio en github.com y sube **el contenido** de esta carpeta (no el zip).
2. Pestaña **Actions → Build APK → Run workflow**.
3. Al terminar (unos 5-8 min), entra en la ejecución y descarga el artefacto **LiquidPlayer-APK** (zip con `app-debug.apk`).
4. Instala el APK (permite "instalar apps desconocidas" para tu navegador/gestor de archivos).

### Opción B – Android Studio
1. File → Open → carpeta `LiquidPlayer`. Espera a que sincronice Gradle.
2. Build → Build Bundle(s) / APK(s) → Build APK(s).
3. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Funciones
- Carrusel horizontal de carátulas: desliza para cambiar de canción.
- Fondo del reproductor con la carátula desenfocada, intensidad ajustable (Personalizar).
- Botones del reproductor activables/desactivables y tamaño de carátula ajustable.
- Ecualizador de 10 bandas (31 Hz – 16 kHz, ±12 dB) con presets.
- Bit-perfect por USB (Android 14+ y DAC/auriculares USB compatibles).
- 6 skins: Cristal, Vinilo, Neón, Aurora, Minimal, Ámbar.
- Reproducción en segundo plano con notificación y controles.
