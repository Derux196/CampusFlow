# CampusFlow

Backend de una plataforma de gestión académica desarrollado con Java y Spring Boot.

## Tecnologías

- Java 25
- Spring Boot 4.1.1
- Spring Data JPA / Hibernate
- MySQL 8
- Maven
- JUnit y Mockito
- Stripe Java SDK

## Arquitectura

El proyecto utiliza una arquitectura organizada por módulos funcionales:

- `usuarios`: gestión y consulta de usuarios.
- `cursos`: gestión y consulta de cursos.

Cada módulo contiene las capas `controller`, `service`, `repository` y `model`.

## Integración de pagos y suscripciones con Stripe

CampusFlow utiliza Stripe Checkout en modo de pruebas para pagos únicos de
USD 20 y para el Plan Individual de USD 10 mensuales. Ambos flujos se inician
desde una sesión autenticada: el backend obtiene el usuario del contexto JWT,
asocia su ID mediante `client_reference_id` y usa exclusivamente los precios
configurados por el servidor.

El pago único usa el modo `PAYMENT`. La suscripción usa el modo
`SUBSCRIPTION`, el precio configurado en `STRIPE_PRICE_PLAN_INDIVIDUAL` y
metadata con `usuarioId`, `plan` y `priceId`. Antes de crear Checkout, el
backend consulta Stripe para comprobar que el precio configurado es recurrente
mensual. El cliente no puede enviar ni sustituir el importe o el precio.

Al iniciar una suscripción, CampusFlow guarda una suscripción local
`PENDIENTE` asociada a la sesión Checkout. Solo un webhook firmado de una
sesión local pendiente y válida puede cambiarla a `ACTIVA`; esto evita que un
evento ficticio o ajeno active un plan.

El endpoint `POST /api/stripe/webhook` es público para que Stripe pueda
entregar eventos, pero valida criptográficamente el encabezado
`Stripe-Signature` antes de procesarlos. Las firmas ausentes o inválidas se
rechazan con HTTP 400. Para `checkout.session.completed`, el backend recupera
la sesión desde Stripe y diferencia los pagos únicos de las suscripciones.

Cada evento de suscripción se registra en `eventos_stripe` con su identificador
`evt_...`, tipo, estado de procesamiento y fecha. La restricción única de ese
identificador, junto con las restricciones únicas de sesión Checkout y
suscripción Stripe, protege la idempotencia ante entregas repetidas. Los
eventos ya procesados, no aplicables o con datos de negocio inválidos se
descartan de forma controlada. Los fallos técnicos de Stripe o de MySQL se
mantienen como errores para permitir el reintento de Stripe.

`PagoService` registra un pago solo cuando:

- Stripe confirma `payment_status` como `paid`.
- El importe es 2000 centavos y la moneda es USD.
- La sesión contiene un `client_reference_id` numérico de un usuario existente.
- No existe ya un pago con el mismo `stripe_session_id`.

Los eventos firmados que no cumplen esas reglas se descartan de forma
controlada y no crean registros. Los pagos válidos se guardan en MySQL con el
usuario asociado, el identificador de sesión de Stripe, el importe, la moneda
y el estado `PAGADO`.

Las suscripciones se almacenan en `suscripciones`, con el usuario, el plan,
el estado (`PENDIENTE`, `ACTIVA` o `CANCELADA`), los identificadores de sesión
Checkout y suscripción Stripe, y las fechas de creación y activación.

### Endpoints de Stripe

| Método | Endpoint | Seguridad | Descripción |
|---|---|---|---|
| POST | `/api/stripe/checkout/pago-unico` | JWT | Crea una sesión Checkout de pago único. |
| POST | `/api/stripe/checkout/suscripcion` | JWT | Crea una sesión Checkout mensual para el usuario autenticado. |
| POST | `/api/stripe/webhook` | Firma Stripe | Procesa eventos firmados de Checkout; no usa JWT porque Stripe no puede enviarlo. |

### Variables de entorno

La configuración depende de variables de entorno. No guardes sus valores en
archivos versionados:

- `JWT_SECRET`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_PRICE_PAGO_UNICO`
- `STRIPE_PRICE_PLAN_INDIVIDUAL`

Para desarrollo local, activa el perfil `dev` y configura `DB_PASSWORD` junto
con las variables de Stripe en tu terminal o administrador de secretos local.
Los archivos `.env`, propiedades locales y logs están excluidos de Git.

### Pruebas de pagos

Las pruebas automatizadas no requieren credenciales reales, MySQL ni llamadas
a Stripe. Para ejecutar la suite, proporciona valores ficticios temporales a
las propiedades de Stripe en el proceso de Maven y ejecuta:

```powershell
.\mvnw.cmd clean test `
  -DSTRIPE_SECRET_KEY=sk_test_placeholder `
  -DSTRIPE_WEBHOOK_SECRET=whsec_test_placeholder `
  -DSTRIPE_PRICE_PAGO_UNICO=price_test_placeholder `
  -DSTRIPE_PRICE_PLAN_INDIVIDUAL=price_test_placeholder
