package servidorHTTP;

import entities.RegistroClimatico;
import servidorHTTP.threads.CalculaMaximasPorSensor;
import servidorHTTP.threads.CalculaMediasPorSensor;
import servidorHTTP.threads.CalculaValorMaximo;
import servidorHTTP.threads.CalculaValoresMedios;

public class ImplMonitoramentoClimatico implements IMonitoramentoHTTP {
    private final CalculaValoresMedios calculaValoresMedios;
    private final CalculaValorMaximo calculaValorMaximo;
    private final CalculaMediasPorSensor calculaMediasPorSensor;
    private final CalculaMaximasPorSensor  calculaMaximasPorSensor;

    public ImplMonitoramentoClimatico(CalculaValoresMedios calculaValoresMedios,
                                      CalculaValorMaximo calculaValorMaximo,
                                      CalculaMediasPorSensor calculaMediasPorSensor,
                                      CalculaMaximasPorSensor calculaMaximasPorSensor) {
        this.calculaValoresMedios = calculaValoresMedios;
        this.calculaValorMaximo = calculaValorMaximo;
        this.calculaMediasPorSensor = calculaMediasPorSensor;
        this.calculaMaximasPorSensor = calculaMaximasPorSensor;
    }

    // =================== Valores Globais ===================
    @Override
    public RegistroClimatico getTemperaturaMaxima() {
        return calculaValorMaximo.getTemperaturaMaxima();
    }

    @Override
    public RegistroClimatico getCO2Maximo() {
        return calculaValorMaximo.getCO2Maximo();
    }

    @Override
    public RegistroClimatico getCOMaximo() {
        return calculaValorMaximo.getCOMaximo();
    }

    @Override
    public RegistroClimatico getNO2Maximo() {
        return calculaValorMaximo.getNO2Maximo();
    }

    @Override
    public RegistroClimatico getSO2Maximo() {
        return calculaValorMaximo.getSO2Maximo();
    }

    @Override
    public RegistroClimatico getPM2_5Maximo() {
        return calculaValorMaximo.getPM2_5Maximo();
    }

    @Override
    public RegistroClimatico getPM10Maximo() {
        return calculaValorMaximo.getPM10Maximo();
    }

    @Override
    public RegistroClimatico getUmidadeMaxima() {
        return calculaValorMaximo.getUmidadeMaxima();
    }

    @Override
    public RegistroClimatico getRuidoMaximo() {
        return calculaValorMaximo.getRuidoMaximo();
    }

    @Override
    public RegistroClimatico getRadiacaoUVMaxima() {
        return calculaValorMaximo.getRadiacaoUVMaximo();
    }

    // =================== Valores Globais ===================

    @Override
    public Double getTemperaturaMedia() {
        return calculaValoresMedios.getTemperaturaMedia();
    }

    @Override
    public Double getCO2Medio() {
        return calculaValoresMedios.getCO2Medio();
    }

    @Override
    public Double getCOMedio() {
        return calculaValoresMedios.getCOMedio();
    }

    @Override
    public Double getNO2Medio() {
        return calculaValoresMedios.getNO2Medio();
    }

    @Override
    public Double getSO2Medio() {
        return calculaValoresMedios.getSO2Medio();
    }

    @Override
    public Double getPM2_5Medio() {
        return calculaValoresMedios.getPM2_5Medio();
    }

    @Override
    public Double getPM10Medio() {
        return calculaValoresMedios.getPM10Medio();
    }

    @Override
    public Double getUmidadeMedia() {
        return calculaValoresMedios.getUmidadeMedia();
    }

    @Override
    public Double getRuidoMedio() {
        return calculaValoresMedios.getRuidoMedio();
    }

    @Override
    public Double getRadiacaoUVMedia() {
        return calculaValoresMedios.getRadiacaoUVMedio();
    }

    // =================== Valores Individuais ===================

    @Override
    public Double getTemperaturaMediaPorSensor(int idSensor) {
        return calculaMediasPorSensor.getTemperaturaMedia(idSensor);
    }

    @Override
    public Double getCO2MedioPorSensor(int idSensor) {
        return calculaMediasPorSensor.getCO2Medio(idSensor);
    }

    @Override
    public Double getCOMedioPorSensor(int idSensor) {
        return calculaMediasPorSensor.getCOMedio(idSensor);
    }

    @Override
    public Double getNO2MedioPorSensor(int idSensor) {
        return calculaMediasPorSensor.getNO2Medio(idSensor);
    }

    @Override
    public Double getSO2MedioPorSensor(int idSensor) {
        return calculaMediasPorSensor.getSO2Medio(idSensor);
    }

    @Override
    public Double getPM2_5MedioPorSensor(int idSensor) {
        return calculaMediasPorSensor.getPM2_5Medio(idSensor);
    }

    @Override
    public Double getPM10MedioPorSensor(int idSensor) {
        return calculaMediasPorSensor.getPM10Medio(idSensor);
    }

    @Override
    public Double getUmidadeMediaPorSensor(int idSensor) {
        return calculaMediasPorSensor.getUmidadeMedia(idSensor);
    }

    @Override
    public Double getRuidoMedioPorSensor(int idSensor) {
        return calculaMediasPorSensor.getRuidoMedio(idSensor);
    }

    @Override
    public Double getRadiacaoUVMediaPorSensor(int idSensor) {
        return calculaMediasPorSensor.getRadiacaoUVMedia(idSensor);
    }

    @Override
    public RegistroClimatico getTemperaturaMaximaPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getTemperaturaMaxima(idSensor);
    }

    @Override
    public RegistroClimatico getUmidadeMaximaPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getUmidadeMaxima(idSensor);
    }

    @Override
    public RegistroClimatico getCO2MaximoPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getCO2Maximo(idSensor);
    }

    @Override
    public RegistroClimatico getCOMaximoPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getCOMaximo(idSensor);
    }

    @Override
    public RegistroClimatico getNO2MaximoPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getNO2Maximo(idSensor);
    }

    @Override
    public RegistroClimatico getSO2MaximoPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getSO2Maximo(idSensor);
    }

    @Override
    public RegistroClimatico getPM2_5MaximoPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getPM2_5Maximo(idSensor);
    }

    @Override
    public RegistroClimatico getPM10MaximoPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getPM10Maximo(idSensor);
    }

    @Override
    public RegistroClimatico getRuidoMaximoPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getRuidoMaximo(idSensor);
    }

    @Override
    public RegistroClimatico getRadiacaoUVMaximaPorSensor(int idSensor) {
        return calculaMaximasPorSensor.getRadiacaoUVMaxima(idSensor);
    }
}
