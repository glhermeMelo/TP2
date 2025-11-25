package microdispositivo.util;

import com.google.gson.Gson;
import microdispositivo.entities.RegistroClimatico;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

public class GeradorDeLeituras implements Runnable {
    private static final Double MAXIMOCO2 = 1000D;
    private static final Double MAXIMOCO = 14D;
    private static final Double MAXIMONO2 = 321D;
    private static final Double MAXIMOSO2 = 41D;
    private static final Double MAXIMOPM2_5 = 76D;
    private static final Double MAXIMOPM10 = 151D;
    private static final Double MAXIMOUMIDADE = 100D;
    private static final Double MAXIMOTEMPERATURA = 50D;
    private static final Double MAXIMORUIDO = 120D;
    private static final Double MAXIMORADIACAOUV = 17D;

    private String idDispositivo;
    private String localizacao;
    private final long intervaloMillis; // intervalo entre leituras quando usado como Runnable
    private Gson gson = new Gson();
    private boolean isActive =  true;

    public GeradorDeLeituras(String idDispositivo, long intervaloMillis, String localizacao) {
        this.idDispositivo = idDispositivo;
        this.intervaloMillis = intervaloMillis;
        this.localizacao = localizacao;
    }

    @Override
    public void run() {
        while (isActive && !Thread.currentThread().isInterrupted()) {
            getLeitura();
            try {
                Thread.sleep(this.intervaloMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void parar() {
        this.isActive = false;
    }

    public String getLeitura() {
        RegistroClimatico registroClimatico = new RegistroClimatico(
                localizacao,
                idDispositivo,
                getHora().toString(),
                getcO2().toString(),
                getCO().toString(),
                getNO2().toString(),
                getSO2().toString(),
                getPM2_5().toString(),
                getPM10().toString(),
                getUmidade().toString(),
                getTemperatura().toString(),
                getRuido().toString(),
                getRadiacaoUV().toString()
        );

        return gson.toJson(registroClimatico);
    }

    private LocalDateTime getHora(){
        return LocalDateTime.now();
    }
    //https://www.co2meter.com/blogs/news/carbon-dioxide-indoor-levels-chart
    private Double getcO2() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMOCO2);
    }

    //https://cetesb.sp.gov.br/ar/padroes-de-qualidade-do-ar/

    /*
    Máximo de Ruim,
    Bom(0-9), Moderada(10-11), Ruim(12-13), MuitoRuim(14-15), Péssima(16>)
     */
    private Double getCO() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMOCO);
    }

    /*
    Máximo de Ruim,
    Bom(0-200), Moderada(201-240), Ruim(241-320), MuitoRuim(321-1130), Péssima(1131>)
     */
    private Double getNO2() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMONO2);
    }

    /*
   Máximo de Moderada,
   Bom(0-20), Moderada(21-40), Ruim(41-365), MuitoRuim(366-800), Péssima(801>)
    */
    private Double getSO2() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMOSO2);
    }

    /*
    Máximo de Ruim,
    Bom(0-25), Moderada(26-50), Ruim(51-75), MuitoRuim(76-125), Péssima(126>)
    */
    private Double getPM2_5() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMOPM2_5);
    }

    /*
    Máximo de Ruim,
    Bom(0-50), Moderada(51-100), Ruim(101-150), MuitoRuim(151-250), Péssima(251>)
    */
    private Double getPM10() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMOPM10);
    }

    //Máximo de 100%
    private Double getUmidade() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMOUMIDADE);
    }

    //Máximo de 50ºC
    private Double getTemperatura() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMOTEMPERATURA);
    }

    //https://www.invivo.fiocruz.br/saude/poluicao-sonora/
    /*
    Ok(0-40), Moderado(41-55), Ruim(56-75), MuitoRuim(76-100), Péssimo(101>)
     */
    private Double getRuido() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMORUIDO);
    }

    //https://satelite.cptec.inpe.br/uv/
    /*
    Baixo(1-2), Moderado(3-5), Alto(6-7), MuitoAlto(8-10), Extremo(11-16+)
     */
    private Double getRadiacaoUV() {
        return ThreadLocalRandom.current().nextDouble(0, MAXIMORADIACAOUV);
    }

    public String getIdDispositivo() {
        return idDispositivo;
    }

    public void setIdDispositivo(String idDispositivo) {
        this.idDispositivo = idDispositivo;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public long getIntervaloMillis() {
        return intervaloMillis;
    }
}
