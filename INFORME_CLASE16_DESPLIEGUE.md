# Informe de validación — Clase 16: CampusFlow en Render, Supabase y Stripe Sandbox

**Proyecto:** CampusFlow — Java AI Backend Engineer (DevSeniorCode)  
**Fecha de las pruebas manuales:** 26 de octubre de 2026 (hora de Colombia; algunos servicios muestran UTC del 27 de octubre)  
**Repositorio:** https://github.com/Derux196/CampusFlow  
**Commit de preparación de despliegue:** `fa450a03c98a2b46bd2ab81b8161f1986e4700c3`  
**Entorno evaluado:** despliegue público de prueba académica. Stripe exclusivamente en Sandbox, sin cobros reales.

## 1. Objetivo

Comprobar el despliegue del backend Java/Spring Boot mediante Docker en Render, su conexión JDBC a PostgreSQL de Supabase y el funcionamiento básico de autenticación y pagos de prueba a través de Stripe Checkout y un webhook público firmado.

## 2. Arquitectura comprobada

```text
Cliente de prueba (PowerShell / navegador)
   | HTTPS
   v
Render — CampusFlow (Spring Boot + Docker)
   |                         ^
   | JDBC / SSL              | POST /api/stripe/webhook
   v                         |
Supabase — PostgreSQL     Stripe Sandbox
```

- Aplicación pública: https://campusflow-khqm.onrender.com
- Salud: `GET /actuator/health`
- Webhook: `POST /api/stripe/webhook`
- Base de datos: PostgreSQL gestionada por Supabase (no se trasladaron los datos del MySQL local).
- El proyecto conserva MySQL para el perfil de desarrollo local; en Render se utiliza `SPRING_PROFILES_ACTIVE=prod`.

## 3. Evidencias de las pruebas manuales

| Prueba | Evidencia observada | Resultado |
| --- | --- | --- |
| Construcción y despliegue Docker | Render indicó `Deploy succeeded | Live` desde el commit `fa450a0`; Spring Boot inició en el puerto 10000 | Aprobada |
| Disponibilidad HTTPS | `GET /actuator/health` devolvió `{"groups":["liveness","readiness"],"status":"UP"}` | Aprobada |
| Esquema PostgreSQL | En Supabase se observaron `usuarios`, `cursos`, `curso_estudiante`, `pagos`, `suscripciones`, `eventos_stripe` | Aprobada |
| Registro de usuario | `POST /api/auth/registro` creó un usuario ficticio `ESTUDIANTE` con ID 1 | Aprobada |
| Persistencia de usuario | El registro apareció en `public.usuarios`; la contraseña se almacenó en forma de hash BCrypt | Aprobada |
| Autenticación | `POST /api/auth/login` devolvió un token JWT, confirmado sin imprimirlo | Aprobada |
| Checkout único | `POST /api/stripe/checkout/pago-unico` autenticado devolvió URL válida de Stripe tras corregir el Price ID | Aprobada |
| Checkout visual | Stripe Sandbox redirigió a `/pago-exitoso.html` | Aprobada |
| Entrega webhook | Stripe Workbench mostró `checkout.session.completed`, estado entregado, HTTP 200 y respuesta `Evento recibido` | Aprobada |
| Pago persistido | `public.pagos`: registro ID 1, estado `PAGADO`, `monto_centavos=2000`, `moneda=USD`, IDs de sesión y Payment Intent | Aprobada |
| Checkout mensual en Render | `POST /api/stripe/checkout/suscripcion` devolvió URL de Stripe Sandbox y creó una fila `PENDIENTE` en `public.suscripciones`, plan `INDIVIDUAL` | Aprobada |
| Activación de suscripción | Tras Checkout Sandbox, la fila ID 1 cambió de `PENDIENTE` a `ACTIVA`, registró `fecha_activacion` y un ID `sub_...` | Aprobada |
| Trazabilidad del webhook de suscripción | `public.eventos_stripe` tiene registro ID 1, `checkout.session.completed`, estado `PROCESADO`, `stripe_event_id` `evt_...` y fecha de procesamiento | Aprobada |

**Alcance del resultado:** la evidencia confirma los flujos de pago único y activación inicial de suscripción mensual en el despliegue público de prueba. La respuesta 200 de un webhook no demostraría por sí sola persistencia; aquí se contrastó además con el registro `PAGADO` en Supabase. La captura de `pagos` no mostraba la columna `usuario_id`, por lo que la asociación exacta debe comprobarse explícitamente si se requiere para la entrega.

## 4. Incidencias y resolución

### 4.1 Error HTTP 500 al crear Checkout

En los registros de Render se observó:

```text
com.stripe.exception.InvalidRequestException:
You specified 'payment' mode but passed a recurring price.
Either switch to 'subscription' mode or use only one-time prices.
```

**Causa:** el valor de `STRIPE_PRICE_PAGO_UNICO` apuntaba a un precio recurrente de Stripe, incompatible con Checkout `mode=payment`.  
**Corrección realizada:** copiar el identificador `price_...` del producto **Acceso único (USD 20, pago único)** y actualizar la variable en Render. El despliegue posterior quedó `Live` y el siguiente Checkout se creó correctamente.

