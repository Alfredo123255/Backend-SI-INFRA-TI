# Integración del backend con el ETL SNMP

El backend y el ETL deben usar la misma base PostgreSQL. En el entorno local se usa `postgres` en `localhost:5432`. Antes de iniciar el backend, configura `DB_PASSWORD` y `ETL_API_KEY`; esta última debe coincidir con la clave configurada en el proceso ETL. `DB_URL` y `ETL_BASE_URL` son opcionales y permiten cambiar las direcciones. El script `iniciar_sistema_local.py` del workspace pasa estas variables a ambos servicios.

El backend expone dos operaciones para el frontend:

## Probar una conexión SNMPv3

`POST http://localhost:8080/api/conexiones-snmp/probar`

```json
{
  "ipGestion": "127.0.0.12",
  "usuario": "monitor_bd_demo",
  "clave": "clave-del-agente",
  "clavePrivacidad": null
}
```

Devuelve `ok`, datos identificados del equipo y el tiempo de respuesta. Si la prueba SNMP falla, el ETL devuelve `ok: false` con `errorTipo` y `mensaje`.

## Registrar la conexión y crear el activo

`POST http://localhost:8080/api/conexiones-snmp`

```json
{
  "ipGestion": "127.0.0.12",
  "usuario": "monitor_bd_demo",
  "clave": "clave-del-agente",
  "clavePrivacidad": null,
  "frecuenciaActualizacion": 300,
  "clusterId": "CLUSTER-LAB-LOCAL"
}
```

`frecuenciaActualizacion` está en segundos. El clúster debe existir en la BD y tener datacenter. El backend guarda la conexión como `Inactivo` y envía su ID junto con el ID del clúster al ETL. Al crear el activo, el ETL vincula la conexión, la cambia a `Activo` y responde `201` con el ID del activo, componentes, métricas y eventos iniciales. Ante un error confirmado, se elimina la conexión recién creada si sigue sin vincular; si el resultado es incierto y ya existe un vínculo, la conexión se conserva para evitar duplicados.

Las claves SNMP se leen desde la BD para el monitoreo posterior. El backend no necesita conocer el tipo de activo: el ETL detecta servidor, chasis, storage o switch a partir del agente SNMP.

### Editar la conexión vinculada a un activo

`GET http://localhost:8080/api/conexiones-snmp/activos/{activoId}` devuelve el ID de la
conexión, IP, usuario, frecuencia, estado y fecha de la última lectura. Nunca devuelve
la clave de autenticación ni la de privacidad.

`PATCH http://localhost:8080/api/conexiones-snmp/activos/{activoId}` actualiza esa misma
conexión, sin crear otro activo. Ejemplo:

```json
{"ipGestion":"127.0.0.11:16100","usuario":"monitor_dl380","frecuenciaActualizacion":300}
```

`clave` es opcional: si falta o está vacía, se conserva la almacenada. Un cambio de IP,
usuario o clave se prueba por SNMP antes de guardar; si falla, la conexión permanece intacta.
Al cambiar la IP, también se actualiza `activo.ip_gestion`. El estado de la conexión lo
gestionan el ETL y la baja del activo, por lo que esta ruta no lo modifica.

## Clústeres

`GET http://localhost:8080/api/clusters` lista todos los clústeres ordenados por nombre.

`POST http://localhost:8080/api/clusters` crea un clúster:

```json
{
  "nombre": "CLUSTER-LAB-02",
  "ambiente": "QA",
  "datacenter": "DC-LAB-LOCAL"
}
```

`ambiente` admite `PRODUCCION`, `QA` o `DESARROLLO` (sin distinguir mayúsculas al recibirlo). El datacenter debe existir. El alta devuelve `201`; un nombre repetido devuelve `409`; un datacenter inexistente devuelve `404`.

## Editar datos administrativos de cualquier activo

`PATCH http://localhost:8080/api/activos/{id}/administrativos`

```json
{
  "responsable": "Equipo de Infraestructura",
  "ordenCompra": "OC-2026-001",
  "fechaEos": "2030-12-31",
  "fechaSoporteSo": null,
  "tipoRed": null,
  "modoOperacion": null,
  "iops": null
}
```

`responsable`, `ordenCompra` y `fechaEos` aplican a servidor (rack o blade), chasis, storage y switch. `fechaSoporteSo` aplica solo a servidores; `tipoRed` y `modoOperacion` solo a switches; `iops` nominal solo a storage. Enviar un campo no nulo incompatible con el tipo devuelve `400`.

Solo se actualizan los campos no nulos. Los omitidos o enviados como `null` conservan su valor previo. Se aceptan también los nombres de BD `orden_compra`, `fecha_eos`, `fecha_soporte_so`, `tipo_red` y `modo_operacion`. La respuesta devuelve los datos administrativos actuales. Hostname, estado, IP, componentes, métricas y `ultima_actualizacion` siguen bajo control del ETL.

## Modo mantenimiento

`PATCH http://localhost:8080/api/activos/{id}/mantenimiento`

```json
{
  "mantenimientoActivo": true,
  "responsable": "Equipo de Operaciones",
  "motivo": "Cambio de fuente de poder"
}
```

Envía `false` para finalizar el mantenimiento. `mantenimientoActivo` es obligatorio; `responsable` y `motivo` son opcionales y quedan en la descripción del evento. La respuesta contiene `activoId`, `mantenimientoActivo` y `cambioRegistrado`. Una solicitud repetida con el mismo valor no genera otro evento. Un activo en estado `Baja` no puede entrar en mantenimiento. Esta operación no cambia `estado_operativo`, `ultima_actualizacion` ni las métricas; el programador SNMP continúa ejecutándose. La supresión de alertas debe leer `mantenimiento_activo` cuando se implemente.
