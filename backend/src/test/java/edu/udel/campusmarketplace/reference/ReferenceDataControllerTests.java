package edu.udel.campusmarketplace.reference;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReferenceDataController.class)
class ReferenceDataControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReferenceDataRepository repository;

	@Test
	void categoriesArePublicAndIncludeTheirDatabaseIds() throws Exception {
		when(repository.findCategories()).thenReturn(List.of(new CategoryResponse(7, "Furniture")));

		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].categoryId").value(7))
				.andExpect(jsonPath("$[0].name").value("Furniture"));
	}

	@Test
	void universitiesIncludeTheirIdentityAndLocation() throws Exception {
		UUID id = UUID.fromString("ba0e8fc7-1c14-4cec-971f-584d95a00001");
		when(repository.findUniversities()).thenReturn(List.of(
				new UniversityResponse(id, "University of Delaware", "Newark", "DE")));

		mockMvc.perform(get("/api/universities"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].universityId").value(id.toString()))
				.andExpect(jsonPath("$[0].universityName").value("University of Delaware"))
				.andExpect(jsonPath("$[0].city").value("Newark"))
				.andExpect(jsonPath("$[0].stateInitials").value("DE"));
	}

	@Test
	void emptyTablesReturnEmptyArrays() throws Exception {
		when(repository.findCategories()).thenReturn(List.of());
		when(repository.findUniversities()).thenReturn(List.of());

		for (String endpoint : List.of("/api/categories", "/api/universities")) {
			mockMvc.perform(get(endpoint)).andExpect(status().isOk()).andExpect(content().json("[]"));
		}
	}

	@Test
	void databaseFailuresReturn503WithoutExposingConnectionDetails() throws Exception {
		var failure = new DataAccessResourceFailureException("Private database connection details");
		when(repository.findCategories()).thenThrow(failure);
		when(repository.findUniversities()).thenThrow(failure);

		for (String endpoint : List.of("/api/categories", "/api/universities")) {
			mockMvc.perform(get(endpoint))
					.andExpect(status().isServiceUnavailable())
					.andExpect(content().json("""
							{"message":"Reference data is temporarily unavailable."}
							"""));
		}
	}
}
