package cliente;

import entities.RegistroClimatico;
import servidorRMI.IMonitoramentoRMI;

import javax.crypto.Cipher;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Scanner;

public abstract class ClienteRMI {
    private HashMap<String, KeyPair> chavesServidorBorda;
    private HashMap<String, KeyPair> chavesDatacenter;
    private String nome;
    private int porta;

    public ClienteRMI(String nome, int porta) {
        this.nome = nome;
        this.porta = porta;
        rodar();
    }

    protected void rodar() {

    }

    private KeyPair gerarParDeChavesRSA() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048, new SecureRandom());
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("RSA não disponível", e);
        }
    }

    private byte[] descriptografarRSA(byte[] bytesChaveSessao, PrivateKey chavePrivada) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, chavePrivada);
            return cipher.doFinal(bytesChaveSessao);
        } catch (Exception e) {
            System.err.println("Erro ao decifrar RSA: " + e.getMessage());
            return null;
        }
    }

    private static void exibirMenu(IMonitoramentoRMI stub) {
        Scanner sc = new Scanner(System.in);

        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n======================================");
            System.out.println("   MONITORAMENTO CLIMÁTICO REMOTO");
            System.out.println("======================================");
            System.out.println("1. Consultar valores máximos");
            System.out.println("2. Consultar valores médios");
            System.out.println("3. Relatório Completo (Todos os dados)");
            System.out.println("4. Consultar valores médios por localização");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");

            try {
                opcao = Integer.parseInt(sc.nextLine());

                if(opcao != 0) {
                    exibirOpcoes(opcao, stub);
                }
            } catch (NumberFormatException e) {
                System.err.println("Entrada inválida: " + e.getMessage());
                opcao = -1;
            }
        }
            System.out.println("Encerrando aplicação cliente.");
            sc.close();
    }

    private static void exibirOpcoes(int opcao, IMonitoramentoRMI stub) {
        Scanner sc = new Scanner(System.in);
        try {
            switch (opcao) {
                case 1: exibirMaximos(stub); break;
                case 2: exibirMedias(stub); break;
                case 3: {
                    exibirMaximos(stub);
                    exibirMedias(stub);
                } break;
                case 4:
                    System.out.print("Digite o ID do sensor (numérico): ");
                    try {
                        int idSensor = Integer.parseInt(sc.nextLine());
                        exibirMediasPorSensor(stub, idSensor);
                    } catch (NumberFormatException e) {
                        System.out.println("ID inválido. Deve ser um número inteiro.");
                    }
                    break;
                default:
                    System.out.println("Opção desconhecida.");
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private static void exibirMediasPorSensor(IMonitoramentoRMI stub, int id) throws RemoteException {
        System.out.println("\n--- MÉDIAS DO SENSOR " + id + " ---");

        System.out.printf("Temperatura: %.2f °C%n", stub.getTemperaturaMediaPorSensor(id));
        System.out.printf("Umidade:     %.2f %%%n", stub.getUmidadeMediaPorSensor(id));
        System.out.printf("CO2:         %.2f ppm%n", stub.getCO2MedioPorSensor(id));
        System.out.printf("CO:          %.2f ppm%n", stub.getCOMedioPorSensor(id));
        System.out.printf("NO2:         %.2f µg/m³%n", stub.getNO2MedioPorSensor(id));
        System.out.printf("SO2:         %.2f µg/m³%n", stub.getSO2MedioPorSensor(id));
        System.out.printf("PM2.5:       %.2f µg/m³%n", stub.getPM2_5MedioPorSensor(id));
        System.out.printf("PM10:        %.2f µg/m³%n", stub.getPM10MedioPorSensor(id));
        System.out.printf("Ruído:       %.2f dB%n", stub.getRuidoMedioPorSensor(id));
        System.out.println("Radiação UV: " + stub.getRadiacaoUVMediaPorSensor(id));
        System.out.println("--------------------------------");
    }

    private static void exibirMaximos(IMonitoramentoRMI stub) throws RemoteException {
        try {
            System.out.println("\n--- VALORES MÁXIMOS REGISTRADOS ---");
            System.out.println("Temperatura" + stub.getTemperaturaMaxima() + "°C");
            System.out.println("Umidade" +  stub.getUmidadeMaxima() + "%");
            System.out.println("CO2" + stub.getCO2Maximo() + " ppm");
            System.out.println("CO" + stub.getCOMaximo() + " ppm");
            System.out.println("NO2" + stub.getNO2Maximo() + " µg/m³");
            System.out.println("SO2" + stub.getSO2Maximo() + " µg/m³");
            System.out.println("PM2.5" + stub.getPM2_5Maximo() + " µg/m³");
            System.out.println("PM10" + stub.getPM10Maximo() + " µg/m³");
            System.out.println("Ruído" + stub.getRuidoMaximo() + " dB");
            System.out.println("Radiação UV" + stub.getRadiacaoUVMaxima());

        } catch (RemoteException e) {
            System.out.println("Erro ao obter valores máximos: " + e.getMessage());
        }
    }

    private static void exibirMedias(IMonitoramentoRMI stub) {
        try {
            System.out.println("\n--- VALORES MÉDIOS REGISTRADOS ---");
            System.out.println("Temperatura: " + stub.getTemperaturaMedia() + "°C");
            System.out.println("Umidade: " +  stub.getUmidadeMedia() + "%");
            System.out.println("CO2: " + stub.getCO2Medio() + " ppm");
            System.out.println("CO: " + stub.getCOMedio() + " ppm");
            System.out.println("NO2: " + stub.getNO2Medio() + " µg/m³");
            System.out.println("SO2: " + stub.getSO2Medio() + " µg/m³");
            System.out.println("PM2.5: " + stub.getPM2_5Medio() + " µg/m³");
            System.out.println("PM10: " + stub.getPM10Medio() + " µg/m³");
            System.out.println("Ruído: " + stub.getRuidoMedio() + " dB");
            System.out.println("Radiação UV: " + stub.getRadiacaoUVMedia());

        } catch (RemoteException e) {
            System.out.println("Erro ao obter valores médios: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("192.168.0.8", 1099);

            IMonitoramentoRMI stub = (IMonitoramentoRMI) registry.lookup("MonitoramentoClimatico");

            System.out.println("Conectado com sucesso ao datacenter via RMI!");

            exibirMenu(stub);
        } catch (AccessException e) {
            System.err.println("Acesso negado ao registro RMI: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("Serviço 'MonitoramentoClimatico' não encontrado no registro.");
            System.err.println("Certifique-se de que o DataCenter está rodando.");
        } catch (RemoteException e) {
            System.err.println("Erro de comunicação remota: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
