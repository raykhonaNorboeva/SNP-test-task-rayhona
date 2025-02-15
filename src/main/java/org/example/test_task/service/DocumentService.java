package org.example.test_task.service;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.example.test_task.entity.TelegramBotConfig;
import org.example.test_task.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;

@Service
public class DocumentService {
    private final TelegramBotConfig botConfig;

    public DocumentService(TelegramBotConfig botConfig) {
        this.botConfig = botConfig;
    }


    public String savePhotoToDisk(TelegramLongPollingBot bot, String photoId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(photoId);
            org.telegram.telegrambots.meta.api.objects.File telegramFile = bot.execute(getFile);

            String fileUrl = telegramFile.getFileUrl(botConfig.getToken());
            InputStream inputStream = new URL(fileUrl).openStream();

            String directoryPath = "uploads/images/";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String filePath = directoryPath + photoId + ".jpg";
            File targetFile = new File(filePath);

            try (OutputStream outputStream = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            return filePath;
        } catch (IOException | TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }


    public XWPFDocument createDocument(UserEntity userData, String photoFilePath) {
        XWPFDocument document = new XWPFDocument();
        XWPFTable table = document.createTable();
        XWPFTableRow row1 = table.getRow(0);
        row1.getCell(0).setText("ФИО:");
        row1.addNewTableCell().setText(userData.getFullName());

        XWPFTableRow row2 = table.createRow();
        row2.getCell(0).setText("Дата рождения:");
        row2.getCell(1).setText(userData.getBirthDate().toString());

        XWPFTableRow row3 = table.createRow();
        row3.getCell(0).setText("Пол:");
        row3.getCell(1).setText(userData.getGender());

        XWPFParagraph divider = document.createParagraph();
        divider.setBorderBottom(Borders.THICK);
        divider.setSpacingBefore(10);
        divider.setSpacingAfter(10);

        try {
            File imageFile = new File(photoFilePath);
            if (imageFile.exists() && imageFile.isFile()) {
                FileInputStream imageStream = new FileInputStream(photoFilePath);

                XWPFParagraph imageParagraph = document.createParagraph();
                imageParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun imageRun = imageParagraph.createRun();
                imageRun.addPicture(imageStream, XWPFDocument.PICTURE_TYPE_JPEG, photoFilePath, Units.toEMU(300), Units.toEMU(300));

                XWPFParagraph captionParagraph = document.createParagraph();
                captionParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun captionRun = captionParagraph.createRun();
                captionRun.setText("Ваше фото");
                captionRun.setItalic(true);
            } else {
                System.out.println("Ошибка: файл изображения не найден по пути: " + photoFilePath);
            }
        } catch (IOException | org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
            e.printStackTrace();
        }
        return document;
    }


    public void sendDocument(Long chatId, XWPFDocument document, TelegramLongPollingBot bot) {
        try {
            File tempFile = File.createTempFile("my_info", ".docx");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                document.write(fos);
            }

            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(String.valueOf(chatId));
            sendDocument.setDocument(new InputFile(tempFile));

            bot.execute(sendDocument);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
