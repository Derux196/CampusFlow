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

## Integración de pagos con Stripe

CampusFlow utiliza Stripe Checkout en modo de pruebas para iniciar un pago único
desde una sesión autenticada. El backend crea una sesión Checkout asociada al
usuario mediante `client_reference_id`; Stripe procesa los datos de pago y
envía un webhook al backend cuando completa el Checkout.

El endpoint `POST /api/stripe/webhook` es público para que Stripe pueda
entregar eventos, pero valida criptográficamente el encabezado
`Stripe-Signature` antes de procesarlos. Las firmas ausentes o inválidas se
rechazan con HTTP 400.

`PagoService` registra un pago solo cuando:

- Stripe confirma `payment_status` como `paid`.
- El importe es 2000 centavos y la moneda es USD.
- La sesión contiene un `client_reference_id` numérico de un usuario existente.
- No existe ya un pago con el mismo `stripe_session_id`.

Los eventos firmados que no cumplen esas reglas se descartan de forma
controlada y no crean registros. Los pagos válidos se guardan en MySQL con el
usuario asociado, el identificador de sesión de Stripe, el importe, la moneda
y el estado `PAGADO`.

### Variables de entorno

La configuración depende de variables de entorno. No guardes sus valores en
archivos versionados:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_PRICE_PAGO_UNICO`
- `STRIPE_PRICE_PLAN_INDIVIDUAL`

Para desarrollo local, activa el perfil `dev` y configura esas variables en tu
terminal o administrador de secretos local. Los archivos `.env`, propiedades
locales y logs están excluidos de Git.

### Pruebas de pagos

Las pruebas automatizadas no requieren credenciales reales, MySQL ni llamadas
a Stripe. Para ejecutar la suite, proporciona valores ficticios temporales a
las variables de Stripe en el proceso de la terminal y ejecuta:

```powershell
.\mvnw.cmd clean test
```

La integración se desarrolló y validó únicamente con el entorno de pruebas de
Stripe. No se han realizado pagos reales y esta configuración no se declara
lista para producción.

## API REST

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/usuarios` | Lista los usuarios registrados |
| GET | `/api/cursos` | Lista los cursos registrados |
| GET | `/api/saludo` | Comprueba que el servidor responde |

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

Las pruebas actuales comprueban el listado de usuarios, el listado de cursos y la carga del contexto de Spring Boot.

## Estado actual

Primera implementación funcional del backend, con consultas de usuarios y cursos, persistencia en MySQL y pruebas automatizadas. El proyecto continúa en desarrollo.
