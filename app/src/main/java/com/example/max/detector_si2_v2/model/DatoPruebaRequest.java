package com.example.max.detector_si2_v2.model;

public class DatoPruebaRequest {
    private int accidente;
    private double acc;
    private double su;
    private int en_vehiculo;
    private int velo;
    private double lat;
    private double lng;

    public DatoPruebaRequest() {
    }

    public int getAccidente() {
        return accidente;
    }

    public void setAccidente(int accidente) {
        this.accidente = accidente;
    }

    public double getAcc() {
        return acc;
    }

    public void setAcc(double acc) {
        this.acc = acc;
    }

    public double getSu() {
        return su;
    }

    public void setSu(double su) {
        this.su = su;
    }

    public int getEn_vehiculo() {
        return en_vehiculo;
    }

    public void setEn_vehiculo(int en_vehiculo) {
        this.en_vehiculo = en_vehiculo;
    }

    public int getVelo() {
        return velo;
    }

    public void setVelo(int velo) {
        this.velo = velo;
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
