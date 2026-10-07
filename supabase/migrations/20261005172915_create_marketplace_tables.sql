-- ============================================
-- CAMPUS MARKETPLACE DATABASE
-- PostgreSQL
-- ============================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- ============================================
-- UNIVERSITIES
-- ============================================

CREATE TABLE universities (
    university_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    university_name VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    state_initials CHAR(2) NOT NULL
);


-- ============================================
-- USERS
-- ============================================

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    university_id UUID NOT NULL,
    password_hash VARCHAR(225) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    profile_img_url VARCHAR(500),

    FOREIGN KEY (university_id)
        REFERENCES universities(university_id)
);


-- ============================================
-- CATEGORIES
-- ============================================

CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);


-- ============================================
-- LISTINGS
-- ============================================

CREATE TABLE listings (
    listing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    category_id INT NOT NULL,

    listing_name VARCHAR(50) NOT NULL,
    location VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    price DECIMAL(10,2) NOT NULL,

    description VARCHAR(500),

    status VARCHAR(20) NOT NULL
        CHECK (status IN ('Available', 'Reserved', 'Sold')),

    condition VARCHAR(20)
        CHECK (condition IN (
            'New',
            'Great',
            'Good',
            'Fair',
            'Poor'
        )),

    FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    FOREIGN KEY (category_id)
        REFERENCES categories(category_id)
);


-- ============================================
-- LISTING IMAGES
-- ============================================

CREATE TABLE listing_images (
    listing_image_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL,

    image_url VARCHAR(500) NOT NULL,
    image_order INT DEFAULT 1,

    FOREIGN KEY (listing_id)
        REFERENCES listings(listing_id)
        ON DELETE CASCADE
);


-- ============================================
-- FAVORITES
-- ============================================

CREATE TABLE favorites (
    user_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, listing_id),

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    FOREIGN KEY (listing_id)
        REFERENCES listings(listing_id)
        ON DELETE CASCADE
);


-- ============================================
-- TRANSACTIONS
-- ============================================

CREATE TABLE transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    listing_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,

    amount DECIMAL(10,2) NOT NULL,

    status VARCHAR(20) NOT NULL
        CHECK (status IN (
            'Pending',
            'Completed',
            'Cancelled',
            'Refunded'
        )),

    date_paid TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (listing_id)
        REFERENCES listings(listing_id),

    FOREIGN KEY (buyer_id)
        REFERENCES users(user_id),

    FOREIGN KEY (seller_id)
        REFERENCES users(user_id),

    CHECK (buyer_id <> seller_id)
);


-- ============================================
-- REVIEWS
-- ============================================

CREATE TABLE reviews (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    transaction_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    reviewee_id UUID NOT NULL,

    rating INT NOT NULL
        CHECK (rating BETWEEN 1 AND 5),

    comment VARCHAR(500),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (transaction_id)
        REFERENCES transactions(transaction_id),

    FOREIGN KEY (reviewer_id)
        REFERENCES users(user_id),

    FOREIGN KEY (reviewee_id)
        REFERENCES users(user_id),

    CHECK (reviewer_id <> reviewee_id)
);


-- ============================================
-- MESSAGES
-- ============================================

CREATE TABLE messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    listing_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,

    message TEXT NOT NULL,

    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (listing_id)
        REFERENCES listings(listing_id),

    FOREIGN KEY (sender_id)
        REFERENCES users(user_id),

    FOREIGN KEY (receiver_id)
        REFERENCES users(user_id),

    CHECK (sender_id <> receiver_id)
);