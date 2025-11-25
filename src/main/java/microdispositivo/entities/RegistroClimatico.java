package microdispositivo.entities;

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
        String radiacaoUV) {
}
