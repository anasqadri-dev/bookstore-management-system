package com.power_puff.bms.architecture;

import com.power_puff.architecturefixtures.controller.InvalidController;
import com.power_puff.architecturefixtures.repository.InvalidRepository;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AnalyzeClasses(
        packages = "com.power_puff.bms",
        importOptions = ImportOption.DoNotIncludeTests.class)
class BackendLayerArchitectureTest {

    private static final ArchRule CONTROLLERS_MUST_NOT_DEPEND_ON_REPOSITORIES =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("business operations must be delegated to services");

    @ArchTest
    static final ArchRule controllersMustNotAccessRepositories =
            CONTROLLERS_MUST_NOT_DEPEND_ON_REPOSITORIES;

    @ArchTest
    static final ArchRule servicesMustNotDependOnControllers =
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule repositoriesMustNotDependOnApplicationLayers =
            noClasses().that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..controller..", "..service..");

    @ArchTest
    static final ArchRule entitiesMustNotDependOnApplicationLayers =
            noClasses().that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..controller..", "..service..", "..repository..");

    @Test
    void controllerToRepositoryShortcutIsDetectedByTheRule() {
        JavaClasses invalidFixture = new ClassFileImporter()
                .importClasses(InvalidController.class, InvalidRepository.class);

        assertThrows(AssertionError.class,
                () -> CONTROLLERS_MUST_NOT_DEPEND_ON_REPOSITORIES.check(invalidFixture));
    }
}
