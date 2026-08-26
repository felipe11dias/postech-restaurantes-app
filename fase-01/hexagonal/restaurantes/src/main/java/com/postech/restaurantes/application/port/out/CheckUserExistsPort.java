package com.postech.restaurantes.application.port.out;

import java.util.UUID;

/**
 * Port de saída: verificações de existência e unicidade.
 *
 * <p>As checagens de e-mail e login recebem o id do usuário atual para poderem
 * ignorá-lo: numa atualização, o próprio registro aparece como "já existente" e
 * bloquearia, sem isso, qualquer edição que mantivesse o mesmo e-mail. Passar
 * {@code null} significa "nenhum registro é exceção" — o caso do cadastro.</p>
 */
public interface CheckUserExistsPort {

    boolean existsById(UUID id);

    boolean existsByEmailExcluding(String email, UUID currentUserId);

    boolean existsByLoginExcluding(String login, UUID currentUserId);
}
