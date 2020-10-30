This was written in Java with the class WebCrawler and the JUnit test class WebCrawlerTest
For now I have submitted the code and I can put the build content in later

To run this - use JUnit : run or debug the method com.taryaganalytics.tests.parsers.WebCrawlerTest.testWebCrawler()

If I had more time, I would add more of the following:
- unit tests
- explore concurrency to make this multi-threaded to improve performance via a ThreadPoolExecutor.
  There can be concurrency issues when dealing with a recursive process and/or where dependencies exist between tasks
  here the dependency is that the built-in latency rests within the process of loading and parsing pages to create lists of new pages to explore
  with the potential added challenge of predicting/detecting MIME types for candidate pages and links
- investigate etiquette and robot.txt
- include extra comments and Javadoc
