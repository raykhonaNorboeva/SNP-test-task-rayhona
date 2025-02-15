package org.example.test_task.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ButtonService {


    public SendMessage getPersonalDataConsentMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы даёте согласие на обработку персональных данных?");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        InlineKeyboardButton agreeButton = new InlineKeyboardButton();
        agreeButton.setText("✅ Да");
        agreeButton.setCallbackData("consent_yes");

        InlineKeyboardButton disagreeButton = new InlineKeyboardButton();
        disagreeButton.setText("❌ Нет");
        disagreeButton.setCallbackData("consent_no");

        InlineKeyboardButton privacyPolicyButton = new InlineKeyboardButton();
        privacyPolicyButton.setText("🔗 Политика конфиденциальности");
        privacyPolicyButton.setUrl("https://hackernoon.com/tagged/javascript");

        buttons.add(Arrays.asList(agreeButton, disagreeButton));
        buttons.add(List.of(privacyPolicyButton));

        keyboardMarkup.setKeyboard(buttons);
        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    public SendMessage getGenderSelectionMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите ваш пол:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        InlineKeyboardButton maleButton = new InlineKeyboardButton();
        maleButton.setText("🚹 Мужской");
        maleButton.setCallbackData("male");

        InlineKeyboardButton femaleButton = new InlineKeyboardButton();
        femaleButton.setText("🚺 Женский");
        femaleButton.setCallbackData("female");

        buttons.add(Arrays.asList(maleButton, femaleButton));
        keyboardMarkup.setKeyboard(buttons);

        message.setReplyMarkup(keyboardMarkup);
        return message;
    }


}
