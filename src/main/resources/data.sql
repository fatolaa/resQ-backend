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