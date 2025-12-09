package entities;

public class InfoServidorBorda {
    private String endereco;
    private boolean isActive;

    public InfoServidorBorda(String endereco) {
        this.endereco = endereco;
        this.isActive = true;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
