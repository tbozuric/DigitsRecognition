package hr.fer.zemris.projekt.gui.providers;

import java.util.Locale;
import java.util.ResourceBundle;

public class MessageProvider {

    private ResourceBundle bundle;

    private static MessageProvider messageProvider = new MessageProvider();

    private MessageProvider() {
        String language = "en";
        this.bundle = ResourceBundle.getBundle("translates",
                Locale.forLanguageTag(language));

    }

    public static MessageProvider getInstance() {
        return messageProvider;
    }

    public String get(String key) {
        return bundle.getString(key);
    }
}
