# resQ-backend

Backend del sistema ResQ: registro de mascotas perdidas, encontradas y abandonadas.

## Requisitos

- **Java 17** (tambien funciona con **Java 21**), siempre verificando con `java -version`
- Git
- Maven viene incluido: se usa `mvnw.cmd`  o `./mvnw` 

> **Ojo con Java 22 o superior:** el backend compila y arranca, pero los tests
> fallan (`Java 25 (69) is not supported by the current version of Byte Buddy`),
> porque la version de Mockito que trae Spring Boot 3.2.5 solo soporta hasta Java 22.
>
> Si tienes varios JDK instalados, apunta `JAVA_HOME` al 17 o 21 solo para esa terminal
> (ajusta la ruta a donde lo tengas instalado):
>
> ```powershell
> $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot"
> $env:Path = "$env:JAVA_HOME\bin;$env:Path"
> java -version
> ```

## Configuración de la base de datos

El backend se conecta a una base de datos **MySQL en la nube**. Lee los datos de
conexion desde variables de entorno:

| Variable | Valor por defecto | Descripcion |
|---|---|---|
| `DB_URL` | `jdbc:mysql://mysql-167744fb-resq-project.i.aivencloud.com:23250/resq_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC` | URL de conexion |
| `DB_USERNAME` | `avnadmin` | Usuario de la base de datos |
| `DB_PASSWORD` | *(vacio)* | Contrasena de la base de datos |

Solo es **obligatorio** definir `DB_PASSWORD`. Lo demas ya apunta a la nube.

## Como configurar DB_PASSWORD super facil en Windows

### Opcion 1: Solo para la terminal actual 

Abre **PowerShell**, pega, y presiona Enter:

```powershell
$env:DB_PASSWORD = "AVNS_xxxxxxxxxxxx"
```

> La variable solo vive en esa ventana. Cierra la terminal y se pierde.

### Opcion 2: Permanente 

```powershell
setx DB_PASSWORD "AVNS_xxxxxxxxxxxx"
```

**IMPORTANTE:** despues de `setx`, **cierra y vuelve a abrir la terminal**
para que el cambio haga efecto. Luego corre el backend.

### Opcion 3: CMD 

```cmd
set DB_PASSWORD=AVNS_xxxxxxxxxxxx
```

(sesion) o `setx DB_PASSWORD "AVNS_xxxxxxxxxxxx"` 

### En macOS / Linux

```bash
export DB_PASSWORD="AVNS_xxxxxxxxxxxx"
./mvnw spring-boot:run
```

> Para que sea permanente, agrega la linea `export` a tu `~/.bashrc` o `~/.zshrc`.

### De donde saco la contraseña

Si no tienes acceso a la cuenta de Aiven del proyecto, **pidesela a un integrante
del equipo** que si lo tenga.

En Aiven:

1. Abre tu servicio `mysql-167744fb` → **Service settings**.
2. Ve a la seccion **Service users**.
3. En la fila de tu usuario , usa el boton **Copiar** que genera la cadena
   `avnadmin:TU_CLAVE_AQUI` — la parte despues de los dos puntos es tu `DB_PASSWORD`.

