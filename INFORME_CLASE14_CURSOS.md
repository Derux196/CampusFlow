# Informe clase 14: gestión de cursos y relaciones académicas

**Proyecto:** CampusFlow
**Tecnologías:** Java 25, Spring Boot 4.1.1, Spring Data JPA, MySQL, Spring
Security, JWT y Maven
**Alcance:** gestión de cursos, asignación de instructores e inscripción segura de
estudiantes.

## Funcionalidades desarrolladas

Se completó el módulo académico de cursos sin modificar el modelo de campos
`titulo` y `descripcion`, sin tocar la lógica JWT y sin modificar datos de MySQL.

- Creación administrativa de cursos con validación Jakarta.
- Consulta pública de listado y detalle de cursos.
- Asignación administrativa de un instructor existente.
- Autoinscripción de estudiantes autenticados.
- Consulta administrativa de cursos por instructor y por estudiante.
- Respuestas DTO que no serializan entidades JPA ni datos sensibles.
- Prevención de inscripciones duplicadas.
- Consultas con `join fetch` del instructor para evitar N+1 al producir listas de
  respuestas.

## Archivos modificados y creados

| Estado | Ruta completa | Cambio |
|---|---|---|
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\cursos\controller\CursoController.java` | Añade creación, detalle, asignación de instructor, autoinscripción y consultas por relación. La lista pública se conserva. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\cursos\service\CursoService.java` | Implementa reglas de negocio, transacciones, validación de roles y transformación a DTO. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\cursos\repository\CursoRepository.java` | Añade consultas de cursos con instructor cargado y comprobación de inscripción existente. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\cursos\dto\CrearCursoRequest.java` | DTO de entrada con `titulo` obligatorio y `descripcion` opcional, ambos con límites de longitud. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\cursos\dto\CursoResponse.java` | DTO de salida con datos del curso e instructor, sin entidades, contraseñas ni listas de estudiantes. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\cursos\mapper\CursoMapper.java` | Conversión explícita de `Curso` a `CursoResponse`. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\config\SecurityConfig.java` | Define permisos para creación, asignación, autoinscripción y consultas relacionales de cursos. Conserva las rutas públicas existentes. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\common\exception\GlobalExceptionHandler.java` | Traduce la inscripción duplicada a HTTP 409 con un mensaje seguro. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\main\java\com\devsenior\Campusflow\common\exception\InscripcionDuplicadaException.java` | Excepción de dominio para impedir una inscripción repetida. |
| Modificado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\test\java\com\devsenior\Campusflow\cursos\service\CursoServiceTest.java` | Pruebas unitarias de reglas de negocio y relaciones. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\src\test\java\com\devsenior\Campusflow\cursos\controller\CursoControllerTest.java` | Pruebas MVC de permisos, validación y códigos HTTP. |
| Creado | `C:\Users\mello_p4jcvg6\Documents\GitHub\CampusFlow\INFORME_CLASE14_CURSOS.md` | Este informe. |

No se modificaron las entidades `Curso` ni `Usuario`, los campos del curso ni el CRUD
de usuarios como parte de esta etapa.

## Endpoints

Todas las rutas están bajo `/api/cursos`.

| Método | Ruta | Permiso | Entrada | Respuesta correcta | Errores relevantes |
|---|---|---|---|---|---|
| `GET` | `/api/cursos` | Público | Sin cuerpo | Lista de `CursoResponse`, 200 | — |
| `GET` | `/api/cursos/{id}` | Público | ID del curso | `CursoResponse`, 200 | 404 si no existe |
| `POST` | `/api/cursos` | `ADMINISTRADOR` | `CrearCursoRequest` | `CursoResponse`, 201 | 400, 401, 403 |
| `PUT` | `/api/cursos/{cursoId}/instructor/{instructorId}` | `ADMINISTRADOR` | IDs en ruta | `CursoResponse`, 200 | 400 por rol incorrecto; 404 por IDs inexistentes; 401/403 |
| `POST` | `/api/cursos/{cursoId}/inscripcion` | `ESTUDIANTE` | Sin cuerpo; el usuario se toma del JWT | `CursoResponse`, 200 | 400 por rol inválido; 404; 409 duplicado; 401/403 |
| `GET` | `/api/cursos/instructores/{instructorId}` | `ADMINISTRADOR` | ID de instructor | Lista de `CursoResponse`, 200 | 400 por rol incorrecto; 404; 401/403 |
| `GET` | `/api/cursos/estudiantes/{estudianteId}` | `ADMINISTRADOR` | ID de estudiante | Lista de `CursoResponse`, 200 | 400 por rol incorrecto; 404; 401/403 |

## DTOs y reglas de negocio

### Creación

`CrearCursoRequest` usa las siguientes validaciones:

```java
@NotBlank
@Size(max = 150)
private String titulo;

