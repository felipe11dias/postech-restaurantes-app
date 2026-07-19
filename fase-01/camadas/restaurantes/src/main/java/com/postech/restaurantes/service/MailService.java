package com.postech.restaurantes.service;

public interface MailService {

    /** Envia o e-mail com o token de redefinição de senha para o endereço informado. */
    void sendPasswordResetEmail(String to, String rawToken);
}
