package app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class Localization {

    public enum Language {
        TR, EN // According to order in localization.csv file
    }

    // Default language: Turkish
    private static Language currentLanguage = Language.TR; 
    
    // <Language Code, <Key, Translated Phrase>>
    private static final Map<Language, Map<String, String>> dictionary = new HashMap<>();

    static {
        for (Language lang : Language.values()) {
            dictionary.put(lang, new HashMap<>());
        }

        // Read and load .csv
        loadFromCSV();
    }

    private static void loadFromCSV() {
        String fileName = "/localization.csv"; 

        try (InputStream is = Localization.class.getResourceAsStream(fileName)) {
            if (is == null) {
                System.err.println("Localization file couldn't be found: " + fileName);
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean isFirstLine = true; 

                while ((line = br.readLine()) != null) {
                    if (isFirstLine) {
                        line = line.replace("\uFEFF", "");
                        isFirstLine = false;
                        if (line.toUpperCase().startsWith("KEY")) continue;
                    }

                    String trimmedLine = line.trim();

                    if (trimmedLine.isEmpty()) continue;
                    if (trimmedLine.startsWith("#")) continue;

                    String[] parts = line.split(";", -1); 

                    if (parts.length >= 3) {
                        String key = parts[0].trim();
                        String trVal = removeQuotes(parts[1].trim());
                        String enVal = removeQuotes(parts[2].trim());

                        dictionary.get(Language.TR).put(key, trVal);
                        dictionary.get(Language.EN).put(key, enVal);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error occured while loading the localization file!");
        }
    }
    
    private static String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    public static void setLanguage(Language lang) {
        currentLanguage = lang;
    }
    
    public static Language getLanguage() {
        return currentLanguage;
    }

    public static String get(String key) {
        Map<String, String> currentMap = dictionary.get(currentLanguage);
        if (currentMap != null && currentMap.containsKey(key)) {
            return currentMap.get(key);
        }
        return key; 
    }

    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }
    
    public static String getKey(String uiText) {
        Map<String, String> currentMap = dictionary.get(currentLanguage);
        for (Map.Entry<String, String> entry : currentMap.entrySet()) {
            if (entry.getValue().equals(uiText)) {
                return entry.getKey();
            }
        }
        return uiText;
    }
    
    // Returns localized text with another text, not with a key
    public static String getFromText(String uiText, Language searchLang, Language targetLang) {
        // Get search language's map
        Map<String, String> sourceMap = dictionary.get(searchLang);
        if (sourceMap == null) return uiText;

        // Find the key of UI text
        String foundKey = null;
        for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
            if (entry.getValue().equals(uiText)) {
                foundKey = entry.getKey();
                break;
            }
        }

        // If key found, find the target language's UI text
        if (foundKey != null) {
            Map<String, String> targetMap = dictionary.get(targetLang);
            if (targetMap != null && targetMap.containsKey(foundKey)) {
                return targetMap.get(foundKey);
            }
        }

        // If key not found, return original UI text
        return uiText;
    }
    
    // Optimized search function to find disposal types in current language
    public static String findLocalizedDisposalTypeName(String typeName) {
    	String localizationKey = "disposaltype" + typeName.toLowerCase();
    	return Localization.get(localizationKey);
    }
}