package com.postech.restaurantes.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Verificação automatizada da arquitetura hexagonal.
 *
 * <p>Sem estes testes, "as dependências apontam para dentro" seria apenas uma frase
 * no relatório — uma promessa que o primeiro import apressado quebraria em silêncio.
 * Aqui ela vira uma condição de build: quem importar Spring dentro do domínio
 * descobre no {@code mvn test}, não na revisão de código (ou nunca).</p>
 *
 * <p>Mapa dos pacotes:</p>
 * <ul>
 *   <li>{@code domain} — núcleo puro; não conhece ninguém.</li>
 *   <li>{@code application} — casos de uso e ports; conhece só o domínio.</li>
 *   <li>{@code adapter.in} / {@code adapter.out} — bordas; conhecem o núcleo, nunca
 *       uma à outra.</li>
 *   <li>{@code config} — raiz de composição; pode conhecer tudo, por definição, já
 *       que é ela quem liga as peças.</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.postech.restaurantes",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "com.postech.restaurantes.domain..";
    private static final String APPLICATION = "com.postech.restaurantes.application..";
    private static final String ADAPTER = "com.postech.restaurantes.adapter..";
    private static final String ADAPTER_IN = "com.postech.restaurantes.adapter.in..";
    private static final String ADAPTER_OUT = "com.postech.restaurantes.adapter.out..";
    private static final String CONFIG = "com.postech.restaurantes.config..";

    /**
     * A regra estrutural completa, na forma que o ArchUnit chama de "onion":
     * domínio no centro, aplicação em volta, adapters na borda.
     */
    @ArchTest
    static final ArchRule respeita_a_arquitetura_hexagonal = onionArchitecture()
            .domainModels(DOMAIN)
            .applicationServices(APPLICATION)
            .adapter("in", ADAPTER_IN)
            .adapter("out", ADAPTER_OUT)
            .ignoreDependency(
                    com.tngtech.archunit.base.DescribedPredicate.describe(
                            "raiz de composição",
                            javaClass -> javaClass.getPackageName()
                                    .startsWith("com.postech.restaurantes.config")),
                    com.tngtech.archunit.base.DescribedPredicate.alwaysTrue())
            .allowEmptyShould(true);

    // ----- as regras individuais, que dizem em português o que falhou -----

    @ArchTest
    static final ArchRule dominio_nao_depende_da_aplicacao = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTER, CONFIG)
            .because("o núcleo do hexágono não conhece quem o usa nem quem o serve");

    /**
     * A regra mais importante do projeto: o domínio compila sem framework algum.
     *
     * <p>É ela que sustenta a afirmação de que as regras de negócio sobrevivem a uma
     * troca de Spring, de ORM ou de protocolo — e a que justifica o custo de manter
     * commands, views e mapeamentos manuais.</p>
     */
    @ArchTest
    static final ArchRule dominio_nao_depende_de_framework = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "javax.persistence..",
                    "com.fasterxml.jackson..", "io.jsonwebtoken..", "io.swagger..")
            .because("as entidades e Value Objects de domínio são Java puro");

    @ArchTest
    static final ArchRule aplicacao_nao_depende_de_adapters = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(ADAPTER, CONFIG)
            .because("os casos de uso falam com o mundo externo apenas através dos ports");

    /**
     * Nem os serviços de aplicação carregam anotações do Spring — é o que permite
     * registrá-los como beans na {@code UseCaseConfiguration} sem que eles saibam
     * disso, e o que torna os testes de caso de uso independentes de contexto.
     */
    @ArchTest
    static final ArchRule aplicacao_nao_depende_de_framework = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "com.fasterxml.jackson..",
                    "io.jsonwebtoken..", "io.swagger..")
            .because("os serviços de aplicação são instanciados pela raiz de composição, "
                    + "não anotados com @Service");

    @ArchTest
    static final ArchRule adapter_de_entrada_nao_conhece_adapter_de_saida = noClasses()
            .that().resideInAPackage(ADAPTER_IN)
            .should().dependOnClassesThat().resideInAPackage(ADAPTER_OUT)
            .because("um adapter fala com o outro apenas pelo vocabulário da aplicação");

    @ArchTest
    static final ArchRule adapter_de_saida_nao_conhece_adapter_de_entrada = noClasses()
            .that().resideInAPackage(ADAPTER_OUT)
            .should().dependOnClassesThat().resideInAPackage(ADAPTER_IN)
            .because("a persistência e a segurança não sabem que existe HTTP");

    /**
     * Os ports são o contrato do hexágono, então precisam ser abstrações — se uma
     * classe concreta entrasse no pacote, os casos de uso passariam a depender de
     * uma implementação e a inversão de dependência estaria quebrada.
     *
     * <p>Records aninhados (como o {@code AuthenticatedPrincipal}) são dados do
     * contrato, não implementações, e por isso ficam de fora da regra.</p>
     */
    @ArchTest
    static final ArchRule ports_sao_interfaces = classes()
            .that().resideInAPackage("com.postech.restaurantes.application.port.in")
            .or().resideInAPackage("com.postech.restaurantes.application.port.out")
            .and().areNotRecords()
            .should().beInterfaces()
            .because("port é contrato; implementação é papel de adapter ou de serviço");

    /**
     * Só a raiz de composição instancia serviços de aplicação. Se um adapter
     * passasse a fazer isso, voltaria a depender da implementação concreta em vez
     * do port — exatamente o acoplamento que a arquitetura evita.
     */
    @ArchTest
    static final ArchRule apenas_a_configuracao_instancia_servicos = noClasses()
            .that().resideInAPackage(ADAPTER)
            .should().dependOnClassesThat()
            .resideInAPackage("com.postech.restaurantes.application.service..")
            .because("os adapters dependem dos input ports, nunca dos serviços concretos");
}
