# TLOD Remastered para Android

Versión 0.30 del proyecto RB – TLOD Remastered. El motor se basa en Severed Chains y conserva su licencia en [source/LICENSE](source/LICENSE).

GitHub Actions compila el motor y los componentes Android con Java 25, Android SDK 35 y R8. El flujo se ejecuta con cambios en `main` o manualmente desde Actions.

El juego descarga el contenido automáticamente desde la Release `content-0.30` de este repositorio. El instalador verifica el tamaño y SHA-256 de `android_app/content-download-0.30.json` antes de activar los archivos.

Para publicar la APK, Actions utiliza `DRAGOON_SIGNING_KEY_BASE64` y comprueba que el certificado coincide con el de la 0.29. Sin el secreto, Actions solo verifica el empaquetado. La APK firmada y su checksum SHA-256 aparecen en los artefactos de la ejecución de Actions. El resumen registra el tamaño del archivo.

El contenido de ejecución y los datos extraídos del CD PAL se preparan aparte. El código del descargador inicia la instalación automáticamente en el primer arranque. La clave de firma se guarda fuera del historial de Git.
