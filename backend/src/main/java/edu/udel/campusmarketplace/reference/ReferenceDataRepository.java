package edu.udel.campusmarketplace.reference;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReferenceDataRepository {

	private final JdbcTemplate jdbc;

	ReferenceDataRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<CategoryResponse> findCategories() {
		return jdbc.query("SELECT category_id, name FROM public.categories ORDER BY name, category_id",
				(row, index) -> new CategoryResponse(row.getInt("category_id"), row.getString("name")));
	}

	public List<UniversityResponse> findUniversities() {
		return jdbc.query("""
				SELECT university_id, university_name, city, state_initials
				FROM public.universities
				ORDER BY university_name, city, state_initials, university_id
				""", (row, index) -> new UniversityResponse(
						row.getObject("university_id", UUID.class), row.getString("university_name"),
						row.getString("city"), row.getString("state_initials")));
	}
}
