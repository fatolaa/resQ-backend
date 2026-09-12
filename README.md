# resQ-backend

Backend del sistema ResQ: registro de mascotas perdidas, encontradas y abandonadas.

## Requisitos

- **Java 17** , siempre verificando con `java -version`
- Git
- Maven viene incluido: se usa `mvnw.cmd`  o `./mvnw` 

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

### De donde saco la contraseña

En Aiven:

1. Abre tu servicio `mysql-167744fb` → **Service settings**.
2. Ve a la seccion **Service users**.
3. En la fila de tu usuario , usa el boton **Copiar** que genera la cadena
   `avnadmin:TU_CLAVE_AQUI` — la parte despues de los dos puntos es tu `DB_PASSWORD`.

## Como correr el backend

En la carpeta del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

Eso significa que conecto a la base de datos correctamente. La API queda en
`http://localhost:8080`.

Si en su lugar aparece un error de conexion o `access denied`, revisa que la
`DB_PASSWORD` este bien definida en esa misma terminal.

## Seguridad

- **NUNCA** subas la contrasena al repositorio: GitHub bloquea el push cuando detecta
  secretos.
- Prefiere dar a cada integrante la contrasena fuera del repositorio y que la defina en su entorno con `setx`.