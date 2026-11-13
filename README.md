# TLOD Remastered para Android

Proyecto privado de RB – TLOD Remastered. La primera compilación de GitHub Actions queda pendiente de incorporar el código fuente de la base 0.30 y configurar su contenido y firma.

## Compilación

En **Actions → Compilar TLOD para Android → Run workflow**, el flujo prepara Java 25, Android SDK 35 y R8. Compila el motor y empaqueta la APK. Verifica el paquete y limita su tamaño a 100 MB.

- `DRAGOON_CONTENT_URL` es la variable de Actions con la URL HTTPS desde la que el teléfono puede descargar el paquete descrito en `android_app/content-download-0.30.json`.
- `DRAGOON_SIGNING_KEY_BASE64` es el secreto de Actions con el almacén de firma interno. La misma firma permite actualizar instalaciones previas.
- La actividad de descarga inicia la instalación en el primer arranque y ofrece reintentar si falla.

El ZIP con los datos PAL y el almacén de firma se gestionan fuera del árbol de código.
