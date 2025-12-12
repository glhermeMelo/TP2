package servidor.datacenter;

import entities.RegistroClimatico;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import servidorRMI.ImplMonitoramentoClimatico;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/monitoramento")
public class MonitoramentoController {

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