package am.ik.blog;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import static am.ik.blog.BlogUiApplicationTests.API_SERVER_PORT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "blog.api.url=http://localhost:"
		+ API_SERVER_PORT)
public class BlogUiApplicationTests {
	static final int API_SERVER_PORT = 55321;
	@Autowired
	BlogClient blogClient;
	@LocalServerPort
	int port;
	MockWebServer server = new MockWebServer();
	WebClient webClient;

	@Before
	public void setup() throws Exception {
		this.server.start(API_SERVER_PORT);
		this.webClient = new WebClient();
	}

	@After
	public void shutdown() throws Exception {
		server.shutdown();
		this.webClient.close();
	}

	@Test
	public void entries() throws Exception {
		this.server.enqueue(new MockResponse()
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/stream+x-jackson-smile")
				.setBody(new Buffer().readFrom(
						new ClassPathResource("data/entries.smile").getInputStream())));
		HtmlPage top = this.webClient.getPage("http://localhost:" + port);
		String xml = top.getBody().querySelector("ul.entries").asXml();
		assertThat(normalize(xml)).isEqualTo("<ul class=\"entries\">\n" + //
				"  <li>\n" + //
				"    <span class=\"visible-inline-on-wide\">\n" + //
				"      \uD83D\uDDC3 {\n" + //
				"      <a href=\"/categories/x/entries\">\n" + //
				"        x\n" + //
				"      </a>\n" + //
				"      /\n" + //
				"      <a href=\"/categories/x,y/entries\">\n" + //
				"        y\n" + //
				"      </a>\n" + //
				"      /\n" + //
				"      <a href=\"/categories/x,y,z/entries\">\n" + //
				"        z\n" + //
				"      </a>\n" + //
				"      }\n" + //
				"    </span>\n" + //
				"    <a href=\"/entries/99999\">\n" + //
				"      Hello World!!\n" + //
				"    </a>\n" + //
				"    <br class=\"invisible-inline-on-wide\"/>\n" + //
				"    <a href=\"http://b.hatena.ne.jp/entry/https://blog.ik.am/entries/99999\">\n"
				+ "      <img src=\"https://b.hatena.ne.jp/entry/image/https://blog.ik.am/entries/99999\"/>\n"
				+ "    </a>\n" + //
				"    \n" + //
				"                \uD83D\uDDD3 \n" + //
				"    <span class=\"visible-inline-on-wide\">\n" + //
				"      Updated at \n" + //
				"    </span>\n" + //
				"    <span>\n" + //
				"      2017-04-01T02:00+09:00\n" + //
				"    </span>\n" + //
				"  </li>\n" + //
				"  <li>\n" + //
				"    <span class=\"visible-inline-on-wide\">\n" + //
				"      \uD83D\uDDC3 {\n" + //
				"      <a href=\"/categories/a/entries\">\n" + //
				"        a\n" + //
				"      </a>\n" + //
				"      /\n" + //
				"      <a href=\"/categories/a,b/entries\">\n" + //
				"        b\n" + //
				"      </a>\n" + //
				"      /\n" + //
				"      <a href=\"/categories/a,b,c/entries\">\n" + //
				"        c\n" + //
				"      </a>\n" + //
				"      }\n" + //
				"    </span>\n" + //
				"    <a href=\"/entries/99998\">\n" + //
				"      Test!!\n" + //
				"    </a>\n" + //
				"    <br class=\"invisible-inline-on-wide\"/>\n" + //
				"    <a href=\"http://b.hatena.ne.jp/entry/https://blog.ik.am/entries/99998\">\n"
				+ "      <img src=\"https://b.hatena.ne.jp/entry/image/https://blog.ik.am/entries/99998\"/>\n"
				+ "    </a>\n" + //
				"    \n" + //
				"                \uD83D\uDDD3 \n" + //
				"    <span class=\"visible-inline-on-wide\">\n" + //
				"      Updated at \n" + //
				"    </span>\n" + //
				"    <span>\n" + //
				"      2017-04-01T00:00+09:00\n" + //
				"    </span>\n" + //
				"  </li>\n" + //
				"  <li>\n" + //
				"    <span class=\"visible-inline-on-wide\">\n" + //
				"      \uD83D\uDDC3 {\n" + //
				"      <a href=\"/categories/x/entries\">\n" + //
				"        x\n" + //
				"      </a>\n" + //
				"      /\n" + //
				"      <a href=\"/categories/x,y/entries\">\n" + //
				"        y\n" + //
				"      </a>\n" + //
				"      }\n" + //
				"    </span>\n" + //
				"    <a href=\"/p/entries/99997\">\n" + //
				"      \uD83C\uDD7F CategoLJ 4\n" + //
				"    </a>\n" + //
				"    <br class=\"invisible-inline-on-wide\"/>\n" + //
				"    <a href=\"http://b.hatena.ne.jp/entry/https://blog.ik.am/p/entries/99997\">\n"
				+ "      <img src=\"https://b.hatena.ne.jp/entry/image/https://blog.ik.am/p/entries/99997\"/>\n"
				+ "    </a>\n" + //
				"    \n" + //
				"                \uD83D\uDDD3 \n" + //
				"    <span class=\"visible-inline-on-wide\">\n" + //
				"      Updated at \n" + //
				"    </span>\n" + //
				"    <span>\n" + "      2017-03-31T00:00+09:00\n" + //
				"    </span>\n" + //
				"  </li>\n" + //
				"</ul>");
	}

