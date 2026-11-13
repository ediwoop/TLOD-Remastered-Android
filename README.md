# TLOD Remastered para Android

Versión 0.30 del proyecto RB – TLOD Remastered. El motor se basa en Severed Chains y conserva su licencia en [source/LICENSE](source/LICENSE).

GitHub Actions compila el motor y los componentes Android con Java 25, Android SDK 35 y R8. El flujo se ejecuta con cambios en `main` o manualmente desde Actions.

Para generar una APK instalable, configura `DRAGOON_CONTENT_URL` con una URL HTTPS accesible al teléfono y `DRAGOON_SIGNING_KEY_BASE64` como secreto de Actions. El paquete servido debe coincidir con el tamaño y SHA-256 de `android_app/content-download-0.30.json`. El límite de la APK es de 100 MB.

El contenido de ejecución y los datos extraídos del CD PAL se preparan aparte. El código del descargador inicia la instalación automáticamente en el primer arranque. La clave de firma se guarda fuera del historial de Git.
