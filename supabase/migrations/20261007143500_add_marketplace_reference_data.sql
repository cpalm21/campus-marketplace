-- Reference data shared by hosted deployments and local development.
-- Preserve existing rows if these values were added through the dashboard.
INSERT INTO public.universities (university_name, city, state_initials)
SELECT university_name, city, state_initials
FROM (VALUES
    ('University of Delaware', 'Newark', 'DE'),
    ('Temple University', 'Philadelphia', 'PA')
) AS defaults (university_name, city, state_initials)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.universities AS existing
    WHERE existing.university_name = defaults.university_name
      AND existing.city = defaults.city
      AND existing.state_initials = defaults.state_initials
);

INSERT INTO public.categories (name)
VALUES
    ('Furniture'),
    ('Textbooks'),
    ('Electronics'),
    ('Dorm Supplies'),
    ('Clothing'),
    ('Free Items')
ON CONFLICT (name) DO NOTHING;
