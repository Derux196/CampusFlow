# Informe clase 14: CRUD administrativo de usuarios

**Proyecto:** CampusFlow
**Tecnologías:** Java 25, Spring Boot 4.1.1, Spring Security, JWT, JPA/MySQL y Maven
**Alcance:** implementación y validación del CRUD administrativo de usuarios de la
clase 14 del módulo 4 de DevSenior.

## 1. Resumen del trabajo

El objetivo fue completar las operaciones administrativas sobre usuarios sin romper la
autenticación JWT ya validada, sin duplicar el registro público de `/api/auth` y sin
exponer contraseñas ni hashes.

Se completaron las operaciones para crear, consultar por identificador, actualizar y
eliminar usuarios desde `/api/usuarios`. La eliminación adopta una política segura:
un usuario que todavía participa en relaciones académicas no se elimina.

También se separó explícitamente el alta administrativa del registro público:

- El registro público sigue permitiendo solamente `ESTUDIANTE` e `INSTRUCTOR`.
- La creación bajo `/api/usuarios` queda protegida para `ADMINISTRADOR` y admite los
  roles que correspondan a una operación administrativa.

## 2. Archivos modificados y creados

### Cambios funcionales del CRUD

| Estado | Ruta completa | Cambio concreto |
|---|---|---|
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\usuarios\controller\UsuarioController.java` | Se añadieron `POST`, `GET /{id}`, `PUT /{id}` y `DELETE /{id}`. La lista existente se conservó. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\usuarios\service\UsuarioService.java` | Se añadieron creación administrativa, consulta por ID y eliminación transaccional; se extrajo la creación común para conservar BCrypt y la comprobación de correo duplicado. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\usuarios\model\Usuario.java` | Se añadió el acceso de lectura de `cursosInscritos`, necesario para impedir una eliminación insegura. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\common\exception\GlobalExceptionHandler.java` | Se añadió la conversión de una eliminación bloqueada por relaciones académicas a HTTP 409 sin detalles internos. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\common\exception\UsuarioConRelacionesException.java` | Excepción de dominio para representar que un usuario mantiene relaciones académicas. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\test\java\com\devsenior\Campusflow\usuarios\service\UsuarioServiceTest.java` | Cobertura de creación administrativa, consulta, eliminación permitida y bloqueo por cursos dictados o inscritos. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\test\java\com\devsenior\Campusflow\common\exception\GlobalExceptionHandlerTest.java` | Cobertura del mapeo de `UsuarioConRelacionesException` a 409. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\test\java\com\devsenior\Campusflow\usuarios\controller\UsuarioControllerTest.java` | Pruebas MVC de CRUD, validaciones, respuestas HTTP, ausencia de contraseña en JSON y acceso sin autenticación. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\INFORME_CLASE14_CRUD.md` | Este informe técnico de transferencia. |

### Archivos revisados y preservados

| Ruta completa | Resultado de la revisión |
|---|---|
| `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\config\SecurityConfig.java` | No se modificó. Conserva JWT sin sesión, `DispatcherType.ERROR`, rutas públicas y protección de `/api/usuarios` para administradores. |
| `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\security\AuthController.java` | No se modificó. Conserva las rutas públicas de login y registro. |
| `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\usuarios\dto\CrearUsuarioRequest.java` | Se reutiliza para la creación. Valida nombre, correo, contraseña y rol. |
| `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\usuarios\dto\ActualizarUsuarioRequest.java` | Se reutiliza para actualización. Valida nombre y permite actualizar preferencias. |
| `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\usuarios\dto\UsuarioResponse.java` | Se reutiliza como salida; no define el campo `password`. |
| `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\usuarios\mapper\UsuarioMapper.java` | Se reutiliza para todas las respuestas y evita serializar la contraseña. |

## 3. Endpoints implementados

La configuración de seguridad existente exige `ROLE_ADMINISTRADOR` en todas las rutas
`/api/usuarios` y `/api/usuarios/**`.

| Método | Ruta | Permiso | Entrada | Respuesta correcta | Códigos de error relevantes |
|---|---|---|---|---|---|
| `GET` | `/api/usuarios` | `ADMINISTRADOR` | Sin cuerpo | Lista de `UsuarioResponse`, 200 | 401 sin JWT; 403 con JWT sin rol administrador |
| `POST` | `/api/usuarios` | `ADMINISTRADOR` | `CrearUsuarioRequest`: `nombre`, `email`, `password`, `rol` | `UsuarioResponse`, 201 | 400 por validación; 409 por correo duplicado; 401/403 por seguridad |
| `GET` | `/api/usuarios/{id}` | `ADMINISTRADOR` | ID en ruta | `UsuarioResponse`, 200 | 404 si no existe; 401/403 por seguridad |
| `PUT` | `/api/usuarios/{id}` | `ADMINISTRADOR` | `ActualizarUsuarioRequest`: `nombre`, `tema`, `notificacionesActivas` | `UsuarioResponse`, 200 | 400 por validación; 404 si no existe; 401/403 por seguridad |
| `DELETE` | `/api/usuarios/{id}` | `ADMINISTRADOR` | ID en ruta | Sin cuerpo, 204 | 404 si no existe; 409 con relaciones académicas; 401/403 por seguridad |

Fragmento representativo del controlador:

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
    usuarioService.eliminarUsuario(id);
    return ResponseEntity.noContent().build();
}
```

El registro público no se duplicó. Permanece en `POST /api/auth/registro` y sigue
sujeto a la regla de roles no administrativos.

## 4. Lógica de negocio

### Validación y persistencia

- Los controladores usan `@Valid` con los DTO existentes.
- `CrearUsuarioRequest` exige nombre y correo no vacíos, correo válido, contraseña con
  longitud mínima y rol no nulo.
- `ActualizarUsuarioRequest` exige nombre no vacío y de longitud válida.
- Antes de guardar se consulta `existsByEmail`; un correo ya registrado provoca
  `EmailDuplicadoException`, gestionada como HTTP 409.
- La creación centralizada aplica `PasswordEncoder.encode(...)` antes de persistir.
  No se almacena una contraseña en texto plano.
- El mapper devuelve exclusivamente los campos definidos por `UsuarioResponse`; por
  diseño no devuelve contraseña ni hash.

### Registro público frente a creación administrativa

La regla se mantiene en el servicio:

```java
if (request.getRol() != RolUsuario.ESTUDIANTE
        && request.getRol() != RolUsuario.INSTRUCTOR) {
    throw new IllegalArgumentException(
            "Solo se permite el registro como estudiante o instructor"
    );
}
```

Esa regla sigue utilizándose desde el registro público. La operación administrativa
llama a una creación interna común, por lo que conserva la comprobación de correo y el
cifrado, pero no reaplica la limitación pública de roles.

### Eliminación segura

`Usuario` tiene dos relaciones que hacen inseguro borrar un registro de forma
automática:

- `cursosDictados`, relación inversa de `Curso.instructor`.
- `cursosInscritos`, relación inversa de `Curso.estudiantes`.

La eliminación se ejecuta dentro de una transacción y comprueba ambas colecciones:

```java
if (!usuario.getCursosDictados().isEmpty()
        || !usuario.getCursosInscritos().isEmpty()) {
    throw new UsuarioConRelacionesException();
}

usuarioRepository.delete(usuario);
```

Así se evita borrar accidentalmente un instructor asignado o un estudiante inscrito.
La excepción se convierte en 409 y no expone información técnica.

## 5. Seguridad

No se modificó la lógica JWT ni las reglas de acceso existentes.

- `SecurityConfig` conserva `SessionCreationPolicy.STATELESS`.
- `DispatcherType.ERROR` conserva `permitAll()` para permitir despachos internos de
  error y evitar que una respuesta de error autorizada se convierta erróneamente en
  401.
- El filtro JWT sigue siendo responsable de validar el token y cargar las autoridades.
- `/api/usuarios` y todas sus subrutas mantienen `hasRole("ADMINISTRADOR")`.
- Login y registro público permanecen bajo `/api/auth`.
- El registro público continúa rechazando el rol `ADMINISTRADOR`.
- Las respuestas CRUD usan `UsuarioResponse`; no exponen contraseña ni hash.

## 6. Pruebas realizadas

### Pruebas enfocadas en el cambio

Se ejecutaron las pruebas de servicio, controlador, excepción global y seguridad
directamente afectadas.

Resultado:

```text
22 pruebas aprobadas, 0 fallos
```

### Suite Maven de pruebas no dependientes del contexto completo

Comando ejecutado:

```powershell
.\mvnw.cmd "-Dtest=SecurityConfigTest,AuthControllerTest,AuthServiceTest,JwtServiceTest,GenerarHashTest,GlobalExceptionHandlerTest,UsuarioServiceTest,UsuarioControllerTest,CursoServiceTest" test
```

Resultado real:

```text
Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Cobertura observada:

- `SecurityConfigTest`: 5 pruebas aprobadas.
- `AuthControllerTest`: 6 pruebas aprobadas.
- `AuthServiceTest`: 3 pruebas aprobadas.
- `JwtServiceTest`: 2 pruebas aprobadas.
- `GlobalExceptionHandlerTest`: 2 pruebas aprobadas.
- `CursoServiceTest`: 1 prueba aprobada.
- `UsuarioControllerTest`: 8 pruebas aprobadas.
- `UsuarioServiceTest`: 12 pruebas aprobadas.
- `GenerarHashTest`: 1 prueba aprobada.

### Suite Maven completa

También se ejecutó:

```powershell
.\mvnw.cmd test
```

La suite completa no terminó correctamente. El único error identificado fue
`CampusflowApplicationTests.contextLoads`, porque el contexto por defecto no logró
construir el `DataSource`:

```text
Failed to determine a suitable driver class
```

No se cambió el código de producción ni se desactivó esta prueba para obtener éxito
artificial. En el entorno usado para esta validación no estaba activa una configuración
de base de datos apta para arrancar el contexto completo. Las pruebas MVC y unitarias
no modificaron registros reales de MySQL.

## 7. Comparación con los requisitos de CRUD de clase 14

| Requisito | Estado | Evidencia |
|---|---|---|
| Listar usuarios | Completo | `GET /api/usuarios` existente y protegido. |
| Crear usuario administrativo | Completo | `POST /api/usuarios`, 201 y BCrypt. |
| Consultar usuario por ID | Completo | `GET /api/usuarios/{id}`, 200/404. |
| Actualizar usuario | Completo | `PUT /api/usuarios/{id}`, validación y 200/400/404. |
| Eliminar usuario | Completo con protección | `DELETE /api/usuarios/{id}`, 204/404/409. |
| Evitar correos duplicados | Completo | Comprobación de repositorio y 409. |
| No exponer contraseña | Completo | DTO de respuesta y prueba MVC. |
| Rechazar accesos no autenticados | Completo | Prueba MVC de creación sin autenticación y pruebas de seguridad existentes. |
| Mantener registro público sin privilegios administrativos | Completo | Regla de roles en `registrarUsuario` y prueba existente. |
| Verificar CRUD contra MySQL real | Pendiente | No se pudo ejecutar el test de contexto completo por configuración de `DataSource`. |

## 8. Problemas conocidos y recomendaciones

1. **Prueba de contexto incompleta.** `CampusflowApplicationTests` necesita una fuente
   de datos disponible para pasar. Antes de continuar, se recomienda configurar un
   perfil de pruebas reproducible con una base aislada, o una estrategia de
   integración adecuada, sin reutilizar datos reales.
2. **Cobertura de integración de JPA pendiente.** La política de borrado seguro está
   cubierta con pruebas unitarias mediante mocks; aún no se verificó contra el esquema
   MySQL real y sus restricciones de claves foráneas.
3. **Cambios de trabajo no relacionados.** `pom.xml` contiene una modificación
   preexistente que añade Actuator. Debe revisarse separadamente antes de incluirla en
   un commit del CRUD.
4. **Utilidad de hash no rastreada.** Existe un test no rastreado para generar hashes.
   Antes de preparar un commit se recomienda decidir si pertenece al repositorio o si
   debe permanecer como herramienta local. Este informe no reproduce valores
   sensibles.
5. **Formateo y estilo.** Parte del código preexistente tiene sangría inconsistente.
   No se realizó un refactor amplio porque no forma parte de la funcionalidad CRUD.

## 9. Estado de Git

La siguiente instantánea se capturó inmediatamente antes de crear este informe. Este
propio archivo aparecerá como no rastreado hasta que se añada explícitamente al índice.
No se realizó `git add`, commit ni push.

Salida de `git status --short`:

```text
 M pom.xml
 M src/main/java/com/devsenior/Campusflow/common/exception/GlobalExceptionHandler.java
 M src/main/java/com/devsenior/Campusflow/usuarios/controller/UsuarioController.java
 M src/main/java/com/devsenior/Campusflow/usuarios/model/Usuario.java
 M src/main/java/com/devsenior/Campusflow/usuarios/service/UsuarioService.java
 M src/test/java/com/devsenior/Campusflow/common/exception/GlobalExceptionHandlerTest.java
 M src/test/java/com/devsenior/Campusflow/usuarios/service/UsuarioServiceTest.java
 ?? src/main/java/com/devsenior/Campusflow/common/exception/UsuarioConRelacionesException.java
 ?? src/test/java/com/devsenior/Campusflow/security/GenerarHashTest.java
 ?? src/test/java/com/devsenior/Campusflow/usuarios/controller/UsuarioControllerTest.java
```

Resumen de `git diff --stat` para archivos rastreados:

```text
 pom.xml                                            |   6 ++
 .../common/exception/GlobalExceptionHandler.java   |  13 ++-
 .../usuarios/controller/UsuarioController.java     |  47 ++++++++
 .../Campusflow/usuarios/model/Usuario.java         |   3 +
 .../usuarios/service/UsuarioService.java           |  47 ++++++--
 .../exception/GlobalExceptionHandlerTest.java      |  18 ++++
 .../usuarios/service/UsuarioServiceTest.java       | 119 +++++++++++++++++++++
 7 files changed, 246 insertions(+), 7 deletions(-)
```

Los archivos no rastreados no aparecen en ese resumen estadístico. Antes de un commit,
se deben revisar y añadir intencionalmente la nueva excepción, las pruebas del
controlador y este informe; se debe decidir de forma independiente sobre la utilidad
de hash existente.
