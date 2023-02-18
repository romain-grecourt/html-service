package com.acme;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.common.reactive.BufferedEmittingPublisher;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.SubscriptionHelper;

/**
 * Pre-fetch the first item.
 *
 * @param <T> the item type
 */
class MultiPrefetchPublisher<T> implements Multi<T> {

    private final AtomicBoolean requested = new AtomicBoolean(true);
    private final BufferedEmittingPublisher<T> emitter = BufferedEmittingPublisher.create();
    private final Flow.Publisher<T> source;
    private Subscriber<? super T> downstream;
    private Subscription upstream;

    MultiPrefetchPublisher(Flow.Publisher<T> source) {
        this.source = source;
        this.source.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                upstream = subscription;
                upstream.request(1);
                emitter.onRequest((n, t) -> {
                    if (!(requested.compareAndSet(true, false) && --n == 0)) {
                        upstream.request(n);
                    }
                });
            }

            @Override
            public void onNext(T item) {
                emitter.emit(item);
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.fail(throwable);
            }

            @Override
            public void onComplete() {
                emitter.complete();
            }
        });
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        if (this.downstream != null) {
            subscriber.onSubscribe(SubscriptionHelper.CANCELED);
            subscriber.onError(new IllegalStateException("Only one Subscriber allowed"));
            return;
        }
        downstream = subscriber;
        emitter.subscribe(downstream);
    }
}
