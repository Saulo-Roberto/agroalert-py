CREATE TABLE IF NOT EXISTS sensor_event (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(50) UNIQUE NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    parcela_id VARCHAR(50) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    valor NUMERIC(10,2) NOT NULL,
    unidad VARCHAR(20),
    event_timestamp TIMESTAMP,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alert (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(50) NOT NULL,
    tipo_alerta VARCHAR(50) NOT NULL,
    nivel VARCHAR(20) NOT NULL,
    mensaje VARCHAR(255),
    estado VARCHAR(20) DEFAULT 'CREATED',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);