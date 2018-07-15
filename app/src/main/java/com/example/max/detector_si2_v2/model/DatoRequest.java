package com.example.max.detector_si2_v2.model;

public class DatoRequest {

    private double aceleracion;
    private double frenado;
    private double cambioCarril;
    private int respLimitVelo;
    private double lat;
    private double lng;

    public DatoRequest() {
    }

    public double getAceleracion() {
        return aceleracion;
    }

    public void setAceleracion(double aceleracion) {
        this.aceleracion = aceleracion;
    }

    public double getFrenado() {
        return frenado;
    }

    public void setFrenado(double frenado) {
        this.frenado = frenado;
    }

    public double getCambioCarril() {
        return cambioCarril;
    }

    public void setCambioCarril(double cambioCarril) {
        this.cambioCarril = cambioCarril;
    }

    public int getRespLimitVelo() {
        return respLimitVelo;
    }

    public void setRespLimitVelo(int respLimitVelo) {
        this.respLimitVelo = respLimitVelo;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
