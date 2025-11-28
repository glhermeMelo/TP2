package servidor;

import servidorRMI.threads.CalculaMaximasPorSensor;
import servidorRMI.threads.CalculaMediasPorSensor;
import servidorRMI.threads.CalculaValorMaximo;
import servidorRMI.threads.CalculaValoresMedios;
import entities.RegistroClimatico;
import servidor.threads.DataCenterAceitaServidoresDeBorda;
import servidorRMI.IMonitoramentoRMI;
import servidorRMI.ImplMonitoramentoClimatico;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DataCenter extends ImplServidor {
    private ConcurrentHashMap<Integer, List<RegistroClimatico>> dadosGlobais;
    private List<Thread> listaThreads;
    private CalculaValorMaximo threadMaximos;
    private CalculaValoresMedios threadMedios;
    private CalculaMaximasPorSensor threadMaximasPorSensor;
    private CalculaMediasPorSensor threadMediasPorSensor;

    public DataCenter(int porta, String ip, String nome) {
        super(porta, ip, nome);
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

            implementarRMI();

            while (isActive) {
                Socket cliente = serverSocket.accept();
                Thread aceitadora = new Thread(new DataCenterAceitaServidoresDeBorda(cliente, chavesClientes, dadosGlobais));
                aceitadora.start();

                listaThreads.add(aceitadora);
            }
        } catch (IOException e) {
            if (isActive) {
                System.err.println("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    private void implementarRMI() {
        try {
            ImplMonitoramentoClimatico refObjRemoto = new ImplMonitoramentoClimatico(threadMedios,
                    threadMaximos, threadMediasPorSensor, threadMaximasPorSensor);

            IMonitoramentoRMI stub = (IMonitoramentoRMI) UnicastRemoteObject
                    .exportObject(refObjRemoto, 0);

            LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

            Registry monitoramentoClimatico = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);

            monitoramentoClimatico.rebind("MonitoramentoClimatico", refObjRemoto);

            System.err.println("MonitoramentoClimatico inicializado com sucesso!");
        } catch (AccessException e) {
            System.err.println("Erro ao acessar o registro remoto: " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("Erro ao inicializar serviço remoto: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        DataCenter dataCenter = new DataCenter(
                8000,
                "192.168.0.8",
                "DT-1");
    }
}
