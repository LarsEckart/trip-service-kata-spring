-- Create user_table
CREATE TABLE user_table (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create trip table
CREATE TABLE trip (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user_trip junction table (many-to-many: users can have multiple trips)
CREATE TABLE user_trip (
    user_id BIGINT NOT NULL,
    trip_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, trip_id),
    FOREIGN KEY (user_id) REFERENCES user_table(id) ON DELETE CASCADE,
    FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE
);

-- Create user_friends junction table (many-to-many: users can have multiple friends)
CREATE TABLE user_friends (
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES user_table(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES user_table(id) ON DELETE CASCADE,
    -- Ensure no self-friendship and no duplicate friendships
    CHECK (user_id != friend_id)
);

-- Create indexes for better performance
CREATE INDEX idx_user_trip_user_id ON user_trip(user_id);
CREATE INDEX idx_user_trip_trip_id ON user_trip(trip_id);
CREATE INDEX idx_user_friends_user_id ON user_friends(user_id);
CREATE INDEX idx_user_friends_friend_id ON user_friends(friend_id);
