package br.com.tapeteskalke.security;

// Cargos com regra especial. O ADMINISTRADOR (quem desenvolve e mantém o sistema) tem tudo o que o DONO tem
// e, além disso, todas as permissões sempre liberadas, sem depender da tabela de permissões.
public final class Cargos {
    public static final String DONO = "DONO";
    public static final String ADMINISTRADOR = "ADMINISTRADOR";

    private Cargos() {}

    // Quem pode gerenciar usuários, permissões e ver salários: DONO ou ADMINISTRADOR.
    public static boolean gestor(String cargo) {
        return DONO.equals(cargo) || ADMINISTRADOR.equals(cargo);
    }
}
