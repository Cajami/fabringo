# Fabringo

Fabringo comienza con bingo de 75 bolas y cartillas de 5 × 5 con columnas B, I, N, G y O. Una sola app móvil de Android permite elegir **Anfitrión** o **Jugador** al entrar. El Anfitrión también puede dirigir el sorteo desde la app instalable en TV o usar su móvil con envío opcional a Chromecast.

## Bases del juego

- **Sorteo.** Presenta los números uno por uno, con avance manual o automático. El Anfitrión elige el patrón ganador antes de iniciar y este queda fijo hasta el final.
- **Cartillas de Jugador.** El Jugador marca manualmente los números conforme se cantan. El límite es de 10 cartillas por Jugador por partida, vinculada o independiente. Un límite de 15 se podrá evaluar para una segunda versión. El centro es libre, cuenta como marcado en todos los patrones y contiene el QR de la cartilla. Al tocarlo se abre una vista ampliada para escanear.
- **Pantalla de TV.** El sorteo debe leerse desde el fondo de la sala. En la app instalable, el anfitrión configura y dirige la partida con el mando. Los controles deben tener un foco visible. [Diseño para TV](https://developer.android.com/design/ui/tv)

### Patrones y ritmo del sorteo

El selector muestra tarjetas con el nombre y una miniatura de 5 × 5. Se pinta el fondo de las casillas requeridas, incluido el centro cuando forma parte de la figura. El centro libre ya satisface esa posición. Durante la partida, una miniatura junto al nombre recuerda el patrón elegido.

| Patrón | Figura en la cartilla | Números que se deben acertar | Letras con números requeridos |
| --- | --- | ---: | --- |
| Cartón lleno | Cuadrícula completa | 24 | B, I, N, G, O |
| Columna B, I, G u O | Una opción para cada columna completa | 5 | La letra elegida |
| Columna N | Columna central completa | 4 | N |
| H | Columnas B y O completas y tercera fila | 12 | B, I, G, O |
| Cuatro esquinas | Una casilla en cada esquina | 4 | B, O |
| X | Las dos diagonales completas | 8 | B, I, G, O |
| Más (+) | Tercera fila y columna N completas | 8 | B, I, N, G, O |
| Letra O | Todo el contorno | 16 | B, I, N, G, O |
| Diamante relleno | Filas centradas de 1, 3, 5, 3 y 1 casillas | 12 | B, I, N, G, O |

Cartón lleno, las cinco columnas individuales y la H ya están definidos. Cuatro esquinas, X, Más (+), Letra O y Diamante relleno son jugables y se proponen para ampliar el selector. Se distinguen los nombres **Columna O** y **Letra O**.

Antes de iniciar, el Anfitrión elige entre **Todas las bolas (75)** y **Solo letras del patrón**. Esta última opción usa todas las bolas de las columnas con al menos una casilla numérica requerida. Por ejemplo, Columna N usa las 15 bolas del 31 al 45 y Cuatro esquinas usa las 30 bolas de B y O. En la H y la X, la única posición en N es el centro libre, así que el filtro usa 60 bolas de B, I, G y O. Se propone mostrar el total antes de iniciar y fijar el conjunto de bolas junto con el patrón. El ritmo manual o automático se puede ajustar durante la partida.

El Anfitrión puede cambiar en cualquier momento entre **Bola siguiente** manual y avance automático cada **5, 10, 15, 20 o 30 segundos**. En automático, se muestra una cuenta regresiva para la próxima bola y hay un botón **Pausar** para detener el sorteo mientras se coteja una cartilla.

## Modos del sorteo

| Modo | Quién controla | Dónde se ve el sorteo |
| --- | --- | --- |
| App instalada en Android TV o Google TV | El anfitrión con el mando. | En la TV. La partida puede iniciarse y operarse desde la app de TV. |
| Solo móvil | El anfitrión desde el móvil. | En el móvil del anfitrión. |
| Móvil con Chromecast | El anfitrión desde el móvil. | En el móvil y, al mismo tiempo, en la TV conectada al Chromecast. |

Cada partida tiene un solo dispositivo anfitrión. En el modo Chromecast, el móvil mantiene el control, la configuración y el estado del sorteo. La TV reproduce los números de esa misma partida. La partida debe poder seguir en el móvil si se corta la transmisión.

La primera prueba de Chromecast será duplicar la pantalla completa del Android físico desde Google Home, con ambos dispositivos en la misma red Wi‑Fi. Se comprobará que la TV también reproduzca el canto. La prueba se puede hacer con una app existente y no requiere una cuenta de desarrollador de Google Cast. La prueba de Fabringo se hará cuando exista la primera versión móvil. La tarifa de USD 5 queda pendiente hasta evaluar si necesitamos integrar un botón **Enviar a TV** con receptor personalizado. El sorteo puede continuar en el móvil si falla la duplicación. [Duplicación de pantalla y audio en Android](https://support.google.com/chromecast/answer/6050397?hl=es) · [Registro de Google Cast](https://developers.google.com/cast/docs/registration)

Queda por confirmar qué modelos físicos usaremos para probar Android TV o Google TV y Chromecast.

## Emisión y validación de tarjetas

El Jugador anuncia «¡Bingo!» en persona. Ante un reclamo, el Anfitrión puede pausar el ritmo automático y validar sucesivamente las cartillas contra el mismo historial. Si dos cartillas resultan ganadoras, se reparte el premio. La partida termina cuando el Anfitrión pulsa **Terminar** y confirma la acción. El seguimiento continuo de cartillas y el botón del Jugador para anunciar bingo quedan fuera de esta fase.

- **Sorteo desde el móvil.** Cuando un jugador canta bingo, el anfitrión escanea el QR y Fabringo dibuja la misma cartilla, con los números en sus posiciones originales. La pantalla de verificación marca automáticamente las casillas cuyos números ya salieron y muestra si se cumple el patrón ganador fijado al iniciar. La comprobación usa el historial del principal. El formato del QR sigue en evaluación.
- **Sorteo desde la app de TV.** El anfitrión verifica los números uno por uno en la tarjeta del jugador, con el historial del sorteo en la TV y el patrón ganador como referencia. Esta modalidad se valida manualmente.

### Formato del QR para 75 bolas

La propuesta recibida serializa una cartilla existente de 5 × 5 recorriendo sus filas de arriba abajo y sus columnas de izquierda a derecha. Cada posición ocupa dos dígitos ASCII, incluidos los ceros iniciales. El QR contiene únicamente el texto de 50 dígitos. El lector reconstruye las 25 posiciones mediante los pares consecutivos.

El Jugador puede importar este QR con la cámara y sin conexión. La app dibuja la cartilla para confirmar su contenido antes de agregarla y rechaza QR inválidos o disposiciones repetidas. Si el QR trae un número válido en el centro, la app lo conserva y el botón **Ver QR** queda debajo de la cartilla. Las cartillas generadas en Fabringo mantienen el centro libre. El lector de verificación del Anfitrión usará este mismo formato.

La cámara muestra el total de cartillas solo después de la primera confirmación. La revisión tiene un título de alto contraste y una barra inferior con **Agregar cartilla** como acción principal y **Descartar y escanear otra** como acción secundaria centrada. Una cartilla aparece en el contador al confirmarla.

Las columnas conservan los rangos B 1–15, I 16–30, N 31–45, G 46–60 y O 61–75. Se validan la matriz de 5 × 5, los valores enteros, los rangos y los números repetidos. El par 13, caracteres 25 y 26 contando desde uno, representa el centro. Fabringo usará un centro libre, codificado como `00` si se adopta este formato. El centro numérico forma parte de los ejemplos del formato recibido. [Reglas de bingo de 75 bolas](https://www.swisslos.ch/en/bingo/informations/instructions-for-playing/instructions-for-playing-and-types.html)

| Caso de referencia | Texto del QR |
| --- | --- |
| Ronda 05, centro libre | `02203148610321324962102300506311274159721230456075` |
| Centro numérico 38 | `06194354611127314768032038566909173557670524395362` |

El contenido identifica la disposición de las casillas. Dos cartillas idénticas producen el mismo texto. Si una partida vinculada necesita acreditar qué cartilla se emitió o a qué jugador se asignó, se podrá contrastar el contenido con el registro de emisión o evaluar un formato que incluya ese dato.

El antecedente reporta una lectura del ejemplo Ronda 05 a 22 × 22 mm en bingo!!. Para Fabringo queda pendiente comprobar el escaneo de la vista ampliada en móviles físicos. La corrección de errores M y el margen blanco de cuatro módulos quedan como parámetros de referencia. [Margen del QR y corrección de errores](https://www.qrcode.com/en/howto/code.html)

### Variante de 90 bolas fuera del alcance inicial

El bingo de 90 bolas suele usar 3 filas y 9 columnas por rangos numéricos, con cinco números por fila y casillas vacías. Las letras BINGO corresponden a las cinco columnas de la variante de 75 bolas. Si se incorpora la variante de 90 bolas, habrá que definir cómo identificarla al escanear, cómo codificar sus espacios vacíos y dónde ubicar el QR. [Formatos de cartilla de 75 y 90 bolas](https://www.swisslos.ch/en/bingo/informations/instructions-for-playing/instructions-for-playing-and-types.html)

## Relación entre sorteo y tarjetas

| Modo | Flujo | Alcance de la unicidad |
| --- | --- | --- |
| Independiente | El anfitrión dirige el sorteo. Cada jugador elige cuántas cartillas quiere y su móvil las genera de forma aleatoria. El marcado es manual. | El móvil evita tableros repetidos entre sus cartillas para esa partida. Puede haber coincidencias entre móviles. |
| Vinculado | El Anfitrión crea la partida y muestra un QR para unirse. Cada Jugador envía su nombre o apodo al escanearlo. Si todavía no tiene uno, la app se lo pide. Se puede configurar de antemano desde el rol Jugador. El Anfitrión ve la lista de personas enlazadas, indica cuántas cartillas entrega a cada una y su dispositivo las genera y distribuye por Firebase. El Jugador recibe sus cartillas y las marca a mano. | El Anfitrión controla los duplicados al emitir las cartillas para la partida. |

El QR de partida lo muestra el anfitrión y lo escanea el jugador para unirse. El QR de cartilla lo muestra el jugador y lo escanea el anfitrión cuando dirige desde el móvil. En una partida vinculada dirigida desde la TV, la TV muestra el QR para unirse y el anfitrión comprueba el bingo manualmente.

### Reparto y juego sin conexión

Firebase se usa durante la preparación de una partida vinculada para el ingreso y el reparto. Al iniciar se cierra el ingreso. El sorteo, el marcado, la comprobación y el cierre funcionan con datos locales hasta terminar la partida.

Como regla de inicio se propone que cada Jugador confirme que recibió y guardó su lote completo. El Anfitrión debería poder excluir de la partida a quien quede pendiente. Conviene probar una pérdida de internet después de guardar las cartillas y verificar que una solicitud repetida durante el reparto devuelve la misma asignación. [Persistencia sin conexión en Cloud Firestore](https://firebase.google.com/docs/firestore/manage-data/enable-offline) · [Operaciones atómicas en Cloud Firestore](https://firebase.google.com/docs/firestore/manage-data/transactions)

### Recuperación de la partida activa

Cada dispositivo conserva una sola partida activa, sea como Anfitrión o Jugador, y guarda su progreso automáticamente. Al reabrir la app se recupera esa partida. El Anfitrión guarda localmente el patrón, el historial de bolas y, en el modo vinculado, los nombres de los Jugadores que recibieron cartillas. Cada Jugador guarda sus propias cartillas y sus marcas. El estado local recuperable del Anfitrión conserva la lista de participantes y prescinde de la asignación de cartillas por persona. Al confirmar **Terminar**, se elimina el estado local de la partida activa en ese dispositivo. El historial de partidas finalizadas queda fuera de esta versión. Si los teléfonos están sin conexión, se propone que cada Jugador cierre su partida local cuando el Anfitrión anuncie el fin.

Queda por definir si la unicidad se mide por disposición o por conjunto de números y el límite de Jugadores por partida vinculada.

## Voz del sorteo

La primera versión tendrá dos voces generadas con IA en español latino, una masculina y una femenina. Cada canto dice la letra y el número completo y vuelve a anunciar la bola tras una pausa aproximada de uno a dos segundos. El inglés queda para una fase posterior.

La app móvil trae 75 locuciones por voz en Ogg y las reproduce al revelar una bola, con o sin internet. El Anfitrión puede cambiar de voz y pulsar **Escuchar muestra** desde la configuración inicial o **Ritmo y voz** durante la partida. Del 1 al 59, la repetición dice la letra y el número completo. Del 60 al 75, dice la letra y luego cada dígito por separado. Por ejemplo, G59 suena «Ge, cincuenta y nueve» en ambos anuncios. G60 suena «Ge, sesenta» y después «Ge, seis, cero». La app deja terminar el anuncio antes de revelar una nueva bola. La reproducción por Chromecast sigue pendiente de validación física.

Las locuciones se generan con `app/mobile/tools/generate_calls.py`. El script requiere Piper 1.8, NumPy, FFmpeg y las rutas a los modelos ONNX Daniela y Ald con su archivo JSON correspondiente. Los créditos y las licencias de los conjuntos de datos quedan en `app/mobile/src/main/assets/calls/LICENCIAS.txt`. [Voz Daniela](https://huggingface.co/rhasspy/piper-voices/blob/main/es/es_AR/daniela/high/MODEL_CARD) · [Voz Ald](https://huggingface.co/rhasspy/piper-voices/blob/main/es/es_MX/ald/medium/MODEL_CARD)

## Entorno Android en Windows

Diagnóstico del 22 de noviembre de 2026.

| Componente | Estado |
| --- | --- |
| Equipo | Windows 11, Ryzen 7 4700U, 15,4 GiB de RAM disponibles para el sistema y aproximadamente 27,5 GiB libres en `C:`. |
| Flutter | Flutter 3.44.0 estable y Dart 3.12.0 instalados. La rama estable publicada al momento del diagnóstico era Flutter 3.50. [Novedades de Flutter](https://docs.flutter.dev/release/whats-new) |
| Android Studio y SDK | Android Studio instalado, plataforma API 36, Build Tools 36.0.0, Command-line Tools y `adb`. [Configuración de Flutter para Android](https://docs.flutter.dev/platform-integration/android/setup) |
| Java | `flutter doctor -v` usa el JDK 21.0.6 incluido en Android Studio. `JAVA_HOME` apunta a JDK 22 y `java` en `PATH` ejecuta JDK 11. Para el proyecto conviene alinear el JDK de Gradle con el que se pruebe en la compilación. [JDK y Gradle en Android](https://developer.android.com/build/jdks) |
| Licencias del SDK | `flutter doctor -v` detectó licencias pendientes. `flutter doctor --android-licenses` muestra los términos para su revisión y aceptación. |
| Dispositivos de prueba | Todas las pruebas móviles se harán en el Android físico del usuario. La experiencia de TV se validará en un televisor o receptor físico compatible según la modalidad elegida. [Pruebas en un dispositivo físico](https://developer.android.com/studio/run/device) |

## Desarrollo Android nativo

El código Android está en `app/mobile`. La primera entrega usa Kotlin y Jetpack Compose para móvil. La aplicación de TV se desarrollará con Compose for TV. La ruta `app/web` se reserva para una etapa posterior. [Gráficos en Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview) · [Compose for TV](https://developer.android.com/training/tv/playback/compose)

Esta entrega permite elegir **Anfitrión** o **Jugador**. Incluye las partidas independientes, el selector visual de patrones, el sorteo manual o automático con pausa, el ánfora animada y el tablero de bolas. El Jugador elige entre dos secciones. En **Cartillas nuevas**, ajusta la cantidad de una a diez con los botones − y + y pulsa **Generar cartillas**. En **Cartillas con QR**, pulsa **Escanear QR** para importar sus cartillas. En el recorrido de escaneo, revisa cada cartilla y pulsa **Agregar cartilla** para sumarla al contador. Puede escanear otra o elegir **Empezar a jugar** con las confirmadas. Todas se marcan manualmente con una X gruesa y tienen una vista de QR ampliado. La partida se guarda en el dispositivo y se elimina al terminar. Al regresar a un sorteo automático, el Anfitrión puede reanudarlo desde su estado guardado.

Desde la versión móvil 0.1.5, el botón **Anfitrión** de la pantalla de inicio muestra un aviso de que el modo está en desarrollo. Desde **Jugador** se pueden generar y escanear cartillas. Las partidas activas que ya estén guardadas se pueden recuperar.

Quedan pendientes la verificación del bingo con la cámara del Anfitrión, las partidas vinculadas por Firebase y la app para TV. La duplicación de pantalla y audio en Chromecast requiere una prueba con el equipo físico.

Para compilar en Windows, usa Android SDK 36 y JDK 21. En este equipo se verificó con el JDK incluido en `Android Studio1`. Desde `app/mobile`, en PowerShell:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio1/jbr'
.\gradlew.bat assembleDebug lintDebug
```

El proyecto fija Gradle 9.1.0, Android Gradle Plugin 9.0.1 y Compose BOM 2025.08.00. La APK de depuración se genera en `app/mobile/build/outputs/apk/debug/Fabringo-debug.apk`. Se verificaron la compilación, la firma de depuración y la codificación y lectura de 200 QR generados con cartillas de prueba. Las pruebas de uso de la aplicación, incluida la lectura con cámara, quedan pendientes en el Android físico del usuario. [Compatibilidad de Android Gradle Plugin 9.0.1](https://developer.android.com/build/releases/agp-9-0-0-release-notes)

## Skills del proyecto

El entorno local tiene instaladas en `.agents/skills/` las skills `flutter-apply-architecture-best-practices`, `flutter-build-responsive-layout` y `flutter-add-widget-test` del equipo de Flutter, además de `adaptive` y `testing-setup` del equipo de Android. El equipo también cuenta con `ui-ux-pro-max` para diseño móvil. Las skills se instalan localmente y su uso es independiente de la compilación de Android. [Skills de Flutter](https://github.com/flutter/agent-plugins) · [Skills de Android](https://github.com/android/skills)

## Dirección visual

La pantalla de entrada muestra el logo horizontal de Fabringo, extraído del diseño izquierdo de la referencia, sobre los botones **Anfitrión** y **Jugador**. El ícono instalable usa la ilustración transparente completa de la bola con F, las cartillas y las bolas de colores como primer plano de un ícono adaptativo con fondo azul noche. [Íconos adaptativos de Android](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)

Los roles se muestran como tarjetas táctiles. **Anfitrión** lleva un micrófono y adelanta que el sorteo está en desarrollo. **Jugador** se destaca en dorado con un ícono de cartilla y describe las dos acciones disponibles, generar o escanear cartillas.

La dirección visual sigue la referencia compartida, con un escenario azul profundo iluminado en azul y violeta, un ánfora de acabado brillante con bolas de colores y una bola principal con luz dorada. Los cinco encabezados del tablero se distinguen por letra y color. Las dos vistas aparecen juntas en la referencia. En el móvil, cada persona entra a la vista de su rol y la TV muestra la vista del Anfitrión.

Las cartillas del Jugador usan **coral**, **verde jade** y **ámbar** para los bordes iluminados y encabezados, con casillas claras y números oscuros. El color varía de una cartilla a otra. Una **X** cruza de esquina a esquina cada casilla marcada. El número se mantiene legible sobre la marca y la casilla conserva su fondo claro.

- **Anfitrión.** La pantalla usa un degradado azul. La bola actual es la pieza más grande y muestra tanto la letra como el número en tamaño destacado, por ejemplo **N 39**, con un pulso breve al salir. Debajo se muestra el progreso sobre el total elegido, por ejemplo **4 de 15**. En un móvil vertical, el tablero de 75 números va debajo en cinco columnas, con los rangos ordenados. En TV o en horizontal, se puede mostrar en cinco filas con la bola principal al costado. Los encabezados **B I N G O** están separados de los rangos. Los números pendientes se muestran a plena intensidad y los cantados se atenúan con opacidad. En el tablero, la última bola también se atenúa y lleva un contorno discreto. Las letras fuera del filtro se identifican como tales. La miniatura del patrón y el contador de tiempo quedan en segundo plano. **Bola siguiente** o **Pausar / Reanudar** quedan a mano según el ritmo. **Terminar** queda apartado de esos controles para facilitar el uso durante el sorteo.
- **Jugador.** Las cartillas completas de 5 × 5 aparecen una debajo de otra y se recorren con desplazamiento vertical. Sus encabezados **B I N G O** tienen una separación visual de la cuadrícula. El encabezado de la pantalla muestra el total, por ejemplo **3 cartillas en juego**. Cada cartilla lleva su posición en una franja inferior integrada, por ejemplo **Cartilla 2 de 3**. El Jugador toca una casilla para marcarla o desmarcarla y el estado se guarda automáticamente. La salida de una bola en el Anfitrión deja intactas las marcas y el aspecto de las cartillas del Jugador. En el centro libre se muestra un QR pequeño que se amplía al tocarlo.

Al sacar una bola, el ánfora se sacude y las bolas de colores se mezclan dentro. Terminado el movimiento, aparecen la letra y el número en la bola dorada y se actualiza el tablero del Anfitrión. En la app, el anuncio de voz acompaña la revelación. Con movimiento reducido, se muestra el resultado sin la sacudida.

Las referencias visuales revisadas incluyen la bola principal y el tablero de **Bingo en Casa**, el tablero agrupado de **Bingo Set** y la presentación de patrones de **bingo!!**. [Bingo en Casa](https://www.bingo.es/app-bingo-para-movil/) · [Bingo Set](https://www.littlebanditgames.com/bingo-set-manual/) · [bingo!!](https://mwm.ai/apps/bingo/981450944)

Priorizar las acciones del anfitrión en el sorteo y el marcado táctil en las tarjetas. Validar objetivos táctiles, tamaño de texto, contraste y TalkBack. Distinguir las casillas marcadas mediante forma o iconos además del color. [Accesibilidad en Flutter](https://docs.flutter.dev/ui/accessibility) · [Accesibilidad en Jetpack Compose](https://developer.android.com/develop/ui/compose/accessibility)
