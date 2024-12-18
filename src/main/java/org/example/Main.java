package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        String url = "https://skillbox.ru/"; // URL для скачивания изображений
        String outputDir = "images"; // Папка для сохранения изображений

        // Создаем директорию для сохранения изображений
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        Document doc = null;
        try {
            // Получаем HTML-документ с добавлением user-agent и обработкой ошибок
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .get();
        } catch (org.jsoup.HttpStatusException e) {
            System.err.println("Ошибка HTTP: " + e.getStatusCode() + " при доступе к URL: " + e.getUrl());
            return;
        } catch (IOException e) {
            System.err.println("Ошибка соединения: " + e.getMessage());
            return;
        }

        // Получаем ссылки на изображения
        Elements images = doc.select("img");
        Set<String> links = new HashSet<>();
        for (Element image : images) {
            String src = image.attr("abs:src");
            if (!src.isEmpty()) {
                links.add(src);
            }
        }

        System.out.println("Найдено изображений: " + links.size());

        // Скачиваем изображения
        int number = 1;
        for (String link : links) {
            try {
                if (doesFileExist(link)) {
                    String extension = getFileExtension(link);
                    String filePath = outputDir + "/" + number++ + "." + extension;

                    ImageDownloader downloader = new ImageDownloader();
                    downloader.download(link, filePath);
                    System.out.println("Скачано: " + filePath);
                } else {
                    System.err.println("Файл не существует: " + link);
                }
            } catch (FileNotFoundException e) {
                System.err.println("Файл не найден: " + link);
            } catch (IOException e) {
                System.err.println("Ошибка скачивания: " + link + " - " + e.getMessage());
            }
        }
    }

    static boolean doesFileExist(String link) {
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    private static String getFileExtension(String link) {
        return link.replaceAll("^.+\\.", "").replaceAll("\\?.+$", "");
    }
}

class ImageDownloader {
    public void download(String link, String filePath) throws IOException {
        if (!Main.doesFileExist(link)) {
            throw new FileNotFoundException("Файл не существует: " + link);
        }

        URL url = new URL(link);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);

        try (InputStream inStream = connection.getInputStream();
             OutputStream outStream = new FileOutputStream(filePath)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        }
    }
}