	@Test
	public void entry() throws Exception {
		this.server.enqueue(new MockResponse()
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody("{\n" + //
						"  \"entryId\": 100,\n" + //
						"  \"content\": \"memo\\n\\n* hoge\\n* foo\\n* bar\",\n" + //
						"  \"created\": {\n" + //
						"    \"name\": \"Toshiaki Maki\",\n" + //
						"    \"date\": \"9998-12-20T02:32:23+09:00\"\n" + //
						"  },\n" + //
						"  \"updated\": {\n" + //
						"    \"name\": \"Toshiaki Maki\",\n" + //
						"    \"date\": \"9998-03-27T02:22:36+09:00\"\n" + //
						"  },\n" + //
						"  \"frontMatter\": {\n" + //
						"    \"title\": \"Hello World!\",\n" + //
						"    \"categories\": [\n" + //
						"      \"a\",\n" + //
						"      \"b\",\n" + //
						"      \"c\"\n" + //
						"    ],\n" + //
						"    \"tags\": [\n" + //
						"      \"Java\",\n" + //
						"      \"Spring\"\n" + //
						"    ]\n" + //
						"  }\n" + //
						"}"));
		HtmlPage top = this.webClient
				.getPage("http://localhost:" + port + "/entries/100");
		String xml = top.getBody().querySelector("article").asXml();
		assertThat(normalize(xml)).isEqualTo("<article>\n" + //
				"  <h2>\n" + //
				"    <a href=\"/entries/100\">\n" + //
				"      Hello World!\n" + //
				"    </a>\n" + //
				"    <a href=\"http://b.hatena.ne.jp/entry/https://blog.ik.am/entries/100\">\n"
				+ //
				"      <img src=\"https://b.hatena.ne.jp/entry/image/https://blog.ik.am/entries/100\"/>\n"
				+ //
				"    </a>\n" + //
				"  </h2>\n" + //
				"  <p class=\"categories\">\n" + //
				"    \uD83D\uDDC3 {\n" + //
				"    <a href=\"/categories/a/entries\">\n" + //
				"      a\n" + //
				"    </a>\n" + //
				"    /\n" + //
				"    <a href=\"/categories/a,b/entries\">\n" + //
				"      b\n" + //
				"    </a>\n" + //
				"    /\n" + //
				"    <a href=\"/categories/a,b,c/entries\">\n" + //
				"      c\n" + //
				"    </a>\n" + //
				"    }\n" + //
				"  </p>\n" + //
				"  <p class=\"tags\">\n" + //
				"    \uD83C\uDFF7 \n" + //
				"    <a href=\"/tags/Java/entries\">\n" + //
				"      Java\n" + //
				"    </a>\n" + //
				"     \uD83C\uDFF7 \n" + //
				"    <a href=\"/tags/Spring/entries\">\n" + //
				"      Spring\n" + //
				"    </a>\n" + //
				"  </p>\n" + //
				"  <p>\n" + //
				"    \n" + //
				"            \uD83D\uDDD3 \n" + //
				"    <span class=\"visible-inline-on-wide\">\n" + //
				"      Updated at \n" + //
				"    </span>\n" + //
				"    9998-03-27T02:22:36+09:00 by\n" + //
				"            Toshiaki Maki \n" + //
				"            \n" + //
				"    <span class=\"visible-inline-on-wide\">\n" + //
				"      \n" + //
				"            \uD83D\uDDD3 Created at 9998-12-20T02:32:23+09:00 by Toshiaki Maki \n"
				+ //
				"            {✒️️ \n" + //
				"      <a href=\"https://github.com/making/blog.ik.am/edit/master/content/00100.md\">\n"
				+ //
				"        Edit\n" + //
				"      </a>\n" + //
				"       \n" + //
				"            ⏰ \n" + //
				"      <a href=\"https://github.com/making/blog.ik.am/commits/master/content/00100.md\">\n"
				+ //
				"        History\n" + //
				"      </a>\n" + //
				"      }\n" + //
				"            \n" + //
				"    </span>\n" + //
				"  </p>\n" + //
				"  <hr/>\n" + //
				"  <div>\n" + //
				"    <p>\n" + //
				"    </p>\n" + //
				"    <p>\n" + //
				"      memo\n" + //
				"    </p>\n" + //
				"    <ul>\n" + //
				"      <li>\n" + //
				"        hoge\n" + //
				"      </li>\n" + //
				"      <li>\n" + //
				"        foo\n" + //
				"      </li>\n" + //
				"      <li>\n" + //
				"        bar\n" + //
				"      </li>\n" + //
				"    </ul>\n" + //
				"    <p>\n" + //
				"    </p>\n" + //
				"  </div>\n" + //
				"</article>");
	}

