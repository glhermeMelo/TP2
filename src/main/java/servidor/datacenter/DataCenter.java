package servidor.datacenter;

import org.springframework.boot.SpringApplication;
import servidor.ImplServidor;
import servidor.threads.ProxyAceitaServidoresDeBorda;
import servidorRMI.ImplMonitoramentoClimatico;
import servidorRMI.threads.CalculaMaximasPorSensor;
import servidorRMI.threads.CalculaMediasPorSensor;
import servidorRMI.threads.CalculaValorMaximo;
import servidorRMI.threads.CalculaValoresMedios;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private static ImplMonitoramentoClimatico servicoMonitoramento;

    public DataCenter(int porta, String ip, String nome, int portaHTTP) {
        super(porta, ip, nome);
        this.portaHTTP = portaHTTP;
        dadosGlobais = new ConcurrentHashMap<>();
        listaThreads = new ArrayList<>();
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

            implementarHTTP();

            while (isActive) {
                Socket cliente = serverSocket.accept();
                Thread aceitadora = new Thread(new ProxyAceitaServidoresDeBorda(cliente,
                        ip, porta,
                        chavesClientes,
                        chavesDatacenter));

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
        // Define a porta dinamicamente
        app.setDefaultProperties(Collections.singletonMap("server.port", String.valueOf(portaHTTP)));
        app.run();

        System.out.println("Servidor HTTP Spring Boot inicializado na porta " + portaHTTP + "!");
    }

    public static ImplMonitoramentoClimatico getServicoMonitoramento() {
        return servicoMonitoramento;
    }
}