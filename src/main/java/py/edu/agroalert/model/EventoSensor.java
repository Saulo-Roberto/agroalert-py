package py.edu.agroalert.model;

public class EventoSensor {

    private String event_id;
    private String sensor_id;
    private String parcela_id;
    private String tipo;
    private double valor;
    private String unidad;
    private String timestamp;

    public EventoSensor() {
    }

    public String getEvent_id() {
        return event_id;
    }

    public void setEvent_id(String event_id) {
        this.event_id = event_id;
    }

    public String getSensor_id() {
        return sensor_id;
    }

    public void setSensor_id(String sensor_id) {
        this.sensor_id = sensor_id;
    }

    public String getParcela_id() {
        return parcela_id;
    }

    public void setParcela_id(String parcela_id) {
        this.parcela_id = parcela_id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "EventoSensor{" +
                "event_id='" + event_id + '\'' +
                ", sensor_id='" + sensor_id + '\'' +
                ", parcela_id='" + parcela_id + '\'' +
                ", tipo='" + tipo + '\'' +
                ", valor=" + valor +
                ", unidad='" + unidad + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}