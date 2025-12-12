package servidor.datacenter;

import org.springframework.boot.SpringApplication;
import servidor.ImplServidor;
import servidor.threads.DatacenterAceita;
import servidor.threads.EnviaRegistros;
import servidorHTTP.ImplMonitoramentoClimatico;
import servidorHTTP.threads.CalculaMaximasPorSensor;
import servidorHTTP.threads.CalculaMediasPorSensor;
import servidorHTTP.threads.CalculaValorMaximo;
import servidorHTTP.threads.CalculaValoresMedios;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import entities.RegistroClimatico;

public class DataCenter extends ImplServidor {
    private ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais;
    private List<Thread> listaThreads;
    private CalculaValorMaximo threadMaximos;
    private CalculaValoresMedios threadMedios;
    private CalculaMaximasPorSensor threadMaximasPorSensor;
    private CalculaMediasPorSensor threadMediasPorSensor;
    private int portaHTTP;

    private List<Integer> portasRepasse;
    private Map<Integer, ConcurrentHashMap<Integer, List<String>>> buffersRepasse;

    private static ImplMonitoramentoClimatico servicoMonitoramento;

    public DataCenter(int porta, String ip, String nome, int portaHTTP, List<Integer> portasRepasse) {
        super(porta, ip, nome);
        this.portaHTTP = portaHTTP;
        dadosGlobais = new ConcurrentHashMap<>();
        listaThreads = new ArrayList<>();
        buffersRepasse = new ConcurrentHashMap<>();
        this.portasRepasse = portasRepasse;

        if (portasRepasse != null) {
            for(Integer p : portasRepasse) {
                buffersRepasse.put(p, new ConcurrentHashMap<>());
            }
        }

        this.threadMaximos = new CalculaValorMaximo(dadosGlobais, 5000);
        this.threadMedios = new CalculaValoresMedios(dadosGlobais, 5000);
        this.threadMaximasPorSensor = new CalculaMaximasPorSensor(dadosGlobais, 5000);
        this.threadMediasPorSensor = new CalculaMediasPorSensor(dadosGlobais, 5000);
        rodar();
    }

    @Override
    public void rodar() {
        System.out.println("DataCenter iniciado na porta " + this.porta);

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            Thread calculadoraMaximosGlobais = new Thread(threadMaximos);
            calculadoraMaximosGlobais.start();

            Thread calculadoraMediosGlobais = new Thread(threadMedios);
            calculadoraMediosGlobais.start();

            Thread calculadoraMaximoSensor = new Thread(threadMaximasPorSensor);
            calculadoraMaximoSensor.start();

            Thread calculadoraMediosSensor = new Thread(threadMediasPorSensor);
            calculadoraMediosSensor.start();

            listaThreads.add(calculadoraMaximosGlobais);
            listaThreads.add(calculadoraMediosGlobais);
            listaThreads.add(calculadoraMaximoSensor);
            listaThreads.add(calculadoraMediosSensor);

            iniciarThreadsRepasse();

            implementarHTTP();

            while (isActive) {
                Socket cliente = serverSocket.accept();
                Thread aceitadora = new Thread(new DatacenterAceita(
                        cliente,
                        chavesClientes,
                        dadosGlobais,
                        portasRepasse,
                        buffersRepasse,
                        nome));

                aceitadora.start();

                listaThreads.add(aceitadora);
            }
        } catch (IOException e) {
            if (isActive) {
                System.err.println("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    private void implementarHTTP() {
        servicoMonitoramento = new ImplMonitoramentoClimatico(threadMedios,
                threadMaximos, threadMediasPorSensor, threadMaximasPorSensor);

        SpringApplication app = new SpringApplication(MonitoramentoController.class);

        app.setDefaultProperties(Collections.singletonMap("server.port", String.valueOf(portaHTTP)));

        app.run();

        System.out.println("Servidor HTTP Spring Boot inicializado na porta " + portaHTTP + "!");
    }

    public static ImplMonitoramentoClimatico getServicoMonitoramento() {
        return servicoMonitoramento;
    }

    private void iniciarThreadsRepasse() {
        if (portasRepasse == null)
            return;

        for (Integer p : portasRepasse) {
            EnviaRegistros enviador = new EnviaRegistros(
                    buffersRepasse.get(p),
                    this.ip,
                    p,
                    this.nome + "-Repassador",
                    2000
            );
            Thread t = new Thread(enviador);
            t.start();
            listaThreads.add(t);
            System.out.println("Repasse de dados configurado para porta " + p);
        }
    }
}
