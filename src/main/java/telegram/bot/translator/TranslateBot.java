package telegram.bot.translator;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslateBot extends TelegramLongPollingBot {

    UserPreferenceRepository preferenceRepo;

    DeeplTranslator translator;

    private final String botName = System.getenv("TRANSLATE_BOT_NAME") != null
            ? System.getenv("TRANSLATE_BOT_NAME")
            : Dotenv.load().get("TRANSLATE_BOT_NAME");

    @Override
    public String getBotUsername() {
        return botName;
    }

    public TranslateBot() throws SQLException {
        super(System.getenv("TRANSLATE_BOT_TOKEN") != null
                ? System.getenv("TRANSLATE_BOT_TOKEN")
                : Dotenv.load().get("TRANSLATE_BOT_TOKEN"));
        this.preferenceRepo = new UserPreferenceRepository();
        this.translator = new DeeplTranslator();
    }

    private final Map<Long, String> languageSetMode = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message message = update.getMessage();
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();

        switch (text) {
            case "/start", "🏠 Main Menu" -> {
                sendTextMessage(chatId, "👋 Welcome! Please choose your source and target languages.");
                sendLanguageMenu(chatId);
                return;
            }
            case "🌐 Set Source Language" -> {
                languageSetMode.put(userId, "source");
                sendTextMessage(chatId, "📥 Select source language:");
                sendLangOptions(chatId);
                languageSetMode.put(userId, "target");
                sendTextMessage(chatId, "📤 Select target language:");
                sendLangOptions(chatId);
                return;
            }
            case "🌐 Set Target Language" -> {
                languageSetMode.put(userId, "target");
                sendTextMessage(chatId, "📤 Select target language:");
                sendLangOptions(chatId);
                languageSetMode.put(userId, "source");
                sendTextMessage(chatId, "📥 Select source language:");
                sendLangOptions(chatId);
                return;

            }
        }

        List<String> langs = List.of("EN", "RU", "FR", "DE", "ES");
        String cleaned = text.replaceAll("[^A-Z]", "").toUpperCase();

        if (langs.contains(cleaned)) {
            if (!languageSetMode.containsKey(userId)) {
                sendTextMessage(chatId, "❗ Please choose whether you're setting source or target language.");
                sendLanguageMenu(chatId);
                return;
            }

            String mode = languageSetMode.get(userId);
            try {
                if ("source".equals(mode)) {
                    preferenceRepo.savePreferences(userId, cleaned, preferenceRepo.getTargetLang(userId));
                    sendTextMessage(chatId, "✅ Source language set to " + cleaned);
                } else {
                    preferenceRepo.savePreferences(userId, preferenceRepo.getSourceLang(userId), cleaned);
                    sendTextMessage(chatId, "✅ Target language set to " + cleaned);
                }
                languageSetMode.remove(userId); // clear after use
            } catch (SQLException e) {
                e.printStackTrace();
                sendTextMessage(chatId, "❌ Error saving language preference");
            }
            return;
        }

        // Translation handling
        try {
            String suggestion = WordHelper.getSpellingSuggestion(text);
            String input = suggestion != null ? suggestion : text;

            String sourceLang = preferenceRepo.getSourceLang(userId);
            String targetLang = preferenceRepo.getTargetLang(userId);
            if (sourceLang == null) sourceLang = "EN";
            if (targetLang == null) targetLang = "RU";

            String ipa = WordHelper.getIPAFromWiktionary(input, sourceLang);
            String audio = WordHelper.getAudioUrl(input);
            String translated = translator.translate(input, sourceLang, targetLang);

            StringBuilder response = new StringBuilder();
            response.append("🔤 Input: ").append(text);
            if (!input.equalsIgnoreCase(text)) {
                response.append("\n💬 Suggestion: ").append(input);
            }
            response.append("\n🗣️ IPA: ").append(ipa);
            response.append("\n🌍 Translation: ").append(translated);
            if (audio != null) {
                response.append("\n🔊 [Audio Pronunciation](").append(audio).append(")");
            }

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText(response.toString());
            msg.setParseMode("Markdown");
            execute(msg);

        } catch (Exception e) {
            e.printStackTrace();
            sendTextMessage(chatId, "❌ Translation failed");
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendLanguageMenu(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "⚙️ Language settings:");
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🌐 Set Source Language");
        row1.add("🌐 Set Target Language");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🏠 Main Menu");

        rows.add(row1);
        rows.add(row2);
        markup.setKeyboard(rows);

        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendLangOptions(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "🗣️ Choose a language:");
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🇬🇧 EN");
        row1.add("🇷🇺 RU");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("🇫🇷 FR");
        row2.add("🇪🇸 ES");
        row2.add("🇩🇪 DE");

        rows.add(row1);
        rows.add(row2);
        markup.setKeyboard(rows);

        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
