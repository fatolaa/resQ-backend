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