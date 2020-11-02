This was written in Java with the class <b>WebCrawler</b> and the JUnit test class <b>WebCrawlerTest</b>
For now I have submitted the code and I can put the build content in later.

To run this use JUnit : run or debug the method com.taryaganalytics.tests.parsers.WebCrawlerTest.<b>testWebCrawler</b>().

It creates a folder called builditTest under the system root folder and creates a file called <b>siteMapResults.txt</b> with the test results.

<b>The algorithm:</b><br>
WebCrawler.<b>crawl()</b> is the initial method that's fired and it accepts three parameters:<br>
- a URL
- an file name for directing ourput
- a setting for node level depth - some web sites might contain a very cumbersome site map if taken to its completion and this setting allows one to limit the number of levels to investigate in order to get better performance in the face of overkill and decreasing marginal returns on the time spent crawling.  If one wants a complete map of the site, they can set node depth to a very high interger.

The crawl() method does some housekeeping at the beginning and end by firstly initializing variables and lastly preparing the results.  At it's core, it launches into a depth first search (DFS) by firing the method  WebCrawler.<b>depthFirstRecursiveCrawl()</b>.  This contains the main logic for crawling and gathering the information from the implied graph formed by the top-level web site URL and its network of child URLs.

The logic used in depthFirstRecursiveCrawl() is the following:<br>
- add the passed URL to a <b>visitedURLs</b> list in order to avoid repeating visits to the same URL again
- validate the URL with UrlValidator.isValid() from Apache Commons
- fetch the raw web page text from the given URL.  At the root level, an assumption is made that the page's content will be text/html.  Recursion is done below to child pages, only when internal to the top-level site and only when it's determined that the content reference by the child URL will be text/html
- add the URL and it's node level info to the site map that's being generated
- extract a list of candidate URLs from the raw page text by looping through the text and matching patterns again a predefined Regex definition for an acceptable URL
- loop though the list of candidate URLs by classifying the URL.  If the URL meets all of the following conditions: <b>1)</b> it's an internal URL; <b>2)</b> its content will be of a non-static MIME type of text/html; <b>3)</b> it hasn't yet been visited; <b>4)</b> the recursion hasn't yet reached the maximum node depth requested ===><br><b>THEN:</b>  recurse with a call to depthFirstRecursiveCrawl() passing the child URL and the node depth<br>
<b>OTHERWISE:</b> merely add the child URL to the site map

<b>Explanation of a coding decision tradeoff:<br></b>
When verifying a page's content type, it would be more accuarate to load the page and then retrieve the precise content type though the following approach:<br>
- URL urlObj = new URL(rootURL);
- URLConnection connection = urlObj.openConnection();
- contentType = connection.getContentType();
While this would be accurate, it would significantly slow performance especially when encountering large static file resources such as sound and video files.  For this reason I merely check the ending of the URL against a list of common file extensions that would be used in static content eg. .jpg, .gif, avi, mp4, etc.

If I had more time, I would add more of the following:
- unit tests
- explore concurrency to make this multi-threaded to improve performance via a ThreadPoolExecutor.
  There can be concurrency issues when dealing with a recursive process and/or where dependencies exist between tasks
  here the dependency is that the built-in latency rests within the process of loading and parsing pages to create lists of new pages to explore
  with the potential added challenge of predicting/detecting MIME types for candidate pages and links
- investigate etiquette issues in conjunction with robot.txt
- include extra comments and Javadoc
