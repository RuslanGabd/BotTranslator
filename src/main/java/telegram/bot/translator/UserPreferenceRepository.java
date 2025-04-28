package telegram.bot.translator;

import telegram.bot.utils.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserPreferenceRepository {

    public UserPreferenceRepository() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.createStatement().executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_preferences (
                    id SERIAL PRIMARY KEY,
                    telegram_user_id BIGINT UNIQUE NOT NULL,
                    source_language TEXT,
                    target_language TEXT
                )
            """);
        }
    }

    public void savePreferences(Long userId, String sourceLang, String targetLang) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO user_preferences (telegram_user_id, source_language, target_language)
                VALUES (?, ?, ?)
                ON CONFLICT (telegram_user_id)
                DO UPDATE SET source_language = EXCLUDED.source_language,
                              target_language = EXCLUDED.target_language
            """);
            stmt.setLong(1, userId);
            stmt.setString(2, sourceLang);
            stmt.setString(3, targetLang);
            stmt.executeUpdate();
        }
    }

    public String getSourceLang(Long userId) throws SQLException {
        return getLang(userId, "source_language");
    }

    public String getTargetLang(Long userId) throws SQLException {
        return getLang(userId, "target_language");
    }

    private String getLang(Long userId, String column) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT " + column + " FROM user_preferences WHERE telegram_user_id = ?"
            );
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString(column);
            }
            return null;
        }
    }
}
