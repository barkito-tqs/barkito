package pt.ua.tqs.barkito;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Boundaries of the modular monolith (Product Specification 4.3). Each module is a package
 * {@code pt.ua.tqs.barkito.<module>} with {@code domain}, {@code application} and {@code adapter} subpackages.
 */
@AnalyzeClasses(packages = "pt.ua.tqs.barkito", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	/** Business logic depends on none of the adapters, and the domain stays free of frameworks. */
	@ArchTest
	static final ArchRule domainDependsOnNothingOutsideItself = noClasses()
			.that().resideInAPackage("..domain..")
			.should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..", "org.springframework..");

	@ArchTest
	static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
			.that().resideInAPackage("..application..")
			.should().dependOnClassesThat().resideInAPackage("..adapter..");

	/** No module reaches into another module's adapters (REST controllers, persistence). */
	@ArchTest
	static final ArchRule adaptersOfDifferentModulesAreIndependent = slices()
			.matching("pt.ua.tqs.barkito.(*).adapter..")
			.should().notDependOnEachOther();

	@ArchTest
	static final ArchRule modulesAreFreeOfCycles = slices()
			.matching("pt.ua.tqs.barkito.(*)..")
			.should().beFreeOfCycles();
}