> El plan gratuito de Aiven **apaga el servicio** cuando no se usa por un tiempo.
> Si el backend no logra conectarse (ver [Problemas comunes](#problemas-comunes)),
> entra a la consola de Aiven y enciende el servicio con **Power on**.

## Como correr el backend

En la carpeta del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

Cuando en la consola aparezca una linea como esta:

```text
Started ResqBackendApplication in 10.23 seconds
```

significa que conecto a la base de datos correctamente. La API queda en
`http://localhost:8080` (puedes probar abriendo `http://localhost:8080/api/reportes`).

Si en su lugar aparece un error, revisa [Problemas comunes](#problemas-comunes).

## Problemas comunes

| Error en la consola | Que significa | Solucion |
|---|---|---|
| `UnknownHostException: No such host is known (mysql-...aivencloud.com)` | El servicio de Aiven esta apagado | Enciendelo desde la consola de Aiven (**Power on**) y espera unos minutos, o usa una [base local con Docker](#opcional-base-de-datos-local-con-docker) |
| `Access denied for user 'avnadmin'` | La contrasena esta mal o no esta definida | Revisa que `DB_PASSWORD` este bien definida **en esa misma terminal** |
| `Port 8080 was already in use` | Ya hay otra app (u otro backend) en el puerto 8080 | Cierra esa app, o levanta en otro puerto con `$env:PORT = "8081"` |
| Los tests fallan con `Byte Buddy` / `Mockito cannot mock this class` | Estas usando Java 22 o superior | Usa Java 17 o 21 (ver [Requisitos](#requisitos)) |

## (Opcional) Base de datos local con Docker

Si la base de la nube no esta disponible, o prefieres trabajar sin tocar los datos
del equipo, puedes levantar un MySQL local con [Docker](https://www.docker.com/products/docker-desktop/).
Al arrancar, el backend crea las tablas solo (`ddl-auto=update`) y `data.sql` carga
los catalogos y la cuenta de administracion demo.

1. Crea el contenedor (solo la primera vez):

   ```powershell
   docker run -d --name resq-mysql -e MYSQL_ROOT_PASSWORD=resq -e MYSQL_DATABASE=resq_db -p 3307:3306 mysql:8.0
   ```

   Se usa el puerto `3307` para no chocar con un MySQL que ya tengas instalado en el `3306`.
   Las siguientes veces basta con `docker start resq-mysql`.

2. En la terminal donde vas a correr el backend, apunta las variables a esa base:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3307/resq_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "resq"
   .\mvnw.cmd spring-boot:run
   ```

> La base local arranca vacia: solo tiene la cuenta demo, no los reportes del equipo.

## Otras variables de entorno (opcionales)

No hace falta definirlas para desarrollar: todas tienen un valor por defecto.

| Variable | Valor por defecto | Para que sirve |
|---|---|---|
| `PORT` | `8080` | Puerto del backend |
| `JWT_SECRET` | clave de desarrollo | Firma de los tokens de sesion. En produccion **hay que** definir una propia (32+ bytes) |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Duracion de la sesion |
| `FOTOS_DIR` | `uploads/reportes` | Carpeta donde se guardan las fotos de los reportes |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | *(vacio)* | Servidor SMTP. Sin `SPRING_MAIL_HOST` no se envian correos: solo se escriben en el log |
| `MAIL_FROM` | `no-reply@resq.local` | Remitente de los correos |

## Tests

```powershell
.\mvnw.cmd test
```

- Requieren **Java 17 o 21** (ver [Requisitos](#requisitos)).
- Usan la misma configuracion de base de datos que el backend: **por defecto se
  conectan a la base compartida del equipo** (no cargan `data.sql`, pero si pueden
  actualizar el esquema). Si no quieres tocarla, define antes las variables de la
  [base local con Docker](#opcional-base-de-datos-local-con-docker).

## Cuenta de administracion (demo)

El panel de administracion exige rol `ADMIN`, asi que al levantar el proyecto
por primera vez se crea una cuenta de demostracion en `src/main/resources/data.sql`:

| Campo | Valor |
|---|---|
| Correo | `admin.demo@resq.com` |
| Contrasena | `Admin123` |

Sirve para entrar al panel sin tener que crear nada a mano. Se inserta con
`INSERT IGNORE`, asi que solo aparece la primera vez y nunca pisa una cuenta que
ya exista.

El correo lleva `.demo` a proposito. En la base compartida del equipo ya hay un
`admin@resq.com` de una carga inicial anterior, guardado con el rol
`ADMINISTRADOR` y un hash de ejemplo que no corresponde a ninguna clave: ese
correo **no** sirve para entrar al panel.

Es una credencial **publica y fija a proposito**: el hash BCrypt esta en el
repositorio y cualquiera que clone el proyecto puede entrar con ella. Borra esa
fila de `data.sql` o genera otro hash antes de usar el proyecto fuera del taller.

## Seguridad

- **NUNCA** subas la contrasena al repositorio: GitHub bloquea el push cuando detecta
  secretos.
- La unica excepcion es el `admin.demo@resq.com` de demostracion de arriba, que esta
  en el repositorio a proposito. No lo tomes como ejemplo para una clave real.
- Prefiere dar a cada integrante la contrasena fuera del repositorio y que la defina en su entorno con `setx`.