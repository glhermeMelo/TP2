package entities;

import java.io.Serializable;

public record RegistroClimatico(
        String localizacao,
        String idDispositivo,
        String horario,
        String cO2,
        String cO,
        String nO2,
        String sO2,
        String pM2_5,
        String pM10,
        String umidade,
        String temperatura,
        String ruido,
        String radiacaoUV) implements Serializable {

    @Override
    public String localizacao() {
        return localizacao;
    }

    @Override
    public String idDispositivo() {
        return idDispositivo;
    }

    @Override
    public String horario() {
        return horario;
    }

    @Override
    public String cO2() {
        return cO2;
    }

    @Override
    public String cO() {
        return cO;
    }

    @Override
    public String nO2() {
        return nO2;
    }

    @Override
    public String sO2() {
        return sO2;
    }

    @Override
    public String pM2_5() {
        return pM2_5;
    }

    @Override
    public String pM10() {
        return pM10;
    }

    @Override
    public String umidade() {
        return umidade;
    }

    @Override
    public String temperatura() {
        return temperatura;
    }

    @Override
    public String ruido() {
        return ruido;
    }

    @Override
    public String radiacaoUV() {
        return radiacaoUV;
    }

    @Override
    public String toString() {
        return "[" +
                "ID:'" + idDispositivo +
                ", Local: '" + localizacao +
                ", Hora: " + horario +
                ", CO2: " + cO2 +
                ", CO: " + cO +
                ", NO2: " + nO2 +
                ", SO2: " + sO2 +
                ", PM2.5: " + pM2_5 +
                ", PM10: " + pM10 +
                ", umidade: " + umidade + "%" +
                ", temperatura: " + temperatura + "°C" +
                ", ruído: " + ruido +
                ", radiacao uv: " + radiacaoUV +
                ']';
    }
}
