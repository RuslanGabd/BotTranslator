package telegram.bot.translator;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DeeplTranslator {

    private final String apiKey;

    public DeeplTranslator() {
           this.apiKey = System.getenv("DEEPL_API_KEY") != null
                ? System.getenv("DEEPL_API_KEY")
                : Dotenv.load().get("DEEPL_API_KEY");
    }

    public String translate(String text, String sourceLang, String targetLang) {
        try {
            URL url = new URL("https://api-free.deepl.com/v2/translate");

            StringBuilder params = new StringBuilder();
            params.append("auth_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
            params.append("&text=").append(URLEncoder.encode(text, StandardCharsets.UTF_8));
            if (sourceLang != null && !sourceLang.isEmpty()) {
                params.append("&source_lang=").append(sourceLang.toUpperCase());
            }
            params.append("&target_lang=").append(targetLang.toUpperCase());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.toString().getBytes(StandardCharsets.UTF_8));
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String response = reader.lines().collect(Collectors.joining());
                JSONObject json = new JSONObject(response);
                return json.getJSONArray("translations")
                        .getJSONObject(0)
                        .getString("text");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Failed to translate.";
        }
    }

}
