package org.example.test_task.botDevices;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.example.test_task.entity.TelegramBotConfig;
import org.example.test_task.entity.UserEntity;
import org.example.test_task.service.ButtonService;
import org.example.test_task.service.DocumentService;
import org.example.test_task.service.user.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TzBot extends TelegramLongPollingBot {

    private final TelegramBotConfig botConfig;
    private final UserService userService;
    private final ButtonService buttonService;
    private final DocumentService documentService;

    private final Map<Long, Integer> userSteps = new HashMap<>();
    private final Map<Long, UserEntity> tempUserData = new HashMap<>();

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText();
        int step = userSteps.getOrDefault(chatId, 0);

        String utmParam = getUtmFromStartCommand(text);

        UserEntity userEntity = tempUserData.getOrDefault(chatId, new UserEntity());
        userEntity.setChatId(chatId);

        if (utmParam != null && userEntity.getUtm() == null) {
            userEntity.setUtm(utmParam);
            tempUserData.put(chatId, userEntity);
            userService.save(userEntity);
        }

        switch (step) {
            case 0 -> {
                sendPersonalDataConsent(chatId);
                userSteps.put(chatId, 1);
            }
            case 1 -> {
                if (text.equalsIgnoreCase("/yes")) {
                    sendMessage(chatId, "Введите ваше полное ФИО: ");
                    userSteps.put(chatId, 2);
                } else {
                    sendMessage(chatId, "🔚 Регистрация отменена ");
                    userSteps.remove(chatId);
                }
            }
            case 2 -> {
                if (!text.matches("^[А-Яа-яЁёA-Za-z]+\\s[А-Яа-яЁёA-Za-z]+(\\s[А-Яа-яЁёA-Za-z]+)?$"
                )) {
                    sendMessage(chatId, "Ошибка! Введите ваше ФИО полностью:");
                } else {
                    userEntity.setFullName(text);
                    sendMessage(chatId, "Введите дату рождения (dd.MM.yyyy):");
                    userSteps.put(chatId, 3);
                }
            }
            case 3 -> {
                if (!isValidDate(text)) {
                    sendMessage(chatId, "Ошибка! Введите дату рождения в формате dd.MM.yyyy:");
                } else {
                    userEntity.setBirthDate(text);
                    sendGenderSelection(chatId);
                }
            }
            case 4 -> {
                if (message.hasPhoto()) {
                    String photoId = message.getPhoto().get(0).getFileId();
                    String photoFilePath = documentService.savePhotoToDisk(this, photoId);

                    if (photoFilePath != null) {
                        userEntity.setPhotoUrl(photoFilePath);
                        sendMessage(chatId, "✅ Данные сохранены!");

                        userService.save(userEntity);
                        XWPFDocument document = documentService.createDocument(userEntity, photoFilePath);
                        documentService.sendDocument(chatId, document, this);

                        tempUserData.remove(chatId);
                        userSteps.remove(chatId);
                    } else {
                        sendMessage(chatId, "Ошибка при сохранении фотографии.");
                    }
                } else {
                    sendMessage(chatId, "❌ Ошибка! Отправьте фотографию, а не текст ");
                }
            }
            default -> sendMessage(chatId, "🤷‍♀️ Неизвестная команда ");
        }
        tempUserData.put(chatId, userEntity);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode(ParseMode.MARKDOWN);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPersonalDataConsent(Long chatId) {
        try {
            execute(buttonService.getPersonalDataConsentMessage(chatId));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendGenderSelection(Long chatId) {
        try {
            execute(buttonService.getGenderSelectionMessage(chatId));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    public void handleCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackData = update.getCallbackQuery().getData();
        UserEntity userEntity = tempUserData.getOrDefault(chatId, new UserEntity());

        if (callbackData.equals("male") || callbackData.equals("female")) {
            userEntity.setGender(callbackData.equals("male") ? "Мужской" : "Женский");

            sendMessage(chatId, "🖼️ Спасибо! Теперь отправьте вашу фотографию.");
            userSteps.put(chatId, 4);
        }
        if (callbackData.equals("consent_yes")) {
            sendMessage(chatId, "Введите ваше полное ФИО: ");
            userSteps.put(chatId, 2);
        }
        if (callbackData.equals("consent_no")) {
            sendMessage(chatId, "🔚 Регистрация отменена.");
            userSteps.remove(chatId);
            tempUserData.remove(chatId);
        }
        tempUserData.put(chatId, userEntity);
    }


    private boolean isValidDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private String getUtmFromStartCommand(String messageText) {
        if (messageText != null && messageText.startsWith("/start")) {
            String[] parts = messageText.split("=");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }


}
