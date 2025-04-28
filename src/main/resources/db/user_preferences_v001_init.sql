-- Create user_preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
                                                id SERIAL PRIMARY KEY,
                                                telegram_user_id BIGINT UNIQUE NOT NULL,
                                                source_language TEXT,
                                                target_language TEXT,
                                                created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
    );

-- Trigger to automatically update 'updated_at' on changes
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER set_updated_at
    BEFORE UPDATE ON user_preferences
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
