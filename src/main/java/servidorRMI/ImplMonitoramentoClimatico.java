package servidorRMI;

import entities.RegistroClimatico;
import servidor.threads.CalculaValorMaximo;
import servidor.threads.CalculaValoresMedios;

import java.rmi.RemoteException;

public class ImplMonitoramentoClimatico implements IMonitoramentoRMI {
    private final CalculaValoresMedios calculaValoresMedios;
    private final CalculaValorMaximo calculaValorMaximo;

    public ImplMonitoramentoClimatico(CalculaValoresMedios calculaValoresMedios, CalculaValorMaximo calculaValorMaximo) {
        this.calculaValoresMedios = calculaValoresMedios;
        this.calculaValorMaximo = calculaValorMaximo;
    }

    @Override
    public RegistroClimatico getTemperaturaMaxima() throws RemoteException {
        return calculaValorMaximo.getTemperaturaMaxima();
    }

    @Override
    public RegistroClimatico getCO2Maximo() throws RemoteException {
        return calculaValorMaximo.getCO2Maximo();
    }

    @Override
    public RegistroClimatico getCOMaximo() throws RemoteException {
        return calculaValorMaximo.getCOMaximo();
    }

    @Override
    public RegistroClimatico getNO2Maximo() throws RemoteException {
        return calculaValorMaximo.getNO2Maximo();
    }

    @Override
    public RegistroClimatico getSO2Maximo() throws RemoteException {
        return calculaValorMaximo.getSO2Maximo();
    }

    @Override
    public RegistroClimatico getPM2_5Maximo() throws RemoteException {
        return calculaValorMaximo.getPM2_5Maximo();
    }

    @Override
    public RegistroClimatico getPM10Maximo() throws RemoteException {
        return calculaValorMaximo.getPM10Maximo();
    }

    @Override
    public RegistroClimatico getUmidadeMaxima() throws RemoteException {
        return calculaValorMaximo.getUmidadeMaxima();
    }

    @Override
    public RegistroClimatico getRuidoMaximo() throws RemoteException {
        return calculaValorMaximo.getRuidoMaximo();
    }

    @Override
    public RegistroClimatico getRadiacaoUVMaxima() throws RemoteException {
        return calculaValorMaximo.getRadiacaoUVMaximo();
    }

    @Override
    public Double getTemperaturaMedia() throws RemoteException {
        return calculaValoresMedios.getTemperaturaMedia();
    }

    @Override
    public Double getCO2Medio() throws RemoteException {
        return calculaValoresMedios.getCO2Medio();
    }

    @Override
    public Double getCOMedio() throws RemoteException {
        return calculaValoresMedios.getCOMedio();
    }

    @Override
    public Double getNO2Medio() throws RemoteException {
        return calculaValoresMedios.getNO2Medio();
    }

    @Override
    public Double getSO2Medio() throws RemoteException {
        return calculaValoresMedios.getSO2Medio();
    }

    @Override
    public Double getPM2_5Medio() throws RemoteException {
        return calculaValoresMedios.getPM2_5Medio();
    }

    @Override
    public Double getPM10Medio() throws RemoteException {
        return calculaValoresMedios.getPM10Medio();
    }

    @Override
    public Double getUmidadeMedia() throws RemoteException {
        return calculaValoresMedios.getUmidadeMedia();
    }

    @Override
    public Double getRuidoMedio() throws RemoteException {
        return calculaValoresMedios.getRuidoMedio();
    }

    @Override
    public Double getRadiacaoUVMedia() throws RemoteException {
        return calculaValoresMedios.getRadiacaoUVMedio();
    }
}
