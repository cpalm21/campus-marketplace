package edu.udel.campusmarketplace.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Runs against a migrated PostgreSQL database; Backend CI supplies it automatically. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = ".+")
class ReferenceDataIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReferenceDataRepository repository;

	@Test
	void categoriesComeFromPostgresInAlphabeticalOrder() throws Exception {
		assertThat(repository.findCategories()).extracting(CategoryResponse::name)
				.contains("Furniture", "Textbooks").isSorted();

		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("Furniture", "Textbooks")))
				.andExpect(jsonPath("$[0].categoryId").isNumber());
	}

	@Test
	void universitiesComeFromPostgresWithUuidIdentifiers() throws Exception {
		assertThat(repository.findUniversities()).extracting(UniversityResponse::universityName)
				.contains("University of Delaware", "Temple University").isSorted();

		mockMvc.perform(get("/api/universities"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].universityName", hasItems("University of Delaware", "Temple University")))
				.andExpect(jsonPath("$[0].universityId").isNotEmpty())
				.andExpect(jsonPath("$[0].city").isString())
				.andExpect(jsonPath("$[0].stateInitials").isString());
	}
}
