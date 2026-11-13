# toolshop-selenium-java

Suite E2E con **Selenium WebDriver puro** (Java + JUnit + Gradle) sobre
[Toolshop / Practice Software Testing](https://practicesoftwaretesting.com), sprint 5.

- UI: <https://practicesoftwaretesting.com>
- API: <https://api.practicesoftwaretesting.com> ([Swagger](https://api.practicesoftwaretesting.com/api/documentation))
- Código de la aplicación: <https://github.com/testsmith-io/practice-software-testing>

## Requisitos

- JDK 21 o superior para lanzar Gradle. El build compila y ejecuta con **Java 25**
  mediante *toolchain*; si no está instalado, el plugin foojay lo descarga.
- Un navegador instalado: Chrome, Firefox o Edge (Safari solo en macOS).
  **Selenium Manager** (incluido en Selenium) resuelve los drivers: no hay
  WebDriverManager ni binarios en el repo.
- Acceso a internet hacia la aplicación y su API.

## Cómo ejecutar

```bash
cp .env.example .env   # completa ADMIN_PASSWORD
```

```bash
./gradlew test
```

| Objetivo | Comando |
|---|---|
| Toda la suite (Chrome, headless) | `./gradlew test` |
| Solo smoke | `./gradlew test -PincludeTags=smoke` |
| Un flujo | `./gradlew test -PincludeTags=F06` |
| Varios flujos (expresión de etiquetas JUnit) | `./gradlew test "-PincludeTags=F06 \| F07"` |
| Otro navegador | `BROWSER=firefox ./gradlew test` (`chrome`, `firefox`, `edge`, `safari`) |
| Con ventana visible | `HEADLESS=false ./gradlew test` |
| Contra Selenium Grid | `GRID_URL=http://localhost:4444 ./gradlew test` |
| Menos hilos | `PARALLELISM=2 ./gradlew test` |
| Formato, lint y tipado | `./gradlew spotlessCheck compileTestJava` (o `./gradlew check`) |
| Corregir formato | `./gradlew spotlessApply` |

El reporte Allure (un único `index.html`) se genera siempre al terminar `test`,
también si hay fallos, en `build/reports/allure-report/allureReport/index.html`.

### Variables de entorno

Todas se leen del entorno y, si faltan, del archivo `.env` (ver [.env.example](.env.example)).
No hay credenciales en el código.

| Variable | Por defecto | Uso |
|---|---|---|
| `BASE_URL` | `https://practicesoftwaretesting.com` | UI bajo prueba |
| `API_URL` | `https://api.practicesoftwaretesting.com` | API para precondiciones y verificaciones |
| `BROWSER` | `chrome` | `chrome`, `firefox`, `edge`, `safari` |
| `HEADLESS` | `true` | Navegador sin ventana (Safari lo ignora) |
| `GRID_URL` | _(vacío)_ | Si existe, usa `RemoteWebDriver` contra ese Grid |
| `TIMEOUT_SECONDS` | `15` | Espera explícita máxima por condición |
| `PARALLELISM` | `4` | Hilos de JUnit en paralelo |
| `ADMIN_EMAIL` | `admin@practicesoftwaretesting.com` | Cuenta de administrador (F10–F12) |
| `ADMIN_PASSWORD` | _(obligatoria)_ | Contraseña del administrador; en CI, secreto `TOOLSHOP_ADMIN_PASSWORD` |

### Selenium Grid

```bash
docker run -d -p 4444:4444 --shm-size=2g selenium/standalone-chrome
```

```bash
GRID_URL=http://localhost:4444 ./gradlew test
```

Las pruebas no cambian: `DriverFactory` crea un `RemoteWebDriver` con las mismas
opciones del navegador y un `LocalFileDetector`, para que el adjunto de F10 se
suba al nodo remoto.

## Versiones

Última versión estable de cada dependencia, consultada en Maven Central, Gradle
Plugin Portal y GitHub Releases el **2026-11-12**. Están fijadas en
[gradle/libs.versions.toml](gradle/libs.versions.toml).

| Componente | Versión | Nota |
|---|---|---|
| Java (toolchain) | 25 (LTS) | Selenium 4.52 exige Java 11 o superior |
| Gradle (wrapper) | 9.9.1 | |
| Selenium Java | 4.52.0 | Publicada el 2026-10-29 (mínimo pedido: 4.51.0) |
| JUnit (BOM) | 6.1.3 | Jupiter + Platform |
| AssertJ | 3.27.7 | La 4.0.0-M1 es *milestone*, se descartó |
| Allure Java (`allure-jupiter`) | 3.1.1 | |
| Allure Report (CLI) | 2.46.2 | Generado por el plugin de Gradle |
| Plugin `io.qameta.allure-report` | 4.3.1 | |
| Jackson (`tools.jackson`) | 3.3.0 | Cliente HTTP: `java.net.http` del JDK |
| Datafaker | 2.7.0 | Datos de usuarios únicos |
| dotenv-java | 3.2.0 | Lectura de `.env` |
| SLF4J / Logback | 2.0.20 / 1.6.6 | |
| Spotless + google-java-format | 8.11.0 / 2.0.0 | Formateador |
| Error Prone / NullAway / JSpecify | 2.52.0 / 0.14.3 / 1.0.1 | Linter y nulabilidad estricta, con `-Werror` |
| Plugin `org.gradle.test-retry` | 1.6.6 | Reintentos, solo en CI |
| Plugin foojay-resolver | 1.0.0 | Descarga del JDK del toolchain |
| GitHub Actions | checkout 7.0.1, setup-java 6.2.0, gradle/actions 6.4.1, upload-artifact 7.0.2 | |

Desde Selenium 4.51 los *bindings* de Java ya no dependen de Guava y
`ExpectedCondition` dejó de implementar `com.google.common.base.Function`. Las
utilidades de espera usan `java.util.function.Function` y no importan Guava. Las
lecturas de atributos usan `getDomProperty` y `getDomAttribute`, no el obsoleto
`getAttribute`.

## Estructura

```
src/test/java/com/toolshop/
  config/      Config (entorno + .env), Browser
  driver/      DriverFactory (local | RemoteWebDriver), DriverExtension (JUnit 5 + ThreadLocal)
  evidence/    FailureEvidence: captura, HTML, URL y logs del navegador → Allure
  support/     Waits (solo WebDriverWait), Money
  api/         ApiClient y servicios: Users, Products, Carts, Invoices, Favorites, Messages, Addresses
  data/        Records del dominio y UserFactory
  session/     Session: inyecta JWT y carrito en el almacenamiento del navegador
  components/  Header, CartTable, DataTable, Pagination, Toast
  pages/       Page Objects de cliente; pages/admin/ para el panel de administración
  tests/       F01…F12 y fixtures compartidos
```

## Decisiones de diseño

- **Page Objects sin PageFactory.** Cada página expone acciones de negocio
  (`fillForm(user)`, `choosePayment(payment)`, `confirmOrder()`) con
  localizadores `By` privados. Las piezas comunes son componentes: `Header`,
  `CartTable`, `DataTable`, `Pagination` y `Toast`. Es el patrón idiomático en
  Selenium puro: sin Screenplay ni wrappers, y fácil de leer para quien viene de
  WebDriver.
- **Selectores.** Primero `data-test` (`BasePage.byTest`). Donde la app no lo
  tiene se usa un atributo accesible: el texto alternativo de la imagen en los
  productos relacionados, la etiqueta `Quantity for …` en el carrito y
  `aria-label` para la paginación. Hay un XPath por texto visible para el aviso
  de carrito vacío y otro para el saludo del checkout. Ningún selector depende de
  clases de estilo, salvo `.alert-success` y `.alert-danger` con
  `role="alert"`, que son los únicos indicadores de esos mensajes.
- **Esperas.** Solo `WebDriverWait` con condiciones explícitas
  ([Waits.java](src/test/java/com/toolshop/support/Waits.java)). No hay *implicit
  waits* ni `Thread.sleep`. `untilAsserted` reintenta una aserción AssertJ hasta
  que pase o venza el tiempo, y conserva el mensaje "esperado vs. actual".
- **Un WebDriver por prueba.** `DriverExtension` lo crea en `beforeEach`, lo
  guarda en un `ThreadLocal` y lo cierra en `afterEach`. JUnit corre en paralelo
  clases y métodos, con `PARALLELISM` hilos.
- **Evidencia solo en fallos.** Antes de cerrar el navegador de una prueba
  fallida se adjuntan al reporte la captura, el HTML, la URL y los logs de
  consola (los logs solo en Chromium; los demás navegadores no los exponen por
  WebDriver).
- **Datos independientes.** Cada prueba registra su propio cliente único
  (correo aleatorio y dirección válida obtenida de `/postcode-lookup`). Crea por
  API el carrito, el pedido o el favorito que necesite. Ninguna prueba depende de
  otra ni de las cuentas compartidas, que se bloquean tras 3 intentos fallidos.
- **Sesión sin login por UI.** Toolshop guarda el JWT en
  `localStorage['auth-token']` y el carrito en `sessionStorage['cart_id']`.
  `Session` los inyecta tras abrir el origen, así que solo F01 y F02 inician
  sesión por la UI.
- **La API como oráculo.** Las aserciones verifican resultados de negocio contra
  la API: totales, líneas, estado del pedido, datos de pago y perfil persistido.
  En el catálogo, la UI debe mostrar exactamente lo que devuelve la API con los
  mismos criterios, y además cumplir la propiedad de negocio (orden, categoría o
  marca).
- **Reintentos solo en CI** (`test-retry`, 2 reintentos con `CI=true`). En local
  un fallo se ve a la primera.

## CI (GitHub Actions)

[.github/workflows/e2e.yml](.github/workflows/e2e.yml):

1. `quality`: Spotless, Error Prone y NullAway.
2. `e2e`: matriz **chrome / firefox / edge** en `ubuntu-latest`, en paralelo y
   headless. Usa la caché de Gradle (`gradle/actions/setup-gradle`) y 4 hilos por
   job. Publica el reporte Allure y los resultados crudos como artefactos aunque
   haya fallos.
3. `safari`: opcional, en `macos-latest`, con un solo hilo. Se activa al lanzar
   el workflow a mano con `safari: true`.

Para ejecutarlo hay que crear el secreto **`TOOLSHOP_ADMIN_PASSWORD`** en el
repositorio. Lanzado a mano, el workflow acepta una expresión de etiquetas, por
ejemplo `smoke`.

## Cobertura

Estado verificado en local con Chrome 157 headless. Cada flujo pasó **3
ejecuciones seguidas** antes de cerrarse.

| ID | Flujo | Pruebas | Etiquetas | Estado |
|---|---|---|---|---|
| F01 | Registro de cliente nuevo y primer login | 4 | smoke, regression | ✅ 3/3 |
| F02 | Login: válidas, inválidas, bloqueo y validaciones | 9 | smoke, regression | ✅ 3/3 |
| F03 | Catálogo: búsqueda, categoría, marca, orden, paginación | 10 | smoke, regression | ✅ 3/3 |
| F04 | Detalle: cantidad, agregar al carrito, relacionados | 5 | smoke, regression | ✅ 3/3 |
| F05 | Carrito: editar, eliminar, totales | 4 | smoke, regression | ✅ 3/3 |
| F06 | Checkout autenticado: dirección, pago, confirmación | 1 | smoke | ✅ 3/3 |
| F07 | Checkout con los 5 medios de pago (parametrizado) | 6 | regression | ✅ 3/3 |
| F08 | Favoritos: agregar, listar, eliminar | 3 | smoke, regression | ✅ 3/3 |
| F09 | Cuenta: perfil y facturas de un pedido | 3 | smoke, regression | ✅ 3/3 ⚠️ |
| F10 | Contacto con adjunto y validaciones | 5 | smoke, regression | ✅ 3/3 ⚠️ |
| F11 | Admin: crear y editar producto, reflejado en el catálogo | 2 | smoke, regression | ✅ 3/3 ⚠️ |
| F12 | Admin: cambiar estado de pedido y verificarlo como cliente | 3 | smoke, regression | ✅ 3/3 ⚠️ |

⚠️ = adaptado a la versión actual del sitio:

- **F09:** las facturas se consultan sobre un pedido que la propia prueba crea
  por API, no sobre el de F06, para que las pruebas sean independientes.
- **F10:** la app solo acepta adjuntos `.txt` de **0 bytes** (regla deliberada
  del sitio de práctica). El caso feliz usa un archivo vacío; los casos con
  contenido o de otro tipo son negativos.
- **F11:** el formulario de edición no precarga stock, marca, categoría ni
  imagen, porque la API de productos no devuelve esos campos. La prueba los
  vuelve a indicar para poder guardar. Además, la búsqueda usa FULLTEXT de MySQL
  e ignora palabras de menos de 3 caracteres, por eso los nombres únicos llevan
  un sufijo de 10 letras.
- **F12:** la UI del cliente no muestra el estado del pedido. El cambio lo hace
  el administrador por UI y el cliente lo verifica con su propio token en
  `GET /invoices/{id}`. Se usan `ON_HOLD` y `COMPLETED`, que la tarea programada
  de la app (cada 10 minutos) no vuelve a mover.

## Riesgos conocidos

- **Reinicio horario.** La base de datos de producción se reinicia cada hora
  (`migrate:fresh --seed` en `Kernel.php`; el README de la app no lo menciona).
  Una ejecución que cruce el minuto 0 puede fallar porque los IDs y los usuarios
  creados desaparecen. Los reintentos de CI lo absorben en la mayoría de los
  casos.
- **Entorno compartido.** Otros usuarios usan el mismo sitio. Las pruebas no
  dependen de datos ajenos, salvo el catálogo semilla (F03 compara con la API
  en el momento). Si alguien crea productos de precio 1–100 durante la
  ejecución, F03 puede fallar puntualmente.
- **Tarea programada de pedidos.** `order:update` corre cada 10 minutos y
  mueve los pedidos nuevos a `ON_HOLD` (transferencia) o `AWAITING_SHIPMENT`
  (resto). F06, F07 y F12 aceptan como estado inicial `AWAITING_FULFILLMENT` o
  el que asigna esa tarea. Si la tarea pasa un pedido a `AWAITING_SHIPMENT`
  justo antes de que F12 elija `ON_HOLD`, esa opción ya no existe y el caso
  falla. Es una ventana de segundos cada 10 minutos, que el reintento de CI
  cubre.
- **Productos de otros usuarios.** En la observación de esta suite apareció
  otro cliente creando productos ("E2E Tool …") durante las ejecuciones. Por
  eso F03 no compara el contenido exacto de la última página.
- **Aislamiento F11 ↔ F03.** F11 crea productos reales. Para que no alteren el
  catálogo que F03 verifica en paralelo, cuestan más de 100 (la portada filtra
  entre 1 y 100) y sus nombres no contienen los términos que busca F03.
- **Cortes de red de la API.** El cliente HTTP reintenta solo las lecturas
  (`GET`) ante cortes de conexión. Por HTTP/1.1 el servidor responde
  `Connection: Upgrade, close` y provocaba reutilización de conexiones cerradas,
  por eso el cliente usa HTTP/2 (por defecto en el JDK).
- **Video y trazas.** Selenium no graba video ni trazas de forma nativa. Hay
  captura, HTML y logs en cada fallo; para video, usa Selenium Grid con el
  contenedor `selenium/video`.
