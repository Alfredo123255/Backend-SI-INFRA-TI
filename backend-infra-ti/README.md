# Backend INFRA-TI

## Acceso mediante usuarios, roles y permisos

La API utiliza Spring Security, contraseñas BCrypt y una sesión del servidor identificada por la cookie `JSESSIONID` (HttpOnly, SameSite=Lax). No utiliza JWT. La sesión se invalida al cerrar sesión y también cuando se detecta que su usuario fue desactivado. Los permisos se vuelven a consultar en cada solicitud, por lo que una sesión abierta no conserva permisos revocados.

### Preparación y primer administrador

1. En PostgreSQL aplica `ETL_INFRA_TI/migrations/004_usuarios_roles.sql` sobre la misma base que usa Java. La migración crea `rol`, `usuario` y los tres roles iniciales; no crea usuarios. Hibernate usa `ddl-auto=validate`, por lo que no sustituye esta migración. En la base local `postgres` esta migración ya fue aplicada durante la implementación.
2. Desde la carpeta `backend-infra-ti`, prepara `.env` tomando `.env.example` como referencia. Conserva las credenciales locales de PostgreSQL y la configuración existente de integración con FastAPI. No sobrescribas un `.env` que ya funciona.
3. Solo para la primera ejecución, define en ese `.env` privado `ADMIN_CORREO`, `ADMIN_NOMBRE` y `ADMIN_CONTRASENA`, o suminístralos como variables del proceso. Escoge tú los valores; la contraseña debe tener al menos ocho caracteres y como máximo 72 bytes UTF-8, que es el límite de BCrypt. Ninguna contraseña inicial viene en el repositorio.
4. Con Java 21, ejecuta `./mvnw.cmd spring-boot:run` desde `backend-infra-ti`. Si `usuario` está vacía y las tres variables tienen contenido, se crea el administrador con la contraseña cifrada mediante BCrypt. Si falta alguna variable o ya existe un usuario, no se crea ninguno. Un correo o contraseña inválidos en la configuración inicial impiden completar el arranque y deben corregirse.
5. Abre el frontend en `http://localhost:5173/login` e ingresa con el correo y la contraseña que definiste. Desde Administrar acceso puedes crear los demás usuarios y editar los nueve permisos de cualquier rol.
6. Después del primer arranque elimina las tres variables `ADMIN_*` del `.env` o del entorno del proceso. El usuario ya queda guardado y no depende de ellas para iniciar sesión.

`.env` está excluido de Git. No incluyas contraseñas, hashes, claves de integración ni capturas de estos valores en archivos versionados. En un despliegue HTTPS define `SESSION_COOKIE_SECURE=true`.

### Contrato de autenticación

- `GET /api/auth/csrf`: prepara la cookie `XSRF-TOKEN` y devuelve su token. Se permite sin sesión.
- `POST /api/auth/login`: recibe `correo` y `contrasena` (también admite `contraseña`); devuelve los datos de sesión. Correo o contraseña incorrectos: 401. Usuario inactivo: 403 con un mensaje diferente.
- `GET /api/auth/me`: devuelve `id`, `nombre`, `correo`, `rol: { id, nombre }` y los nombres de los permisos habilitados. Sin sesión devuelve 401 JSON, sin redirección.
- `POST /api/auth/logout`: invalida la sesión y devuelve 204.

Las solicitudes POST, PUT, PATCH y DELETE, incluido el login, deben enviar el valor de la cookie `XSRF-TOKEN` en `X-XSRF-TOKEN`. Tras login y logout el token cambia; solicita uno nuevo. El frontend gestiona este flujo. En Postman activa su cookie jar, ejecuta primero `GET /api/auth/csrf`, copia el token al encabezado y conserva `JSESSIONID` entre solicitudes.

`FRONTEND_ORIGINS` define los orígenes permitidos con credenciales; deben ser orígenes concretos, sin comodín. Usa el mismo hostname para front y back (por ejemplo, ambos `localhost`); las cookies de `localhost` y `127.0.0.1` son distintas. En desarrollo Vite dirige `/api` al backend del mismo origen del navegador.

### Administración

| Ruta | Operación | Permiso |
| --- | --- | --- |
| `/api/usuarios` | GET, POST | administrar_usuarios |
| `/api/usuarios/{id}` | PUT (nombre y rol, sin modificar correo) | administrar_usuarios |
| `/api/usuarios/{id}/estado` | PATCH `{ "activo": true/false }` | administrar_usuarios |
| `/api/roles` | GET, POST | administrar_usuarios |
| `/api/roles/{id}/permisos` | PUT con los nueve booleanos | administrar_usuarios |
| `/api/roles/{id}` | DELETE, solo si no tiene usuarios | administrar_usuarios |

La creación de un usuario recibe `nombre`, `correo`, `contrasenaTemporal` y `rol_id` (también admite `rolId`). El correo se normaliza a minúsculas. No existe DELETE de usuarios ni se devuelven hashes en las respuestas. Un rol nuevo comienza con `ver_inventario` y los otros ocho permisos desactivados.

No hay un bloqueo especial por nombre para Administrador: se rechaza con 409 cualquier cambio de rol, desactivación o modificación de permisos que deje al sistema sin un usuario activo con `administrar_usuarios`. Las escrituras se serializan con un bloqueo transaccional PostgreSQL para sostener esta regla ante solicitudes simultáneas. Tampoco se permite desactivar al usuario de la propia sesión ni eliminar roles con usuarios asignados.

Los GET de inventario requieren `ver_inventario`; las escrituras de conexiones, datos administrativos, mantenimiento, baja, umbrales y ubicaciones exigen su permiso correspondiente. Una sesión válida sin permiso recibe 403. `exportar` se aplica en el navegador porque los reportes se generan allí.

### Pruebas

`AccesoSeguridadTest` prueba autenticación, sesión, CSRF, CORS, revocación de permisos, los roles iniciales, baja por Operador, restricciones 409 y bootstrap. `ManejoGlobalExcepcionesTest` conserva sus comprobaciones de errores usando una sesión con permiso de lectura.

Ejecuta la suite contra una base de pruebas con el esquema y migraciones aplicados, definiendo `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` fuera del código. Para los casos del último administrador esa base debe empezar sin usuarios. Deja vacías las tres variables `ADMIN_*` durante las pruebas y ejecuta `./mvnw.cmd test`. Las pruebas de acceso generan sus contraseñas al ejecutarse y revierten sus datos con transacciones.

Referencias: [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [sesiones](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html) y [CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html) de Spring Security.


## Alertas durante mantenimiento y baja

El inicio de mantenimiento y la baja cierran administrativamente las alertas abiertas en la misma transacción de la acción. Se conserva su causa y se añade el motivo de cierre; no se modifica la última lectura ni se declara una recuperación. El historial distingue estos eventos con `cierre_alerta_<campo>` y el frontend los muestra como «Cierre de alerta / Cerrada».

Al terminar mantenimiento se notifica al ETL una lectura inmediata, manteniendo la frecuencia configurada. Si la condición SNMP persiste en una medición posterior, el ETL abre otro episodio con un nuevo identificador y fecha. EOS mantiene su evaluación por calendario. La baja no reactiva el monitoreo. Esta política utiliza las tablas existentes, sin migraciones adicionales.
