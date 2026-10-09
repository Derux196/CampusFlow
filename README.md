# CampusFlow

Backend de una plataforma de gestión académica desarrollado con Java y Spring Boot.

## Tecnologías

- Java 25
- Spring Boot 4.1.1
- Spring Data JPA / Hibernate
- MySQL 8
- Maven
- JUnit y Mockito

## Arquitectura

El proyecto utiliza una arquitectura organizada por módulos funcionales:

- `usuarios`: gestión y consulta de usuarios.
- `cursos`: gestión y consulta de cursos.

Cada módulo contiene las capas `controller`, `service`, `repository` y `model`.

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
