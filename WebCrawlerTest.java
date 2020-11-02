package com.taryaganalytics.tests.parsers;

import com.taryaganalytics.parsers.WebCrawler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WebCrawlerTest {

    @Test
    public void testWebCrawler() throws Exception {
        WebCrawler crawler = new WebCrawler();
        Map<String, ArrayList<String>> results;
        results = crawler.crawl("https://wiprodigital.com", "siteMapResults.txt", 5);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertTrue(results.containsKey("https://wiprodigital.com"));
        System.out.println("all done.");
    }

}