@Size(max = 500)
private String descripcion;
```

No se cambia el significado de los campos existentes. Un título ausente o vacío se
rechaza antes de llegar al servicio.

### Asignación de instructor

El servicio localiza el curso y el usuario. Solo se permite la asignación cuando el
usuario tiene `RolUsuario.INSTRUCTOR`; de lo contrario, se responde con 400. Los
recursos inexistentes se responden con 404.

### Política de inscripción

La política elegida es **autoinscripción exclusiva del estudiante autenticado**:

```java
return cursoService.inscribirEstudiante(
        cursoId,
        authentication.getName()
);
```

El endpoint no recibe un ID de estudiante, por lo que un estudiante no puede inscribir
a otro usuario. Spring Security exige `ROLE_ESTUDIANTE`; el servicio vuelve a validar
el rol obtenido desde la base de datos.

Antes de agregar la relación, se usa una consulta de existencia:

```java
if (cursoRepository.existsByIdAndEstudiantesId(
        cursoId,
        estudiante.getId())) {
    throw new InscripcionDuplicadaException();
}
```

Una inscripción repetida devuelve 409. Cursos o usuarios inexistentes devuelven 404.

### Respuestas y carga de relaciones

`CursoResponse` solo devuelve identificador, título, descripción e información básica
del instructor. No incluye la entidad `Usuario`, correos, contraseñas, hashes, JWT ni
la colección de estudiantes; esto evita referencias circulares y exposición de datos.

Las consultas de listado usan `left join fetch c.instructor`. La consulta por
estudiante usa `distinct` y carga el instructor en la misma consulta. Esto evita la
carga N+1 del instructor al mapear las respuestas.

Las operaciones que leen relaciones perezosas o modifican la relación muchos a muchos
están anotadas con `@Transactional`.

## Seguridad

Se preservaron las reglas existentes:

- `SessionCreationPolicy.STATELESS`.
- Filtro JWT existente sin modificaciones.
- `DispatcherType.ERROR` con `permitAll()`.
- Login y registro público bajo `/api/auth`.
- Protección administrativa de `/api/usuarios`.
- Listado público existente de cursos.

Las nuevas reglas son:

```java
.requestMatchers(HttpMethod.POST, "/api/cursos")
    .hasRole("ADMINISTRADOR")
.requestMatchers(HttpMethod.PUT, "/api/cursos/*/instructor/*")
    .hasRole("ADMINISTRADOR")
.requestMatchers(HttpMethod.POST, "/api/cursos/*/inscripcion")
    .hasRole("ESTUDIANTE")
