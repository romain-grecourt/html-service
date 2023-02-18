package com.acme;

import io.helidon.common.reactive.Multi;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import static com.acme.DataChunks.release;

public class TestService implements Service {

    @Override
    public void update(Routing.Rules rules) {
        rules.any(this::filter)
             .post(this::post);
    }

    private void filter(ServerRequest req, ServerResponse res) {
        req.content().registerFilter(publisher -> {
            UTF8Decoder utf8Decoder = new UTF8Decoder();
            return req.content()
                      .flatMap(Multi::just, 1, true, 1) // throttle one-by-one
                      .map(release(utf8Decoder::decode)) // decode whole utf-8 characters
                      .map(s -> s.replace('a', 'z')) // replacing only one character for simplicity
                      .map(DataChunks::create);
        });
    }

    private void post(ServerRequest req, ServerResponse res) {
        // a normal handler, un-aware of the transformation performed by the filter
        req.content().as(String.class)
           .forSingle(s -> {
               System.out.println(s);
               res.send();
           });
    }
}
