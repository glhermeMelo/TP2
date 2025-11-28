package servidorRMI;

import entities.RegistroClimatico;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IMonitoramentoRMI extends Remote {
    RegistroClimatico getTemperaturaMaxima() throws RemoteException;
    RegistroClimatico getCO2Maximo() throws RemoteException;
    RegistroClimatico getCOMaximo() throws RemoteException;
    RegistroClimatico getNO2Maximo() throws RemoteException;
    RegistroClimatico getSO2Maximo() throws RemoteException;
    RegistroClimatico getPM2_5Maximo()  throws RemoteException;
    RegistroClimatico getPM10Maximo() throws RemoteException;
    RegistroClimatico getUmidadeMaxima() throws RemoteException;
    RegistroClimatico getRuidoMaximo() throws RemoteException;
    RegistroClimatico getRadiacaoUVMaxima() throws RemoteException;

    Double getTemperaturaMedia() throws RemoteException;
    Double getCO2Medio() throws RemoteException;
    Double getCOMedio() throws RemoteException;
    Double getNO2Medio() throws RemoteException;
    Double getSO2Medio() throws RemoteException;
    Double getPM2_5Medio()  throws RemoteException;
    Double getPM10Medio() throws RemoteException;
    Double getUmidadeMedia() throws RemoteException;
    Double getRuidoMedio() throws RemoteException;
    Double getRadiacaoUVMedia() throws RemoteException;

}
