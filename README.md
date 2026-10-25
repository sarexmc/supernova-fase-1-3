# Dedsafio Space (Fabric 1.20.1)

Mod de Minecraft Java **1.20.1 con Fabric**: dos supernovas, búnker asignable, nave espacial de hierro,
viajes interplanetarios y la dimensión Gleba. Se entrega **por fases**.

> **Estado honesto:** este código fue escrito sin acceso a red ni a Minecraft. **No ha sido compilado ni ejecutado.**
> Cada fase debe compilarse y probarse en tu máquina; los errores que salgan se corrigen en la siguiente iteración.

## Compilar

Requisitos: JDK 17.

```bash
gradle wrapper --gradle-version 8.5   # una sola vez: genera gradlew y gradle-wrapper.jar
./gradlew build                       # Windows: gradlew.bat build
./gradlew runClient                   # cliente de pruebas (usa un mundo NUEVO)
./gradlew runServer
```

Versiones en `gradle.properties` (Minecraft 1.20.1, Yarn 1.20.1+build.10, Loader 0.15.11, Fabric API 0.92.2+1.20.1).
Comprueba que sigan vigentes en https://fabricmc.net/develop.

## Comandos (permiso configurable, por defecto nivel 2)

| Comando | Efecto |
|---|---|
| `/dedsafio supernova parte1` / `parte2` / `completa` | Inicia la secuencia. Si ya hay una en curso, se rechaza (no se reinicia). |
| `/dedsafio supernova cancelar` | Detiene la secuencia actual. |
| `/dedsafio supernova reiniciar` | Detiene y borra el estado de "supernovas explotadas". |
| `/dedsafio bunker asignar` | Guarda tu posición, dimensión y orientación como búnker. **No toca ningún bloque.** |
| `/dedsafio bunker ver` | Muestra el búnker asignado (cualquier jugador). |
| `/dedsafio bunker quitar` | Elimina la asignación. **No toca ningún bloque.** |
| `/dedsafio viaje gleba` / `tierra` | Viaje administrativo con comprobación de chunk y punto seguro. |

## Configuración: `config/dedsafio.json`

Duraciones de cada fase (cuentas atrás de 60 s por defecto), nivel de permiso de comandos y nivel de detalle de VFX.

## Seguridad del mundo

- El mod **no escribe ni borra bloques** en estas fases. Solo guarda `data/dedsafio_state.dat` (búnker, supernovas, puntos de regreso).
- Se añade la dimensión `dedsafio:gleba` por datapack. No altera dimensiones existentes.
- Prueba siempre en un **mundo nuevo** o en una copia de seguridad.

## Estado por fases

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Proyecto Gradle/Fabric, config, registros | Escrito, sin compilar |
| 2 | Máquina de estados de supernovas, cuentas atrás, sincronización, overlay 2D, 8 sonidos propios | Escrito, sin compilar. El 3D (estrella, cielo, explosión, shaders) es la Fase 4 |
| 3 | Búnker asignable y persistente | Escrito, sin compilar |
| 4 | VFX/shaders: estrellas, explosiones, Tierra, Gleba | Pendiente |
| 5 | Nave, base, tanque, combustible, despegue con Ctrl | Pendiente |
| 6 | Dimensión Gleba real, generación propia, regreso | Pendiente (hay un placeholder de dimensión) |
| 7 | Armadura negra con cuernos, bloque de carne, minerales | Pendiente |

## Pruebas pendientes de la Fase 1-3 (marca cuando las hagas)

- [ ] El mod carga sin errores en `runClient`.
- [ ] `/dedsafio supernova parte1`: cuenta de 60 s, gris progresivo, destello, oscuridad que se desvanece.
- [ ] `/dedsafio supernova parte2`: tinte rojo moderado, blanco solo al final, destello, rayo de izquierda a derecha.
- [ ] `/dedsafio supernova completa` encadena ambas sin reiniciar la Parte 1.
- [ ] `bunker asignar` → reiniciar servidor → `bunker ver` conserva los datos.
- [ ] Ningún bloque cambia al usar los comandos del búnker.
- [ ] `/dedsafio viaje gleba` y `tierra` llevan a un punto seguro y vuelven.
- [ ] Los 8 sonidos suenan sincronizados.

## Sonidos

Generados proceduralmente con `tools/generate_sounds.py` (numpy + scipy + ffmpeg). Son originales del mod.
Son una primera versión; se pueden reemplazar por audio de mayor calidad con los mismos nombres.

## Licencia

Sin licencia definida (`All-Rights-Reserved` en `fabric.mod.json`). Cámbiala antes de publicar si lo deseas.
No se incluyen assets de Dedsafio 3 ni de terceros.
