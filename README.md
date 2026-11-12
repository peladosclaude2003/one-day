# One Day

App personal de Android para organizar el día y seguir el progreso en el gimnasio.

## Secciones
- **Hoy**: resumen del día, agenda, rachas y semana de gimnasio.
- **Pendientes**: tareas con fecha, hora, prioridad y recordatorio.
- **Rachas**: días sin fumar u otros vicios, récord personal y metas.
- **Gym**: entrenamientos por rutina, series (peso × repeticiones), historial en calendario y gráficas de progreso.

Los datos se guardan solo en el teléfono. En **Ajustes** (engrane en "Hoy") se puede guardar o restaurar una copia de seguridad.

## Compilar
```bash
./gradlew assembleRelease
```
El APK queda en `app/build/outputs/apk/release/app-release.apk`.

Pruebas (recorren la app y guardan capturas en `app/build/shots/`):
```bash
./gradlew testDebugUnitTest
```

## Llave de firma
La carpeta `keystore/` (archivo `one-day.jks` + `keystore.properties`) **no se sube a git**. Debe estar presente
al compilar para que cada versión nueva se instale encima de la anterior sin borrar los datos.

Hecho con Kotlin + Jetpack Compose.
