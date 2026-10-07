-- Local demo data only. Universities and categories are inserted by migrations.

-- ============================================
-- USERS
-- ============================================

INSERT INTO users (
    first_name,
    last_name,
    email,
    university_id,
    password_hash,
    profile_img_url
)
VALUES
(
    'Sean',
    'Smith',
    'sean.smith@example.com',
    (
        SELECT university_id
        FROM universities
        WHERE university_name = 'University of Delaware'
    ),
    'fake_hash_sean',
    NULL
),
(
    'Emma',
    'Judd',
    'emma.judd@example.com',
    (
        SELECT university_id
        FROM universities
        WHERE university_name = 'University of Delaware'
    ),
    'fake_hash_emma',
    NULL
),
(
    'Christian',
    'Palmer',
    'christian.palmer@example.com',
    (
        SELECT university_id
        FROM universities
        WHERE university_name = 'University of Delaware'
    ),
    'fake_hash_christian',
    NULL
),
(
    'Mia',
    'Pfaff',
    'mia.pfaff@example.com',
    (
        SELECT university_id
        FROM universities
        WHERE university_name = 'University of Delaware'
    ),
    'fake_hash_mia',
    NULL
),
(
    'Yazan',
    'Alsuraibi',
    'yazan.alsuraibi@example.com',
    (
        SELECT university_id
        FROM universities
        WHERE university_name = 'University of Delaware'
    ),
    'fake_hash_yazan',
    NULL
);


-- ============================================
-- LISTINGS
-- ============================================

INSERT INTO listings (
    user_id,
    category_id,
    listing_name,
    location,
    price,
    description,
    status,
    condition
)
VALUES
(
    (
        SELECT user_id
        FROM users
        WHERE email = 'sean.smith@example.com'
    ),
    (
        SELECT category_id
        FROM categories
        WHERE name = 'Furniture'
    ),
    'Wooden Desk',
    'North Campus',
    40.00,
    'Wooden desk in good condition. Great for a dorm or apartment.',
    'Available',
    'Good'
),
(
    (
        SELECT user_id
        FROM users
        WHERE email = 'emma.judd@example.com'
    ),
    (
        SELECT category_id
        FROM categories
        WHERE name = 'Electronics'
    ),
    'Mini Fridge',
    'South Academy Street',
    65.00,
    'Mini fridge used for one school year. Works perfectly.',
    'Available',
    'Great'
),
(
    (
        SELECT user_id
        FROM users
        WHERE email = 'christian.palmer@example.com'
    ),
    (
        SELECT category_id
        FROM categories
        WHERE name = 'Textbooks'
    ),
    'Calculus Textbook',
    'Perkins Student Center',
    25.00,
    'Calculus textbook with some highlighting.',
    'Available',
    'Good'
),
(
    (
        SELECT user_id
        FROM users
        WHERE email = 'mia.pfaff@example.com'
    ),
    (
        SELECT category_id
        FROM categories
        WHERE name = 'Dorm Supplies'
    ),
    'Desk Lamp',
    'East Campus',
    12.50,
    'Small desk lamp with adjustable brightness.',
    'Available',
    'Great'
),
(
    (
        SELECT user_id
        FROM users
        WHERE email = 'yazan.alsuraibi@example.com'
    ),
    (
        SELECT category_id
        FROM categories
        WHERE name = 'Free Items'
    ),
    'Plastic Storage Bins',
    'Main Street',
    0.00,
    'Two plastic storage bins. Free to anyone who can pick them up.',
    'Available',
    'Fair'
);


-- ============================================
-- LISTING IMAGES
-- ============================================

INSERT INTO listing_images (
    listing_id,
    image_url,
    image_order
)
VALUES
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Wooden Desk'
    ),
    'https://example.com/images/wooden-desk-1.jpg',
    1
),
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Wooden Desk'
    ),
    'https://example.com/images/wooden-desk-2.jpg',
    2
),
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Mini Fridge'
    ),
    'https://example.com/images/mini-fridge-1.jpg',
    1
),
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Calculus Textbook'
    ),
    'https://example.com/images/calculus-textbook-1.jpg',
    1
);


-- ============================================
-- FAVORITES
-- ============================================

INSERT INTO favorites (
    user_id,
    listing_id
)
VALUES
(
    (
        SELECT user_id
        FROM users
        WHERE email = 'emma.judd@example.com'
    ),
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Wooden Desk'
    )
),
(
    (
        SELECT user_id
        FROM users
        WHERE email = 'sean.smith@example.com'
    ),
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Mini Fridge'
    )
),
(
    (
        SELECT user_id
        FROM users
        WHERE email = 'mia.pfaff@example.com'
    ),
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Calculus Textbook'
    )
);


-- ============================================
-- MESSAGES
-- ============================================

INSERT INTO messages (
    listing_id,
    sender_id,
    receiver_id,
    message
)
VALUES
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Wooden Desk'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'emma.judd@example.com'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'sean.smith@example.com'
    ),
    'Hey, is the desk still available?'
),
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Mini Fridge'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'sean.smith@example.com'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'emma.judd@example.com'
    ),
    'Would you take $55 for the mini fridge?'
),
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Calculus Textbook'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'yazan.alsuraibi@example.com'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'christian.palmer@example.com'
    ),
    'Does the textbook include the access code?'
);


-- ============================================
-- TRANSACTIONS
-- ============================================

INSERT INTO transactions (
    listing_id,
    buyer_id,
    seller_id,
    amount,
    status,
    date_paid
)
VALUES
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Wooden Desk'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'emma.judd@example.com'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'sean.smith@example.com'
    ),
    40.00,
    'Completed',
    CURRENT_TIMESTAMP
),
(
    (
        SELECT listing_id
        FROM listings
        WHERE listing_name = 'Calculus Textbook'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'mia.pfaff@example.com'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'christian.palmer@example.com'
    ),
    25.00,
    'Completed',
    CURRENT_TIMESTAMP
);


-- ============================================
-- REVIEWS
-- ============================================

INSERT INTO reviews (
    transaction_id,
    reviewer_id,
    reviewee_id,
    rating,
    comment
)
VALUES
(
    (
        SELECT transaction_id
        FROM transactions
        WHERE listing_id = (
            SELECT listing_id
            FROM listings
            WHERE listing_name = 'Wooden Desk'
        )
        LIMIT 1
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'emma.judd@example.com'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'sean.smith@example.com'
    ),
    5,
    'Easy pickup and the desk was exactly as described.'
),
(
    (
        SELECT transaction_id
        FROM transactions
        WHERE listing_id = (
            SELECT listing_id
            FROM listings
            WHERE listing_name = 'Calculus Textbook'
        )
        LIMIT 1
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'mia.pfaff@example.com'
    ),
    (
        SELECT user_id
        FROM users
        WHERE email = 'christian.palmer@example.com'
    ),
    5,
    'Quick response and the textbook was in great condition.'
);