```

La suite cubre pagos únicos existentes, creación de parámetros de Checkout de
suscripción, asociación del usuario autenticado, validación de firma, eventos
duplicados, eventos inválidos, activación local y persistencia aislada con H2.
Las pruebas no contactan Stripe ni modifican la base MySQL local.

La integración se desarrolló y validó únicamente con el entorno de pruebas de
Stripe. No se han realizado pagos reales y esta configuración no se declara
lista para producción. Se verificaron manualmente en Sandbox un Checkout de
pago único y un Checkout de suscripción mensual, incluida la activación local
de la suscripción y el registro del evento firmado.

### Verificación manual en sandbox

La verificación manual realizada en Stripe Sandbox confirmó que:

1. Checkout crea el pago único y la suscripción mensual con los precios
   configurados por el servidor.
2. Stripe CLI reenvía el webhook firmado a
   `http://localhost:8080/api/stripe/webhook`.
3. La suscripción local cambia de `PENDIENTE` a `ACTIVA` tras
   `checkout.session.completed`.
4. El evento queda registrado con estado `PROCESADO`.
5. La página de éxito se sirve públicamente después de Checkout.

Para repetirla, configura las variables localmente, inicia el backend y Stripe
CLI, completa Checkout con datos de prueba y confirma que un reintento del
mismo evento no crea filas duplicadas.

No uses `stripe trigger` genérico para validar una activación: sus datos no
corresponden necesariamente a la sesión pendiente creada por CampusFlow.

## Preparación de despliegue: Render y Supabase

CampusFlow está preparado para ejecutarse en un Web Service de Render usando
PostgreSQL administrado por Supabase. Esta preparación no crea recursos
externos, no migra datos de MySQL y no constituye un despliegue realizado.

### Arquitectura prevista

```text
Cliente HTTPS -> Render Web Service (Spring Boot + Docker)
                     |                     |
                     | JDBC con SSL         | webhook firmado
                     v                     v
              Supabase PostgreSQL       Stripe Sandbox
```

Render proporciona el puerto mediante `PORT`; Spring Boot usa `8080` como
valor local predeterminado. El health check público es `GET /actuator/health`
sin detalles internos. Los endpoints de usuarios, Checkout y suscripciones
continúan protegidos por JWT. El webhook de Stripe sigue siendo público
únicamente porque valida su firma criptográfica.

### Perfiles de datos

| Perfil | Motor | Configuración | Uso |
|---|---|---|---|
| `dev` | MySQL local | `application-dev.properties` | Desarrollo local; conserva la base existente. |
| `test` | H2 en memoria | `application-test.properties` | Pruebas aisladas, sin MySQL, Supabase ni Stripe real. |
| `prod` | PostgreSQL Supabase | `application-prod.properties` | Render; usa variables de entorno y SSL en la URL JDBC. |

El perfil `prod` usa `spring.jpa.hibernate.ddl-auto=update`. No usa
`create` ni `create-drop`, por lo que Hibernate no elimina tablas existentes.
Para cambios futuros de esquema se recomienda introducir migraciones
versionadas con Flyway antes de evolucionar datos de producción; no copies
automáticamente datos de MySQL a Supabase.

### Variables de Render y Supabase

Configura estos nombres como variables de entorno en Render, sin registrar sus
valores en Git:

- `SPRING_PROFILES_ACTIVE` con el perfil `prod`.
- `SPRING_DATASOURCE_URL`: URL JDBC del **Session Pooler** de Supabase que
  incluya `sslmode=require`.
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `DB_POOL_MAX_SIZE` (opcional; el valor por defecto es `5`).
- `APP_PUBLIC_BASE_URL`: URL HTTPS pública definitiva de Render, sin barra
  final.
- `JWT_SECRET`
- `STRIPE_SECRET_KEY` de Sandbox/test.
- `STRIPE_WEBHOOK_SECRET` del endpoint público de Stripe.
- `STRIPE_PRICE_PAGO_UNICO`
- `STRIPE_PRICE_PLAN_INDIVIDUAL`

