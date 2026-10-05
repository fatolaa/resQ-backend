INSERT IGNORE INTO tipo_caso (codigo, nombre, orden) VALUES
  ('PERDIDA',    'Perdida',    1),
  ('ENCONTRADA', 'Encontrada', 2),
  ('ABANDONADA', 'Abandonada', 3);

INSERT IGNORE INTO estado_reporte (codigo, nombre, orden) VALUES
  ('PENDIENTE',  'Pendiente',  1),
  ('EN_PROCESO', 'En proceso', 2),
  ('RESUELTO',   'Resuelto',   3),
  ('CANCELADO',  'Cancelado',  4);

INSERT IGNORE INTO rol (codigo, nombre, orden) VALUES
  ('USUARIO',    'Usuario',    1),
  ('VOLUNTARIO', 'Voluntario', 2),
  ('ADMIN',      'Admin',      3);

-- Normaliza los roles que una carga inicial antigua dejo fuera del dominio de
-- arriba. Son filas que ninguna version de este archivo sembro: quedaron
-- cargadas por fuera del repositorio, y por eso sus password_hash no son
-- BCrypt valido (28 caracteres, cuando BCrypt siempre mide 60).
--
-- 'ADMINISTRADOR' es el nombre largo de 'ADMIN'. Con ese valor las cuentas no
-- entraban al panel (que exige hasRole("ADMIN")) y tampoco se podian editar: las
-- funciones administrativas son admin-only, y al devolverles un rol que no esta
-- en ROLES_VALIDOS el PUT/PATCH terminaba en 400.
--
-- 'CIUDADANO' es solo otra forma de decir 'USUARIO', y el frontend ya la
-- trataba como tal (esGestorDeReportes("CIUDADANO") era false, o sea que se
-- comportaba como un usuario comun). Mapearla no cambia ningun permiso, solo
-- deja el valor dentro del dominio.
--
-- Ambos UPDATE son idempotentes: en la segunda corrida no queda ninguna fila que
-- actualizar.
UPDATE usuario SET rol = 'ADMIN'   WHERE rol = 'ADMINISTRADOR';
UPDATE usuario SET rol = 'USUARIO' WHERE rol = 'CIUDADANO';

-- No hay un DELETE para 'ORGANIZACION' a proposito, y no debe agregarse. Ese rol
-- existio en una carga manual antigua (hashes que decian literalmente
-- "ejemploHashOrg001", y direcciones de Bogota en un proyecto de Cochabamba), y
-- sus 3 cuentas se borraron de la base compartida el 2026-10-05. Ese DELETE fue
-- una limpieza puntual y no va aca: este archivo corre en CADA arranque, asi que
-- un DELETE dejaria sin avisar cualquier cuenta de organizacion que alguien
-- creara mas adelante. Si vuelve a aparecer rol='ORGANIZACION', es dato nuevo y
-- hay que decidir que hacer con el.

-- Cuenta de administracion para poder entrar al panel (HU-23). El panel exige
-- rol 'ADMIN', asi que sin esta fila ninguna base recien creada tiene con quien
-- probarlo.
--
-- El correo lleva ".demo" a proposito, para que no se confunda con una cuenta
-- real. Antes justificaba el ".demo" que en la base compartida hubiera un
-- admin@resq.com de una carga inicial antigua, con un password_hash de ejemplo
-- que no era un BCrypt; esas cuentas de relleno se borraron el 2026-10-05, pero
-- el nombre queda para que la credencial publica de abajo no parezca real.
--
-- ATENCION: la clave es fija y publica a proposito, es una credencial de
-- demostracion. El "password_hash" es un BCrypt de 'Admin123'. Borra esta fila
-- o cambia el hash antes de usar esto fuera del taller.
--
-- El IGNORE evita el error de email duplicado en los arranques siguientes, ya
-- que este archivo corre en cada inicio; el id se lo deja a AUTO_INCREMENT
-- porque otras cuentas pueden estar usando los ids bajos. Nunca se actualiza una
-- fila existente: si alguien cambia la clave de esta cuenta, no se revierte al
-- reiniciar.
INSERT IGNORE INTO usuario (nombre, email, telefono, password_hash, rol)
  VALUES ('Admin Demo', 'admin.demo@resq.com', '70000000',
          '$2a$10$0XM0wP7eGmey4roTHNAmc.768bHK/d7YECbYpQMnyhqFpWmmg2dNu', 'ADMIN');

-- Coordenadas de demonstracion para que el mapa de casos (HU-14) tenga pines.
-- Puntos aproximados de zonas del area metropolitana de Cochabamba.
-- La guarda "AND latitud IS NULL" evita pisar la ubicacion que un usuario
-- haya cargado despues, ya que este archivo corre en cada arranque.
UPDATE reporte SET latitud = -17.389200, longitud = -66.156700
  WHERE id_reporte = 1 AND latitud IS NULL;   -- Plaza 14 de Septiembre
UPDATE reporte SET latitud = -17.391300, longitud = -66.163700
  WHERE id_reporte = 2 AND latitud IS NULL;   -- Parque Urbano Central
UPDATE reporte SET latitud = -17.399000, longitud = -66.164300
  WHERE id_reporte = 3 AND latitud IS NULL;   -- Mercado Los Pozos
UPDATE reporte SET latitud = -17.401400, longitud = -66.152000
  WHERE id_reporte = 4 AND latitud IS NULL;   -- Terminal de micros, Calicanto
UPDATE reporte SET latitud = -17.397200, longitud = -66.156000
  WHERE id_reporte = 6 AND latitud IS NULL;   -- Castillo de la Recoleta
UPDATE reporte SET latitud = -17.393500, longitud = -66.162000
  WHERE id_reporte = 7 AND latitud IS NULL;   -- Hospital San Pedro
UPDATE reporte SET latitud = -17.407900, longitud = -66.172000
  WHERE id_reporte = 8 AND latitud IS NULL;   -- UMSS, campus central
UPDATE reporte SET latitud = -17.414000, longitud = -66.170000
  WHERE id_reporte = 9 AND latitud IS NULL;   -- Tiquipaya, zona Sur
UPDATE reporte SET latitud = -17.354000, longitud = -66.148000
  WHERE id_reporte = 10 AND latitud IS NULL;  -- Sacaba, zona Norte
UPDATE reporte SET latitud = -17.409000, longitud = -66.193000
  WHERE id_reporte = 11 AND latitud IS NULL;  -- Blanco Galindo, Quillacollo