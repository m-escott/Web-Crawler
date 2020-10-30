package com.taryaganalytics.tests.parsers;

import com.taryaganalytics.configs.BHS;
import com.taryaganalytics.configs.BaseHaSeferConfig;
import com.taryaganalytics.configs.TF;
import com.taryaganalytics.dao.DAOFactory;
import com.taryaganalytics.parsers.WebCrawler;
import com.taryaganalytics.tests.BaseTest;
import com.taryaganalytics.util.MorphUtils;
import com.taryaganalytics.util.ProcessUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.validation.constraints.AssertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@ContextConfiguration(classes = {BaseHaSeferConfig.class})
public class WebCrawlerTest extends BaseTest {
    protected static String TEMPFILES;

    @Before
    public void setUp() throws Exception {
        BaseHaSeferConfig config = new BaseHaSeferConfig();
        TEMPFILES = config.TEMPFILES();
        setAutowireTargetClasses(new ArrayList(Arrays.asList(DAOFactory.class, TF.class, BHS.class, MorphUtils.class, ProcessUtils.class)));
        super.setUp();
    }

    @Test
    public void testWebCrawler() throws Exception {
        WebCrawler crawler = new WebCrawler();
        Map<String, ArrayList<String>> results;
        results = crawler.crawl("https://wiprodigital.com", "c://builditTest//siteMapResults.txt", 0, 5);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertTrue(results.containsKey("https://wiprodigital.com"));
        System.out.println("all done.");
    }

}
