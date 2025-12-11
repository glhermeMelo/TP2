package servidor.util;

import entities.RegistroClimatico;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public  class AnalisadorDeRegistrosClimaticos {
    private static final Double MAXIMOCO2 = 400D;
    private static final Double MAXIMOCO = 11D;
    private static final Double MAXIMONO2 = 240D;
    private static final Double MAXIMOSO2 = 40D;
    private static final Double MAXIMOPM2_5 = 50D;
    private static final Double MAXIMOPM10 = 100D;
    private static final Double MINIMOUMIDADE = 50D;
    private static final Double MAXIMOTEMPERATURA = 30D;
    private static final Double MINIMOTEMPERATURA = 0D;
    private static final Double MAXIMORUIDO = 60D;
    private static final Double MAXIMORADIACAOUV = 5D;

    private static final String IP_IDS = "192.168.0.7";
    private static final int PORTA_IDS = 6500;

    public static void analisarRegistroClimatico(RegistroClimatico registroClimatico) {
        double co2 = Double.parseDouble(registroClimatico.cO2());
        double co = Double.parseDouble(registroClimatico.cO());
        double no2 = Double.parseDouble(registroClimatico.nO2());
        double so2 = Double.parseDouble(registroClimatico.sO2());
        double pm25 = Double.parseDouble(registroClimatico.pM2_5());
        double pm10 = Double.parseDouble(registroClimatico.pM10());
        double umidade = Double.parseDouble(registroClimatico.umidade());
        double temperatura = Double.parseDouble(registroClimatico.temperatura());
        double ruido = Double.parseDouble(registroClimatico.ruido());
        double uv = Double.parseDouble(registroClimatico.radiacaoUV());

        String id = registroClimatico.idDispositivo();
        String localizacao = registroClimatico.localizacao();

        if (co2 > MAXIMOCO2) emitirAlerta(id, localizacao, "CO2 Crítico", co2 + " ppm");
        if (co > MAXIMOCO)   emitirAlerta(id, localizacao, "Nível de CO Alto", co + " ppm");
        if (no2 > MAXIMONO2) emitirAlerta(id, localizacao, "Poluição NO2 Alta", no2 + " µg/m³");
        if (so2 > MAXIMOSO2) emitirAlerta(id, localizacao, "Poluição SO2 Alta", so2 + " µg/m³");
        if (pm25 > MAXIMOPM2_5)  emitirAlerta(id, localizacao, "Partículas Finas (PM2.5) Altas", pm25 + " µg/m³");
        if (pm10 > MAXIMOPM10)   emitirAlerta(id, localizacao, "Partículas Inaláveis (PM10) Altas", pm10 + " µg/m³");

        if (temperatura > MAXIMOTEMPERATURA) emitirAlerta(id, localizacao, "Calor Extremo", temperatura + " °C");
        if (temperatura < MINIMOTEMPERATURA) emitirAlerta(id, localizacao, "Frio Intenso", temperatura + " °C");
        if (umidade < MINIMOUMIDADE)  emitirAlerta(id, localizacao, "Baixa Umidade", umidade + "%");

        if (ruido > MAXIMORUIDO) emitirAlerta(id, localizacao, "Poluição Sonora", ruido + " dB");
        if (uv > MAXIMORADIACAOUV) emitirAlerta(id, localizacao, "Radiação UV Perigosa", "Índice " + uv);
    }

    private static void emitirAlerta(String id, String local, String tipoAlerta, String valor) {
        System.err.println("=====================================================");
        System.err.println("[ALERTA] " + tipoAlerta.toUpperCase());
        System.err.println("Dispositivo: " + id + " | Local: " + local);
        System.err.println("Valor medido: " + valor);
        System.err.println("=====================================================");

        try (Socket socket = new Socket(IP_IDS, PORTA_IDS);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                String payload = "ANOMALIA|ALERTA|" + id + "|" + tipoAlerta + " em " + local;
                writer.println(payload);

            } catch (IOException e) {
            System.err.println("Erro ao notificar IDS: " + e.getMessage());
        }
    }
}
