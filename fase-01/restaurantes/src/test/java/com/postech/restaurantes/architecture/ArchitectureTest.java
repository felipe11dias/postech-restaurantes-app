package com.postech.restaurantes.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Testes de arquitetura. Transformam as regras da arquitetura em camadas
 * (documentadas no relatório) em verificações automáticas executadas no build.
 *
 * allowEmptyShould(true) evita falha enquanto pacotes ainda estão sendo
 * preenchidos ao longo das etapas; as regras passam a "morder" conforme as
 * classes são adicionadas.
 */
@AnalyzeClasses(
        packages = "com.postech.restaurantes",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule respeita_arquitetura_em_camadas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_nao_acessam_repositories_diretamente = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entidades_nao_dependem_de_vos = noClasses()
            .that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAPackage("..vo..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entidades_nao_dependem_de_controllers = noClasses()
            .that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositories_sao_interfaces = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().resideInAPackage("..repository..")
            .should().beInterfaces()
            .allowEmptyShould(true);
}
