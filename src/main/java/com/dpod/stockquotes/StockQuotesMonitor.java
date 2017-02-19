package com.dpod.stockquotes;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class StockQuotesMonitor {

    private static final Logger logger = LoggerFactory.getLogger(StockQuotesMonitor.class);

    public static void main(String[] args) {
        trustToAllSSLCertificates();
        String htmlString = downloadHtmlToString("https://ru.investing.com/currencies/usd-rub");
        String value = selectValueFromHtml(htmlString, "span#last_last");
        logger.info("result = " + value);
    }

    private static String selectValueFromHtml(String htmlString, String cssSelector) {
        Document doc = Jsoup.parse(htmlString);
        Elements links = doc.select(cssSelector);
        if (CollectionUtils.isNotEmpty(links)) {
            if (links.size() > 1) {
                logger.warn("Found more than one occurence on the web page by selector " + cssSelector);
            }
            return links.get(0).text();
        }
        return null;
    }

    private static String downloadHtmlToString(String urlString) {
        InputStream is = null;
        try {
            URLConnection connection = openConnection(urlString);
            is = connection.getInputStream();
            String html = IOUtils.toString(is, Charset.forName("UTF-8"));
            logger.info("HTML page is downloaded.");
            return html;
        } catch (IOException e) {
            logger.error("Error while reading html from website.", e);
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static URLConnection openConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        // setup userAgent (without it we will get 403 HTML error in response)
        connection.setRequestProperty("User-Agent", "NING/1.0");
        return connection;
    }

    private static void trustToAllSSLCertificates() {
        try {
            // Create a new trust manager that trust all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };
            // Activate the new trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            logger.info("SSL certificates settings are done.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
