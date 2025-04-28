-- Create the user_emails table
CREATE TABLE IF NOT EXISTS user_emails (
                                           id SERIAL PRIMARY KEY,
                                           telegram_user_id BIGINT UNIQUE NOT NULL,
                                           email TEXT NOT NULL,
                                           created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
    );

-- Create a trigger function to auto-update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column_email()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

-- Create a trigger to call the function before any update
CREATE TRIGGER set_updated_at_email
    BEFORE UPDATE ON user_emails
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column_email();
