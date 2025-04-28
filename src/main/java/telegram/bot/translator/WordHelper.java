package telegram.bot.translator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class WordHelper {

    // Get IPA transcription for English words using DictionaryAPI
    public static String getTranscription(String word) {
        try {
            URL url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + URLEncoder.encode(word, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = reader.lines().collect(Collectors.joining());
                JSONArray jsonArray = new JSONArray(response);
                JSONObject first = jsonArray.getJSONObject(0);
                JSONArray phonetics = first.getJSONArray("phonetics");

                for (int i = 0; i < phonetics.length(); i++) {
                    JSONObject p = phonetics.getJSONObject(i);
                    if (p.has("text")) return p.getString("text");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "–";
    }

    // Get pronunciation audio URL (if available)
    public static String getAudioUrl(String word) {
        try {
            URL url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + URLEncoder.encode(word, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = reader.lines().collect(Collectors.joining());
                JSONArray jsonArray = new JSONArray(response);
                JSONObject first = jsonArray.getJSONObject(0);
                JSONArray phonetics = first.getJSONArray("phonetics");

                for (int i = 0; i < phonetics.length(); i++) {
                    JSONObject p = phonetics.getJSONObject(i);
                    if (p.has("audio") && !p.getString("audio").isEmpty()) {
                        return p.getString("audio");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Suggest closest spelling correction for a word
    public static String getSpellingSuggestion(String word) {
        try {
            URL url = new URL("https://api.datamuse.com/sug?s=" + URLEncoder.encode(word, StandardCharsets.UTF_8));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = reader.lines().collect(Collectors.joining());
                JSONArray suggestions = new JSONArray(response);

                if (suggestions.length() > 0) {
                    return suggestions.getJSONObject(0).getString("word");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Get IPA transcription from Wiktionary for a given language
    public static String getIPAFromWiktionary(String word, String langSection) {
        try {
            String url = "https://en.wiktionary.org/wiki/" + URLEncoder.encode(word, "UTF-8");
            Document doc = Jsoup.connect(url).get();

            // Look for the section of the specified language
            Elements langHeaders = doc.select("span.mw-headline[id=" + langSection + "]");
            if (!langHeaders.isEmpty()) {
                Element langHeader = langHeaders.first();
                Element h2 = langHeader.parent();
                Elements ipaSpans = h2.parent().select("span.IPA");
                if (!ipaSpans.isEmpty()) {
                    return ipaSpans.first().text();
                }
            }

            // Fallback: search entire page
            Elements fallback = doc.select("span.IPA");
            if (!fallback.isEmpty()) {
                return fallback.first().text();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "–";
    }
}
