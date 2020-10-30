package com.taryaganalytics.parsers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WebCrawler {
    public final Logger logger = Logger.getLogger("WebCrawler");
    protected Map<String, ArrayList<String>> visitedURLs;
    protected String siteMap;
    protected String levelMargin = "----";
    protected UrlValidator validator = new UrlValidator(); //assumes DEFAULT_SCHEMES = {"http", "https", "ftp"};
    protected String rootURL = null, rootHost = null;
    protected final String urlRegex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
    protected final int INTERNAL=1, EXTERNAL=2, STATIC=3, UNKNOWN=4;
    protected int maxNodeDepth = 0;

    public Map<String, ArrayList<String>> crawl(String url, String outputFile, int startingNodeDepth, int maxNodeDepth) throws Exception {
        long timeStarted = (new Date()).getTime();

        rootURL = url;
        this.maxNodeDepth = maxNodeDepth;

        visitedURLs = new ConcurrentHashMap<>();
        siteMap = "";

        //todo - make this multi-threaded, possibly through ThreadPoolExecutor.  There can be concurrency issues when dealing with a recursive process and/or where dependencies exist between tasks
        //here the dependency is that the built-in latency rests within the process of loading and parsing pages to create lists of new pages to explore
        //with the potential added challenge of predicting/detecting MIME types for candidate pages and links

        crawl(url, INTERNAL, startingNodeDepth);

        System.out.println(siteMap);
        stringToFile(siteMap, outputFile);

        long totalSeconds = ((new Date()).getTime() - timeStarted)/1000;

        System.out.println("Total duration in seconds: " + totalSeconds);

        return visitedURLs;
    }

    protected void crawl(String url, int urlType, int nodeDepth) throws Exception {
        System.out.println("Will now crawl: " + url);

        String domain = null;

        //todo - check url for validity or throw Exception - have test catch various Exception types
        //todo - check the validated URL that actually retrieves text o throw Exception
        //todo - if domain == null ==> capture the domain definition
        //visit the text and build list of all links to pages within the initial domain
        //    links to other pages under the same domain - will end in / or .com or :NNNN - DOMAIN: - check against visited map
        //        VISITED if already exist in map - ==> exit
        //        UNVISITED if not exist
        //    links to external URLs - EXTERNAL: ==> exit
        //    links to static content such as images or sound eg. jpg, png, mp4 STATIC: ==> exit
        //    loop through all DOMAIN:UNVISITED links and recurse, passing level # as breadcrumb

        visitedURLs.putIfAbsent(url, new ArrayList<>()); //do this even if can't get content since don't want to revisit

        if (!validator.isValid(url)) {
            if (nodeDepth == 0)
                return;
            else
                //throw new Exception("bad url");
                System.out.println("invalid URL: " + url);
            return;
        }

        if (nodeDepth == 0) {
            URL urlObj = new URL(rootURL);
            rootHost = urlObj.getHost(); //eg. www.google.com
        }

        String rawText = "";
        try {
            rawText = IOUtils.toString(new URL(url), UTF_8);
        }
        catch (Exception e) {
            logger.info("An error has been encountered when loading " + url + " : " + e.getMessage());
            return;
        }

        addUrlToSiteMap(url, urlType, nodeDepth);

        ArrayList<String> childURLs = extractURLs(rawText, urlRegex);

        visitedURLs.put(url, childURLs);

        int targetUrlType;
        String adjustedTargetURL;
        for (String targetURL : childURLs) {
            targetUrlType = classifyTargetURL(targetURL);

            //create adjustedTargetURL to avoid generating duplicated URLs which are basically the same : some with "/" at end and some without
            if (Objects.equals(StringUtils.right(targetURL, 1), "/"))
                adjustedTargetURL = StringUtils.chop(targetURL); //truncate the "/"
            else
                adjustedTargetURL = targetURL + "/";

            if (targetUrlType == INTERNAL && !visitedURLs.containsKey(targetURL) && !visitedURLs.containsKey(adjustedTargetURL) && nodeDepth < maxNodeDepth)
                crawl(targetURL, targetUrlType, nodeDepth+1);
            else
                addUrlToSiteMap(targetURL, targetUrlType, nodeDepth+1);
        }

    }

    protected void addUrlToSiteMap(String url, int urlType, int nodeDepth) {
        String urlDescription = urlType == INTERNAL ? "INTERNAL" : (urlType == EXTERNAL ? "EXTERNAL" : (urlType == STATIC ? "STATIC" : "UNKNOWN"));
        siteMap += StringUtils.repeat(levelMargin, nodeDepth) + url + " : " + urlDescription + "\r\n";
    }

    protected ArrayList<String> extractURLs(String pageText, String urlPattern) {
        ArrayList<String> urls = new ArrayList<>();

        try {
            Pattern p = Pattern.compile(urlPattern);
            Matcher matcher = p.matcher(pageText);

            while (matcher.find()) {
                String url = pageText.substring(matcher.start(), matcher.end());
                if (urls.indexOf(url) < 0) { //should avoid storing duplicate URLs since they can occur multiple times inside a page
                    urls.add(url);
                }
            }
            return urls;
        }
        catch (RuntimeException e) {
            return urls;
        }
    }

    protected int classifyTargetURL(String url) {
        String contentType = "";
        try {
            //it would be faster to merely check the url for a suffix ending in a common media file extension eg. .mp4 but there's no guarantees as to the content tyoe the url has
            //unless the folowing is done
            //URLConnection connection = urlObj.openConnection();
            //contentType = connection.getContentType();
            if (url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".gif") || url.endsWith(".wav") || url.endsWith(".mp3") ||
                    url.endsWith(".mp4") || url.endsWith(".jpeg") || url.endsWith(".doc") || url.endsWith(".docx") || url.endsWith(".pdf") ||
                    url.endsWith(".ppt") || url.endsWith(".pptx") || url.endsWith(".xsl") || url.endsWith(".xslx"))
                return STATIC;

            URL urlObj = new URL(url);
            if (!Objects.equals(urlObj.getHost(), rootHost))
                return EXTERNAL;

            return INTERNAL;
        }
        catch (Exception e) {
            return UNKNOWN;
        }
    }

    protected void stringToFile(String str, String fileName) throws Exception {
        File file = new File(fileName);
        FileUtils.writeStringToFile(file, str, Charset.defaultCharset());
    }

}

