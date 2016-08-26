package com.seastar.task.push;

import org.apache.http.HttpResponse;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * Created by wjl on 2016/8/25.
 */
public class PushResponseConsumer extends AsyncCharConsumer<String> {
    private StringBuilder builder = new StringBuilder();

    @Override
    protected void onResponseReceived(final HttpResponse response) {
    }

    @Override
    protected void onCharReceived(final CharBuffer buf, final IOControl ioctrl) throws IOException {
        if (buf.hasArray())
            builder.append(buf.array());
    }

    @Override
    protected void releaseResources() {
    }

    @Override
    protected String buildResult(final HttpContext context) {
        return builder.toString();
    }
}