	@Test
	public void categories() throws Exception {
		this.server.enqueue(new MockResponse()
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody(
						"[[\"a\", \"b\", \"c\"],[\"hoge\", \"foo\"],[\"hoge\", \"foo\", \"bar\"]]"));
		HtmlPage top = this.webClient.getPage("http://localhost:" + port + "/categories");
		String xml = top.getBody().querySelector("ul").asXml();
		assertThat(normalize(xml)).isEqualTo("<ul class=\"categories\">\n" + //
				"  <li>\n" + //
				"    \uD83D\uDDC3 {\n" + //
				"    <a href=\"/categories/a/entries\">\n" + //
				"      a\n" + //
				"    </a>\n" + //
				"    /\n" + //
				"    <a href=\"/categories/a,b/entries\">\n" + //
				"      b\n" + //
				"    </a>\n" + //
				"    /\n" + //
				"    <a href=\"/categories/a,b,c/entries\">\n" + //
				"      c\n" + //
				"    </a>\n" + //
				"    }\n" + //
				"  </li>\n" + //
				"  <li>\n" + //
				"    \uD83D\uDDC3 {\n" + //
				"    <a href=\"/categories/hoge/entries\">\n" + //
				"      hoge\n" + //
				"    </a>\n" + //
				"    /\n" + //
				"    <a href=\"/categories/hoge,foo/entries\">\n" + //
				"      foo\n" + //
				"    </a>\n" + //
				"    }\n" + //
				"  </li>\n" + //
				"  <li>\n" + //
				"    \uD83D\uDDC3 {\n" + //
				"    <a href=\"/categories/hoge/entries\">\n" + //
				"      hoge\n" + //
				"    </a>\n" + //
				"    /\n" + //
				"    <a href=\"/categories/hoge,foo/entries\">\n" + //
				"      foo\n" + //
				"    </a>\n" + //
				"    /\n" + //
				"    <a href=\"/categories/hoge,foo,bar/entries\">\n" + //
				"      bar\n" + //
				"    </a>\n" + //
				"    }\n" + //
				"  </li>\n" + //
				"</ul>");
	}

	@Test
	public void tags() throws Exception {
		this.server.enqueue(new MockResponse()
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setBody("[\"hoge\", \"foo\", \"bar\"]"));
		HtmlPage top = this.webClient.getPage("http://localhost:" + port + "/tags");
		String xml = top.getBody().querySelector("ul").asXml();
		assertThat(normalize(xml)).isEqualTo("<ul class=\"tags\">\n" + //
				"  <li>\n" + //
				"    \uD83C\uDFF7 \n" + //
				"    <a href=\"/tags/hoge/entries\">\n" + //
				"      hoge\n" + //
				"    </a>\n" + //
				"  </li>\n" + //
				"  <li>\n" + //
				"    \uD83C\uDFF7 \n" + //
				"    <a href=\"/tags/foo/entries\">\n" + //
				"      foo\n" + //
				"    </a>\n" + //
				"  </li>\n" + //
				"  <li>\n" + //
				"    \uD83C\uDFF7 \n" + //
				"    <a href=\"/tags/bar/entries\">\n" + //
				"      bar\n" + //
				"    </a>\n" + //
				"  </li>\n" + //
				"</ul>");
	}

	static String normalize(String text) {
		if (text == null) {
			return text;
		}
		return text.trim().replace("\r", "");
	}
}
