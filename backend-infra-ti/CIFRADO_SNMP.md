# Configuración del cifrado SNMP

Antes de iniciar el backend, configurar `SNMP_CREDENTIAL_KEY_FILE` con la ruta
absoluta de un archivo privado que contenga 32 bytes aleatorios codificados en
Base64 URL. También se admite `SNMP_CREDENTIAL_KEY` desde un gestor de secretos.
La misma llave debe estar disponible para FastAPI y el programador ETL. Nunca
guardarla en el repositorio, en la base de datos ni dentro de una imagen Docker.
En contenedores, montarla como secreto de solo lectura.

El servicio cifra `monitoreo_snmp.clave` y `clave_privacidad` con AES-256-GCM antes
de guardar. El usuario SNMP permanece legible y las respuestas no incluyen las
claves. El límite conservador de una clave es 158 bytes UTF-8, para respetar el
VARCHAR(255) existente. No se cambia el esquema ni se configura Hibernate para
crear o modificar tablas: la aplicación mantiene `ddl-auto=validate`.

Para convertir instalaciones previas, detener backend y programador, respaldar
de forma privada la llave y ejecutar en el repo ETL:

```text
python -m etl.migrar_credenciales
python -m etl.migrar_credenciales --verificar
```

La segunda comprobación debe informar cero pendientes. La conversión es atómica
e idempotente. Una llave perdida no puede recuperarse desde la base de datos;
cambiarla exige descifrar con la anterior y volver a cifrar con la nueva.

La edición de conexiones prueba las credenciales antes de guardar. Dentro de la
misma transacción, `ActualizacionSnmpService` emite `pg_notify` con el ID de la
conexión. PostgreSQL entrega el aviso solo después del commit; el programador
adelanta una lectura completa y conserva la frecuencia configurada. Un aviso no
certifica una lectura exitosa ni elimina por sí solo el estado Sin conexión.

Las pruebas del backend requieren una llave de prueba propia, diferente de la
de producción. La suite local se ejecutó en una base test existente sin DDL.
