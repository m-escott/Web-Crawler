package com.taryaganalytics.parsers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * WebCrawler is a static class that generates a URL site map based upon a few parameters
 * @author Michael Escott
 */
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

    /************************************************
     * Creates a Map containing pairs of <URL, a list of child URLS> based upon a root URL
     * @param url A <CODE>String</CODE> : The root URL that will be crawled
     * @param outputFileName A <CODE>String</CODE> : name of the file that will store the text of the site map
     * @param maxNodeDepth An <CODE>int</CODE> : maximum number of levels to pursure in the depth first search
     * @return A <CODE>Map<String, ArrayList<String>></CODE> object.
     ************************************************/
    public Map<String, ArrayList<String>> crawl(String url, String outputFileName, int maxNodeDepth) throws Exception {
        long timeStarted = (new Date()).getTime();

        rootURL = url;
        this.maxNodeDepth = maxNodeDepth;

        //these two items will get populated within depthFirstRecursiveCrawl() below
        visitedURLs = new HashMap<>();
        siteMap = "";

        //todo - make this multi-threaded, possibly through ThreadPoolExecutor. See README.MD for more details
        depthFirstRecursiveCrawl(url, INTERNAL, 0); //jump into the depth first search with initial node depth set at 0 - the web site root

        System.out.println(siteMap);
        FileUtils.writeStringToFile(new File(outputFileName), siteMap, Charset.defaultCharset());

        long totalSeconds = ((new Date()).getTime() - timeStarted)/1000;

        System.out.println("Total duration in seconds: " + totalSeconds);

        return visitedURLs;
    }

    /************************************************
     * Performs a recursive depth first search based upon the current URL that was passed
     * @param url A <CODE>String</CODE> : The URL that will be crawled
     * @param urlType An <CODE>int</CODE> : Will typycally be INTERNAL, EXTERNAL, or STATIC as defined above
     * @param nodeDepth An <CODE>int</CODE> : the current level/distance from the root URL in the site map
     ************************************************/
    protected void depthFirstRecursiveCrawl(String url, int urlType, int nodeDepth) throws Exception {
        /*
        The logic used in depthFirstRecursiveCrawl() is the following:
        - add the passed URL to a visitedURLs list in order to avoid repeating visits to the same URL again
        - validate the URL with UrlValidator.isValid() from Apache Commons
        - fetch the raw web page text from the given URL. At the root level, an assumption is made that the page's content will be text/html. Recursion is done below to child pages, only when internal to the top-level site and only when it's determined that the content reference by the child URL will be text/html
        - add the URL and it's node level info to the site map that's being generated
        - extract a list of candidate URLs from the raw page text by looping through the text and matching patterns again a predefined Regex definition for an acceptable URL
        - loop though the list of candidate URLs by classifying the URL. If the URL meets all of the following conditions: 1) it's an internal URL; 2) its content will be of a non-static MIME type of text/html; 3) it hasn't yet been visited; 4) the recursion hasn't yet reached the maximum node depth requested ===>
          THEN: recurse with a call to depthFirstRecursiveCrawl() passing the child URL and the node depth
          OTHERWISE: merely add the child URL to the site map
        */
        System.out.println("Will now crawl: " + url);

        String domain = null;

        visitedURLs.putIfAbsent(url, new ArrayList<>()); //do this even if can't get content since don't want to revisit

        if (!validator.isValid(url)) {
            System.out.println("invalid URL: " + url);
            return;
        }

        if (rootHost == null) { //initialize rootHost String if not yet defined
            URL urlObj = new URL(rootURL);
            rootHost = urlObj.getHost(); //eg. www.google.com
        }

        String rawPageText = "";
        try {
            //attempt to fetch web page content from URL. Assume that the root will point to non-static HTML content.  Rules below enforce that any child URL will only be loaded if non-static
            rawPageText = IOUtils.toString(new URL(url), UTF_8);
        }
        catch (Exception e) {
            logger.info("An error has been encountered when loading " + url + " : " + e.getMessage());
            return;
        }

        //if made it this far then assume the URL contains valid content so can add it to the site map
        addUrlToSiteMap(url, urlType, nodeDepth);

        //now start preparing to visit all child URLs referenced in this page by collecting and scraping a list of URLs from the web content
        ArrayList<String> childURLs = extractURLs(rawPageText);

        //add all collected URLs to the visited collection so don't attempt repeat visits
        visitedURLs.put(url, childURLs);

        //now loop through the list of collected URLs and recursively visit any internal site URL containing non-static content
        int targetUrlType;
        String adjustedTargetURL;
        for (String targetURL : childURLs) {
            targetUrlType = classifyTargetURL(targetURL); //need to classify the URL to determine whether INTERNAL, EXTERNAL or STATIC

            //create adjustedTargetURL to avoid generating duplicated URLs which are basically the same : some with "/" at end and some without
            if (Objects.equals(StringUtils.right(targetURL, 1), "/"))
                adjustedTargetURL = StringUtils.chop(targetURL); //truncate the "/"
            else
                adjustedTargetURL = targetURL + "/";

            if (targetUrlType == INTERNAL && !visitedURLs.containsKey(targetURL) && !visitedURLs.containsKey(adjustedTargetURL) && nodeDepth < maxNodeDepth)
                depthFirstRecursiveCrawl(targetURL, targetUrlType, nodeDepth+1);
            else
                addUrlToSiteMap(targetURL, targetUrlType, nodeDepth+1);
        }

    }

    /************************************************
     * Adds a URL with other descriptive info and indenting to the site map string that eventually will be returned
     * @param url A <CODE>String</CODE> : The URL that will be added to the site map
     * @param urlType An <CODE>int</CODE> : Will typycally be INTERNAL, EXTERNAL, or STATIC as defined above
     * @param nodeDepth An <CODE>int</CODE> : the current level/distance from the root URL in the site map
     ************************************************/
    protected void addUrlToSiteMap(String url, int urlType, int nodeDepth) {
        //format the String for the new site map entry row
        String urlDescription = urlType == INTERNAL ? "INTERNAL" : (urlType == EXTERNAL ? "EXTERNAL" : (urlType == STATIC ? "STATIC" : "UNKNOWN"));
        siteMap += StringUtils.repeat(levelMargin, nodeDepth) + url + "(" + nodeDepth + ") : " + urlDescription + "\r\n";
    }

    /************************************************
     * Adds a URL with other descriptive info and indenting to the site map string that eventually will be returned
     * @param pageText A <CODE>String</CODE> : The page content loaded from a URL reference
     * @return An <CODE>ArrayList<String></CODE> object containing all child URLs detected.
     ************************************************/
    protected ArrayList<String> extractURLs(String pageText) {
        ArrayList<String> urls = new ArrayList<>();

        try {
            Pattern p = Pattern.compile(urlRegex);
            Matcher matcher = p.matcher(pageText);

            //based on the regex pattern submitted, loop through web content to identify a list of referenced URLs
            while (matcher.find()) {
                String url = pageText.substring(matcher.start(), matcher.end());
                if (urls.indexOf(url) < 0) { //should avoid storing duplicate URLs since they can occur multiple times inside a page
                    urls.add(url);
                }
            }
            return urls;
        }
        catch (Exception e) {
            return urls;
        }
    }

    /************************************************
     * Determine whether a URL is INTERNAL, EXTERNAL or STATIC
     * @param url A <CODE>String</CODE> : The URL being analyzed
     * @return An <CODE>int</CODE> typically with the value of INTERNAL, EXTERNAL or STATIC as defined above
     ************************************************/
    protected int classifyTargetURL(String url) {
        try {
            //it would be faster to merely check the url for a suffix ending in a common media file extension eg. .mp4 but there's no guarantees as to the content tyoe the url has
            //unless the folowing is done
            //URLConnection connection = urlObj.openConnection();
            //contentType = connection.getContentType();
            List<String> fileExt3 = Arrays.asList(".jpg", ".png", ".gif", ".wav", ".avi", ".mp3", ".mp4", ".doc", ".pdf", ".ppt", ".xsl");
            List<String> fileExt4 = Arrays.asList(".jpeg", ".docx", ".pptx", ".xslx");
            if (fileExt3.contains(StringUtils.right(url, 3)) || fileExt4.contains(StringUtils.right(url, 4)))
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

}

