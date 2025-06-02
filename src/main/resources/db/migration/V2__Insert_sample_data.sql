-- Insert sample users
INSERT INTO user_table (id, username) VALUES 
(1, 'alice'),
(2, 'bob'),
(3, 'charlie'),
(4, 'diana');

-- Insert sample trips
INSERT INTO trip (id, name) VALUES 
(1, 'Trip to Paris'),
(2, 'Tokyo Adventure'),
(3, 'London Business Trip'),
(4, 'Beach Vacation'),
(5, 'Mountain Hiking');

-- Setup friendships (bidirectional)
INSERT INTO user_friends (user_id, friend_id) VALUES 
(1, 2), -- alice is friends with bob
(2, 1), -- bob is friends with alice
(1, 3), -- alice is friends with charlie
(3, 1), -- charlie is friends with alice
(2, 4), -- bob is friends with diana
(4, 2); -- diana is friends with bob

-- Assign trips to users
INSERT INTO user_trip (user_id, trip_id) VALUES 
(1, 1), -- alice went to Paris
(1, 3), -- alice went to London
(2, 1), -- bob went to Paris (shared trip with alice)
(2, 2), -- bob went to Tokyo
(3, 4), -- charlie went to beach
(3, 5), -- charlie went hiking
(4, 2), -- diana went to Tokyo (shared trip with bob)
(4, 4); -- diana went to beach (shared trip with charlie)
