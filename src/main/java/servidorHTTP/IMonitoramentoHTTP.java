package servidorHTTP;

import entities.RegistroClimatico;

public interface IMonitoramentoHTTP {
    RegistroClimatico getTemperaturaMaxima() ;
    RegistroClimatico getCO2Maximo() ;
    RegistroClimatico getCOMaximo() ;
    RegistroClimatico getNO2Maximo() ;
    RegistroClimatico getSO2Maximo() ;
    RegistroClimatico getPM2_5Maximo()  ;
    RegistroClimatico getPM10Maximo() ;
    RegistroClimatico getUmidadeMaxima() ;
    RegistroClimatico getRuidoMaximo() ;
    RegistroClimatico getRadiacaoUVMaxima() ;

    Double getTemperaturaMedia() ;
    Double getCO2Medio() ;
    Double getCOMedio() ;
    Double getNO2Medio() ;
    Double getSO2Medio() ;
    Double getPM2_5Medio()  ;
    Double getPM10Medio() ;
    Double getUmidadeMedia() ;
    Double getRuidoMedio() ;
    Double getRadiacaoUVMedia() ;

    Double getTemperaturaMediaPorSensor(int idSensor) ;
    Double getCO2MedioPorSensor(int idSensor) ;
    Double getCOMedioPorSensor(int idSensor) ;
    Double getNO2MedioPorSensor(int idSensor) ;
    Double getSO2MedioPorSensor(int idSensor) ;
    Double getPM2_5MedioPorSensor(int idSensor)  ;
    Double getPM10MedioPorSensor(int idSensor) ;
    Double getUmidadeMediaPorSensor(int idSensor) ;
    Double getRuidoMedioPorSensor(int idSensor) ;
    Double getRadiacaoUVMediaPorSensor(int idSensor) ;

    RegistroClimatico getTemperaturaMaximaPorSensor(int idSensor);
    RegistroClimatico getUmidadeMaximaPorSensor(int idSensor);
    RegistroClimatico getCO2MaximoPorSensor(int idSensor);
    RegistroClimatico getCOMaximoPorSensor(int idSensor);
    RegistroClimatico getNO2MaximoPorSensor(int idSensor);
    RegistroClimatico getSO2MaximoPorSensor(int idSensor);
    RegistroClimatico getPM2_5MaximoPorSensor(int idSensor);
    RegistroClimatico getPM10MaximoPorSensor(int idSensor);
    RegistroClimatico getRuidoMaximoPorSensor(int idSensor);
    RegistroClimatico getRadiacaoUVMaximaPorSensor(int idSensor);
}
