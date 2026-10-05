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

-- Normaliza el rol de las cuentas que una carga inicial antigua dejo como
-- 'ADMINISTRADOR' (HU-23). El dominio de arriba solo admite 'ADMIN', y el panel
-- exige hasRole("ADMIN"), asi que con el valor largo esas cuentas no entraban al
-- panel y tampoco se podian editar: las funciones administrativas son
-- admin-only y al devolverles un rol invalido el PUT/PATCH terminaba en 400.
--
-- Es idempotente: en la segunda corrida no queda ninguna fila que actualizar.
-- No se tocan los otros roles que quedaron sueltos ('CIUDADANO',
-- 'ORGANIZACION') porque no tienen equivalencia obvious en el dominio, y decidir
-- a que rol degradarlos es una decision de producto, no una limpieza de datos.
UPDATE usuario SET rol = 'ADMIN' WHERE rol = 'ADMINISTRADOR';

-- Cuenta de administracion para poder entrar al panel (HU-23). El panel exige
-- rol 'ADMIN', asi que sin esta fila ninguna base recien creada tiene con quien
-- probarlo.
--
-- El correo es a proposito "admin.demo@" y no "admin@": en la base compartida
-- del equipo ya existe un admin@resq.com de una carga inicial anterior, con un
-- password_hash de ejemplo de 26 caracteres que no es un BCrypt valido (BCrypt
-- siempre mide 60), asi que no corresponde a ninguna clave. Con INSERT IGNORE el
-- choque de email hacia que esta fila no se insertara nunca ahi, y sin cuenta no
-- habia forma de probar el panel. El UPDATE de arriba ya le normaliza el rol;
-- lo que sigue sin poder es entrar, por el hash.
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