package servidor.datacenter;

import entities.RegistroClimatico;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import servidorHTTP.ImplMonitoramentoClimatico;

import java.util.LinkedHashMap;
import java.util.Map;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/monitoramento")
public class MonitoramentoController {

    @GetMapping("/sensor/{id}/medias")
    public Map<String, Double> getTodasMediasPorSensor(@PathVariable int id) {
        Map<String, Double> medias = new LinkedHashMap<>();
        medias.put("temperatura", getService().getTemperaturaMediaPorSensor(id));
        medias.put("umidade", getService().getUmidadeMediaPorSensor(id));
        medias.put("co2", getService().getCO2MedioPorSensor(id));
        medias.put("co", getService().getCOMedioPorSensor(id));
        medias.put("no2", getService().getNO2MedioPorSensor(id));
        medias.put("so2", getService().getSO2MedioPorSensor(id));
        medias.put("pm2_5", getService().getPM2_5MedioPorSensor(id));
        medias.put("pm10", getService().getPM10MedioPorSensor(id));
        medias.put("ruido", getService().getRuidoMedioPorSensor(id));
        medias.put("radiacaouv", getService().getRadiacaoUVMediaPorSensor(id));
        return medias;
    }

    @GetMapping("/sensor/{id}/maximos")
    public Map<String, RegistroClimatico> getTodosMaximosPorSensor(@PathVariable int id) {
        Map<String, RegistroClimatico> maximos = new LinkedHashMap<>();
        maximos.put("temperatura", getService().getTemperaturaMaximaPorSensor(id));
        maximos.put("umidade", getService().getUmidadeMaximaPorSensor(id));
        maximos.put("co2", getService().getCO2MaximoPorSensor(id));
        maximos.put("co", getService().getCOMaximoPorSensor(id));
        maximos.put("no2", getService().getNO2MaximoPorSensor(id));
        maximos.put("so2", getService().getSO2MaximoPorSensor(id));
        maximos.put("pm2_5", getService().getPM2_5MaximoPorSensor(id));
        maximos.put("pm10", getService().getPM10MaximoPorSensor(id));
        maximos.put("ruido", getService().getRuidoMaximoPorSensor(id));
        maximos.put("radiacaouv", getService().getRadiacaoUVMaximaPorSensor(id));
        return maximos;
    }

    private ImplMonitoramentoClimatico getService() {
        return DataCenter.getServicoMonitoramento();
    }

    // =======================================================
    // 1. Resumo de Médias por Sensor (Igual ao Cliente RMI)
    // =======================================================
    @GetMapping("/sensor/{id}/resumo")
    public Map<String, Object> getResumoPorSensor(@PathVariable int id) {
        Map<String, Object> resumo = new HashMap<>();
        ImplMonitoramentoClimatico service = getService();

        resumo.put("id_sensor", id);
        resumo.put("temperatura", service.getTemperaturaMediaPorSensor(id));
        resumo.put("umidade", service.getUmidadeMediaPorSensor(id));
        resumo.put("co2", service.getCO2MedioPorSensor(id));
        resumo.put("co", service.getCOMedioPorSensor(id));
        resumo.put("no2", service.getNO2MedioPorSensor(id));
        resumo.put("so2", service.getSO2MedioPorSensor(id));
        resumo.put("pm2_5", service.getPM2_5MedioPorSensor(id));
        resumo.put("pm10", service.getPM10MedioPorSensor(id));
        resumo.put("ruido", service.getRuidoMedioPorSensor(id));
        resumo.put("radiacao_uv", service.getRadiacaoUVMediaPorSensor(id));

        return resumo;
    }

    // =======================================================
    // 2. Resumo de Médias Globais
    // =======================================================
    @GetMapping("/medias/geral")
    public Map<String, Object> getMediasGerais() {
        Map<String, Object> resumo = new HashMap<>();
        ImplMonitoramentoClimatico service = getService();

        resumo.put("temperatura", service.getTemperaturaMedia());
        resumo.put("umidade", service.getUmidadeMedia());
        resumo.put("co2", service.getCO2Medio());
        resumo.put("co", service.getCOMedio());
        resumo.put("no2", service.getNO2Medio());
        resumo.put("so2", service.getSO2Medio());
        resumo.put("pm2_5", service.getPM2_5Medio());
        resumo.put("pm10", service.getPM10Medio());
        resumo.put("ruido", service.getRuidoMedio());
        resumo.put("radiacao_uv", service.getRadiacaoUVMedia());

        return resumo;
    }

    // =======================================================
    // 3. Resumo de Máximos Globais
    // =======================================================
    @GetMapping("/maximos/geral")
    public Map<String, Object> getMaximosGerais() {
        Map<String, Object> resumo = new HashMap<>();
        ImplMonitoramentoClimatico service = getService();

        resumo.put("temperatura", service.getTemperaturaMaxima());
        resumo.put("umidade", service.getUmidadeMaxima());
        resumo.put("co2", service.getCO2Maximo());
        resumo.put("co", service.getCOMaximo());
        resumo.put("no2", service.getNO2Maximo());
        resumo.put("so2", service.getSO2Maximo());
        resumo.put("pm2_5", service.getPM2_5Maximo());
        resumo.put("pm10", service.getPM10Maximo());
        resumo.put("ruido", service.getRuidoMaximo());
        resumo.put("radiacao_uv", service.getRadiacaoUVMaxima());

        return resumo;
    }
}