No se requirió modificar Java ni realizar otro commit por esta incidencia.

### 4.2 Tabla `eventos_stripe` sin filas tras el pago único

Se comprobó que `eventos_stripe` tenía cero registros a pesar de que el webhook devolvió HTTP 200 y `pagos` contenía el pago validado.

La inspección de `StripeWebhookController`, `PagoService`, `SuscripcionService` y sus entidades/repositorios confirmó que:

- En modo `PAYMENT`, el controlador invoca `PagoService.registrarPagoCheckout(session)`; no crea un `EventoStripe`.
- La idempotencia de pago único se basa en la comprobación de `stripe_session_id` y su restricción única en la base de datos.
- En modo `SUBSCRIPTION`, `SuscripcionService` registra eventos por `stripe_event_id` en `eventos_stripe`, con restricción de unicidad.

**Conclusión:** cero filas en `eventos_stripe` después de un pago único es el comportamiento previsto; no es un defecto que exija corrección. La recepción de un evento duplicado en la nube no fue ensayada manualmente en esta tanda de pruebas.

### 4.3 Error HTTP 500 al crear suscripción mensual

Render registró `java.lang.IllegalStateException: El precio configurado no es mensual recurrente`. La variable `STRIPE_PRICE_PLAN_INDIVIDUAL` apuntaba a un precio no mensual. Se corrigió con el identificador `price_...` de **Plan Individual (USD 10 por mes)**, se aplicó la nueva configuración en Render y el servicio volvió a `Live`.

Después se comprobó la creación de la fila local `PENDIENTE`, su transición a `ACTIVA` y el evento de suscripción `PROCESADO`. No se requirió cambio al código Java.

## 5. Seguridad y configuración

Las siguientes variables se configuraron en Render; este informe **no contiene sus valores secretos**:

| Variable | Uso |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Perfil `prod` |
| `SPRING_DATASOURCE_URL` | URL JDBC de PostgreSQL, con TLS según configuración del proyecto |
| `SPRING_DATASOURCE_USERNAME` | Usuario de PostgreSQL de Supabase |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base de datos de Supabase |
| `JWT_SECRET` | Firma de tokens JWT |
| `STRIPE_SECRET_KEY` | Clave secreta de **Stripe Sandbox** |
| `STRIPE_PRICE_PAGO_UNICO` | Price ID del pago único USD 20 |
| `STRIPE_PRICE_PLAN_INDIVIDUAL` | Price ID recurrente USD 10/mes |
| `APP_PUBLIC_BASE_URL` | `https://campusflow-khqm.onrender.com` |
| `STRIPE_WEBHOOK_SECRET` | Secreto de firma del destino público en Stripe (diferente del secreto usado por Stripe CLI) |

El destino **CampusFlow-Render-Sandbox** escucha `checkout.session.completed` en `https://campusflow-khqm.onrender.com/api/stripe/webhook`. Las credenciales, tokens, contraseñas y secretos `whsec_...` no deben añadirse a documentación, capturas publicadas ni commits.

## 6. Pruebas automatizadas y límites

Antes del despliegue, Copilot informó de una ejecución local de `./mvnw.cmd clean test` con variables Stripe **ficticias**: **99 pruebas ejecutadas, 0 fallos, 0 errores, BUILD SUCCESS**. Esa ejecución fue aislada, sin pagos reales ni acceso a bases externas.

Esta evidencia es distinta de la validación manual en Render/Supabase y no representa una prueba automatizada realizada sobre el servicio desplegado.

## 7. Estado y tareas pendientes

**Validado:** Docker en Render, endpoint de salud, esquema PostgreSQL, registro, login JWT, Checkout único y pago persistido de USD 20, Checkout mensual de USD 10, transición `PENDIENTE` → `ACTIVA`, identificador de suscripción `sub_...` y persistencia del evento `checkout.session.completed` como `PROCESADO` en Supabase.

**Pendiente o fuera de alcance de estas evidencias:**

1. Verificar de forma explícita `usuario_id` del pago si la rúbrica exige demostrar asociación.
2. Comprobar reentrega/idempotencia en nube si se solicita como evidencia específica; evitar compras duplicadas.
3. Renovaciones automáticas y cancelaciones de suscripción: extensiones no implementadas, según el alcance documentado.
4. Endurecimiento y revisión antes de cualquier uso real: configuración de seguridad, migraciones versionadas, gestión de secretos, monitoreo y preparación para producción.

## 8. Conclusión

La clase 16 cuenta con una implementación desplegada y **validación manual satisfactoria de los flujos principales de pago único y activación inicial de suscripción mensual**, desde CampusFlow en Render hasta Stripe Sandbox, recepción de webhooks y persistencia en PostgreSQL de Supabase. La aplicación no se considera lista para cobros reales, y las renovaciones/cancelaciones automáticas permanecen fuera de estas pruebas.

---

**Evidencias originales:** capturas de Render `Live`, health `UP`, Supabase `usuarios`, Supabase `pagos`, Supabase `eventos_stripe` y Stripe Workbench con webhook HTTP 200, compartidas durante la validación. Por seguridad, no se incorporan automáticamente capturas con valores sensibles a este documento público.
