# Modos Original y Remastered

Original es la referencia del juego base. Los cambios de arte, geometría visual,
cámara, proyección, interfaz, menús, animaciones y efectos de Remastered deben
estar condicionados al modo Remastered. Antes de editar, revisa las fuentes
del proyecto y el comportamiento original correspondiente.

- Original conserva el encuadre 4:3 y el menú de la base del motor. Remastered
  conserva el marco 16:9, los fondos extendidos y su menú de tres opciones.
- Seleccionar un modo no cambia las preferencias gráficas guardadas del otro.
  Comparar recursos en un visor no debe cambiar el modo del juego.
- Los dos modos comparten las partidas. Las correcciones necesarias para que
  el port arranque y funcione en Android pueden ser comunes.
- Al cambiar código que comparten los modos, verifica los dos y el cambio en
  vivo. Para cambios gráficos compara la misma escena y posición de cámara.
  Una compilación correcta no sustituye la comprobación visual.