```

Las consultas relacionales por ID se limitaron a administradores para no revelar
arbitrariamente las matrículas o cargas académicas de otras personas.

## Pruebas realizadas

### Pruebas focalizadas

Se ejecutaron las pruebas MVC y de manejo de excepciones directamente afectadas:

```text
23 pruebas aprobadas, 0 fallos
```

Cobertura nueva de cursos:

- `CursoServiceTest`: 11 pruebas.
  - creación, detalle e inexistentes;
  - asignación válida e instructor inválido;
  - autoinscripción, duplicado y rol no estudiante;
  - consultas por instructor y estudiante.
- `CursoControllerTest`: 12 pruebas.
  - acceso público al listado y detalle, incluido 404 para un curso inexistente;
  - 401 sin autenticación para creación e inscripción;
  - 403 para estudiante al crear;
  - creación y asignación administrativa, incluido 404 para curso inexistente;
  - validación 400 para cursos inválidos e instructor sin el rol requerido;
  - autoinscripción de estudiante;
  - rechazo de inscripción como administrador y 404 para curso inexistente;
  - 409 por duplicado;
  - consultas administrativas por instructor y estudiante, y rechazo para estudiante.
- `GlobalExceptionHandlerTest`: 3 pruebas, incluida la respuesta 409 para una
  inscripción duplicada.
- `CursoRepositoryIntegrationTest`: 3 pruebas JPA con H2 en memoria.
  - `findByIdWithInstructor` carga el instructor mediante `join fetch`;
  - `findByEstudianteIdWithInstructor` devuelve únicamente los cursos del
    estudiante y carga el instructor;
  - `existsByIdAndEstudiantesId` detecta una matrícula existente.

### Suite Maven completa

La suite completa se ejecutó después de añadir un perfil `test` con H2 en memoria y
una clave JWT exclusiva de pruebas. `CampusflowApplicationTests` activa ese perfil,
por lo que la carga de contexto no depende de MySQL, credenciales reales ni secretos
del entorno.

```text
85 pruebas aprobadas, 0 fallos
```

No se desactivó ninguna prueba ni se modificaron los perfiles de producción. El
arranque del contexto completo queda verificado frente a H2; la integración contra
MySQL real sigue fuera del alcance de la suite automática.

## Problemas pendientes y recomendaciones

1. La validación de duplicados se realiza mediante consulta previa. Si el sistema
   procesa inscripciones concurrentes, conviene reforzarla con una restricción única
   en la base de datos y manejar su excepción de integridad como 409.
2. No se han implementado pagos, Stripe ni funcionalidades fuera de la gestión
   académica de cursos.

## Estado de Git

Instantánea tomada antes de crear este informe:

```text
 M pom.xml
 M src/main/java/com/devsenior/Campusflow/common/exception/GlobalExceptionHandler.java
 M src/main/java/com/devsenior/Campusflow/config/SecurityConfig.java
 M src/main/java/com/devsenior/Campusflow/cursos/controller/CursoController.java
 M src/main/java/com/devsenior/Campusflow/cursos/repository/CursoRepository.java
 M src/main/java/com/devsenior/Campusflow/cursos/service/CursoService.java
 M src/test/java/com/devsenior/Campusflow/cursos/service/CursoServiceTest.java
 ?? src/main/java/com/devsenior/Campusflow/common/exception/InscripcionDuplicadaException.java
 ?? src/main/java/com/devsenior/Campusflow/cursos/dto/
 ?? src/main/java/com/devsenior/Campusflow/cursos/mapper/
 ?? src/test/java/com/devsenior/Campusflow/cursos/controller/
 ?? INFORME_CLASE14_CRUD.md
```

El archivo `pom.xml` conserva una corrección previa del encabezado XML para que Maven
pueda analizar el proyecto; no introduce dependencias ni cambios de cursos.

Resumen de `git diff --stat` para archivos rastreados:

```text
 pom.xml                                            |   1 -
 .../common/exception/GlobalExceptionHandler.java   |  12 +
 .../Campusflow/config/SecurityConfig.java          |  22 ++
 .../cursos/controller/CursoController.java         |  66 ++++-
 .../cursos/repository/CursoRepository.java         |  39 +++
 .../cursos/service/CursoService.java               | 150 +++++++++++-
 .../cursos/service/CursoServiceTest.java           | 269 +++++++++++++++++++--
 7 files changed, 531 insertions(+), 28 deletions(-)
```

No se realizó `git add`, commit ni push. Los artefactos temporales o de nombre extraño
existentes en el directorio se excluyeron deliberadamente de este informe y no se
modificaron.
