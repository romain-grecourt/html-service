package com.acme;

import java.util.concurrent.Flow;

import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import static com.acme.DataChunks.release;

public class HTMLService implements Service {

    private static final String PAGE_START = """
            <html>
             <head>
              <style type="text/css">
                body {
                  counter-reset: log;
                  padding: 0;
                  text-decoration: none;
                  font: 13px "Source Code Pro", Menlo, Monaco, Consolas, "Courier New", monospace;
                  display: block;
                  position: relative;
                  color: #E0E0E0;
                  background-color: #424242;
                }
                body > div.line {
                  position: relative;
                  display: block;
                }
                body > div.line > pre {
                  margin: 0;
                  display: inline-block;
                  color: #eee;
                  line-height: 20px;
                  word-break: break-all;
                  white-space: pre-wrap;
                }  </style>
             <body>
              <div class="line">
               <pre>
            """;

    private static final String PAGE_END = "</pre>\n  </div>\n </body>\n</html>\n";

    @Override
    public void update(Routing.Rules rules) {
        rules.any(this::filter)
             .post(this::post);
    }

    private void filter(ServerRequest req, ServerResponse res) {
        res.headers()
           // because we "pipe" the request payload
           .add(Http.Header.CONNECTION, "close")
           .contentType(MediaType.TEXT_HTML);
        req.content().registerFilter(HTMLService::toHTML);
        req.next();
    }

    private void post(ServerRequest req, ServerResponse res) {
        res.send(req.content());
    }

    private static Flow.Publisher<DataChunk> toHTML(Flow.Publisher<DataChunk> publisher) {
        UTF8Decoder utf8Decoder = new UTF8Decoder();
        // Using MultiPrefetchPublisher to pre-fetch the first item
        Multi<String> html = new MultiPrefetchPublisher<>(publisher)
                .flatMap(Multi::just, 1, true, 1) // throttle one-by-one
                .map(release(utf8Decoder::decode)) // decode whole utf-8 characters
                .map(HTMLEntities::encode); // encode HTML escape entities

        // Make a proper HTML page
        // Note that we "pipe" the request payload into the response
        // Because we concatenate as the 2nd item, it is subscribed lazily.
        // The "auto-drain" detects that the request content has not been consumed and subscribes in order to "drain".
        // It triggers an "Already subscribed" error.
        // We use MultiPrefetchPublisher above to start subscription eagerly.
        Multi<String> page = Multi.concat(Single.just(PAGE_START), html, Single.just(PAGE_END));

        // map Multi<String> -> Multi<DataChunk>
        return page.map(DataChunks::create);
    }
}
