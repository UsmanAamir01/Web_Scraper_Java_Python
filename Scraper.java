package java_proj;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scraper {

    // Base website URL
    private static final String WEBSITE_URL = "https://papers.nips.cc";
    // Directory where data will be saved
    private static final String SAVE_DIRECTORY = "E:/Java_Scraper/";
    // CSV file to store paper details
    private static final String CSV_FILE_PATH = SAVE_DIRECTORY + "output.csv";
    
    // Scraper configuration
    private static final int REQUEST_TIMEOUT = 200000; // Max time for a request (ms)
    private static final int MAX_THREADS = 20; // Number of threads for parallel execution
    private static final int STARTING_YEAR = 2019;
    private static final int ENDING_YEAR = 2023;
    private static final int THREAD_SLEEP_TIME = 2000; // Sleep time between requests (ms)

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        createCSVFile(); // Create CSV file if not exists

        try {
            // Fetch the homepage HTML
            Document homepage = Jsoup.connect(WEBSITE_URL).timeout(REQUEST_TIMEOUT).get();
            Elements yearLinks = homepage.select("a[href^=/paper_files/paper/]");

            for (Element link : yearLinks) {
                String linkText = link.text();
                Integer extractedYear = extractYearFromText(linkText);
                
                // Process only the years in the given range
                if (extractedYear != null && extractedYear >= STARTING_YEAR && extractedYear <= ENDING_YEAR) {
                    String yearPageUrl = WEBSITE_URL + link.attr("href");
                    threadPool.submit(() -> processYearPage(yearPageUrl, extractedYear));
                }
            }
        } catch (IOException e) {
            System.err.println("Error connecting to homepage: " + e.getMessage());
        }
        threadPool.shutdown();
    }

    // Extracts year from a text string
    private static Integer extractYearFromText(String inputText) {
        Pattern pattern = Pattern.compile("(\\d{4})");
        Matcher matcher = pattern.matcher(inputText);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    // Processes a specific year's page and fetches paper links
    private static void processYearPage(String yearPageUrl, int year) {
        try {
            Thread.sleep(THREAD_SLEEP_TIME);
            Document yearPage = Jsoup.connect(yearPageUrl).timeout(REQUEST_TIMEOUT).get();
            Elements paperLinks = yearPage.select("ul.paper-list li a[href$=Abstract-Conference.html]");

            for (Element paper : paperLinks) {
                String paperPageUrl = WEBSITE_URL + paper.attr("href");
                processPaperPage(paperPageUrl, year);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error processing year page: " + e.getMessage());
        }
    }

    // Processes a specific paper's page to extract metadata and download PDF
    private static void processPaperPage(String paperPageUrl, int year) {
        try {
            Thread.sleep(THREAD_SLEEP_TIME);
            Document paperPage = Jsoup.connect(paperPageUrl).timeout(REQUEST_TIMEOUT).get();

            String title = paperPage.selectFirst("h4").text();
            String authors = paperPage.select("i").text();
            String abstractText = paperPage.selectFirst("p").text();
            
            // Extract PDF link
            Element pdfDownloadLink = paperPage.selectFirst("a[href$=Paper-Conference.pdf]");
            String pdfUrl = (pdfDownloadLink != null) ? WEBSITE_URL + pdfDownloadLink.attr("href") : "N/A";

            // Save details to CSV
            writeMetadataToCSV(year, title, authors, abstractText, pdfUrl);
            
            // Download PDF if available
            if (pdfDownloadLink != null) {
                downloadPdfFile(pdfUrl, year);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error processing paper page: " + e.getMessage());
        }
    }

    // Downloads the PDF file from the given URL
    private static void downloadPdfFile(String pdfUrl, int year) {
        String pdfFileName = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
        String yearFolderPath = SAVE_DIRECTORY + year + "/";

        try {
            Files.createDirectories(Paths.get(yearFolderPath));
            URL fileUrl = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(REQUEST_TIMEOUT);
            connection.setReadTimeout(REQUEST_TIMEOUT);

            Thread.sleep(THREAD_SLEEP_TIME);
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(yearFolderPath + pdfFileName)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("Downloaded: " + pdfFileName + " to " + yearFolderPath);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error downloading PDF: " + e.getMessage());
        }
    }

    // Creates the CSV file to store paper details
    private static void createCSVFile() {
        File csvFile = new File(CSV_FILE_PATH);
        try {
            Files.createDirectories(Paths.get(SAVE_DIRECTORY));
            if (!csvFile.exists()) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"))) {
                    writer.write("Year,Title,Authors,Abstract,PDF Link\n");
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Error creating CSV file: " + e.getMessage());
        }
    }

    // Writes extracted data to CSV
    private static synchronized void writeMetadataToCSV(int year, String title, String authors, String abstractText, String pdfUrl) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CSV_FILE_PATH, true), "UTF-8"))) {
            title = title.replace("\"", "'");
            authors = authors.replace("\"", "'");
            abstractText = abstractText.replace("\"", "'");
            writer.write(String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\"\n", year, title, authors, abstractText, pdfUrl));
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
        }
    }
}