Render proporciona `PORT` automáticamente; no establezcas un puerto fijo de
producción. El archivo [render.yaml](./render.yaml) declara los nombres de las
variables y `autoDeploy: false`, pero no incluye ningún valor secreto.

### Configurar Supabase manualmente

1. Crea un proyecto Supabase y espera a que PostgreSQL esté disponible.
2. En **Connect**, copia la conexión del **Session Pooler** y conviértela a
   formato JDBC: `jdbc:postgresql://<host>:5432/postgres?sslmode=require`.
3. Configura esa URL, usuario y contraseña exclusivamente en Render.
4. No uses la conexión directa si Render no tiene IPv6 disponible.
5. Antes de guardar datos reales, define una estrategia de backup y
   migraciones versionadas. El primer arranque con `update` crea o actualiza
   el esquema sin operaciones de borrado.

### Desplegar manualmente en Render

1. Crea un **Web Service** desde este repositorio y selecciona la rama `main`.
2. Elige el runtime **Docker**. Render detecta [Dockerfile](./Dockerfile), por
   lo que no debes definir comandos alternativos de build o start.
3. Configura las variables anteriores, usa `/actuator/health` como health
   check y deja `SPRING_PROFILES_ACTIVE=prod`.
4. Crea el servicio solo después de revisar los valores y logs de construcción.
5. Cuando Render asigne la URL HTTPS, configúrala como `APP_PUBLIC_BASE_URL`.
6. Verifica `GET /actuator/health`, el login y una ruta privada sin JWT antes
   de probar Checkout.

### Configurar Stripe Sandbox con URL pública

1. Conserva claves y precios de **Sandbox/test**; no uses claves `sk_live_`.
2. En Stripe, crea o actualiza el endpoint de webhook público:
   `https://<tu-servicio>.onrender.com/api/stripe/webhook`.
3. Copia el secreto de ese endpoint únicamente a
   `STRIPE_WEBHOOK_SECRET` en Render.
4. Comprueba que las URLs de éxito y cancelación generadas usan
   `APP_PUBLIC_BASE_URL` y las páginas públicas `.html`.
5. Realiza una prueba manual controlada después del despliegue y revisa que
   eventos, pagos y suscripciones se persistan una sola vez.

### Checklist previo al despliegue

- [ ] La suite Maven pasa usando valores ficticios, sin bases de datos externas.
- [ ] La imagen Docker se construye localmente sin credenciales.
- [ ] Supabase usa Session Pooler, JDBC y `sslmode=require`.
- [ ] Render tiene todas las variables requeridas y el perfil `prod`.
- [ ] `APP_PUBLIC_BASE_URL` coincide con la URL HTTPS real de Render.
- [ ] El health check devuelve `UP` y no expone detalles.
- [ ] Stripe Sandbox apunta al webhook público y usa su secreto específico.
- [ ] No hay secretos, logs, `.env` ni datos locales en Git.

**Pendiente de comprobación manual:** todavía no se ha creado ni desplegado un
servicio Render, no se ha conectado una base Supabase y no se ha probado Stripe
contra una URL pública.

## API REST

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/usuarios` | Lista los usuarios registrados |
| GET | `/api/cursos` | Lista los cursos registrados |
| GET | `/api/saludo` | Comprueba que el servidor responde |
| POST | `/api/stripe/checkout/pago-unico` | Inicia Checkout de pago único |
| POST | `/api/stripe/checkout/suscripcion` | Inicia Checkout mensual autenticado |

## Base de datos

Crear la base de datos en MySQL:

```sql
CREATE DATABASE IF NOT EXISTS campusflow_db;
```

Configurar la conexión mediante `application.properties` y proporcionar la contraseña utilizando la variable de entorno `DB_PASSWORD`.

## Ejecutar el proyecto

Desde la carpeta principal, utilizando PowerShell:

```powershell
$env:DB_PASSWORD="TU_CONTRASEÑA_MYSQL"
.\mvnw.cmd spring-boot:run
```

El servidor se ejecutará en `http://localhost:8080`.

## Pruebas

Ejecutar las pruebas automatizadas:

```powershell
.\mvnw.cmd test
```

Las pruebas actuales cubren usuarios, cursos, seguridad JWT, pagos únicos,
Checkout de suscripción, webhooks firmados y persistencia aislada de eventos
Stripe.

## Estado actual

Backend académico con usuarios, cursos, pagos únicos y suscripciones mensuales
implementados. La renovación y cancelación automática de suscripciones
(`invoice.paid` y `customer.subscription.deleted`) son extensiones pendientes.
La activación de suscripciones con una sesión de Stripe Sandbox fue verificada
manualmente. El proyecto continúa en desarrollo.
